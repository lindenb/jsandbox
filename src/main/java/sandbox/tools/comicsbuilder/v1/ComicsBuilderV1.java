package sandbox.tools.comicsbuilder.v1;

import java.awt.Dimension;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.script.Bindings;
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
import org.w3c.dom.Text;

import com.beust.jcommander.ParametersDelegate;

import sandbox.Launcher;
import sandbox.nashorn.NashornParameters;
import sandbox.nashorn.NashornUtils;
import sandbox.svg.SVG;
import sandbox.tools.central.ProgramDescriptor;
import sandbox.util.function.FunctionalMap;


public class ComicsBuilderV1 extends Launcher {
	private static final String NS2="https://ns2";
	private static final String PREFIX2="ns2:";
	private static final String USER_DATA_SHAPE="shape";
	
    @ParametersDelegate
    private NashornParameters nashorParams =new   NashornParameters();

    private static long ID_GENERATOR=0L;
    
    private static String nextId() {
    	return "n"+(++ID_GENERATOR);
    }
    
    private interface DOMHelper {
    	Document getDocument();
    	
    	default String toString(Object o) {
    		if(o==null) return "";
    		return String.valueOf(o);
    	}
    	default String namespaceForPrefix(String pfx) {
    		if(PREFIX2.endsWith(pfx)) return NS2;
    		throw new IllegalArgumentException(""+pfx);
    		}
    	default Text text(Object o) {
    		return (o==null?null:getDocument().createTextNode(toString(o)));
			}
    	default Element element(String s) {
			return element(s,null,FunctionalMap.empty());
			}
    	default Element element(String s,Object content,FunctionalMap<String, Object> atts) {
    		Element e = getDocument().createElementNS(SVG.NS,s);
    		Text t=text(content);
    		if(t!=null) e.appendChild(t); 
    		if(atts!=null && !atts.isEmpty()) {
    			for(String key:atts.keySet()) {
    				String v= toString(atts.get(key));
    				if(v==null) continue;
    				int colon=key.indexOf(":");
    				if(colon!=-1) {
    					e.setAttributeNS(
							namespaceForPrefix(key.substring(0,colon)),
							key, v
							);
    					}
    				else
    					{
    					e.setAttribute(key, v);
    					}
    				}
    			}
    		return e;
			}
    }
    
    public class Def implements DOMHelper {
    	final Page page;
    	final Element e;
    	Def(Page page,Element e) {
    		this.page=page;
    		this.e=e;
    		}
    	@Override
    	public Document getDocument() {
    		return page.dom;
    		}
    	public String getId() {
    		return e.getAttribute("id");
    		}
    	public String getAnchor() {
    		return "#"+getId();
    		}
    	public String getUrl() {
    		return "url("+getAnchor()+")";
    		}
    	public Def clip(Def shape) {
    		final String id1= nextId();
    		final Element clipPath= element("clipPath",null,
    			FunctionalMap.of("id",id1)	
    			);
    		final Element use1 = element("use",null,
    			FunctionalMap.of(
    				"x",0, "y",0,
    				"href", shape.getAnchor()
    				)	
    			);
    		clipPath.appendChild(use1);
    		
    		page.svgDefs.appendChild(clipPath);
    		
    		Shape awtShape =Shape.class.cast(shape.e.getUserData(USER_DATA_SHAPE));
    		Rectangle2D r=awtShape.getBounds2D();
    		System.err.println(r);
    		AffineTransform tr=AffineTransform.getTranslateInstance(-r.getX(),-r.getY());
    		awtShape = tr.createTransformedShape(awtShape);
    		
    		String id2=nextId();
    		Element g2 = element("g",null,
    			FunctionalMap.of( "id",id2)
    			);
    		Element use2 = element("use",null,
	    			FunctionalMap.of(
	    				"x",0, "y",0,
	    				"href", getAnchor(),
	    				"clip-path","url(#"+id1+")"
	    				)	
	    			);
	    	g2.appendChild(use2);
	    	page.svgDefs.appendChild(g2);
    		
	    	

    		Element g3 = element("g",null,
    			FunctionalMap.of( "id",nextId())
    			);
    		Element use3 = element("use",null,
	    			FunctionalMap.of(
	    				"x",0, "y",0,
	    				"href","#"+id2,
	    				"transform","translate("+toString(-r.getX())+","+toString(-r.getY())+")"
	    				)	
	    			);
	    	g3.appendChild(use3);
	    	page.svgDefs.appendChild(g3);
	    	
	    	
	    	g3.setUserData(USER_DATA_SHAPE, tr.createTransformedShape(awtShape),null);
    		page.svgDefs.appendChild(g3);
    		return new Def(this.page,g3);
    		}
    	}
    
