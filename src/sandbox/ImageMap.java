package sandbox;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.beust.jcommander.Parameter;

import sandbox.io.IOUtils;
/**
 * 
 * conversion of image to indexed color-map and save modified image as well as an histogram of each color.
 * 
 * @author lindenb
 *
 */
public class ImageMap extends Launcher {
	private static final Logger LOG = Logger.builder(ImageMap.class).build();

	private static final int OPAQUE = 255; 
	@Parameter(names={"-p","--palette"},
			description="Palette definition. A list of color used for the indexed colors."
					+ "Predefined: gray<integer>, rainbow, cool,warm,thermal. "
					+ "If ends with '.txt', interpreted as a file with colors. One color per line."
					+ "Else RGB components separated by semicolons"
					)
	private String paletteName="gray10";
	
	@Parameter(names={"-o","--out"},description="output image file")
	private File outputImageFile = null;
	@Parameter(names={"-t","--table"},description="output table")
	private File outputTableFile = null;
	@Parameter(names={"-ho","--horizontal"},description="horizontal table layout")
	private boolean horizontal_table_layout = false;

	private final ColorParser colorParser = ColorParser.getInstance();
	
	private List<Color> getPalette() {
		final Set<Color> colorset=new HashSet<>();
		if(paletteName.endsWith(".txt"))
			{
			BufferedReader r=null;
			try {
				r= IOUtils.openBufferedReader(this.paletteName);
				colorset.addAll(
					r.lines().
					filter(L->!(L.startsWith("#") || L.trim().isEmpty())).
					map(colorParser).
					filter(C->C!=null).
					collect(Collectors.toSet())
					);
				r.close();
				}
			catch(final Exception err) {
				LOG.error(err);
				throw new RuntimeException(err);
				}
			}
		if(paletteName.toLowerCase().startsWith("gray"))
			{
			int n = Integer.parseInt(paletteName.substring(4).trim());
			if(n<1) n =1;
			LOG.info("palette gray"+n);
			float g=0f;
			for(int i=0;i<=n;i++)
				{
				colorset.add(new Color(g, g, g,1f));
				g+=1f/(n+1);
				}
			}
		else if( paletteName.equalsIgnoreCase("rainbow"))
			{
			colorset.addAll(Arrays.asList(Color.BLACK,Color.blue, Color.cyan, Color.green, Color.yellow, Color.orange, Color.red,Color.WHITE));
			}
		else if( paletteName.equalsIgnoreCase("cool"))
			{
			colorset.addAll(Arrays.asList(Color.BLACK,Color.green, Color.blue, new Color(255, 0, 255),Color.WHITE));
			}
		else if( paletteName.equalsIgnoreCase("warm"))
			{
			colorset.addAll(Arrays.asList(Color.BLACK,Color.red, Color.orange, Color.yellow,Color.WHITE));
			}
		else if( paletteName.equalsIgnoreCase("thermal"))
			{
			colorset.addAll(Arrays.asList(
					Color.black, Color.red, Color.orange, Color.yellow, Color.green,
					Color.blue, new Color(255, 0, 255), Color.white
					));
			}
		else
			{
			final String tokens[]=paletteName.split("[;]");
			for(final String s:tokens) {
				if(s.trim().isEmpty()) continue;
				final Color c= this.colorParser.apply(s);
				if(c==null) continue;
				colorset.add(c);
				}
			}
		final List<Color> array=new ArrayList<>(colorset);
		if(array.size()<=1) throw new IllegalArgumentException("no enough colors defined");
		if(array.size()>255) throw new IllegalArgumentException("too many indexed colors");
		return array;
	}

	private IndexColorModel createColorModel(final List<Color>  colors) 
	{ 
		

		final byte[] lRed = new byte[colors.size()]; 
		final byte[] lGreen = new byte[colors.size()]; 
		final byte[] lBlue = new byte[colors.size()]; 
		final byte[] lAlpha = new byte[colors.size()]; 

		for (int lColorNo = 0; lColorNo < colors.size(); lColorNo++) 
		{ 
			final Color lColor = colors.get(lColorNo); 
			lRed[lColorNo] = (byte) lColor.getRed(); 
			lGreen[lColorNo] = (byte) lColor.getGreen(); 
			lBlue[lColorNo] = (byte) lColor.getBlue(); 
			lAlpha[lColorNo] = (byte) OPAQUE; 
		} 

		final IndexColorModel lModel = new IndexColorModel(
				8,
				colors.size(),
				lRed,lGreen,lBlue,lAlpha
				); 
		return lModel; 
	} 	

