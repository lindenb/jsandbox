package sandbox;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public abstract class Launcher
	{
	private static final Logger LOG = Logger.builder(Launcher.class).build();
	
	public abstract int doWork(final List<String> args);
	
	@Parameter(description = "Files")
	private List<String> files=new ArrayList<>();
	@Parameter(names = {"-h","--help"},description="print help and exit", help = true)
	private boolean print_help = false;

	public int instanceMain(final String args[]) {
		final JCommander jCommander = new JCommander();
		try
			{
			jCommander.addObject(this);
			jCommander.parse(args);
			}
		 catch(final com.beust.jcommander.ParameterException err) {
		 	System.err.println("There was an error in the input parameters.");
		 	System.err.println(err.getMessage());
			return -1; 
		 	}
		if(print_help)
			{
			jCommander.usage();
			return 0;
			}
		
		try {		
			int ret = doWork(this.files);
			return ret;
			}
		catch(final Throwable err)
			{
			LOG.error(err);
			return -1;
			}
		}

	
	public void instanceMainWithExit(final String args[]) {
		final int ret =  instanceMain(args);
		System.exit(ret);
		}
	}
