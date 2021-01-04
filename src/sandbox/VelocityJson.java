package sandbox;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import sandbox.io.IOUtils;

import java.io.*;
import java.util.List;

public class VelocityJson extends AbstractApplication {
	public static class Tool {
		public JsonElement json(final String j) {
			return  new JsonParser().parse(j);
		}
		public String readURL(final String path) throws IOException {
			final Reader r = IOUtils.openReader(path);
			final String s= IOUtils.readReaderContent(r);
			r.close();
			return  s;
		}
		
		public String readFile(final String f) throws IOException {
			return  IOUtils.readFileContent(new File(f));
		}
		public String escapeC(final String s) {
			final StringBuilder sb =new StringBuilder(s.length());
			for(int i=0;i< s.length();++i) {
				char c=s.charAt(i);
				switch(c) {
				case '\n': sb.append("\\n"); break;
				case '\r': sb.append("\\r"); break;
				case '\t': sb.append("\\t"); break;
				case '\\': sb.append("\\\\"); break;
				case '\'': sb.append("\\\'"); break;
				case '\"': sb.append("\\\""); break;
				default: sb.append(c); break;
				}
			}
			return sb.toString();
			}
		@Override
		public String toString() {
			return "TOOLS "+getClass().getName();
			}
	}
	
	@Override
	protected void fillOptions(Options options) {
		options.addOption(Option.builder("f").longOpt("file").desc("read json file. syntax key=file. this is a list of files. Must be ended with '--'.").hasArgs().build());
		options.addOption(Option.builder("s").longOpt("string").desc("read json string. syntax key=json-expr. this is a list of files. Must be ended with '--'.").hasArgs().build());
		super.fillOptions(options);
	}
	
	@Override
	protected int execute(final CommandLine cmd)
	    	{
		  	final VelocityContext context= new VelocityContext();
		  	context.put("tool", new Tool());
		  	
			try {
				for(final String opt: new String[]{"f","s"}) {
					if(cmd.hasOption(opt)) {
				  		for(final String kvs  : cmd.getOptionValues(opt)) {
				  			int eq = kvs.indexOf('=');
				  			if(eq<1) {
				  				LOG.severe("No '=' found in "+kvs);
				  				return -1;
				  			}
				  			final String k = kvs.substring(0,eq).trim();
				  			if(context.containsKey(k)) {
				  				LOG.severe("Duplicate key "+k+" in velocity context.");
				  				return -1;
				  			}
				  			final String v = kvs.substring(eq+1).trim();
				  			final JsonParser jr = new JsonParser();
				  			final JsonElement element;
				  			if( opt.equals("f")) {
				  				element = jr.parse(new FileReader(v));
				  				}
				  			else
					  			{
				  				element = jr.parse(v);
					  			}
				  			context.put(k, element);
				  		}
				  	}
				}
				
		    	final List<String> args = cmd.getArgList();
		    	final PrintWriter w=new PrintWriter( System.out);
		    	if(args.isEmpty())
		    		{
		    		LOG.info("read template from stdin");
                    final VelocityEngine ve = new VelocityEngine();
                    ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "string");
                    ve.setProperty("string.resource.loader.class","org.apache.velocity.runtime.resource.loader.StringResourceLoader");
                    ve.init();
                    final String templText = IOUtils.readStreamContent(System.in);
                    StringResourceLoader.getRepository().putStringResource("t1", templText);
                    final Template template = ve.getTemplate("t1");
                    template.merge( context, w);
		    		}
		    	else
			    	{
		    		for(final String filename: args)
		                    {
			    			LOG.info("read template from "+filename);
		                    final File file=new File(filename);
		                    final VelocityEngine ve = new VelocityEngine();
		                    ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
		                    ve.setProperty("file.resource.loader.class","org.apache.velocity.runtime.resource.loader.FileResourceLoader");
		                    ve.setProperty("file.resource.loader.path",file.getParent());
		                    ve.init();
		                    final Template template = ve.getTemplate(file.getName());
		                    template.merge( context, w);
		                    }
			    	}
		    	w.flush();
		    	w.close();
		    	return 0;
	        	}
			catch(final Exception err) {
				err.printStackTrace();
				return -1;
				}
	    	}
	
		public static void main(final String[] args) {
				new VelocityJson().instanceMainWithExit(args);
			}
		}
