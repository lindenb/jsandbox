package sandbox;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import com.beust.jcommander.Parameter;

public class MosaicOfPictures extends Launcher
	{
	private static final Logger LOG=Logger.builder(MosaicOfPictures.class).build();
	
	
	private MosaicOfPictures()
		{
		}
	
	private Set<String> readPictures(BufferedReader r)
		throws IOException
		{
		final Set<String> set=new LinkedHashSet<String>();
		String line;
		while((line=r.readLine())!=null)
			{
			if(StringUtils.isBlank(line)|| line.startsWith("#")) continue;
			final int tab=line.indexOf('\t');
			if(tab==-1)
				{
				set.add(line.trim());
				}
			else
				{
				set.add(line.substring(0, tab));
				}
			}
		return set;
		}
	
	@Parameter(names={"-w","-width","--width"},description="final image width")
	private int image_size=400;
	@Parameter(names={"-o","--out"},description="image output")
	private File outFile=null;
	@Parameter(names={"-x","--extend"},description="square INSIDE picture")
	private boolean squareinside=false;
	@Parameter(names={"-b","--background"},description="background color")
	private String backgroundStr="black";

	@Override
	public int doWork(final java.util.List<String> args) {
		
		final ImageUtils imageUtils = ImageUtils.getInstance();
		try
			{
			final Set<String>  picts;	
			final Color background= ColorParser.getInstance().apply(this.backgroundStr);
			if(background==null) {
				LOG.error("Bad background color : "+this.backgroundStr);
				return -1;
				}
			if(this.outFile==null)
				{
				LOG.error("No output file");
				return -1;
				}			
			if(args.isEmpty())
				{
				picts=readPictures(new BufferedReader(new InputStreamReader(System.in)));
				}
			else if(args.size()==1)
				{
				try(final BufferedReader r= Files.newBufferedReader(Paths.get(args.get(0)))) {
					picts=readPictures(r);
					}
				}
			else
				{
				LOG.error("Illegal number of arguments.");
				return -1;
				}
			
			LOG.info("Number of images: "+picts.size());
			if(picts.isEmpty())
				{
				LOG.error("No images");
				return -1;
				}
			final int per_side=(int)Math.ceil(Math.sqrt(picts.size()));
			final double one_length =image_size/Math.ceil(Math.sqrt(picts.size()));
			
			
			BufferedImage img=new BufferedImage(
					image_size,
					image_size,
					BufferedImage.TYPE_INT_RGB
					);
			final Graphics2D g=img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setColor(background);
			g.fillRect(0,0,image_size,image_size);
			
			
			int y=0;
			int x=0;
			int nReads=0;
			for(final String file:picts)
				{
				++nReads;
				LOG.info(file+" "+nReads+"/"+picts.size());
				BufferedImage img2 = imageUtils.read(file);
				if(img2==null)
					{
					LOG.error("Cannot read "+file);
					return -1;
					}
				double w=img2.getWidth();
				double h=img2.getHeight();
				
				
			    if(squareinside) {
				  if(w>h ) {
				        img2 = img2.getSubimage((int)((w-h)/2.0),0,(int)h,(int)h);
				        w=h;
				        }
				   else //h>w
				        {
				        img2 = img2.getSubimage(0,(int)((h-w)/2.0),(int)w,(int)w);
				        h=w;
				        }
				    }
				double ratio=w/h;

				    while(w>one_length || h> one_length)
					    {
					    w*=0.99999;
					    h=w/ratio;
					    }
				    g.drawImage(
						    img2,
						    (int)(x*one_length+(one_length-(int)w)/2.0),
						    (int)(y*one_length+(one_length-(int)h)/2.0),
						    (int)w,
						    (int)h,
						    null);
				img2=null;
				
				x++;
				if(x>=per_side)
					{
					x=0;
					++y;
					}
				}
			g.dispose();
			LOG.info("Saving");
			ImageIO.write(img, outFile.getName().toLowerCase().endsWith(".png")?"PNG":"JPG", outFile);
			return 0;
			}
		catch(final Throwable err)
			{
			LOG.error(err.getMessage());
			return -1;
			}
		}
	public static void main(final String[] args) throws Exception
		{
		new MosaicOfPictures().instanceMainWithExit(args);
		}
	}
