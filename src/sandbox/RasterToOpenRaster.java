/*
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	May-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Compilation:
 *        ant raster2ora
 * Usage:
 *        java -jar raster2ora.jar -h
 */
package sandbox;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * RasterToOpenRaster
 *
 */
public class RasterToOpenRaster
	{
	private String format="JPG";
	private BufferedImage srcImage;
	private double ratio=1.0;
    private File fileout=null;

    private void run() throws XMLStreamException,IOException
    	{
    	BufferedImage image=srcImage;
    	if(ratio>0 && ratio!=1)
    		{
    		AffineTransform tx = AffineTransform.getScaleInstance(ratio, ratio);
    		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
    		image = op.filter(image, null);
    		}
    	FileOutputStream fout=new FileOutputStream(fileout);
    	ZipOutputStream zout=new ZipOutputStream(fout);

    	//mime
    	ZipEntry entry=new ZipEntry("mimetype");
    	zout.putNextEntry(entry);
    	zout.write(new String("image/openraster\n").getBytes());
    	zout.closeEntry();

    	entry=new ZipEntry("data/layer001."+(format.equals("PNG")?"png":"jpg"));
    	zout.putNextEntry(entry);
    	ImageIO.write(image,format, zout);
    	zout.closeEntry();


    	//thumb
    	double zoom=1;
    	if(srcImage.getWidth()>srcImage.getHeight())
    		{
    		zoom= 64.0/srcImage.getWidth();
    		}
    	else
    		{
    		zoom= 64.0/srcImage.getHeight();
    		}
    	AffineTransform tx = AffineTransform.getScaleInstance(zoom,zoom);
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
		entry=new ZipEntry("Thumbnails/thumbnail.png");
		zout.putNextEntry(entry);
    	ImageIO.write(op.filter(srcImage, null),"PNG", zout);
    	zout.closeEntry();

    	entry=new ZipEntry("stack.xml");
    	zout.putNextEntry(entry);
    	XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
    	XMLStreamWriter w= xmlfactory.createXMLStreamWriter(zout,"UTF-8");
    	w.writeStartDocument("UTF-8","1.0");
    	w.writeStartElement("image");
    	w.writeAttribute("w",String.valueOf( image.getWidth()));
    	w.writeAttribute("h",String.valueOf( image.getHeight()));
    	w.writeStartElement("stack");

	    	w.writeEmptyElement("layer");
	    	w.writeAttribute("src","data/layer001."+(format.equals("PNG")?"png":"jpg"));
	    	w.writeAttribute("x","0");
	    	w.writeAttribute("y","0");
	    	w.writeAttribute("opacity","0.4");
	    	w.writeAttribute("visibility","visible");


    	w.writeEndElement();//stack
    	w.writeEndElement();//image
    	w.writeEndDocument();
    	w.flush();
    	zout.closeEntry();



    	zout.finish();
    	zout.flush();
    	fout.flush();
    	fout.close();



    	}

	public static void main(String[] args)
		{
		RasterToOpenRaster app=new RasterToOpenRaster();
		try {
			int optind=0;
			while(optind<args.length)
				{
				if(args[optind].equals("-h"))
					{
					System.out.println("Pierre Lindenbaum PhD. 2011");
					System.out.println("-x (float) image ratio (1.0)");
					System.out.println("-o <fileout.ora> filename out. (required)");
					return;
					}
				else if(args[optind].equals("-o"))
					{
					app.fileout=new File(args[++optind]);
					}
				else if(args[optind].equals("-x"))
					{
					app.ratio=Double.parseDouble(args[++optind]);
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

			if(app.fileout==null)
				{
				System.err.println("output file missing");
				return;
				}

			if(!app.fileout.getName().endsWith(".ora"))
				{
				System.err.println("output file should end with .ora ("+app.fileout+")");
				return;
				}

			if(optind+1!=args.length)
				{
				System.err.println("Illegal number of arguments");
				return;
				}



			String filename=args[optind];
			/* MyPaint only want PNG
			  if(filename.toLowerCase().endsWith(".png")) */
				{
				app.format="PNG";
				}
			if( filename.startsWith("http://") ||
				filename.startsWith("https://") ||
				filename.startsWith("ftp://"))
				{
				app.srcImage=ImageIO.read(new URL(filename));
				}
			else
				{
				app.srcImage=ImageIO.read(new File(filename));
				}

			app.run();
			}
		catch (Exception e)
			{
			e.printStackTrace();
			}
		}
	}
