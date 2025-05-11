package sandbox.tools.comicsbuilder.v1;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Dimension2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.script.ScriptEngine;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import sandbox.Launcher;
import sandbox.awt.AffineTransformFactory;
import sandbox.io.IOUtils;
import sandbox.nashorn.NashornParameters;
import sandbox.nashorn.NashornUtils;
import sandbox.svg.SVG;
import sandbox.svg.SVGUtils;
import sandbox.tools.central.ProgramDescriptor;
import sandbox.util.function.FunctionalMap;
import sandbox.xml.DOMHelper;


public class ComicsBuilderV1 extends Launcher {
	
    @ParametersDelegate
    private NashornParameters nashorParams =new   NashornParameters();
    @Parameter(names="-o",description="output directory",required = true)
    private File outputDirectory = null;

    private static long ID_GENERATOR=0L;
    
    private static String nextId() {
    	return "n"+(++ID_GENERATOR);
    }
    
    public abstract class Sprite {
    	boolean saved_flag=false;
    	long id = (++ID_GENERATOR);
    	public abstract BufferedImage getImage();
    	public abstract Shape getShape();
    	

    	
    	public Sprite clip(Shape shape) {
    		Area area=new Area(this.getShape());
    		area.intersect(new Area(shape));
    		shape = area;
    		Rectangle rect = shape.getBounds();
    		BufferedImage img=new BufferedImage(rect.width,rect.height, getImage().getType());
    		AffineTransform tr = AffineTransformFactory.newInstance().translate(-rect.getX(),-rect.getY()).make();
    		Shape shape2 = tr.createTransformedShape(shape);
    		Graphics2D g= (Graphics2D)img.getGraphics();
    		g.setClip(shape2);
    		g.drawImage(getImage(), (int)-rect.getX(),(int) -rect.getY(), null);
    		g.dispose();
    		return new DefaultSprite(img,shape2);
    		}
    	public Sprite frame() {
    		int x=20;
    		BufferedImage img=new BufferedImage(getImage().getWidth()+x*2,getImage().getHeight()+x*2,getImage().getType());
    		Graphics2D g= (Graphics2D)img.getGraphics();
    		AffineTransform tr = AffineTransformFactory.newInstance().translate(x,x).make();
    		Area shape2 = new Area(tr.createTransformedShape(getShape()));
    		BasicStroke stroke=new BasicStroke(x);
    		Area shape3 = new Area(stroke.createStrokedShape(shape2));
    		g.fill(shape3);
    		g.setClip(shape2);
    		g.drawImage(getImage(),x,x,null);
    		g.dispose();
    		shape2.exclusiveOr(shape3);
    		return new DefaultSprite(img,shape2);
    	}
    	
    	public Sprite mirrorH() {
    		BufferedImage img=new BufferedImage(getImage().getWidth(),getImage().getHeight(),getImage().getType());
    		AffineTransform tr = AffineTransformFactory.newInstance().
    				translate(getImage().getWidth(),0).
    				scale(-1,1).
    				make();
    		Shape  shape2 = tr.createTransformedShape(getShape());
    		Graphics2D g= (Graphics2D)img.getGraphics();
    		g.setColor(Color.RED);
    		g.fillRect(0, 0, getImage().getWidth(), getImage().getHeight());
    		g.setClip(shape2);
    		g.drawImage(getImage(),tr,null);
    		g.dispose();
    		return new DefaultSprite(img,shape2);
    		}
    	
    	public File saveAs(File directory) throws IOException {
    		File f = new File(directory,String.valueOf(this.id)+".png");
    		if(!saved_flag) {
	    		ImageIO.write(this.getImage(), "PNG", f);
	    		saved_flag=true;
	    		}
    		return f;
    		}
    	}
    
    private class DefaultSprite extends Sprite {
    	protected final BufferedImage image;
    	protected final Shape shape;
    	DefaultSprite(BufferedImage image, Shape shape) {
			this.image = image;
    		this.shape=shape;
        	}
    	@Override
    	public BufferedImage getImage() {
    		return this.image;
    		}
    	@Override
    	public Shape getShape() {
    		return this.shape;
    		}
    	}
    
    
    

    public class ShapeBuilder {
    	private List<Point2D.Double> points=new ArrayList<>();
    	public ShapeBuilder add(double x,double y) {
    		this.points.add(new Point2D.Double(x, y));
    		return this;
    		}
    	
    
    	public Shape make() {
    		if(this.points.isEmpty()) throw new IllegalArgumentException();
    		GeneralPath gp=new GeneralPath();
    		gp.moveTo(points.get(0).getX(),points.get(0).getY());
    		for(int i=1;i< points.size();++i) {
        		gp.lineTo(points.get(i).getX(),points.get(i).getY());
    			}
    		gp.closePath();
    		this.points.clear();
    		return gp;
    		}
    	}
    
