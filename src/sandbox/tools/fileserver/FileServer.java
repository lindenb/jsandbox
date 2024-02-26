package sandbox.tools.fileserver;

import java.io.File;
import java.util.List;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;


import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;

public class FileServer extends Launcher
	{
	private static final Logger LOG = Logger.builder(FileServer.class).build();
	@Parameter(names={"-P","--port"},description="Port")
	private int port = 8080;
	@Parameter(names={"-D","--base"},description="Base directory")
	private File baseDir = null;
	
	@Override
	public int doWork(final List<String> args) {
		try {
			if(this.baseDir==null)
				{
				LOG.warning("using ucrren tdirecotry");
				this.baseDir = new File(System.getProperty("user.dir","."));
				}
		
			final Server server = new Server(this.port);
			final ResourceHandler resource_handler = new ResourceHandler();
			resource_handler.setDirectoriesListed(true);
        	resource_handler.setWelcomeFiles(new String[]{"README.md","README.txt", "index.html",});
        	resource_handler.setResourceBase(this.baseDir.getPath());
			
		    final HandlerList handlers = new HandlerList();
       	 	handlers.setHandlers(new Handler[] { resource_handler, new DefaultHandler() });
        	server.setHandler(handlers);
        	LOG.info("http://localhost:"+this.port+"/");
			server.start();
			server.join();
			return 0;
			}
		catch(final Throwable err) {
			LOG.error(err);
			return -1;
			}
		finally
			{
			
			}
	}
	
	public static void main(final String[] args) {
		new FileServer().instanceMainWithExit(args);
	}
	
}
