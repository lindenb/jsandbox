package sandbox;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class MosaicOfPictures extends AbstractApplication
	{
	private static final Logger LOG=Logger.getLogger("lindenb");
	
	
	private MosaicOfPictures()
		{
		}
	
	private Set<String> readPictures(BufferedReader r)
		throws IOException
		{
		 Set<String> set=new LinkedHashSet<String>();
		String line;
		while((line=r.readLine())!=null)
			{
			if(line.isEmpty() || line.startsWith("#")) continue;
			int tab=line.indexOf('\t');
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
	
	@Override
	protected void fillOptions(Options options) {
		options.addOption(Option.builder("c").longOpt("width").desc("final image width").hasArg(true).build());
		options.addOption(Option.builder("o").longOpt("out").desc("image output").hasArg(true).build());		
		super.fillOptions(options);
		}
	
	@Override
	protected int execute(CommandLine cmd)
		{
			try {
			int image_size=400;
			Set<String>  picts=null;
			File outFile=null;
			
			if(cmd.hasOption('c'))
				{
				image_size= Integer.parseInt(cmd.getOptionValue('c'));
				}
			if(cmd.hasOption('o'))
				{
				outFile= new File(cmd.getOptionValue('o'));
				}
			else
				{
				LOG.severe("No output file");
				return -1;
				}
			
			final List<String> args = cmd.getArgList();
			
			if(args.isEmpty())
				{
				picts=readPictures(new BufferedReader(new InputStreamReader(System.in)));
				}
			else if(args.size()==1)
				{
				BufferedReader r=new BufferedReader(new FileReader(args.get(0)));
				picts=readPictures(r);
				r.close();
				}
			else
				{
				LOG.severe("Illegal number of arguments.");
				return -1;
				}
			
			
			if(picts.isEmpty())
				{
				LOG.severe("No images");
				return -1;
				}
			int per_side=(int)Math.ceil(Math.sqrt(picts.size()));
			double one_length =image_size/Math.ceil(Math.sqrt(picts.size()));
			
			
			BufferedImage img=new BufferedImage(
					image_size,
					image_size,
					BufferedImage.TYPE_INT_RGB
					);
			Graphics2D g=img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	
			int y=0;
			int x=0;
			int nReads=0;
			for(String file:picts)
				{
				++nReads;
				LOG.info(file+" "+nReads+"/"+picts.size());
				BufferedImage img2 = null;
				if( file.startsWith("http://") ||
					file.startsWith("https://") ||
					file.startsWith("ftp://"))
					{
					img2=ImageIO.read(new URL(file));
					}
				else
					{
					img2=ImageIO.read(new File(file));
					}
				if(img2==null)
					{
					LOG.severe("Cannot read "+file);
					return -1;
					}
				double w=img2.getWidth();
				double h=img2.getHeight();
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
			ImageIO.write(img, "JPG", outFile);
			return 0;
			}
		catch(Exception err)
			{
			err.printStackTrace();
			LOG.severe(err.getMessage());
			return -1;
			}
		}
	public static void main(String[] args) throws Exception
		{
		new MosaicOfPictures().instanceMainWithExit(args);
		}
	}