	public class Page implements DOMHelper {
		private final Document dom;
		private final Set<Long> sprites=new HashSet<>();
		Dimension dim;
		Element svgRoot;
		Element svgDefs;
		Element svgBody;
		Page(Document dom,Dimension dim) {
			this.dom = dom;
			this.dim = dim;
			svgRoot=element("svg",null,FunctionalMap.of(
					"width", dim.getWidth(),
					"height", dim.getHeight()
					));
			dom.appendChild(svgRoot);
			svgRoot.setAttribute("viewBox","0 0 "+dim.width+" "+dim.height);
			svgRoot.setAttribute("width", String.valueOf(dim.width));
			svgRoot.setAttribute("height", String.valueOf(dim.height));
			svgDefs = element("defs");
			svgRoot.appendChild(svgDefs);
			svgBody = element("g");
			svgRoot.appendChild(svgBody);
			}
		@Override
		public String getDefaultNamespace() {
			return SVG.NS;
			}
		@Override
		public Document getDocument() {
			return dom;
			}
		public Sprite use(Sprite sprite, double x,double y) throws IOException {
			if(!this.sprites.contains(sprite.id)) {
				this.sprites.add(sprite.id);
				final String id1= nextId();
				final Element clipPath = element("clipPath",null,FunctionalMap.of(
	    				"id", id1
						)) ;
				
				final Element svgPath = element("path",null,
						FunctionalMap.of("d", SVGUtils.shapeToPath(sprite.getShape()))
						);
				clipPath.appendChild(svgPath);
				this.svgDefs.appendChild(clipPath);
				
				File f=sprite.saveAs(ComicsBuilderV1.this.outputDirectory);
				final Element img= element("image",
						null,
						FunctionalMap.of(
								"id", "n"+sprite.id,
								"href",f.toURI().toURL(),
								"mask","url(#"+id1+")"
								)
						);
				this.svgDefs.appendChild(img);
				}
			
			Element u = element("use",
					null,
					FunctionalMap.of("id",nextId(),"x",x,"y",y,"href","#n"+sprite.id)
					);
			svgBody.appendChild(u);
			return sprite;
			}
		
		
		
		
		public void saveAs(String fname) {
			exportXml(this.dom,new StreamResult(new File(fname)));
		}
		
		
		
		
		public void view() {
			try {
				TransformerFactory tf=TransformerFactory.newInstance();
				Transformer tr=tf.newTransformer();
				tr.transform(
						new DOMSource(this.getDocument()),
						new StreamResult(System.out)
						);
				}
			catch(Throwable err) {
				err.printStackTrace();
				}
			}
		}
	
	
	
	public class Context  {
		DocumentBuilder domBuilder;
		Context() {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			try {
				this.domBuilder=dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				throw new IllegalArgumentException(e);
				}
		 	}
		
		public Page createPage(final int w,final int h) {
			return new Page(this.domBuilder.newDocument(),new Dimension(w,h));
			}
		
		
		public ShapeBuilder newShapeBuiilder() {
			return new ShapeBuilder();
		}
		
		public Dimension2D dimension(final double w,final double h) {
			return new Dimension2D() {
				@Override
				public double getWidth() {
					return w;
					}
				@Override
				public double getHeight() {
					return h;
					}
				@Override
				public void setSize(Dimension2D d) {
					setSize(d.getWidth(),d.getHeight());
					}
				@Override
				public void setSize(double width, double height) {
					throw new UnsupportedOperationException();
					}
				};
		}
		public Rectangle2D createRectangle(double x, double y,double w,double h) {
			return new Rectangle2D.Double(x,y,w,h);
		}
		
		
		public Sprite getImage(final String href) throws IOException {
			BufferedImage image;
			if(IOUtils.isURL(href)) {
				image = ImageIO.read(new URL(href));
				}
			else
				{
				image = ImageIO.read(new File(href));
				}	
			return new DefaultSprite(image,new Rectangle(0,0,image.getWidth(),image.getHeight()));
			}
		
	}
	
	private static void exportXml(Node root, StreamResult out) {
		try {
			TransformerFactory tf=TransformerFactory.newInstance();
			Transformer tr=tf.newTransformer();
			tr.transform(
					new DOMSource(root),
					out
					);
			}
		catch(Throwable err) {
			err.printStackTrace();
			}
		}
	
	
	@Override
	public int doWork(List<String> args) {
		try {
			   // Here we are generating Nashorn JavaScript Engine 
	        final ScriptEngine ee = NashornUtils.makeRequiredEngine();
			ee.put("context",new Context());
			try(Reader r=	this.nashorParams.getReader()) {
				ee.eval(r);
				}
			return 0;
			}
		catch(Throwable err) {
			err.printStackTrace();
			return -1;
			}
		
		}
	
	 public static ProgramDescriptor getProgramDescriptor() {
    	return new ProgramDescriptor() {
    		@Override
    		public String getName() {
    			return "comicsbuilder1";
    			}
    		};
    	}
	
	public static void main(String[] args) {
		new ComicsBuilderV1().instanceMainWithExit(args);
	}
}
