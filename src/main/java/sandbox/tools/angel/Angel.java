package sandbox.tools.angel;


import java.awt.Color;
import java.io.Console;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;


import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.lang.StringUtils;
import sandbox.tools.central.ProgramDescriptor;

public class Angel extends Launcher {
	@Parameter(names= { "--password","-P"},description="password")
    private String cliPassword=null;

	@Parameter(names= { "--plain"},description="no gui")
    private boolean plain=false;
    
    @Override
    public int doWork(List<String> args) {
        String host = super.oneAndOnlyOneFile(args);
        final String masterPassword;
        if(StringUtils.isBlank(cliPassword)) {
	        final Console console = System.console();
	        if(console==null)  {
	        	System.err.println("> No Console available");
	        	return -1;
	        	}
	        char[] pwd = console.readPassword("Enter password : ");
	        if(pwd==null) {
	        	System.err.println("> No Console available");
	        	return -1;
	        	}
	    	masterPassword = new String(pwd);  // Replace with your actual master password
	        }
        else
	        {
	        masterPassword = cliPassword;
	        }
        if (masterPassword != null && !masterPassword.isEmpty()) {
	        String domain = extractDomain(host);
	        final Transcoder tr;
	        tr = new XCode();
            String password = tr.make(domain, masterPassword);
	        if(plain) {
	            JLabel jlabel = new JLabel(password);
	            int gray=250;
	            jlabel.setForeground(new Color(gray,gray,gray));
	            JOptionPane.showMessageDialog(null, jlabel);
	            }
            else
	            {
	            System.out.println("Generated password: " + password);
	            }
        } else {
            System.out.println("Master password is required.");
        }
        return 0;
    }
   
    private static String extractDomain(String url) {
        String host = url.replaceAll("https?://", "").split("/")[0];
        final String domain;

        // Check if the URL matches a known domain pattern
        if (host.matches(".*\\.[a-z]{2,3}$")) {
            domain = host;
        } else {
        	String[] tokens =host.split("\\.");
            domain = tokens[tokens.length - 2] + "." + tokens[tokens.length - 1];
        }

        return domain;
    	}

    // Method to compute SHA-1 hash and convert to base64
  

   private static abstract class Transcoder {
	   abstract String make(final String host,String p);
   	}
   
   private static class DefaultTranscoder extends Transcoder {
	@Override
	String make(String host, String p) {
	      String combined = p + ":" + host;
          return  b64_sha1(combined).substring(0, 8) + "1a";  // First 8 chars and append "1a"
		  }
	
	 private String b64_sha1(String input) {
	        try {
	            MessageDigest md = MessageDigest.getInstance("SHA-1");
	            byte[] hash = md.digest(input.getBytes());
	          	return String.format("%032X", new BigInteger(1, hash));
	            //return Base64.getEncoder().encodeToString(hash);
	        } catch (Throwable e) {
	           throw new RuntimeException(e);
	        }
	    }
  	}
   
   
   /*
   private class Transcoder {
	   final String b64pad="";
	   final int chrsz=8;
	   Object b64_sha1(String s) {
		   return binb2b64(core_sha1(str2binb(s),s.length()*chrsz));
		   }
	   Object core_sha1(byte[] x,int len){x[len>>5]|=0x80<<(24-len);x[((len 64>>9)<<4) 15]=len;var w=Array(80);
	   	int a=1732584193;
	   	int b=-271733879;
	   	int c=-1732584194;
	   	int d=271733878;
	   	int e=-1009589776;
	   	for(int i=0;i<x.length;i =16)
	   		{
	   		int olda=a;
	   		int oldb=b;
	   		var oldc=c;var oldd=d;var olde=e;
	   		for(var j=0;j<80;j++)
	   			{
	   			if(j<16) {w[j]=x[i j];}
	   			else {
                    w[j] = rol(w[j - 3] ^ w[j - 8] ^ w[j - 14] ^ w[j - 16], 1);
	   				}
                int t=safe_add(safe_add(rol(a,5),sha1_ft(j,b,c,d)),safe_add(safe_add(e,w[j]),sha1_kt(j)));
                e=d;
                d=c;
                c=rol(b,30);
                b=a;
                a=t;
                }
	   		a=safe_add(a,olda);
	   		b=safe_add(b,oldb);
	   		c=safe_add(c,oldc);
	   		d=safe_add(d,oldd);
	   		e=safe_add(e,olde);
	   		}
	   	return new byte[a,b,c,d,e];
	   	}
	   	int sha1_ft(int t,int b,int c,int d) {
	   		if(t<20) return (b&c)|((~b)&d);
	   		if(t<40)return b^c^d;
	   		if(t<60)return (b&c)|(b&d)|(c&d);
	   		return b^c^d;
	   		}
	   	int sha1_kt(int t){
	   		return (t<20)?1518500249:(t<40)?1859775393:(t<60)?-1894007588:-899497514;
	   		}
	   	int safe_add(int x,int y)
	   		{
	   		int lsw=(x&0xFFFF) (y&0xFFFF);
	   		var msw=(x>>16) (y>>16) (lsw>>16);
	   		return (msw<<16)|(lsw&0xFFFF);
	   		}
	   	int rol(num,cnt)
	   		{
	   		return (num<<cnt)|(num>>>(32-cnt));
	   		}
	   	byte[] str2binb(String str){
	   		var bin=Array();
	   		var mask=(1<<chrsz)-1;
	   		for(var i=0;i<str.length()*chrsz;i =chrsz)bin[i>>5]|=(str.charCodeAt(i/chrsz)&mask)<<(24-i);
	   		return bin;
	   		}

   		}
   */
   
