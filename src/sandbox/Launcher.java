package sandbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import sandbox.io.IOUtils;

public abstract class Launcher
	{
	private static final Logger LOG = Logger.builder(Launcher.class).build();
	protected static final String OUTPUT_OR_STANDOUT= "File output (or stdout if undefined)";
	
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
	
	protected BufferedReader  openBufferedReader(final String pathOrNull) throws IOException {
	if(pathOrNull==null)
		{
		return IOUtils.openBufferedReader(System.in);
		}
	else
		{
		return IOUtils.openBufferedReader(pathOrNull);
		}
	}	

	
	protected OutputStream openPathAsOuputStream(final Path path) throws IOException {
		if( path==null) return System.out;
		OutputStream os = Files.newOutputStream(path);
		if(path.getFileName().toString().toLowerCase().endsWith(".gz")) os=new GZIPOutputStream(os);
		return os;
		}
	
	protected BufferedReader  openBufferedReader(final List<String> args) throws IOException {
		return openBufferedReader(oneFileOrNull(args));
		}	
	
	protected String oneFileOrNull(final List<String> args) {
		if(args.isEmpty()) return null;
		if(args.size()==1) return args.get(0);
		throw new IllegalArgumentException("expected one zero file one input but got "+args.size());
		}
	protected String oneAndOnlyOneFile(final List<String> args) {
		if(args.size()==1) return args.get(0);
		throw new IllegalArgumentException("expected one file as argument got "+args.size());
		}
	
	public void instanceMainWithExit(final String args[]) {
		final int ret =  instanceMain(args);
		System.exit(ret);
		}
	}