	@Override
	public int doWork(final List<String> args) {
		PrintWriter pw = null;
		try
			{
			if(args.isEmpty()) {
				LOG.error("path missing");
				return -1;
				}
			if(this.outputImageFile!=null && args.size()!=1) {
				LOG.error("cannot save image if args != 1");
				return -1;
				}
			final List<Color>  colors = getPalette();

			 if(this.outputTableFile!=null)
			 	{
				pw = new PrintWriter(this.outputTableFile);
				if(this.horizontal_table_layout)
					{
					pw.print("#");
					for(final Color C:colors)
						{
						pw.print("C");
						pw.print(String.format("0%3d",C.getRed()));
						pw.print("_");
						pw.print(String.format("0%3d",C.getGreen()));
						pw.print("_");
						pw.print(String.format("0%3d",C.getBlue()));
						pw.print("\t");
						}
					pw.println("FILE");
					}
				else
					{
					pw.println("#R\tG\tB\tcount\tfraction\tfile");
					}
			 	}
			

			for(final String imgPath: args) {
				final File imgIn=new File(imgPath);
				final Map<Color, Long> colorUsage = new HashMap<>(colors.size());
				for(final Color c:colors) {
					colorUsage.put(c, 0L);
					}
				
				
				final BufferedImage img = ImageIO.read(imgIn);
				if(img.getWidth()==0 || img.getHeight()==0) {
					LOG.error("empty image " + imgPath);
					return -1;
					}
				final IndexColorModel indexColorModel = createColorModel(colors) ;
	
	
				final BufferedImage lImage=new BufferedImage(
						img.getWidth(),
						img.getHeight(),
						BufferedImage.TYPE_BYTE_INDEXED,
						indexColorModel
						);
				
				final Graphics2D g=lImage.createGraphics();
				g.drawImage(img, 0, 0,null);
				g.dispose();
				
				
				 for (int x = 0; x < lImage.getWidth(); x++) {
			            for (int y = 0; y < lImage.getHeight(); y++) {
			            	final Color c = new Color(lImage.getRGB(x, y));
			            	if(!colorUsage.containsKey(c)) continue;
			            	colorUsage.put(c, 1L + colorUsage.getOrDefault(c, 0L));
			            }
			        }
				 
				 if(this.outputTableFile!=null) {
					final double total = colorUsage.values().stream().mapToLong(L->L.longValue()).sum();
					
					if(this.horizontal_table_layout)
						{
						for(final Color col: colors)
						 	{
							pw.print((total==0.0?0.0:colorUsage.get(col)/total));
							pw.print("\t");
						 	}
						pw.println(imgPath);
						}
					else
						{
						
						 for(final Color col: colorUsage.keySet())
						 	{
							 pw.print(
									 col.getRed()+"\t"+ 
									 col.getGreen()+"\t"+ 
									 col.getBlue()+"\t"+ 
									 colorUsage.get(col)+"\t"+ 
									 (total==0.0?0.0:colorUsage.get(col)/total)
									 );
							 pw.println("\t"+imgPath);
						 	}
						}
					 pw.flush();
					 pw.close();
				 	}
				 
				
				if(this.outputImageFile!=null) {
					LOG.info("saving to "+outputImageFile);
					if(outputImageFile.equals(imgIn)) {
						LOG.error("Cannot overwrite input image");
						return -1;
						}
					if(!outputImageFile.getName().toLowerCase().endsWith(".png")) {
						LOG.error("output image must end with '.png'");
						return -1;
						}
					ImageIO.write(lImage,"PNG",outputImageFile);
					}
				}
			return 0;
		}
		catch(final Exception err) {
			LOG.error(err);
			return -1;
		}
	}
	public static void main(final String[] args) {
		new ImageMap().instanceMain(args);
	}
}
