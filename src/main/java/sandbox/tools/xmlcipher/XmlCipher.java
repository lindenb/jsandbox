/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Reference:
 *   http://www.exampledepot.com/egs/javax.crypto/PassKey.html
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Motivation:
 * 		crypt XML
 * Compilation:
 *        cd jsandbox; ant xmlcipher
 * Usage:
 *        java -jar dist/xmlcipher.jar [options] (stdin|file|uri)
 */
package sandbox.tools.xmlcipher;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.cipher.CipherCodec;
import sandbox.cipher.CipherCodecFactory;
import sandbox.lang.StringUtils;

//
public class XmlCipher extends Launcher {
	@Parameter(names={"-o","--output"},description="output name")
	private Path fileout = null; 
	@Parameter(names={"-d","--decode"},description="decode")
	private boolean decode = false;
	@Parameter(names={"-e","--encode"},description="encode")
	private boolean encode = false;
	@Parameter(names={"-p","--password"},description="password")
	private String password = null;
	
	// 8-byte Salt
    private static final byte[] SALT =
		{
        (byte)0xA5, (byte)0x9B, (byte)0xC9, (byte)0x32,
        (byte)0x48, (byte)0x35, (byte)0xE2, (byte)0x03
    	};
   private CipherCodec cipherCodec;
   private  XmlCipher()
	   {
	 	}
   
   
  private String encrypt(String str) {
      	return cipherCodec.encode(str);
	    }

  private String decrypt(String str) {
	  return cipherCodec.decode(str);
  	}
  

	
	private String process(String s,boolean encode)
		{
		if(encode)
			{
			return encrypt(s);
			}
		else
			{
			return decrypt(s);
			}
		}
	
	private void process(XMLEventReader r, XMLEventWriter w,boolean encode) throws XMLStreamException
		{
		final XMLEventFactory evtF = XMLEventFactory.newFactory();
		while(r.hasNext()) {
			final XMLEvent evt=r.nextEvent();
			switch(evt.getEventType()) {
				case XMLEvent.CDATA:
					String s = evt.asCharacters().getData();
					if(!StringUtils.isBlank(s)) s=process(s,encode);
					w.add(evtF.createCData(s));
					break;
				case XMLEvent.CHARACTERS:
					s = evt.asCharacters().getData();
					if(!StringUtils.isBlank(s)) s=process(s,encode);
					w.add(evtF.createCharacters(s));
					break;
				case XMLEvent.START_ELEMENT:
					final StartElement E = evt.asStartElement();
					List<Attribute> atts = new ArrayList<>() ;
					for(Iterator<Attribute> it = E.getAttributes();it.hasNext();) {
						final Attribute att = it.next();
						s = att.getValue();
						if(!StringUtils.isBlank(s)) s=process(s,encode);
						atts.add(evtF.createAttribute(att.getName(),s));
						}
					final QName qName=E.getName();

					w.add(evtF.createStartElement(
							qName.getPrefix(),qName.getNamespaceURI(),qName.getLocalPart()
							,atts.iterator(),E.getNamespaces()
						));
					break;
				default:
					w.add(evt);
				}
			}
		}
	
	@Override
	public int doWork(List<String> args)
		{
		try
			{
			final char[] passPhrase;
			if(StringUtils.isBlank(password))
				{
				if(System.console()==null)
					{
					System.err.println("Undefined password.");
					return -1;
					}
				passPhrase =System.console().readPassword("[xmlcipher] Password ?");
				if(passPhrase==null)
					{
					System.err.println("Undefined password.");
					return -1;
					}
				}
			else
				{
				passPhrase = this.password.toCharArray();
				}
			
	      
	        

	        // Prepare the parameter to the ciphers
	        if(this.decode && this.encode) {
	        	System.err.println("decode and encode ???");
	        	return -1;
	        	}
	        else  if(!this.decode && !this.encode) {
	        	System.err.println("!decode and !encode ???");
	        	return -1;
	        	}

	        this.cipherCodec = new CipherCodecFactory().
	        		setPassPhrase(this.password).
	        		setSalt(SALT).
	        		make();

	        
		  final XMLOutputFactory xof = XMLOutputFactory.newInstance();
		  
try(Reader reader = super.openBufferedReader(super.oneFileOrNull(args))) { 
		final XMLInputFactory xif = XMLInputFactory.newInstance();
		xif.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
		xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
		final XMLEventReader r= xif.createXMLEventReader(reader);

	       try(Writer writer = super.openPathAsPrintWriter(this.fileout)) {
		       XMLEventWriter w= xof.createXMLEventWriter(writer);
		       if(encode)
		    	{
		    	this.process(r,w,true);
		    	}
		    else if(this.decode)
		    	{
		    	this.process(r,w,false);
		    	}
		       w.close();
	    	   writer.flush();
	       }
		}
				
				
				
			    
			return 0;
			} 
		catch(Throwable err)
			{
			err.printStackTrace();
			return -1;
			}
		}
	
	public static void main(String[] args) {
		new XmlCipher().instanceMain(args);
		}
	}
  