    public class ShapeBuilder implements DOMHelper {
    	Page page;
    	List<Point2D.Double> points=new ArrayList<>();
    	public ShapeBuilder add(double x,double y) {
    		this.points.add(new Point2D.Double(x, y));
    		return this;
    		}
    	@Override
    	public Document getDocument() {
    		return page.getDocument();
    		}
    	public Def makePolygon() {
    		return makeShape("polygon");
    		}
    	private Def makeShape(String tag) {
    		double minx =  this.points.stream().mapToDouble(P->P.getX()).min().orElse(0);
    		double maxx =  this.points.stream().mapToDouble(P->P.getX()).max().orElse(0);
    		double miny =  this.points.stream().mapToDouble(P->P.getY()).min().orElse(0);
    		double maxy =  this.points.stream().mapToDouble(P->P.getY()).max().orElse(0);
    		Element shape= element(tag,null,
    				FunctionalMap.of(
    					"id",nextId(),
    					PREFIX2+":minx",minx,
    					PREFIX2+":miny",miny,
    					PREFIX2+":width",maxx-minx,
    					PREFIX2+":height",maxy-miny,
						"points", this.points.stream().
	    					map(P->toString(P.getX())+","+toString(P.getY())).
	    					collect(Collectors.joining(" "))
    					)
    				);
    		shape.setUserData(
    				USER_DATA_SHAPE,
    				new Rectangle2D.Double(minx,miny,(maxx-minx),(maxy-miny)),
    				null
    				);
    		page.svgDefs.appendChild(shape);
    		this.points.clear();
    		return new Def(page,shape);
    		}
    	}
    
	public class Page implements DOMHelper {
		private final Document dom;
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
		public Document getDocument() {
			return dom;
			}
		public Element use(Def def, double x,double y) {
			Element u = element("use",
					null,
					FunctionalMap.of("id",nextId(),"x",x,"y",y,"href","#"+def.getId())
					);
			svgBody.appendChild(u);
			return u;
			}
		
		
		public Def createRectangle(double x,double y,double w,double h) {
			Element shape= element("rect",null,
    				FunctionalMap.of(
    					"id",nextId(),
    					"x",x,
    					"y",y,
    					"width",w,
    					"height",h
    					)
    				);
			this.svgDefs.appendChild(shape);
			shape.setUserData(
					USER_DATA_SHAPE,
					new Rectangle2D.Double(x, y, w, h),
					null
					);
    		return new Def(this,shape);
			}
		
		public Def getImage(final String href) throws IOException {
			final URL url=new URL(href);
			try(InputStream input= url.openStream()) {
				try(ImageInputStream in = ImageIO.createImageInputStream(Objects.requireNonNull(input,()->"Cannot create input image for "+url))){
				    final Iterator<ImageReader> readers = ImageIO.getImageReaders(Objects.requireNonNull(in,()->"Cannot create inputstream  for "+url));
				    if (readers.hasNext()) {
				        ImageReader reader = Objects.requireNonNull(readers.next());
				        try {
				            reader.setInput(in);
				            Dimension dim= new Dimension(reader.getWidth(0), reader.getHeight(0));
				            final Element e = element("image",null,
				            	FunctionalMap.of(
				            		"id",nextId(),
				            		"href", url,
				            		"width", dim.getWidth(),
				            		"height", dim.getHeight()
				            		)	
				            	);
				        	e.setUserData(
				        		USER_DATA_SHAPE,
			    				new Rectangle2D.Double(0,0,dim.getWidth(), dim.getHeight()),
			    				null
			    				);
				        	this.svgDefs.appendChild(e);
				            return new Def(this,e);
				        } finally {
				            reader.dispose();
				        }
				    }
				}
				}
			throw new IOException("Cannot find decoder for "+url);
		}
		
		public void saveAs(String fname) {
			exportXml(this.dom,new StreamResult(new File(fname)));
		}
		
		
		public Def clip(Def image,Def E) {
    		Element g= element("g",null,FunctionalMap.of(
    				"clip-path","url("+E.getId()+")",
    				"id", nextId()));
    		Element u= element("use",null,FunctionalMap.of("href","#"+nextId(),"x",0,"y",0));
    		g.appendChild(u);
    		this.svgDefs.appendChild(g);
    		return new Def(this,g);
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
	
	public class Case {
		Case() {
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
		public Rectangle2D rect(double x, double y,double w,double h) {
			return new Rectangle2D.Double(x,y,w,h);
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
	        final Bindings ctx= ee.createBindings();
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
