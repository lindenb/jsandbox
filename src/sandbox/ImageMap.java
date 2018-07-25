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
	
	private Color parseColor(final String s) {
		if(s.equalsIgnoreCase("BLACK")) return Color.BLACK;
		if(s.equalsIgnoreCase("BLUE")) return Color.BLUE;
		if(s.equalsIgnoreCase("CYAN")) return Color.CYAN;
		if(s.equalsIgnoreCase("GRAY")) return Color.GRAY;
		if(s.equalsIgnoreCase("GREEN")) return Color.GREEN;
		if(s.equalsIgnoreCase("MAGENTA")) return Color.MAGENTA;
		if(s.equalsIgnoreCase("ORANGE")) return Color.ORANGE;
		if(s.equalsIgnoreCase("PINK")) return Color.PINK;
		if(s.equalsIgnoreCase("RED")) return Color.RED;
		if(s.equalsIgnoreCase("WHITE")) return Color.WHITE;
		if(s.equalsIgnoreCase("YELLOW")) return Color.YELLOW;

		final String tokens[]=s.split("[,]");
		if(tokens.length!=3) throw new IllegalArgumentException("bad rgb color components:"+s);
		return new Color(
				Integer.parseInt(tokens[0]),
				Integer.parseInt(tokens[1]),
				Integer.parseInt(tokens[2])
				);
	}
	
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
					map(L->parseColor(L)).
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
			colorset.addAll(Arrays.asList(Color.blue, Color.cyan, Color.green, Color.yellow, Color.orange, Color.red));
			}
		else if( paletteName.equalsIgnoreCase("cool"))
			{
			colorset.addAll(Arrays.asList(Color.green, Color.blue, new Color(255, 0, 255)));
			}
		else if( paletteName.equalsIgnoreCase("warm"))
			{
			colorset.addAll(Arrays.asList(Color.red, Color.orange, Color.yellow));
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
				final Color c=parseColor(s);
				if(c==null) continue;
				colorset.add(c);
				}
			}
		final List<Color> array=new ArrayList<>(colorset);
		if(array.size()<=1) throw new IllegalArgumentException("no enough colors defined");
		if(array.size()>255) throw new IllegalArgumentException("too many indexed points");
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
		try
		{
			final String imgPath = oneFileOrNull(args);
			final File imgIn=new File(imgPath);
			final List<Color>  colors = getPalette();
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
				final PrintWriter pw = new PrintWriter(this.outputTableFile);
				pw.println("#R\tG\tB\tcount\tfraction");
				 for(final Color col: colorUsage.keySet())
				 	{
					 pw.println(
							 col.getRed()+"\t"+ 
							 col.getGreen()+"\t"+ 
							 col.getBlue()+"\t"+ 
							 colorUsage.get(col)+"\t"+ 
							 (total==0.0?0.0:colorUsage.get(col)/total)
							 );
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
