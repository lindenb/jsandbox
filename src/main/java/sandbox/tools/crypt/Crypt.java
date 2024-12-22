package sandbox.tools.crypt;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.io.IOUtils;
import sandbox.lang.StringUtils;
import sandbox.tools.central.ProgramDescriptor;

public class Crypt  extends Launcher {
    private static final int SALT_LENGTH = 16; // Salt length in bytes
    private static final int ITERATIONS = 65536; // Number of PBKDF2 iterations
    private static final int KEY_LENGTH = 256; // Key length in bits
	
    @Parameter(names={"-o","--output"},description=OUTPUT_OR_STANDOUT)
    private Path out = null; 
	@Parameter(names= { "--password","-P"},description="password")
    private String cliPassword=null;
	@Parameter(names= { "--decode","-D"},description="decode encrypted input.")
    private boolean do_decode=false;

	

    @Override
    public int doWork(List<String> args) {
        final char[] masterPassword;
        final String input=super.oneFileOrNull(args);
        if(StringUtils.isBlank(cliPassword)) {
	        final Console console = System.console();
	        if(console==null)  {
	        	getLogger().error("> No Console available");
	        	return -1;
	        	}
	        char[] pwd = console.readPassword("Enter password : ");
	        if(pwd==null) {
	        	getLogger().error("> No Console available");
	        	return -1;
	        	}
	    	masterPassword = pwd;  // Replace with your actual master password
	        }
        else
	        {
	        masterPassword = cliPassword.toCharArray();
	        }
       
       try(
    		   InputStream is = StringUtils.isBlank(input) || input.equals("-")?System.in: IOUtils.openStream(input);
    		   OutputStream os=IOUtils.openPathAsOutputStream(this.out)) {
	        if(this.do_decode) {
	        	decode(is, os, masterPassword);
	        	}
	        else {
	        	encode(is, os, masterPassword);
	        	}
	        os.flush();
        return 0;
       	}
       catch(Throwable err) {
		   getLogger().error(err);
		   return -1;
	   		}
       }
	
	
    private byte[] generateSalt() {
        final SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return salt;
    	}
    
	private void encode(InputStream inputStream,OutputStream outputStream,char[] password) throws IOException  {
   		try {
   		// Create a random salt
            byte[] salt = generateSalt();

            // Derive a key from the password and salt
            SecretKey secretKey = deriveKeyFromPassword(password, salt);

            // Initialize the cipher in encryption mode
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
                // First write the salt to the output (needed for decryption)
                outputStream.write(salt);

                // Encrypt the input stream and write it to the output
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byte[] encrypted = cipher.update(buffer, 0, bytesRead);
                    if (encrypted != null) {
                        outputStream.write(encrypted);
                    }
                }
                byte[] finalBlock = cipher.doFinal();
                if (finalBlock != null) {
                    outputStream.write(finalBlock);
                }
            
  
   			}
   		catch(IOException err) {
   			throw err;
   		}
   		catch(Throwable err) {
   			throw new IOException(err);
   		}
		}
    
   	private void decode(InputStream inputStream,OutputStream outputStream,char[] password) throws IOException  {
	   		try {
              // Read the salt from the input (first SALT_LENGTH bytes)
              byte[] salt = new byte[SALT_LENGTH];
              if (inputStream.read(salt) != SALT_LENGTH) {
                  throw new IllegalArgumentException("Invalid input: missing or corrupted salt.");
              }

              // Derive the key using the salt and password
              SecretKey secretKey = deriveKeyFromPassword(password, salt);

              // Initialize the cipher in decryption mode
              Cipher cipher = Cipher.getInstance("AES");
              cipher.init(Cipher.DECRYPT_MODE, secretKey);

              // Decrypt the input stream and write it to the output
              byte[] buffer = new byte[1024];
              int bytesRead;
              while ((bytesRead = inputStream.read(buffer)) != -1) {
                  byte[] decrypted = cipher.update(buffer, 0, bytesRead);
                  if (decrypted != null) {
                      outputStream.write(decrypted);
                  }
              }
              byte[] finalBlock = cipher.doFinal();
              if (finalBlock != null) {
                  outputStream.write(finalBlock);
              }
	   		}
	   		catch(IOException err) {
	   			throw err;
	   		}
	   		catch(Throwable err) {
	   			throw new IOException(err);
	   		}
   		}
   


   private static SecretKey deriveKeyFromPassword(char[] password, byte[] salt) throws Exception {
       final PBEKeySpec keySpec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
       final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
       final byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
       return new SecretKeySpec(keyBytes, "AES");
   		}
   
    public static void main(String[] args) {
		new Crypt().instanceMainWithExit(args);
		}
    
    public static ProgramDescriptor getProgramDescriptor() {
    	return new ProgramDescriptor() {
    		@Override
    		public String getName() {
    			return "crypt";
    			}
    		};
    	}
}