   		private class XCode extends Transcoder {
	           private static final int CHARSZ = 8;
	           private static final String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";


	           private int[] core_sha1(int[] x, int len) {
	               x[len >> 5] |= 0x80 << (24 - len % 32);
	               x[((len + 64 >> 9) << 4) + 15] = len;

	               int[] w = new int[80];
	               int a = 0x67452301;
	               int b = 0xEFCDAB89;
	               int c = 0x98BADCFE;
	               int d = 0x10325476;
	               int e = 0xC3D2E1F0;

	               for (int i = 0; i < x.length; i += 16) {
	                   int olda = a;
	                   int oldb = b;
	                   int oldc = c;
	                   int oldd = d;
	                   int olde = e;

	                   for (int j = 0; j < 80; j++) {
	                       if (j < 16) {
	                           w[j] = x[i + j];
	                       } else {
	                           w[j] = rol(w[j - 3] ^ w[j - 8] ^ w[j - 14] ^ w[j - 16], 1);
	                       }
	                       int t = safe_add(safe_add(rol(a, 5), sha1_ft(j, b, c, d)),
	                               safe_add(safe_add(e, w[j]), sha1_kt(j)));
	                       e = d;
	                       d = c;
	                       c = rol(b, 30);
	                       b = a;
	                       a = t;
	                   }

	                   a = safe_add(a, olda);
	                   b = safe_add(b, oldb);
	                   c = safe_add(c, oldc);
	                   d = safe_add(d, oldd);
	                   e = safe_add(e, olde);
	               }

	               return new int[]{a, b, c, d, e};
	           }

	           private int sha1_ft(int t, int b, int c, int d) {
	               if (t < 20) return (b & c) | (~b & d);
	               if (t < 40) return b ^ c ^ d;
	               if (t < 60) return (b & c) | (b & d) | (c & d);
	               return b ^ c ^ d;
	           }

	           private int sha1_kt(int t) {
	               if (t < 20) return 0x5A827999;
	               if (t < 40) return 0x6ED9EBA1;
	               if (t < 60) return 0x8F1BBCDC;
	               return 0xCA62C1D6;
	           }

	           private int safe_add(int x, int y) {
	               int lsw = (x & 0xFFFF) + (y & 0xFFFF);
	               int msw = (x >> 16) + (y >> 16) + (lsw >> 16);
	               return (msw << 16) | (lsw & 0xFFFF);
	           }

	           private  int rol(int num, int cnt) {
	               return (num << cnt) | (num >>> (32 - cnt));
	           }

	           private int[] str2binb(String str) {
	               int[] bin = new int[(str.length() * CHARSZ + 31) >> 5];
	               int mask = (1 << CHARSZ) - 1;
	               for (int i = 0; i < str.length() * CHARSZ; i += CHARSZ) {
	                   bin[i >> 5] |= (str.charAt(i / CHARSZ) & mask) << (24 - i % 32);
	               }
	               return bin;
	           }

	           private String binb2b64(int[] binarray) {
	               StringBuilder str = new StringBuilder();
	               int triplet, length = binarray.length * 4;

	               for (int i = 0; i < length; i += 3) {
	                   triplet = (((binarray[i >> 2] >> 8 * (3 - i % 4)) & 0xFF) << 16)
	                           | (((i + 1 < length ? binarray[(i + 1) >> 2] >> 8 * (3 - (i + 1) % 4) : 0) & 0xFF) << 8)
	                           | ((i + 2 < length ? binarray[(i + 2) >> 2] >> 8 * (3 - (i + 2) % 4) : 0) & 0xFF);

	                   for (int j = 0; j < 4; j++) {
	                       if (i * 8 + j * 6 > binarray.length * 32) {
	                           str.append("=");
	                       } else {
	                           str.append(BASE64_CHARS.charAt((triplet >> 6 * (3 - j)) & 0x3F));
	                       }
	                   }
	               }
	               return str.toString();
	           }
	       

		   	@Override
		    String make(String host, String p) {		
	               String input = p + ":" + host;
	               int[] hash = core_sha1(str2binb(input), input.length() * CHARSZ);
	               return binb2b64(hash).substring(0, 8) + "1a";
	           	}
	   
   			}
    
    public static void main(String[] args) {
		new Angel().instanceMainWithExit(args);
	}
    
    public static ProgramDescriptor getProgramDescriptor() {
    	return new ProgramDescriptor() {
    		@Override
    		public String getName() {
    			return "angel";
    			}
    		@Override
    		public boolean isHidden() {
    			return true;
    			}
    		};
    	}
}
