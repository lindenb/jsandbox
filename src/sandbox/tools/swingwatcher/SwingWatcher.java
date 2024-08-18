package sandbox.tools.swingwatcher;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import sandbox.Launcher;
import sandbox.io.IOUtils;
import sandbox.swing.xml.SwingXmlContext;
import sandbox.tools.central.ProgramDescriptor;
import sandbox.xml.minidom.Element;
import sandbox.xml.minidom.MiniDomReader;

public class SwingWatcher extends Launcher{
	private File file;
	private Timer timer = new Timer();
	private TimerTask mytask;
	private long lastModif=0L;
	private transient java.awt.Window current_window=null;
	
	private void closeWindow(final java.awt.Window newWin) {
		try {
			SwingUtilities.invokeAndWait(()->{
			if(this.current_window!=null) {
				this.current_window.setVisible(false);
				this.current_window.dispose();
				current_window=null;
				}
			if(newWin!=null) {
				current_window=newWin;
				final Dimension screen  = Toolkit.getDefaultToolkit().getScreenSize();
				current_window.setBounds(50, 50, screen.width-100, screen.height-100);
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
		MiniDomReader xmlReader=new MiniDomReader();
		Element root;
		try {
			root=xmlReader.parsePath(this.file.toPath());
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
		if(!ctx.getTopWindow().isPresent()) return;
		closeWindow( ctx.getTopWindow().get());
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
