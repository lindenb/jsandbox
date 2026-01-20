package sandbox.tools.swingwatcher;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import sandbox.Launcher;
import sandbox.io.IOUtils;
import sandbox.swing.xml.SwingXmlContext;
import sandbox.tools.central.ProgramDescriptor;

public class SwingWatcher extends Launcher {
	private File file;
	private Timer timer = new Timer();
	private TimerTask mytask;
	private long lastModif=0L;
	private transient java.awt.Window current_window=null;
	private transient java.awt.Rectangle bounds=null;
	private void closeWindow(final java.awt.Window newWin) {
		try {
			SwingUtilities.invokeAndWait(()->{
			if(this.current_window!=null) {
				this.bounds = this.current_window.getBounds();
				this.current_window.setVisible(false);
				this.current_window.dispose();
				current_window=null;
				}
			if(newWin!=null) {
				current_window=newWin;
				if(this.bounds==null) {
					final Dimension screen  = Toolkit.getDefaultToolkit().getScreenSize();
					current_window.setBounds(50, 50, screen.width-100, screen.height-100);
					}
				else
					{
					current_window.setBounds(this.bounds);
					}
				current_window.setVisible(true);
				}
			});
		} catch(Throwable err) {
			getLogger().error(err);
		}
	}
	
	private void reloadXml() {
		if(this.file==null || !this.file.exists()) {
			closeWindow(null);
			return;
			}
		long last= this.file.lastModified();
		if(this.lastModif>= last) return;
		this.lastModif =last;
		System.err.println("Reload... "+this.file+" "+new Date());
		Element root;
		try {
			final DocumentBuilderFactory dbf=DocumentBuilderFactory.newDefaultNSInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db=dbf.newDocumentBuilder();
			final Document dom = db.parse(this.file);
			root=dom.getDocumentElement();
			}
		catch(final Throwable err)
			{
			closeWindow(null);
			err.printStackTrace();
			return;
			}
		if(root==null) {
			closeWindow(null);
			System.err.println("no root int "+this.file);
			return;
			}
		closeWindow(null);
		SwingXmlContext ctx;
		try {
			ctx = SwingXmlContext.of(root);
			}
		catch(final Throwable err)
			{
			closeWindow(null);
			err.printStackTrace();
			return;
			}
		final Optional<Object> top= ctx.getInstance();
		if(!top.isPresent()) {
			closeWindow(null);
			System.err.println("no object was parsed");
			return;
			}
		if(!(top.get() instanceof Window)) {
			closeWindow(null);
			System.err.println("root in xml is not an instance of "+Window.class.getName());
			return;
			}
		closeWindow( Window.class.cast(top.get()));
		}
	
	@Override
	public int doWork(List<String> args) {
		try {
			this.file = new File(oneAndOnlyOneFile(args));
			IOUtils.assertFileExists(this.file);
			
			
			this.mytask = new TimerTask() {
				@Override
				public void run() {
						reloadXml();
						
					};
				};
			
			this.timer.scheduleAtFixedRate(mytask,1000,2500);
			Runtime.getRuntime().addShutdownHook(new Thread(()->{closeWindow(null);}));
			}
		catch(Throwable err) {
			getLogger().error(err);
			return -1;
			}
		return 0;
		}
	
	public static ProgramDescriptor getProgramDescriptor() {
		return new ProgramDescriptor() {
			@Override
			public String getName() {
				return SwingWatcher.class.getSimpleName().toLowerCase();
				}
			};
		}
	
	public static void main(String[] args) {
		new SwingWatcher().instanceMain(args);
		}

}
