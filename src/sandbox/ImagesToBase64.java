/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	June-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * Reference:
 *
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Wiki
 *
 * Motivation:
 * 		Encodes some images to base64 embedded in html
 * Compilation:
 *       ant images2base64
 * Execution:
 * 		 java -jar dist/images2base64.jar -w 400 images*.png > file.html
 */
package sandbox;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;


/**
 *
 * ImagesToBase64
 *
 */
public class ImagesToBase64
	{
	private final static String BASE64 =
	"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";


	private static void encode(InputStream dataIn,StringBuilder out)
		throws IOException
		{

		char output[] = new char[4];
		int restbits = 0;
		 int chunks = 0;
		int c;
		int nFill=0;

		while((c=dataIn.read())!=-1)
			{
			int ic = ( c >= 0 ? c : (c & 0x7F) + 128);
			//array3[nFill]=(byte)ic;

		    switch (nFill)
		        {
		        case 0:
		        	{
		        	output[nFill] = BASE64.charAt(ic >>> 2);
		            restbits = ic & 0x03;
		            nFill++;
		            break;
		        	}
		       case 1:
		    	    {
		    		output[nFill] = BASE64.charAt((restbits << 4) | (ic >>> 4));
		    	    restbits = ic & 0x0F;
		    	    nFill++;
		            break;
		    	    }
		       case 2:
		    	   	{
		    	   	output[nFill  ] = BASE64.charAt((restbits << 2) | (ic >>> 6));
		    	   	output[nFill+1] = BASE64.charAt(ic & 0x3F);
		            out.append(new String(output));
		            // keep no more the 76 character per line
		            chunks++;
		            nFill=0;
		            break;
		    	   	}
		        }
			} // for

			/* final */
			switch (nFill)
			{    case 1:
		         	 output[1] = BASE64.charAt((restbits << 4));
		             output[2] = output[3] = '=';
		             out.append(new String(output));
		             break;
		         case 2:
		         	 output[2] = BASE64.charAt((restbits << 2));
		             output[3] = '=';
		             out.append(new String(output));
		             break;
			}

		}

	public static void main(String[] args)
		{
		try
			{
			int width=-1;
			int optind=0;
			while(optind< args.length)
				{
				if(args[optind].equals("-h") ||
				   args[optind].equals("-help") ||
				   args[optind].equals("--help"))
					{
					System.err.println("Pierre Lindenbaum PhD. 2011");
					System.err.println("Options:");
					System.err.println(" -h help; This screen.");
					System.err.println(" -w <width> (optional) force width.");
					return;
					}
				else if(args[optind].equals("-w"))
					{
					width=Integer.parseInt(args[++optind]);
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
			XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
			XMLStreamWriter w= xmlfactory.createXMLStreamWriter(System.out,"UTF-8");
			w.writeStartDocument("UTF-8","1.0");
			w.writeStartElement("html");
			w.writeStartElement("body");
			 w.writeCharacters("\n");
			w.writeStartElement("div");

            while(optind< args.length)
                    {


                    String filename=args[optind++];
                    BufferedImage img=null;
                    boolean is_url=false;
                    if(	filename.startsWith("http://") ||
                		filename.startsWith("https://") ||
                		filename.startsWith("ftp://"))
                    	{
                    	is_url=true;
                    	img=ImageIO.read(new URL(filename));
                    	}
                    else
                    	{
                    	File f=new File(filename);
                    	img=ImageIO.read(f);
                    	filename=f.getName();
                    	}
                  	if(width!=-1 && img.getWidth()>width)
                    	{
                    	double ratio=((double)width/(double)img.getWidth());
                    	BufferedImage img2=new BufferedImage(
                    			width,
                    			(int)(img.getHeight()*ratio),
                    			BufferedImage.TYPE_INT_RGB
                    			);
                    	Graphics2D g=(Graphics2D)img2.getGraphics();
                    	g.drawImage(img,0,0,img2.getWidth(),img2.getHeight(),null);
                    	g.dispose();
                    	img=img2;
                    	}
                  	w.writeEmptyElement("br");
                  	w.writeCharacters("\n");
                  	w.writeStartElement("div");
                  	w.writeAttribute("style","text-align:center;");
                  	if(is_url)
                  		{
                  		w.writeStartElement("a");
                  		w.writeAttribute("href","filename");
                  		w.writeAttribute("target","_blank");
                  		}
                  	w.writeEmptyElement("img");
                	w.writeAttribute("width",String.valueOf(img.getWidth()));
                	w.writeAttribute("height",String.valueOf(img.getHeight()));
                	if(!is_url)
                		{
                		w.writeAttribute("title",filename);
                		}
                	StringBuilder encoded=new StringBuilder("data:image/png;base64,");
                	ByteArrayOutputStream baos=new ByteArrayOutputStream();
                	ImageIO.write(img, "PNG", baos);
                	baos.flush();
                	baos.close();
                	ByteArrayInputStream bais=new ByteArrayInputStream(baos.toByteArray());
                	encode(bais,encoded);
                	bais.close();


                	w.writeAttribute("src",encoded.toString());


                  	if(is_url)
                  		{
                  		 w.writeEndElement();
                  		}
                    w.writeEndElement();
                    w.writeCharacters("\n");
                    }

			w.writeEndElement();//div
			 w.writeCharacters("\n");
			w.writeEndElement();//body
			w.writeEndElement();//html
			w.writeEndDocument();
			w.flush();
			w.close();
			}
		catch(Throwable err)
			{
			err.printStackTrace();
			}

		}
	}
