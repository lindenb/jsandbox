/**

Les donnees de bouletmaton sont isssue de  http://www.zanorg.net/bouletmaton/
(c) Boulet et Kek / bouletcorp.com  et zanorg.com

http://www.zanorg.net/bouletmaton/js/boulet.js
**/
package sandbox;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * http://www.zanorg.net/bouletmaton/js/interface.js
 * 
 *
 */
public class BouletMaton extends Application {
	  private static final Logger LOG = Logger.builder(BouletMaton.class).build();
	  private static final String BASE="http://www.zanorg.net/bouletmaton/";
	  private static final int WIDTH =  550;
	  private static final int HEIGHT = 646;
	  private static final int MAX_COLORS=12;
	  private static final int ICON_SIZE=68;
	  //https://stackoverflow.com/questions/31172090
	  private static class CachedImage
	  	{
		String url;
		Image img;
		long when;
		void touch() { this.when = System.currentTimeMillis();}
	  	}
	  private final Map<String,CachedImage> imageCache = new HashMap<>();
	  
	  private Image getImageByUrl(final String url) 
	  	{
		if( url ==null) return null;
		CachedImage img = this.imageCache.get(url);
		if(img!=null) { img.touch(); return img.img;}
		if(this.imageCache.size()>50) {
			CachedImage ancient = null;
			for(String u:this.imageCache.keySet()) {
				img = this.imageCache.get(u);
				if(ancient==null || ancient.when > img.when) {
					ancient=img;
					}
				}
			this.imageCache.remove(ancient.url);
			}
		LOG.debug(url);
		img = new CachedImage();
		img.url = url;
		img.touch();
		img.img = new  Image(url);
		this.imageCache.put(url, img);
		return img.img;
	  	}
	  private class Part {
		private final String name;
		private Image icon = null; 
		private ToggleButton button = null;
		private int num= 1;
		private int max=12;
		private Function<Part, String> imageUrlProvider = P-> BASE+"assets/"+P.name+P.num+".png";
		private String cacheURL=null;
		private Image cacheImage=null;
		private boolean show = true;
		
