package sandbox.tools.nashornserver;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;

import com.beust.jcommander.Parameter;

import sandbox.Logger;
import sandbox.Launcher;
import sandbox.io.IOUtils;
import sandbox.tools.central.ProgramDescriptor;

public class NashornServer extends Launcher {
	private static final Logger LOG=Logger.builder(NashornServer.class).build();
	
    @Parameter(names={"-f","--script"},description="javascript file",required = true)
	private Path javascriptFile=null;
    @Parameter(names={"-P","--port"},description="server port")
	private int serverPort = 8080;
    @Parameter(names={"-d","--path"},description="servlet path")
	private String servletPath="/";
	
	private static class Handler  extends AbstractHandler {
		final Path javascriptFile;
		Handler(Path javascriptFile) {
			this.javascriptFile=javascriptFile;
			}
		
		 
		@Override
		public void handle(String target, Request baseRequest, final HttpServletRequest httpReq, final HttpServletResponse httpResp)
				throws IOException, ServletException {
			final ScriptEngine scriptEngine;
			try {
				final ScriptEngineManager mgr=new  ScriptEngineManager();
				scriptEngine= mgr.getEngineByExtension("js");
				if(scriptEngine==null)
		     		{
					LOG.error("Cannot get JS engine");
					throw new ServletException("Cannot get a javascript engine");
		     		}	
			
			try (Reader scriptReader = Files.newBufferedReader(this.javascriptFile)) {
				 scriptEngine.eval(scriptReader);
				 Invocable.class.cast(scriptEngine).invokeFunction("handle",target,baseRequest,httpReq,httpResp);
				}
			} catch(java.lang.NoSuchMethodException e) {
				throw new ServletException("file \""+this.javascriptFile+"\" is missing a method handle(target,baseRequest,req,resp)",e);
				}
				catch (final IOException e) {
				throw e;
			}  catch (final Throwable e) {
				throw new ServletException(e);
			}
		}
		
	}
	@Override
	public int doWork(List<String> args) {
		IOUtils.assertFileExists(this.javascriptFile);
		
		 final Server server = new Server(this.serverPort);
		 try { 
			 
		     final HashSessionIdManager idmanager = new HashSessionIdManager();
		     server.setSessionIdManager(idmanager);
			 
	        // Create the SessionHandler (wrapper) to handle the sessions
	        final HashSessionManager manager = new HashSessionManager();
	        final SessionHandler sessions = new SessionHandler(manager);
		     
	        
			 final Handler handler=new Handler(javascriptFile);
			 sessions.setHandler(handler);			
			 final ContextHandler context = new ContextHandler();
			 context.setContextPath(this.servletPath);
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

    public static ProgramDescriptor getProgramDescriptor() {
    	return new ProgramDescriptor() {
    		@Override
    		public String getName() {
    			return "nashornserver";
    			}
    		};
    	}	
	
}
