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
import java.util.Set;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

public class MosaicOfPictures
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
	
	private void run(String[] args) throws Exception
		{
		int image_size=400;
		Set<String>  picts=null;
		File outFile=null;
		int optind=0;
		while(optind< args.length)
			{
			if(args[optind].equals("-h") ||
			   args[optind].equals("-help") ||
			   args[optind].equals("--help"))
				{
				System.err.println("Options:");
				System.err.println(" -h help; This screen.");
				System.err.println(" -c (width) final image size");
				System.err.println(" -o (file) fileout.jpg");
				return;
				}
			else if(args[optind].equals("-c"))
				{
				image_size=Integer.parseInt(args[++optind]);
				}
			else if(args[optind].equals("-o"))
				{
				outFile=new File(args[++optind]);
				}
			else if(args[optind].equals("--"))
				{
				optind++;
				break;
				}
			else if(args[optind].startsWith("-"))
				{
				System.err.println("Unknown option "+args[optind]);
				return;
				}
			else 
				{
				break;
				}
			++optind;
			}
		
		if(outFile==null)
			{
			System.err.println("No output file");
			System.exit(-1);
			}
		
		if(optind==args.length)
			{
			picts=readPictures(new BufferedReader(new InputStreamReader(System.in)));
			}
		else if(optind+1==args.length)
			{
			BufferedReader r=new BufferedReader(new FileReader(args[optind]));
			picts=readPictures(r);
			r.close();
			}
		else
			{
			System.err.println("Illegal number of arguments.");
			System.exit(-1);
			}
		
		
		if(picts.isEmpty())
			{
			System.err.println("No images");
			System.exit(-1);
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
			BufferedImage img2=null;
			if(file.startsWith("http://") || file.startsWith("https://") || file.startsWith("ftp://"))
				{
				img2=ImageIO.read(new URL(file));
				}
			else
				{
				img2=ImageIO.read(new File(file));
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
		}
	public static void main(String[] args) throws Exception
		{
		new MosaicOfPictures().run(args);
		}
	}
