package sandbox;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public abstract class AbstractApplication
	{
	protected enum Status {OK,EXIT_ERROR,EXIT_SUCCESS};
	protected static Logger LOG=Logger.getLogger("jsandbox");
	static {
		LOG.setLevel(Level.FINE);
		}
	private Options options = new Options();
	protected AbstractApplication()
		{
		
		}
	protected String getProgramName()
		{
		return this.getClass().getName();
		}
	
	protected String getProgramDescription() {
		return "";
	}
	
	protected void usage()
		{
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(getProgramName()+ " : " + getProgramDescription(), getOptions());
		}
	
	protected Options getOptions()
		{
		return options;
		}
	
	protected void fillOptions(final Options options)
		{
		
		 Option optProxyHost = Option.builder("proxyHost").
				hasArg().
				longOpt("proxyHost").
				desc("proxy host").
				build();
		 Option optProxyPort = Option.builder("proxyPort").
				hasArg().
				longOpt("proxyPort").
				argName("port").
				desc("proxy host").
				build();
		 Option optLogLevel = Option.builder("logLevel").
				hasArg().
				longOpt("logLevel").
				argName("level").
				desc("Log level").
				build();
		 Option optHelp = Option.builder("h").
				hasArg(false).
				longOpt("help").
				desc("help").
				build();

		options.addOption(optLogLevel);
		options.addOption(optProxyHost);
		options.addOption(optProxyPort);
		options.addOption(optHelp);
		}
	protected void fillOptions()
		{
		fillOptions(getOptions());
		}
	
	protected CommandLineParser createCommandLineParser()
		{
		return new DefaultParser();
		}
	
	abstract protected int execute(final CommandLine cmd);
	
	public static Logger getLogger()
		{
		return LOG;
		}
	
	protected void log(Level level,Object o)
		{
		if(o!=null && o instanceof Throwable )
			{
			String message=((Throwable) o).getMessage();
			if(message==null) message=o.getClass().getName();
			LOG.log(level,message, ((Throwable) o));
			return;
			}
		LOG.log(level,String.valueOf(o));
		}
	
	protected void info(Object o)
		{
		log(Level.INFO,o);
		}
	protected void warning(Object o)
		{
		log(Level.WARNING,o);
		}
	protected void debug(Object o)
		{
		log(Level.FINEST,o);
		}
	protected void error(Object o)
		{
		log(Level.SEVERE,o);
		}

	

	
	protected Status decodeOptions(final CommandLine cmd)
		{
		if(cmd.hasOption("help"))
			{
			usage();
			return Status.EXIT_SUCCESS;
			}
		if(cmd.hasOption("proxyHost"))
			{
			System.setProperty("http.proxyHost",cmd.getOptionValue("proxyHost"));
			System.setProperty("https.proxyHost",cmd.getOptionValue("proxyHost"));
			}
		if(cmd.hasOption("proxyPort"))
			{
			System.setProperty("http.proxyPort",cmd.getOptionValue("proxyPort"));
			System.setProperty("https.proxyPort",cmd.getOptionValue("proxyPort"));
			}
		if(cmd.hasOption("log-level"))
			{
			try
				{
				LOG.setLevel(Level.parse(cmd.getOptionValue("log-level")));
				}
			catch(Exception err)
				{
				System.err.println(err.getMessage());
				return Status.EXIT_ERROR;
				}
			
			}
		return Status.OK;
		}
	
	public int instanceMain(String args[])
		{
		fillOptions();
		final CommandLineParser parser = createCommandLineParser();
		CommandLine cmd = null;
		try
			{
			cmd = parser.parse(getOptions(), args);
			}
		catch(ParseException err)
			{
			System.err.println(err.getMessage());
			return -1;
			}
		switch( decodeOptions(cmd))
			{
			case EXIT_ERROR: return -1;
			case EXIT_SUCCESS : return 0;
			default:break;
			}
		
		return execute(cmd);
		}
	public void instanceMainWithExit(String args[])
		{
		int ret= instanceMain(args);
		System.exit(ret);
		}
	
	}
