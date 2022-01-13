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

import java.io.File;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.StringUtils;

//
public class XmlCipher extends Launcher {
	@Parameter(names={"-o","--output"},description="output name")
	private File fileout = null; 
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
   private static final int ITERATION_COUNT = 23;
   private Cipher theCipher;
   private  XmlCipher()
	   {
	 	}
   
   
  private String encrypt(String str) throws Exception
	  	{
       // Encode the string into bytes using utf-8
       byte[] utf8 = str.getBytes("UTF8");

       // Encrypt
       byte[] enc = theCipher.doFinal(utf8);

        return Base64.getEncoder().encodeToString(enc);
	    }

  private String decrypt(String str)  throws Exception
  	{
      // Decode base64 to get bytes
      byte[] dec = Base64.getDecoder().decode(str);

      // Decrypt
      byte[] utf8 = theCipher.doFinal(dec);

      // Decode using utf-8
      return new String(utf8, "UTF8");
  	}
  

	
	private String process(String s,boolean encode) throws Exception
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
	
	private void process(Node root,boolean encode) throws Exception
		{
		for(Node n1=root.getFirstChild(); n1!=null; n1=n1.getNextSibling())
			{
			if(n1.getNodeType()==Node.ELEMENT_NODE)
				{
				Element e1=Element.class.cast(n1);
				if(e1.hasAttributes())
					{
					NamedNodeMap attMap=e1.getAttributes();
					List<Attr> atts=new ArrayList<Attr>(attMap.getLength());
					for(int i=0;i<attMap.getLength();++i)
						{
						atts.add((Attr)attMap.item(i));
						}
					for(Attr att:atts)
						{
						if(att.getPrefix()!=null && att.getPrefix().equals("xml")) continue;
						if(att.getPrefix()!=null && att.getPrefix().equals("xmlns")) continue;
						if(att.getNodeName().equals("xmlns")) continue;
						att.setNodeValue(process(att.getValue(), encode));
						e1.setAttributeNode(att);
						}
					}
				}
			else if(n1.getNodeType()==Node.TEXT_NODE)
				{
				Node parent=n1.getParentNode();
				boolean hasElement=false;
				if(parent!=null && parent.getNodeType()==Node.ELEMENT_NODE)
					{
					for(Node n2=n1.getFirstChild();n2!=null; n2=n2.getNextSibling())
						{
						if(n2.getNodeType()!=Node.ELEMENT_NODE) continue;
						hasElement=true;
						break;
						}
					if(!hasElement)
						{
						String text=Text.class.cast(n1).getData();
						if(text.trim().length()>0)
							{
							text=process(text, encode);
							Text t=n1.getOwnerDocument().createTextNode(text);
							parent.replaceChild(t, n1);
							n1=t;
							}
						}
					}
				}
			process(n1, encode);
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
			
			

	        // Create the key
	        KeySpec keySpec = new PBEKeySpec(passPhrase, SALT, ITERATION_COUNT);
	        SecretKey key = SecretKeyFactory.getInstance(
	            "PBEWithMD5AndDES").generateSecret(keySpec);
	      
	        theCipher = Cipher.getInstance(key.getAlgorithm());
	       

	        // Prepare the parameter to the ciphers
	        AlgorithmParameterSpec paramSpec = new PBEParameterSpec(SALT, ITERATION_COUNT);
	        if(this.decode && this.encode) {
	        	System.err.println("decode and encode ???");
	        	return -1;
	        	}
	        else if(this.encode)
	        	{
	        	this.theCipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
	        	}
	        else if(this.decode)
	        	{
	        	this.theCipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
	        	}
	        else
	        	{
	        	System.err.println("decode or encode ???");
	        	return -1;
	        	}
					
				DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
				f.setCoalescing(true);
				f.setNamespaceAware(true);
				f.setValidating(false);
				f.setIgnoringComments(false);
				DocumentBuilder docBuilder= f.newDocumentBuilder();
				Document dom=null;
				
				
			    if(args.isEmpty())
                    {
                    dom=docBuilder.parse(System.in);
                    }
                else if(args.size()==1)
                    {
                    String filename=args.get(0);
                    if(	filename.startsWith("http://") ||
                    	filename.startsWith("https://") ||
                    	filename.startsWith("ftp://"))
                    	{
                    	dom=docBuilder.parse(filename);
                    	}
                    else
                    	{
                    	dom=docBuilder.parse(new File(filename));
                    	}
                    }
                else
                	{
                	System.err.println("IllegalNumber of arguments.");
                	return -1;
                	}
			    
			    if(encode)
			    	{
			    	this.process(dom,true);
			    	}
			    else if(this.decode)
			    	{
			    	this.process(dom,false);
			    	}
			
			TransformerFactory factory=TransformerFactory.newInstance();
			Transformer transformer=factory.newTransformer();
			if(fileout!=null)
				{
				transformer.transform(new DOMSource(dom), new StreamResult(fileout));
				}
			else
				{
				transformer.transform(new DOMSource(dom), new StreamResult(System.out));
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
  