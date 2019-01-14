/*
 * original site http://www.editions-delcourt.fr/bouletmaton/
 * par http://www.bouletcorp.com Boulet
 * et http://www.zanorg.com Kek 
 * Editions Delcourt
 * 
 */
package sandbox;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class BouletMatonSpriteFactory {
	  private static final String SVG="http://www.w3.org/2000/svg";
	  private static final String XLINK="http://www.w3.org/1999/xlink";
	  public static final int WIDTH =  550;
	  public static final int HEIGHT = 646;
	  
	  private static class Component {
		  final String name;
		  String opt;
		  String longopt;
		  String description;
		  int value=-1;
		  int min=1;
		  int max=-1;
		  boolean required=true;
		  void set(final CommandLine cmd) {
			  if(!cmd.hasOption(opt())) return;
			  final String s = cmd.getOptionValue(opt());
			  try {
				int v = Integer.parseInt(s);
				if(v<this.min) {
					System.err.println("Bad value (low) for option -"+opt());
					return ;
				}
				if(this.max!=-1 && v>this.max) {
					System.err.println("Bad value (up) for option -"+opt());
					return ;
				}
				this.value = v;
			} catch (final Exception e) {
				System.err.println("Bad value for option -"+opt());
			}
		  }
		  String path() { return this.name+this.value;}
		  Component(final String name) { this.name=name;}
		  Component value(int i) { this.value=i; return this;}
		  Component min(int i) { this.min=i; return this;}
		  Component max(int i) { this.max=i; return this;}
		  Component required(boolean b) { this.required=b; return this;}
		  Component opt(final String i) { this.opt=i; return this;}
		  Component longopt(final String i) { this.longopt=i; return this;}
		  Component description(final String i) { this.description=i; return this;}
		  String opt() { return opt==null?name:opt;}
		  String longopt() { return longopt==null?name:longopt;}
		  String desc() { return description==null?longopt():description;}
		  Option option() {
			return Option.builder(opt()).hasArg(true).argName(longopt().toUpperCase()).desc(desc()+"["+this.min+"-"+this.max+"]").build();  
		  	}
		  void rnd(final Random r) {
			  if(!required && r.nextInt(4)>1) {
				  this.value = -1;
			  } else {
			  this.value= this.min+ r.nextInt(1+this.max-this.min);
			  }
		}
	  }
	  
	  private final Component barbe = new Component("bar").opt("bar").max(14);
	  private final Component color = new Component("coul").opt("color").max(12);
	  private final Component skin = new Component("peau").opt("skin").max(4);
	  private final Component boucle = new Component("boucle").opt("boucle").max(11);
	  private final Component chapo = new Component("chapo").opt("hat").max(19);
	  private final Component collier = new Component("col").opt("collier").max(19);
	  private final Component head = new Component("tete").opt("collier").max(59);
	  private final Component glasses = new Component("lunette").opt("glasses").max(16);
	  private final Component eyes = new Component("ye").opt("eye").max(48);
	  private final Component bouche = new Component("bou").opt("mouse").max(62);
	  private final Component oreille = new Component("or").opt("oreille").max(7);
	  private final Component sec = new Component("src").opt("sec").max(7);
	  private final Component components[]=new Component[]{
			  barbe,color,skin,boucle,chapo,collier,head,glasses,eyes,bouche,
			  oreille,sec
	  	};
	  
	  

	  private static final String imageUrlStr(final String s) {
				return "http://www.zanorg.net/bouletmaton/assets/"+s+".png";
		}
	  
	  private static final URL imageUrl(final String s) {
			try {
				return new URL(imageUrlStr(s));
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return null;
			}
		}
	  
	  private abstract class ImageInserter {
		  
		  abstract protected void insert(String urlstr);
		  
		  private void paint(final Component c1) {
			  if(c1.value==-1) return;
			  insert(c1.path()); 
		  }
		  
		  private void paint(final Component c1,final Component c2) {
			  if(c1.value==-1) return;
			  if(c2.value==-1) return;
			  insert(c1.path()+"/"+c2.path()); 
		  }
		  
		  public void paint() {
			  
			  paint(skin,head); 
			  paint(skin,bouche); 
			  paint(skin,oreille); 
			  paint(skin,sec); 
			  paint(color,barbe); 
			  paint(boucle);  	
			  paint(collier); 
			  paint(eyes); 
			  paint(glasses); 
			  paint(chapo); 
		  }
	  }
		
	  private class Graphics2DImageInserter extends ImageInserter {
		  final Graphics2D g;
		  Graphics2DImageInserter(final Graphics2D g) {
			  this.g=g;
		  }
		 
		@Override
		protected void insert(String urlStr) {
			  if(urlStr==null) return;
			  final URL url = imageUrl(urlStr);
			  if(url==null) return;
			  try {
				  final BufferedImage img = ImageIO.read(url);
				  g.drawImage(img, 0, 0, null);
			  } catch(Exception err) {
				  err.printStackTrace();
			  }	
		}
		  
	  }
	  
	  private class SVGImageInserter extends ImageInserter {
		  final Document dom;
		  final Element root;
		  SVGImageInserter(final Document dom,final Element root) {
			  this.dom=dom;
			  this.root=root;
		  }
		 
		@Override
		protected void insert(String urlStr) {
			  if(urlStr==null) return;
			  final String url = imageUrlStr(urlStr);
			  if(url==null) return;
			  final Element img = dom.createElementNS(SVG, "image");
			  img.setAttributeNS(XLINK,"xlink:href", url);
			  img.setAttribute("x","0");
			  img.setAttribute("y","0");
			  img.setAttribute("width",String.valueOf(WIDTH));
			  img.setAttribute("height",String.valueOf(HEIGHT));
			  img.setAttribute("title",url);
			  img.setAttribute("style","opacity:0.3;");

			  this.root.appendChild(img);
			  }
		  
	  }
	  
	  public Document svg() {
		  try {
			  DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			  Document dom = dbf.newDocumentBuilder().newDocument();
			  final Element root= dom.createElementNS(SVG, "svg");
			  dom.appendChild(root);
			  root.setAttribute("width", String.valueOf(WIDTH));
			  root.setAttribute("height", String.valueOf(HEIGHT));
			  new SVGImageInserter(dom, root).paint();
			  return dom;
		  } catch(Exception err) {
			  throw new RuntimeException(err);
		  }
	  }
	  
	  public BufferedImage image() {
		  final BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		  final Graphics2D g= (Graphics2D)img.getGraphics();
		  new Graphics2DImageInserter(g).paint();
		  g.dispose();
		  return img;
	  }
	  
public BouletMatonSpriteFactory() {
	
}
public BouletMatonSpriteFactory random(int i) {
	final Random rand = (i==0?new Random(): new Random(i));
	for(final Component c : components) {
		c.rnd(rand);
	}
	return this;
	}


public static void main(String[] args) {
	new Main().instanceMainWithExit(args);
}

private static final class Main extends AbstractApplication{
	final BouletMatonSpriteFactory factory = new BouletMatonSpriteFactory();

	@Override
	protected void fillOptions(final Options options) {
		for(final Component c : factory.components) {
			options.addOption(c.option());
		}
		
		Option.builder("f").longOpt("format").hasArg(true).desc("output format svg or png").build();
		options.addOption(Option.builder("o").longOpt("out").hasArg(true).desc("output filename").build());
		options.addOption(Option.builder("rnd").longOpt("rando").hasArg(true).desc("build random using seed").build());
		super.fillOptions(options);
		}
	private int num(final String s) {
		try { return Integer.parseInt(s);} catch(NumberFormatException err) { return -1;}
	}
	
	@Override
	protected int execute(final CommandLine cmd) {
		try {
			
			
			if(cmd.hasOption("rnd")) factory.random(num(cmd.getOptionValue("rnd")));
			for(final Component c : factory.components) {
				c.set(cmd);
			}
			
			String format="svg";
			if(cmd.hasOption("f")) format=cmd.getOptionValue("f");
			if(format.equals("svg")) {
				final TransformerFactory tff = TransformerFactory.newInstance();
				final Transformer tr=tff.newTransformer();
				final StreamResult result;
				result= (cmd.hasOption("o")?
						new StreamResult(new File(cmd.getOptionValue("o"))):
						new StreamResult(System.out)
						);
				
				tr.transform(new DOMSource(factory.svg()), result);
				return 0;
			} else {
				if(!cmd.hasOption("o")) {
					System.err.println("Cannot save as PNG because option -o undefined");
					return -1;
				}
				ImageIO.write(factory.image(), "png", new File(cmd.getOptionValue("o")));
				return 0;
				}
		} catch(Exception err) {
			err.printStackTrace();
			return -1;
		}
	}
}

}