		Part(final String name)
			{
			this.name= name;
			}
		int getMax() { return this.max;}
		Part setNum(int v) { this.num = v; return this;}
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
				icon = BouletMaton.this.getImageByUrl(getIconUrl());
				}
			return icon;
			}
		public ToggleButton getButton() 
			{
			if(this.button==null) {
				final Image img = getIconImage();
				this.button = new ToggleButton("",new ImageView(img));
				this.button.setTooltip(new Tooltip(this.name));
				this.button.setUserData(Part.this);
				this.button.setPrefSize(ICON_SIZE,ICON_SIZE);
				this.button.getStylesheets().add(".button:focused{-fx-background-color: darkgray;}");
				this.button.setOnAction(AE->{
					if(BouletMaton.this.showHidePart.isSelected()!=this.show) {
						BouletMaton.this.showHidePart.setSelected(this.show);
						}
					});
				
				}
			return this.button;
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
			if(!this.show) return null;
			final String url = this.getImageUrl();
			if(url==null) {
				this.cacheImage=null;
				this.cacheURL=null;
				}
			else if(!url.equals(this.cacheURL))
				{
				this.cacheURL = url;
				
				this.cacheImage= BouletMaton.this.getImageByUrl(url);
				}
			return this.cacheImage;
			}
	  	}
	
	  
	private final List<Part> pars = new ArrayList<>();
	private ToggleGroup togglePart = null;
	Canvas drawingArea;
	CheckBox showHidePart;
	
	 public  BouletMaton()
	  	{
		 	final Part _coul1 = new Part("coul1").setMax(4).setUrlProvider(P->null);
			final Part _coul2 = new Part("coul2").setMax(MAX_COLORS).setUrlProvider(P->null);;
			final Part _coul3 = new Part("coul3").setMax(MAX_COLORS).setUrlProvider(P->null);;
			
			final Function<Part,String> poColor = P-> BASE + "assets/peau" + _coul1.num + "/" + P.name + P.num + ".png";
			final Function<Part,String> cheColor = P-> BASE + "assets/coul" + _coul2.num + "/" + P.name + P.num + ".png";
			final Function<Part,String> barColor = P-> BASE + "assets/coul" + _coul3.num + "/" + P.name + P.num + ".png";
				
		 
			//pars.add(new Part("po").setMax(14));
			pars.add(new Part("cor").setMax(21).setUrlProvider(poColor));
			pars.add(new Part("col").setMax(11).setNum(11));
			pars.add(new Part("tete").setMax(54).setUrlProvider(poColor));
			pars.add(new Part("coi").setMax(33).setUrlProvider(cheColor));
			pars.add(new Part("bou").setMax(62).setUrlProvider(poColor));
			pars.add(new Part("ye").setMax(48));
			pars.add(new Part("bar").setMax(14).setNum(14).setUrlProvider(barColor));
			pars.add(new Part("so").setMax(21));
			pars.add(new Part("lunette").setMax(16).setNum(16));
			pars.add(new Part("nez").setMax(26).setUrlProvider(poColor));
			pars.add(new Part("tache").setMax(4).setNum(5));
			pars.add(new Part("ore").setMax(7).setUrlProvider(poColor));
			pars.add(new Part("boucle").setMax(10).setNum(10));
			pars.add(new Part("chapo").setMax(19).setNum(19));
			pars.add(new Part("sec").setMax(5).setNum(5));
			pars.add(new Part("divers").setMax(13).setNum(13).setUrlProvider(poColor));
					
			pars.add(_coul1);
			pars.add(_coul2);
			pars.add(_coul3);
	  	}
	  

	
	
	@Override
	public void start(final Stage primaryStage) throws Exception {
		final BorderPane contentPane  = new BorderPane();
		primaryStage.setTitle("BouletMaton");
		final MenuBar menuBar =new MenuBar();
		Menu menu = new Menu("File");
		
		MenuItem i = new MenuItem("Save Image As...");
		i.setOnAction(AE->doMenuSaveImageAs(primaryStage));
		menu.getItems().add(i);
		i = new MenuItem("Save Svg As...");
		i.setOnAction(AE->doMenuSaveSvgAs(primaryStage));
		menu.getItems().add(i);
		i = new MenuItem("Read Svg As ...");
		i.setOnAction(AE->doMenuReadSvg(primaryStage));
		menu.getItems().add(i);
		menu.getItems().add(new SeparatorMenuItem());
		i = new MenuItem("Random...");
		i.setOnAction(AE->doRandom());
		menu.getItems().add(i);
		menu.getItems().add(new SeparatorMenuItem());
		i = new MenuItem("Quit...");
		i.setOnAction(AE->Platform.exit());
		menu.getItems().add(i);
		
		menuBar.getMenus().add(menu);
		contentPane.setTop(menuBar);
		
		this.drawingArea = new Canvas(WIDTH, HEIGHT);
		contentPane.setCenter(new ScrollPane(drawingArea));
		
		final BorderPane rightPane = new BorderPane();
		contentPane.setRight( rightPane );
		
		this.togglePart = new ToggleGroup();
		for(final Part part:this.pars) {
			part.getButton().setToggleGroup(this.togglePart);
			}
		this.pars.get(0).getButton().setSelected(true);
		
		final TilePane gridPane = new TilePane();
		gridPane.setPrefColumns(3);
		for(final Part part:this.pars) {
			gridPane.getChildren().add(part.getButton());
			}
		rightPane.setCenter(gridPane);
		
		FlowPane top = new FlowPane();
		top.setAlignment(Pos.BASELINE_CENTER);
		rightPane.setTop(top);
		Button left = new Button("\u25C0");
		left.setPrefSize(ICON_SIZE,ICON_SIZE);
		left.setOnAction(AE->choice(-1));
		top.getChildren().add(left);
		
		Button right = new Button("\u25B6");
		right.setPrefSize(ICON_SIZE,ICON_SIZE);
		right.setOnAction(AE->choice(1));
		top.getChildren().add(right);
		
		this.showHidePart = new CheckBox("Show");
		this.showHidePart.setSelected(true);
		top.getChildren().add(this.showHidePart);
		this.showHidePart.setOnAction(AE->doShowHide());
		
		contentPane.setBottom(new Label("Data from www.zanorg.net/bouletmaton/"));
		
        primaryStage.setScene(new Scene(contentPane));
        primaryStage.sizeToScene();
       
        primaryStage.setOnShowing(AE->paintDrawingArea());
        primaryStage.setOnShown(AE->paintDrawingArea());
        primaryStage.show();
		}
	
	private void doShowHide()
		{
		final Toggle toggle = this.togglePart.getSelectedToggle();
		if(toggle==null) return;
		final Part p = (Part)(((ToggleButton)toggle).getUserData());
		if(p==null) return;
		boolean show = this.showHidePart.isSelected();
		if(p.show == show) return;
		p.show=show;
		paintDrawingArea();
		}
	
	void choice(final int shift)
		{
		final Toggle toggle = this.togglePart.getSelectedToggle();
		if(toggle==null) return;
		final Part p = (Part)(((ToggleButton)toggle).getUserData());
		if(p==null) return;
		LOG.debug("changing "+p.name+" was "+ p.num+" max="+p.max);
		int n= p.num + shift;
		if(n > p.getMax()) {
			n=1;
			}
		else if(n<1)
			{
			n = p.getMax();
			}
		p.setNum(n);
		LOG.debug(p.name+" "+p.num);
		paintDrawingArea();
		}
	
	void paintDrawingArea()
		{
		LOG.debug("Drawing");
		GraphicsContext g=this.drawingArea.getGraphicsContext2D();
		g.clearRect(0, 0, this.drawingArea.getWidth(), this.drawingArea.getHeight());
		for(final Part p:this.pars)
			{
			Image img = p.getImage();
			if(img==null) continue;
			g.drawImage(img, 0, 0);
			}
		LOG.debug("Drawing end");
		}
	
	private void doMenuSaveImageAs(Stage primaryStage)
		{
		 FileChooser fileChooser = new FileChooser();
         
         //Set extension filter
         FileChooser.ExtensionFilter extFilter = 
                 new FileChooser.ExtensionFilter("png files (*.png)", "*.png");
         fileChooser.getExtensionFilters().add(extFilter);
        
         //Show save file dialog
         File file = fileChooser.showSaveDialog(primaryStage);
          
         if(file != null){
             try {
            	 SnapshotParameters snapshotParameters = new SnapshotParameters();
            	 snapshotParameters.setFill(Color.TRANSPARENT);
            	 
                 WritableImage writableImage = new WritableImage(BouletMaton.WIDTH, BouletMaton.HEIGHT);
                 this.drawingArea.snapshot(snapshotParameters, writableImage);
                 RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                 ImageIO.write(renderedImage, "png", file);
             } catch (IOException ex) {
            	 LOG.error(ex);
             }
         }
		}
	
	
	private void doMenuSaveSvgAs(Stage primaryStage)
		{
		 FileChooser fileChooser = new FileChooser();
	     
	     //Set extension filter
	     FileChooser.ExtensionFilter extFilter = 
	             new FileChooser.ExtensionFilter("svg files (*.svg)", "*.svg");
	     fileChooser.getExtensionFilters().add(extFilter);
	    
	     //Show save file dialog
	     File file = fileChooser.showSaveDialog(primaryStage);
	      
	     if(file != null){
	         try {
	        	 PrintWriter pw=new PrintWriter(file);
	             javax.xml.stream.XMLStreamWriter w= XMLOutputFactory.newFactory().createXMLStreamWriter(pw);
	             w.writeStartElement("svg");
	             w.writeDefaultNamespace("http://www.w3.org/2000/svg");
	             w.writeNamespace("xlink", "http://www.w3.org/1999/xlink");
	             w.writeNamespace("bouletmaton", BASE);
	             w.writeAttribute("width",String.valueOf(WIDTH)+"px");
	             w.writeAttribute("height",String.valueOf(HEIGHT)+"px");
	             w.writeAttribute("version","1.1");
	             w.writeStartElement("g");
	             for(final Part p: this.pars) {
	            	 String url = p.getImageUrl();
	            	 if( (url==null || !p.show) && !p.name.startsWith("coul")) continue;
	            	 w.writeEmptyElement("image");
	            	 w.writeAttribute("x","0");
	            	 w.writeAttribute("y","0");
	            	 w.writeAttribute("width",String.valueOf(WIDTH)+"px");
		             w.writeAttribute("height",String.valueOf(HEIGHT)+"px");
		             if(url!=null) {
		            	 w.writeAttribute("xlink:href", url);
		             }
	            	 w.writeAttribute("bouletmaton:name", p.name);
	            	 w.writeAttribute("bouletmaton:num", String.valueOf(p.num));
	             	}
	             w.writeEndElement();//g
	             w.writeEndElement();
	             w.flush();
	             w.close();
	             pw.close();
	         } catch (final Exception ex) {
	            LOG.error(ex);
	         }
	     }
		}
	
	
	
	private void doMenuReadSvg(Stage primaryStage)
		{
		 FileChooser fileChooser = new FileChooser();
         
         //Set extension filter
         FileChooser.ExtensionFilter extFilter = 
                 new FileChooser.ExtensionFilter("svg files (*.svg)", "*.svg");
         fileChooser.getExtensionFilters().add(extFilter);
        
         //Show save file dialog
         File file = fileChooser.showOpenDialog(primaryStage);
          
         if(file != null){
             try {
            	 Map<String,Integer> name2num=new HashMap<>();
            	 FileReader fr =new FileReader(file);
                 XMLEventReader r= XMLInputFactory.newFactory().createXMLEventReader(fr);
                 while(r.hasNext())
                 	{
                	XMLEvent evt = r.nextEvent();
                	if(!evt.isStartElement()) continue;
                	StartElement start = evt.asStartElement();
                	if(!start.getName().getLocalPart().equals("image")) continue;
                 	Attribute name = start.getAttributeByName(new QName(BASE, "name"));
                 	if(name==null) {
                 		LOG.warning("No  boulet:name in image");
                 		continue;
                 	}
                 	Attribute num = start.getAttributeByName(new QName(BASE, "num"));
                 	if(num==null) {
                 		LOG.warning("No  boulet:num in image");
                 		continue;
                 	}
                 	name2num.put(name.getValue(), Integer.parseInt(num.getValue()));
                 	}
                 r.close();
                 fr.close();
                for(final Part p:this.pars)
                	{
                	if(!name2num.containsKey(p.name)) {
                		LOG.debug("unsetting "+p.name);
                		p.show=false;
                		}
                	else
                		{
                		LOG.debug("setting "+p.name+" to "+name2num.get(p.name));
                		p.num = name2num.get(p.name);
                		p.show=true;
                		}
                	}
                 paintDrawingArea();
             } catch (final Exception ex) {
                LOG.error(ex);
             }
         }
		}
	
	private void doRandom() {
		final Random rand = new Random(System.currentTimeMillis());
		for(final Part part: this.pars) {
			part.num = 1+ rand.nextInt(part.max);
			}
		paintDrawingArea();
		}
	
	public static void main(final String[] args) throws Exception {
	launch(args);
	}
}
