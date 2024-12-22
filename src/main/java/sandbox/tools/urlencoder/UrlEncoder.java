package sandbox.tools.urlencoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.io.IOUtils;
import sandbox.tools.central.ProgramDescriptor;

public class UrlEncoder extends Launcher {
	@Parameter(names= { "--decode","-D"},description="decode")
    private boolean decode=false;
	@Parameter(names= { "--encoding","-E"},description="encoding")
    private String encodingStr = "UTF-8";	
	@Override
	public int doWork(List<String> args) {
		if(!args.isEmpty()) {
			System.err.println("no argument is expected. Use stdin");
			return -1;
			}
		try(BufferedReader br = IOUtils.openBufferedReader(System.in)) {
			final Charset charset = Charset.forName(encodingStr);
			for(;;) {
				String line = br.readLine();
				if(line==null) break;
				line = decode ? java.net.URLDecoder.decode(line, charset) : java.net.URLEncoder.encode(line, charset);
				System.out.println(line);
				}
			}
		catch(IOException err) {
			
			}
		return 0;
		}
	
	 public static void main(String[] args) {
			new UrlEncoder().instanceMainWithExit(args);
		}
	    
    public static ProgramDescriptor getProgramDescriptor() {
    	return new ProgramDescriptor() {
    		@Override
    		public String getName() {
    			return "urlencoder";
    			}
    		};
    	}
}
