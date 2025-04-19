package sandbox.tools.feedburner;
import java.nio.file.Path;
import java.util.List;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.tools.central.ProgramDescriptor;


public class FeedBurner extends Launcher {

	private static final Logger LOG=Logger.builder(FeedBurner.class).build();
	
	private class Context extends  sandbox.minilang.minilang2.MiniLang2Context {
		
		}
	
	@Parameter(names= {"-o","--out"},description = "ouput file or stdout")
	private Path outputFile = null;
	
	
	
	
	@Override
	public int doWork(final List<String> args)
		{
		try {
			final String input = oneFileOrNull(args);
			return 0;
			}
		catch(Throwable err) {
			LOG.error(err);
			return -1;
			}
		}


    public static ProgramDescriptor getProgramDescriptor() {
    	return new ProgramDescriptor() {
    		@Override
    		public String getName() {
    			return "feedburner";
    			}
    		};
    	}

    public static void main(final String args[]) {
	new FeedBurner().instanceMainWithExit(args);
	}
}
