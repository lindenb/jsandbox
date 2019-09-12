package sandbox.http;

import java.util.List;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;

public abstract class AbstractJettyAppication extends Launcher {
	private static final Logger LOG = Logger.builder(AbstractJettyAppication.class).build();
	@Parameter(names={"--port"},description="server port. default 8080.")
	private int port = 8080;

	private  Server server = null;
	
	protected Server createNewServer(final int port) {
		return  new Server(this.port);
		}
	
	protected abstract Handler createHandler();

	protected String getContexPath() {
		return "/"+this.getClass().getSimpleName().toLowerCase();
	}
	
	@Override
	public int doWork(final List<String> args) {
		try {
			this.server = createNewServer(this.port);
	        final HashSessionManager manager = new HashSessionManager();
	        final SessionHandler sessions = new SessionHandler(manager);
			final ContextHandler context = new ContextHandler();
			context.setContextPath(getContexPath());
			context.setHandler(sessions);
			sessions.setHandler(createHandler());
			
			server.setHandler(context);
			LOG.info("Starting server on port "+this.port+" "+getContexPath());
			server.start();
	        server.dumpStdErr();
	        server.join();
			return 0;
			}
		catch(final Throwable err) {
			return -1;
			}
		finally
			{
			this.server=null;
			}
		
		}
}
