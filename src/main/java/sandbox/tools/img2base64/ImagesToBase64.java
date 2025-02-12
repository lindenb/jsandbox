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
package sandbox.tools.img2base64;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.tools.central.ProgramDescriptor;


/**
 * 
 * ImagesToBase64
 *
 */
public class ImagesToBase64 extends Launcher
	{
	private static final Logger LOG=Logger.builder(ImagesToBase64.class).build();

	@Parameter(names={"-w","-width","--width"},description="final image width")
	private int width=-1;

	@Override
	public int doWork(final List<String> args) {
		try
			{
			final XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
			final XMLStreamWriter w= xmlfactory.createXMLStreamWriter(System.out,"UTF-8");
			w.writeStartDocument("UTF-8","1.0");
			w.writeStartElement("html");
			w.writeStartElement("body");
			w.writeCharacters("\n");
			w.writeStartElement("div");
			                      
            for(String filename:args)
                    {
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
                    	final File f=new File(filename);
                    	img=ImageIO.read(f);
                    	filename=f.getName();
                    	}
                  	if(width!=-1 && img.getWidth()>width)
                    	{
                    	double ratio=((double)width/(double)img.getWidth());
                    	final BufferedImage img2=new BufferedImage(
                    			width,
                    			(int)(img.getHeight()*ratio),
                    			BufferedImage.TYPE_INT_RGB
                    			);
                    	final Graphics2D g=(Graphics2D)img2.getGraphics();
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
                	final ByteArrayOutputStream baos=new ByteArrayOutputStream();
                	ImageIO.write(img, "PNG", baos);
                	baos.flush();
                	baos.close();
                	w.writeAttribute("src","data:image/png;base64,"+Base64.getEncoder().encodeToString(baos.toByteArray()));
                	
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
			return 0;
			} 
		catch(Throwable err)
			{
			LOG.error(err);
			return -1;
			}		
		}
	
	
	public static ProgramDescriptor getProgramDescriptor() {
		return new ProgramDescriptor() {
			@Override
			public String getName() {
				return "img2base64";
				}
			};
		}
	
	public static void main(final String[] args) {
		new ImagesToBase64().instanceMainWithExit(args);
	}
	}
