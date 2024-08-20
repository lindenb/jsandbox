package sandbox;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;

import sandbox.io.IOUtils;

public class NashornServer extends AbstractApplication {
	private static Logger LOG=Logger.getLogger("nashornserver");
	
	
	private static class Handler  extends AbstractHandler {
		final File javascriptFile;
		Handler(File javascriptFile) {
			this.javascriptFile=javascriptFile;
		}
		@Override
		public void handle(String target, Request baseRequest, final HttpServletRequest httpReq, final HttpServletResponse httpResp)
				throws IOException, ServletException {
				final ScriptEngineManager mgr=new  ScriptEngineManager();
				final ScriptEngine scriptEngine= mgr.getEngineByExtension("js");
				if(scriptEngine==null)
		     		{
					throw new ServletException("Cannot get a javascript engine");
		     		}	
				
			FileReader scriptReader= null;
			try {
				 scriptReader = new FileReader(this.javascriptFile);
				 scriptEngine.eval(scriptReader);
				 scriptReader.close();
				 Invocable.class.cast(scriptEngine).invokeFunction("handle",target,baseRequest,httpReq,httpResp);
			} catch(java.lang.NoSuchMethodException err) {
				throw new ServletException("file \""+this.javascriptFile+"\" is missing a method handle(target,baseRequest,req,resp)",err);
			}
				catch (final IOException e) {
				throw e;
			}  catch (final Exception e) {
				throw new ServletException(e);
			} finally {
				IOUtils.close(scriptReader);
			}
		}
		
	}
	
	@Override
	protected void fillOptions(Options options) {
		options.addOption(Option.builder("f").longOpt("script").hasArg(true).desc("javascript file.").build());
		options.addOption(Option.builder("P").longOpt("port").hasArg(true).desc("port. Default:8080").build());
		options.addOption(Option.builder("p").longOpt("path").hasArg(true).desc("servlet path . default: \"/\"").build());
		super.fillOptions(options);
	}

	@Override
	protected int execute(final CommandLine cmd) {
		if(!cmd.hasOption("f")) {
		LOG.severe("option -f missing");
		return -1;
		}
		
		final File javascriptFile = new File(cmd.getOptionValue("f"));
		if(!javascriptFile.exists()) {
			LOG.severe("Cannot open "+javascriptFile);
			return -1;
			}
		 final Server server = new Server(Integer.parseInt(cmd.getOptionValue("P","8080")));
		 try { 
		     final HashSessionIdManager idmanager = new HashSessionIdManager();
		     server.setSessionIdManager(idmanager);
			 
	        // Create the SessionHandler (wrapper) to handle the sessions
	        final HashSessionManager manager = new HashSessionManager();
	        final SessionHandler sessions = new SessionHandler(manager);
		     
	        
			 final Handler handler=new Handler(javascriptFile);
			 sessions.setHandler(handler);
			
			 final ContextHandler context = new ContextHandler();
			 context.setContextPath(cmd.getOptionValue("p", "/"));
			 context.setHandler(sessions);
			server.setHandler(context);
			server.start();
	        server.dumpStdErr();
	        server.join();
	        return 0;
		 } catch(final Exception err) {
			 err.printStackTrace();
			 return -1;
		 }
	}
	public static void main(String[] args) {
		new NashornServer().instanceMainWithExit(args);
	}

}
