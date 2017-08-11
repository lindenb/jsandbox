/**

Les donnees de bouletmaton sont isssue de  http://www.zanorg.net/bouletmaton/
(c) Boulet et Kek / bouletcorp.com  et zanorg.com

http://www.zanorg.net/bouletmaton/js/boulet.js
**/
package sandbox;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Callback;


public class BouletMaton extends Application {
	  private static final int WIDTH =  550;
	  private static final int HEIGHT = 646;
	  
	  //https://stackoverflow.com/questions/31172090
	  
	  private static class Part {
		 // cor,tete,bou,ye,so,coi,nez,ore, col,boucle,divers,lunette,sec,bar,chapo,tache;
		private final String name;
		private Image icon = null;
		Part(final String name)
			{
			this.name= name;
			}
		public String getIconUrl() {
			return "http://www.zanorg.net/bouletmaton/assets/bt_"+name+".png";
			}
		public Image getIconImage()
			{
			if(icon==null) {
				icon = new Image(getIconUrl());
				}
			return icon;
			}
		@Override
		public String toString() {
			return name;
			}
	  	}
	  
	  private final List<Part> pars = new ArrayList<>();

	 public  BouletMaton()
	  	{
		Part p = new Part("cor");
		pars.add(p);
	  	}
	  
	  public static class Person
	  	{
		
		public Person()
			{
			
			}
	  	}


		private static final URL imageUrl(String s) {
			try {
				return new URL("http://www.zanorg.net/bouletmaton/assets/"+s+".png");
			} catch (final MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
	
		private static class PersonFrame extends BorderPane {
			private Canvas drawingArea =null;
			
			
			PersonFrame() {
				super();
				
				
				}
			
			
		}
	Callback<ListView<Part>,ListCell<Part>> cellFactory = R->
		{
		ListCell<Part> LC= new ListCell<Part>()
				{
			 	@Override
	            protected void updateItem(Part p, boolean empty) {
			 		 super.updateItem(p, empty);
			 		 if (p == null || empty) {
		                    setGraphic(null);
		                } else {
		                	 setGraphic(new ImageView(p.getIconImage()));
		                }
			 		}
				};
		return LC;
		};
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		final BorderPane root  = new BorderPane();
		primaryStage.setTitle("BouletMaton");
		MenuBar menuBar =new MenuBar();
		Menu menu = new Menu("File");
		
		MenuItem i = new MenuItem("hhh");
		i.setOnAction(AE->{});
		menu.getItems().add(i);
		menuBar.getMenus().add(menu);
		root.setTop(menuBar);
		
		Canvas drawingArea = new Canvas(WIDTH, HEIGHT);
		root.setCenter( drawingArea);
		
		FlowPane bottom = new FlowPane();
		ComboBox<Part> combo = new ComboBox<>(FXCollections.observableArrayList(this.pars));
		
		combo.setCellFactory(cellFactory);
		
		bottom.getChildren().add(combo);
		Button left = new Button("<-");
		bottom.getChildren().add(left);
		
		Button right = new Button("->");
		bottom.getChildren().add(right);
		
		root.setBottom(bottom);
        primaryStage.setScene(new Scene(root));
        primaryStage.sizeToScene();
        primaryStage.show();
        
		}
		
	public static void main(String[] args) throws Exception {
	System.setProperty("http.proxyHost","cache.ha.univ-nantes.fr");
	System.setProperty("http.proxyPort","3128");
	launch(args);
	}
}
