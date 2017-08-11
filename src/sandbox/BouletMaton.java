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
import java.util.function.Function;

import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * http://www.zanorg.net/bouletmaton/js/interface.js
 * 
 *
 */
public class BouletMaton extends Application {
	  private static final String BASE="http://www.zanorg.net/bouletmaton/";
	  private static final int WIDTH =  550;
	  private static final int HEIGHT = 646;
	  private static final int MAX_COLORS=12;
	  private static final int ICON_SIZE=68;
	  //https://stackoverflow.com/questions/31172090
	  
	  private static class Part {
		 // cor,tete,bou,ye,so,coi,nez,ore, col,boucle,divers,lunette,sec,bar,chapo,tache;
		private final String name;
		private Image icon = null;
		private SimpleIntegerProperty num=new SimpleIntegerProperty(1);
		private int max=12;
		private Function<Part, String> imageUrlProvider = P-> BASE+"assets/"+P.name+P.num.intValue()+".png";
		private String cacheURL=null;
		private Image cacheImage=null;
		
		Part(final String name)
			{
			this.name= name;
			}
		int getMax() { return this.max;}
		Part setNum(int v) { this.num.set(v); return this;}
		Part setMax(int v) { this.max=v; return this;}
		Part setUrlProvider( Function<Part, String> fun) {
			this.imageUrlProvider=fun;
			return this;
			}
		public String getIconUrl() {
			return BASE+"assets/bt_"+name+".png";
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
		public String getImageUrl() {
			return this.imageUrlProvider.apply(this);
			}
		public Image getImage()
			{
			final String url = this.getImageUrl();
			if(url==null) {
				this.cacheImage=null;
				this.cacheURL=null;
				}
			else if(!url.equals(this.cacheURL))
				{
				this.cacheURL = url;
				System.err.println(url);
				this.cacheImage=new Image(url);
				}
			return this.cacheImage;
			}
	  	}
	
	  
	private final List<Part> pars = new ArrayList<>();
	ComboBox<Part> combo = null;
	Canvas drawingArea;
	
	 public  BouletMaton()
	  	{
		 	final Part _coul1 = new Part("coul1").setMax(4).setUrlProvider(P->null);
			final Part _coul2 = new Part("coul2").setMax(MAX_COLORS).setUrlProvider(P->null);;
			final Part _coul3 = new Part("coul3").setMax(MAX_COLORS).setUrlProvider(P->null);;
			
			final Function<Part,String> poColor = P-> BASE + "assets/peau" + _coul1.num.intValue() + "/" + P.name + P.num.intValue() + ".png";
			final Function<Part,String> cheColor = P-> BASE + "assets/coul" + _coul2.num.intValue() + "/" + P.name + P.num.intValue() + ".png";
			final Function<Part,String> barColor = P-> BASE + "assets/coul" + _coul3.num.intValue() + "/" + P.name + P.num.intValue() + ".png";
				
		 
			pars.add(new Part("po").setMax(14));
			pars.add(new Part("cor").setMax(21).setUrlProvider(poColor));
			pars.add(new Part("col").setMax(11).setUrlProvider(poColor));
			pars.add(new Part("tete").setMax(54).setUrlProvider(poColor));
			pars.add(new Part("coi").setMax(33).setUrlProvider(cheColor));
			pars.add(new Part("bou").setMax(62).setUrlProvider(poColor));
			pars.add(new Part("ye").setMax(48));
			pars.add(new Part("bar").setMax(14).setUrlProvider(barColor));
			pars.add(new Part("so").setMax(21));
			pars.add(new Part("lunette").setMax(16));
			pars.add(new Part("nez").setMax(26).setUrlProvider(poColor));
			pars.add(new Part("tache").setMax(4));
			pars.add(new Part("ore").setMax(7).setUrlProvider(poColor));
			pars.add(new Part("boucle").setMax(10));
			pars.add(new Part("chapo").setMax(19));
			pars.add(new Part("sec").setMax(5));
			pars.add(new Part("divers").setMax(13).setUrlProvider(poColor));
					
			pars.add(_coul1);
			pars.add(_coul2);
			pars.add(_coul3);
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
			 		 setText("");
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
		
		drawingArea = new Canvas(WIDTH, HEIGHT);
		root.setCenter( drawingArea);
		
		FlowPane bottom = new FlowPane();
		this.combo = new ComboBox<>(FXCollections.observableArrayList(this.pars));
		
		combo.setCellFactory(cellFactory);
		combo.setButtonCell(cellFactory.call(null));
		combo.getSelectionModel().select(0);
		combo.setPrefSize(ICON_SIZE,ICON_SIZE);
		
		bottom.getChildren().add(combo);
		Button left = new Button("<-");
		left.setPrefSize(ICON_SIZE,ICON_SIZE);
		left.setOnAction(AE->choice(-1));
		bottom.getChildren().add(left);
		
		Button right = new Button("->");
		right.setPrefSize(ICON_SIZE,ICON_SIZE);
		right.setOnAction(AE->choice(1));
		bottom.getChildren().add(right);
		
		
		
		root.setBottom(bottom);
        primaryStage.setScene(new Scene(root));
        primaryStage.sizeToScene();
        primaryStage.show();
        primaryStage.setOnShowing(AE->paintDrawingArea());
		}
	
	void choice(int shift)
		{
		Part p = this.combo.getSelectionModel().getSelectedItem();
		if(p==null) return;
		
		int n= p.num.get() + shift;
		if(n > p.getMax()) {
			n=1;
			}
		else if(n<1)
			{
			n = p.getMax();
			}
		p.setNum(n);
		System.err.println(p.name+" "+p.num);
		paintDrawingArea();
		}
	void paintDrawingArea()
		{
		GraphicsContext g=this.drawingArea.getGraphicsContext2D();
		g.setFill(Color.WHITE);
		g.fillRect(0, 0, this.drawingArea.getWidth(), this.drawingArea.getHeight());
		for(final Part p:this.pars)
			{
			Image img = p.getImage();
			if(img==null) continue;
			g.drawImage(img, 0, 0);
			}
		System.err.println("Drawing end");
		}
		
	public static void main(String[] args) throws Exception {
	launch(args);
	}
}
