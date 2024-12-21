package sandbox.tools.angel;


import java.awt.Color;
import java.io.Console;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.tools.central.ProgramDescriptor;

public class Angel extends Launcher {
	@Parameter(names= { "--pasword","-P"},description="password")
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
	        String combined = masterPassword + ":" + domain;
            String password = b64_sha1(combined).substring(0, 8) + "1a";  // First 8 chars and append "1a"
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
