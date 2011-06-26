/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	June-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * Reference:
 *   http://www.exampledepot.com/egs/javax.crypto/PassKey.html
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Motivation:
 * 		crypt XML
 * Compilation:
 *        cd jsandbox; ant xmlcypher
 * Usage:
 *        java -jar dist/xmlcypher.jar
 */
package sandbox;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.ArrayList;
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

//
public class XmlCipher
	{
	private final static String BASE64 =
	"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";


	
	// 8-byte Salt
    private static final byte[] SALT =
		{
        (byte)0xA5, (byte)0x9B, (byte)0xC9, (byte)0x32,
        (byte)0x48, (byte)0x35, (byte)0xE2, (byte)0x03
    	};
   private static final int ITERATION_COUNT = 23;
   private Cipher theCipher;
   private  XmlCipher(char passPhrase[], boolean encode) throws Exception
	   {
        // Create the key
        KeySpec keySpec = new PBEKeySpec(passPhrase, SALT, ITERATION_COUNT);
        SecretKey key = SecretKeyFactory.getInstance(
            "PBEWithMD5AndDES").generateSecret(keySpec);
      
        theCipher = Cipher.getInstance(key.getAlgorithm());
       

        // Prepare the parameter to the ciphers
        AlgorithmParameterSpec paramSpec = new PBEParameterSpec(SALT, ITERATION_COUNT);
        if(encode)
        	{
        	theCipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
        	}
        else
        	{
        	theCipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
        	}
	 	}
   
   
  private String encrypt(String str) throws Exception
	  	{
       // Encode the string into bytes using utf-8
       byte[] utf8 = str.getBytes("UTF8");

       // Encrypt
       byte[] enc = theCipher.doFinal(utf8);

        return encodeBase64(enc);
	    }

  private String decrypt(String str)  throws Exception
  	{
      // Decode base64 to get bytes
      byte[] dec = decodeBase64(str);

      // Decrypt
      byte[] utf8 = theCipher.doFinal(dec);

      // Decode using utf-8
      return new String(utf8, "UTF8");
  	}
  

	/***
	*  Decodes BASE64 encoded string.
	*/
	private static byte[] decodeBase64(String dataIn) throws IOException 
		{
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		byte output[] = new byte[3];
		int state=0;
		 char alpha='\0';
		
		for(int i=0;i<dataIn.length();++i)
			{
		    byte c;
		    alpha=dataIn.charAt(i);
		    if (Character.isWhitespace(alpha))
		    	{
		    	continue;
		    	}
		    else if ((alpha >= 'A') && (alpha <= 'Z'))
				{
				c = (byte)(alpha - 'A');
				}
			else if ((alpha >= 'a') && (alpha <= 'z'))
				{
				c = (byte)(26 + (alpha - 'a'));
				}
			else if ((alpha >= '0') && (alpha <= '9'))
				{
				c = (byte)(52 + (alpha - '0'));
				}
		   	else if (alpha=='+')
		   		{
		   		c = 62;
		   		}
			else if (alpha=='/')
				{
				c = 63;
				}
		   	else if (alpha=='=')
		   		{
		   		break; // end
		   		}
			else
				{
				throw new IOException("Illegal character for Base64 \'"+alpha+"\' in "+dataIn); // error
				}
		   
		
		    switch(state)
		        {   case 0: output[0] = (byte)(c << 2);
		        			state++;
		                    break;
		            case 1: output[0] |= (byte)(c >>> 4);
		                    output[1] = (byte)((c & 0x0F) << 4);
		        			state++;
		                    break;
		            case 2: output[1] |= (byte)(c >>> 2);
		                    output[2] =  (byte)((c & 0x03) << 6);
		        			state++;
		                    break;
		            case 3: output[2] |= c;
		                    out.write(output);
		        			state=0;
		                    break;
		        }
			} // for
		
		if (alpha=='=') /* then '=' found, but the end of string */
		    switch(state)
		    {   
		    case 2: out.write(output,0,1); break;
			case 3: out.write(output,0,2); break;
			default: break;
		    }
		out.flush();
		return out.toByteArray();
		} // decode

	/**
	 *  Encodes binary data by BASE64 method.
	 *  @param data binary data as byte array
	 *  @return encoded data as String
	 */
	private static String encodeBase64(byte dataIn[]) throws IOException
			{
			StringBuilder out=new StringBuilder();
			char output[] = new char[4];
			int restbits = 0;
			 int chunks = 0;
			int c;
			int nFill=0;
			
			for(int i=0;i< dataIn.length;++i)
				{
				c=dataIn[i];
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
			            out.append(output);
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
			             out.append(output);
			             break;
			         case 2:
			         	 output[2] = BASE64.charAt((restbits << 2));
			             output[3] = '=';
			             out.append(output);
			             break;
				}
			
		return out.toString();
		} // encode()	
	
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
	
	
	public static void main(String[] args)
		{
		try
			{
			File fileout=null;
			Boolean encode=null;
			char password[]=null;
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
					System.err.println(" -e 'encode'");
					System.err.println(" -d 'decode'");
					System.err.println(" -o 'fileout' (optional)");
					System.err.println(" -p <password> (optional).");
					return;
					}
				else if(args[optind].equals("-o"))
					{
					fileout=new File(args[++optind]);
					}
				else if(args[optind].equals("-p"))
					{
					password=args[++optind].toCharArray();
					}
				else if(args[optind].equals("-e"))
					{
					if(encode!=null && encode!=Boolean.TRUE)
						{
						System.err.println("ambigous program");
						return;
						}
					encode=Boolean.TRUE;
					}
				else if(args[optind].equals("-d"))
					{
					if(encode!=null &&encode!=Boolean.FALSE)
						{
						System.err.println("ambigous program");
						return;
						}
					encode=Boolean.FALSE;
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
			if(password==null)
				{
				if(System.console()==null)
					{
					System.err.println("Undefined password.");
					return;
					}
				password=System.console().readPassword("[xmlcipher] Password ?");
				if(password==null)
					{
					System.err.println("Undefined password.");
					return;
					}
				}
			
					
				DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
				f.setCoalescing(true);
				f.setNamespaceAware(true);
				f.setValidating(false);
				f.setIgnoringComments(false);
				DocumentBuilder docBuilder= f.newDocumentBuilder();
				Document dom=null;
				
				
			    if(optind==args.length)
                    {
                    dom=docBuilder.parse(System.in);
                    }
                else if(optind+1==args.length)
                    {
                    String filename=args[optind++];
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
                	return;
                	}
			    if(encode==null)
			    	{
			    	System.err.println("undefined program decode or encode ??");
                	return;
			    	}
			    else if(encode)
			    	{
			    	XmlCipher app=new XmlCipher(password,encode);
			    	app.process(dom,true);
			    	}
			    else if(!encode)
			    	{
			    	XmlCipher app=new XmlCipher(password,encode);
			    	app.process(dom,false);
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
			} 
		catch(Throwable err)
			{
			err.printStackTrace();
			}
		}
	
	}
  