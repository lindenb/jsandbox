/*
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	May-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Reference
 * 	http://supercomputingblog.com/graphics/oil-painting-algorithm/
 * Compilation:
 *        ant oilpainting
 * Usage:
 *        java -jar oilpainting.jar
 */
package sandbox;


import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import javax.imageio.ImageIO;


/**
 * OilPainting
 *
 */
public class OilPainting
	{
	private int radius=10;
	private int intensityLevels=24;


	private BufferedImage run(BufferedImage sourceImage)
		{
		BufferedImage dest=new BufferedImage(
			sourceImage.getWidth(),
			sourceImage.getHeight(),
			sourceImage.getType()
			);

		int averageR[] = new int[intensityLevels];
		int averageG[]=new int[intensityLevels];
		int averageB[]=new int[intensityLevels];
		int intensityCount[]=new int[intensityLevels];

		for(int x=0;x< sourceImage.getWidth();++x)
			{
			int left = Math.max(0,x-radius);
			int right = Math.min(x+radius,dest.getWidth()-1);
			for(int y=0;y< sourceImage.getHeight();++y)
				{

				int top = Math.max(0,y-radius);
				int bottom = Math.min(y+radius,dest.getHeight()-1);

				Arrays.fill(averageR,0);
				Arrays.fill(averageG,0);
				Arrays.fill(averageB,0);
				Arrays.fill(intensityCount,0);
				int maxIndex=-1;

				for(int j=top;j<=bottom;++j)
					{
					for(int i=left;i<=right;++i)
						{
						if(!inRange(x,y,i, j)) continue;

						int rgb = sourceImage.getRGB(i,j);

						int red = (rgb >> 16)&0xFF;
						int green = (rgb >>8)&0xFF;
						int blue = (rgb )&0xFF;
						int intensityIndex = (int)((((red+green+blue)/3.0)/256.0)*intensityLevels);

						intensityCount[intensityIndex]++;
						averageR[intensityIndex] += red;
						averageG[intensityIndex] += green;
						averageB[intensityIndex] += blue;

						if( maxIndex==-1 ||
							intensityCount[maxIndex]< intensityCount[intensityIndex]
							)
							{
							maxIndex = intensityIndex;
							}
						}
					}

				int curMax = intensityCount[maxIndex];
				int r = averageR[maxIndex] / curMax;
				int g = averageG[maxIndex] / curMax;
				int b = averageB[maxIndex] / curMax;

				int rgb=((r << 16) | ((g << 8) | b));
				dest.setRGB(x,y,rgb);
				}
			}


		return dest;
		}

	private boolean inRange(int cx,int cy,int i,int j)
		{
		double d;
		//d= java.awt.geom.Line2D.ptLineDist(cx, cy, cx-radius, cy-radius, i, j);
		d=Point2D.distance(i, j,cx,cy);
		return d<radius;
		}

	public static void main(String[] args)
		{
		File outFile=null;
		OilPainting app=new OilPainting();
		try {
			int optind=0;
			while(optind<args.length)
				{
				if(args[optind].equals("-h"))
					{
					System.out.println("Pierre Lindenbaum PhD 2011");
					System.out.println(" -o <fileout> ");
					System.out.println(" -r  <radius> = "+app.radius);
					System.out.println(" -L  <intensity-level> = "+app.intensityLevels);
					return;
					}
				else if(args[optind].equals("-o"))
					{
					outFile= new File(args[++optind]);
					}
				else if(args[optind].equals("-r"))
					{
					app.radius = Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-L"))
					{
					app.intensityLevels = Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("--"))
					{
					optind++;
					break;
					}
				else if(args[optind].startsWith("-"))
					{
					System.err.println("Unnown option: "+args[optind]);
					return;
					}
				else
					{
					break;
					}
				++optind;
				}
			if(optind+1!=args.length)
				{
				System.err.println("Illegal number of arguments");
				return;
				}
			if(outFile==null)
				{
				System.err.println("File out missing");
				return;
				}

			String filename=args[optind];
			BufferedImage sourceImage=null;
			if( filename.startsWith("http://") ||
				filename.startsWith("https://") ||
				filename.startsWith("ftp://"))
				{
				sourceImage=ImageIO.read(new URL(filename));
				}
			else
				{
				sourceImage=ImageIO.read(new File(filename));
				}


			String format="jpg";
			String fname=outFile.getName().toLowerCase();
			if(fname.endsWith(".jpg") || fname.endsWith(".jpeg"))
				{
				format="jpg";
				}
			else if(fname.endsWith(".png"))
				{
				format="png";
				}
			else
				{
				System.err.println("Bad image format: "+fname);
				return;
				}
			BufferedImage img = app.run(sourceImage);
			ImageIO.write(img, format, outFile);
			}
		catch (Exception e)
			{
			e.printStackTrace();
			}
		}
}
