package sandbox;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class JFXWatcher extends Application {
	private Stage stage;
	private File file;
	private Timer timer = new Timer();
	private TimerTask mytask;
	private long lastModif=0L;
	
	@Override
	public void start(final Stage primaryStage) throws Exception {
		this.file=new File(this.getParameters().getRaw().get(0));
		this.stage=primaryStage;
		this.stage.show();
		
		mytask = new TimerTask() {
			@Override
			public void run() {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						reloadXml();
					}
				});
			}
		};
		
		this.timer.scheduleAtFixedRate(mytask,1000,2500);
		
		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				timer.cancel();
				
			}
		});
		}
	private void reloadXml() {
		if(this.file==null || !this.file.exists()) return;
		long last= this.file.lastModified();
		if(this.lastModif>= last) return;
		this.lastModif =last;
		System.err.println("Reload... "+this.file+" "+new Date());
		final URL url;
		try {
			url =this.file.toURI().toURL();
			}
		catch(final MalformedURLException err)
			{
			err.printStackTrace();
			return;
			}
		try 
			{
			FXMLLoader loader=new FXMLLoader(url);
			Parent root=(Parent)loader.load();
			if(root==null) return;
			stage.setScene(new Scene(root));
			}
		catch(Exception err)
			{
			err.printStackTrace();
			return;
			}
		}
	public static void main( String[] args) {
		if(args.length!=1) {
			System.err.println("expected one XML arguments");
			System.exit(-1);
		}
	launch(args);
	}
	
}
