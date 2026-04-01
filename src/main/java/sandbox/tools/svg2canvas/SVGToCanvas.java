package sandbox.tools.svg2canvas;

import java.awt.Shape;

import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;


import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


import sandbox.Launcher;
import sandbox.awt.Dimension2D;
import sandbox.lang.StringUtils;
import sandbox.svg.SVG;
import sandbox.xml.XMLException;

/**
 * Transforms a Scalable Vector Graphics SVG
 * to javascript/Canvas
 * @author Pierre Lindenbaum
 *
 */
public class SVGToCanvas
	extends Launcher
	{
	private Integer overrideWidth=null;
	private boolean useBase64ForImages=false;
	private int precision=2;
	private static long ID_GENERATOR=System.currentTimeMillis();
	private static int VAR_GENERATOR=0;
	private Map<String,Definition> id2definition=new HashMap<String, Definition>();
	
	public SVGToCanvas()
		{
		}
	
	private enum Selector
		{
		STROKE,FILL,OPACITY,STROKE_WIDTH,
		STROKE_LINECAP,
		STROKE_LINEJOIN,
		STROKE_MITERLIMIT,
		FONT_FAMILY,
		FONT_SIZE,
		FONT_WEIGHT,
		FONT_STYLE,
		TEXT_ANCHOR
		}
	
	
	private static class Definition
		{
		Element element;
		String var;
		}
	
	private static class Text
		{
		String text;
		double x;
		double y;
		Text(double x,double y,String text)
			{
			this.x=x;
			this.y=y;
			this.text=text;
			}
		}
	private static class Image
		{
		String url=null;
		Point2D top;
		Dimension2D size=null;
		}
	/**
	 * 
	 * State
	 *
	 */
	private static class State
		{
		State prev=null;
		Text text=null;
		Image image=null;
		Shape shape=null;
		AffineTransform tr=null;
		List<State> children=new ArrayList<State>();
		Map<Selector,String> selector= new HashMap<Selector,String>();
		
		
		AffineTransform getTransform()
			{
			AffineTransform t=this.tr;
			if(t==null) t=new AffineTransform();
			AffineTransform old=null;
			if(prev!=null)
				{
				old=prev.getTransform();
				old.concatenate(t);
				t=old;
				}
			return t;
			}
		
		public String get(Selector sel)
			{
			String o=selector.get(sel);
			if(o==null && prev!=null) return prev.get(sel);
			return o;
			}
		
		public double getOpacity()
			{
			String s=(String)selector.get(Selector.OPACITY);
			double curr=(s==null?1.0:Double.parseDouble(s));
			double prevValue=(prev==null?1.0:prev.getOpacity());
			return curr*prevValue;
			}
		}
	

	
	private PrintStream output= System.out;
	private File fileout=null;
	
	private void parse(
			State parent,
			Element e) throws XMLException
		{
		State state= new State();
		state.prev=parent;
		parent.children.add(state);
		
		if(e.hasAttributes())
			{
			NamedNodeMap atts=e.getAttributes();
			for(int i=0;i< atts.getLength();++i)
				{
				Attr att= Attr.class.cast(atts.item(i));
				if(att.getNamespaceURI()!=null) continue;
				String s=att.getName();
				applyStyle(state,s,att.getValue());
				}
			}
		
		String shapeName= e.getLocalName();
		
		if(!SVG.NS.equals(e.getNamespaceURI()))
			{
			for(Node c=e.getFirstChild();c!=null;c=c.getNextSibling())
				{
				if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
				parse(state,Element.class.cast(c));
				}
			}
		else if(shapeName==null)
			{
			LOG.warning("shapeName is null");
			}
		else if(shapeName.equals("g"))
			{
			
			for(Node c=e.getFirstChild();c!=null;c=c.getNextSibling())
				{
				if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
				parse(state,Element.class.cast(c));
				}
			}
		else if(shapeName.equals("path"))
			{
			Attr d= e.getAttributeNode("d");
			if(d!=null && !StringUtils.isBlank(d.getValue()))
				{
				Shape shape;
				try
				{
				shape=new SVGPathParser(d.getValue()).path();
				} catch(ParseException err)
					{
					throw new InvalidXMLException(e,"Cannot parse '"+d.getValue()+"' "+err.getMessage());
					}
				state.shape=shape;
				}
			}
		else if(shapeName.equals("polyline"))
			{
			Attr points= e.getAttributeNode("points");
			if(points!=null)
				{
				Shape shape  = SVGUtils.polylineToShape(points.getValue());
				state.shape=shape;
				}
			}
		else if(shapeName.equals("polygon"))
			{
			Attr points= e.getAttributeNode("points");
			if(points!=null)
				{
				Shape shape  = SVGUtils.polygonToShape(points.getValue());
				state.shape=shape;
				}
			}
		else if(shapeName.equals("rect"))
			{
			
			Attr x= e.getAttributeNode("x");
			Attr y= e.getAttributeNode("y");
			Attr w= e.getAttributeNode("width");
			Attr h= e.getAttributeNode("height");
			if(x!=null && y!=null && w!=null && h!=null)
				{
				Shape shape =new Rectangle2D.Double(
					Double.parseDouble(unit(x.getValue())),
					Double.parseDouble(unit(y.getValue())),	
					Double.parseDouble(unit(w.getValue())),	
					Double.parseDouble(unit(h.getValue()))
					);
				state.shape=shape;
				}
			}
		else if(shapeName.equals("line"))
			{
			Attr x1= e.getAttributeNode("x1");
			Attr y1= e.getAttributeNode("y1");
			Attr x2= e.getAttributeNode("x2");
			Attr y2= e.getAttributeNode("y2");
			if(x1!=null && y1!=null && x2!=null && y2!=null)
				{
				Shape shape =new Line2D.Double(
					Double.parseDouble(unit(x1.getValue())),
					Double.parseDouble(unit(y1.getValue())),	
					Double.parseDouble(unit(x2.getValue())),	
					Double.parseDouble(unit(y2.getValue()))
					);
				state.shape=shape;
				}
			}
		else if(shapeName.equals("circle"))
			{
			Attr cx= e.getAttributeNode("cx");
			Attr cy= e.getAttributeNode("cy");
			Attr r= e.getAttributeNode("r");
			if(cx!=null && cy!=null && r!=null)
				{
				double radius=Double.parseDouble(unit(r.getValue()));
				Shape shape =new Ellipse2D.Double(
					Double.parseDouble(unit(cx.getValue()))-radius,
					Double.parseDouble(unit(cy.getValue()))-radius,	
					radius*2,	
					radius*2
					);
				state.shape=shape;
				}
			}
		else if(shapeName.equals("ellipse"))
			{
			Attr cx= e.getAttributeNode("cx");
			Attr cy= e.getAttributeNode("cy");
			Attr rx= e.getAttributeNode("rx");
			Attr ry= e.getAttributeNode("ry");
			if(cx!=null && cy!=null && rx!=null && ry!=null)
				{
				double radiusx=Double.parseDouble(unit(rx.getValue()));
				double radiusy=Double.parseDouble(unit(ry.getValue()));
				Shape shape =new Ellipse2D.Double(
					Double.parseDouble(unit(cx.getValue()))-radiusx,
					Double.parseDouble(unit(cy.getValue()))-radiusy,	
					radiusx*2,	
					radiusy*2
					);
				state.shape=shape;
				}
			}
		else if(StringUtils.isIn(shapeName,
			"title","desc","metadata","flowRoot"))
			{
			//ignore
			}
		else if(shapeName.equals("defs"))
			{
			for(Node c=e.getFirstChild();c!=null;c=c.getNextSibling())
				{
				if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
				if(!SVG.NS.equals(c.getNamespaceURI())) continue;
				Attr id= Element.class.cast(c).getAttributeNode("id");
				if(id==null) continue;
				Definition def= new Definition();
				def.element=Element.class.cast(c);
				this.id2definition.put(id.getValue(),def);
				}
			}
		else if(shapeName.equals("text"))
			{
			Attr x= e.getAttributeNode("x");
			Attr y= e.getAttributeNode("y");
			if(x!=null && y!=null)
				{
				state.text= new Text(
					Double.parseDouble(unit(x.getValue())),
					Double.parseDouble(unit(y.getValue())),
					e.getTextContent()
					);
				}
			}
		else if(shapeName.equals("image"))
			{
			Attr x= e.getAttributeNode("x");
			Attr y= e.getAttributeNode("y");
			Attr w= e.getAttributeNode("width");
			Attr h= e.getAttributeNode("height");
			Attr url= e.getAttributeNodeNS(XLINK.NS, "href");
			if(x!=null && y!=null &&  url!=null)
				{
				Image img= new Image();
				img.url=url.getValue();
				img.top =new Point2D.Double(
						Double.parseDouble(unit(x.getValue())),
						Double.parseDouble(unit(y.getValue()))
						);
				if(w!=null && h!=null )
					{
					img.size= new Dimension2D.Double(
						Double.parseDouble(unit(w.getValue())),	
						Double.parseDouble(unit(h.getValue()))
						);
					}
				state.image=img;
				}
			}
		else if(shapeName.equals("svg"))
			{
			for(Node c=e.getFirstChild();c!=null;c=c.getNextSibling())
				{
				if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
				parse(state, Element.class.cast(c));
				}
			}
		else if(shapeName.equals("a"))
			{
			for(Node c=e.getFirstChild();c!=null;c=c.getNextSibling())
				{
				if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
				parse(state, Element.class.cast(c));
				}
			}
		else
			{
			LOG.warning("cannot display <"+e.getLocalName()+">");
			}
		
		}
	
	/**
	 * applyStyle
	 */
	private void applyStyle(
		State state,
		String key,
		String value
		) throws XMLException
		{
		if(StringUtils.isBlank(key) || StringUtils.isBlank(value)) return;
		
		
		
		if(key.equals("style"))
			{
			for(String style:value.split("[;]+"))
				{
				int j=style.indexOf(':');
				if(j!=-1)
					{
					applyStyle(
						state,
						style.substring(0,j).trim(),
						style.substring(j+1).trim());
					}
				}
			return;
			}
		
		if(key.equals("transform"))
			{
			if(state.shape==null) state.tr= new AffineTransform();
			AffineTransform tr= SVGTransformParser.parse(value);
			state.tr.concatenate(tr);
			return;
			}
		if(key.equals("fill-opacity")) key="opacity";//TODO
		for(Selector sel:Selector.values())
			{
			if(key.equals(sel.name().toLowerCase().replace('_', '-')))
				{
				state.selector.put(sel, value);
				return;
				}
			}
		if(StringUtils.isIn(key,"d", "x","y","width","height","id","points","rx","ry","class",
				"cx","cy")) return;
	    LOG.info("Not handled :"+key+"="+value);
		}
	
	@Override
	protected int processArg(String[] args, int optind)
		{
		if(args[optind].equals("-o"))
			{
			this.fileout=new File(args[++optind]);
			return optind;
			}
		else if(args[optind].equals("-p"))
			{
			this.precision=Integer.parseInt(args[++optind]);
			if(this.precision<0) throw new IllegalArgumentException("Bad precision "+this.precision);
			return optind;
			}
		else if(args[optind].equals("-w"))
			{
			this.overrideWidth=Integer.parseInt(args[++optind]);
			if(this.overrideWidth<=0) throw new IllegalArgumentException("Bad width "+this.overrideWidth);
			return optind;
			}
		else if(args[optind].equals("-b"))
			{
			this.useBase64ForImages=!this.useBase64ForImages;
			return optind;
			}
		return super.processArg(args, optind);
		}
	
	private void startHTML()
		{
		print("<html><body>");
		}
	
	private void endHTML()
		{
		print("<div><i>Author: "+Me.FIRST_NAME+" "+Me.LAST_NAME+" ( <a href='mailto:"+Me.MAIL+"'>"+
			Me.MAIL+"</a> ) <a href='"+Me.WWW+"'>"	+
			Me.WWW+"</a></i>"+
			"</div>");
		print("</body></html>");
		}
	
	private String unit(String s)
		{
		if(s.endsWith("in"))
			{
			return String.valueOf(Double.parseDouble(s.substring(0,s.length()-2).trim())*72);
			}
		if(s.endsWith("px") ||
				s.endsWith("pt"))
				{
				return s.substring(0,s.length()-2).trim();
				}
		return s;
		}
	
	void print(String s)
		{
		output.print(s);
		if(LOG.getLevel()!=Level.OFF)
			{
			output.println();
			}
		}
	/** extract the argument of a url(#id) argument */
	@SuppressWarnings("unused")
	private static String urlArg(String s)
		{
		if(s==null) return null;
		s=s.replace(" ","").trim();
		if(!s.startsWith("url(")) return null;
		int i=s.indexOf(')');
		if(i==-1) return null;
		s= s.substring(4,i);
		if(s.startsWith("#")) s=s.substring(1);
		if(StringUtils.isBlank(s)) return null;
		return s;
		}
	/** DOES NOT WORK because values can be given as percent */
	@SuppressWarnings("unused")
	private String paintUrl(String url)
		{
		Definition def=this.id2definition.get(url);
		if(def==null) return null;
		if(def.var!=null) return def.var;
		
		StringWriter w=new StringWriter();
		
		if(def.element.getLocalName().equals("linearGradient"))
			{
			Attr x1= def.element.getAttributeNode("x1");
			Attr y1= def.element.getAttributeNode("x2");
			Attr x2= def.element.getAttributeNode("y1");
			Attr y2= def.element.getAttributeNode("y2");
			if(x1==null || y1==null || x2==null || y2==null) return null;
			
			VAR_GENERATOR++;
			def.var="g"+VAR_GENERATOR;
			w.append(def.var+"=c.createLinearGradient("+
				x1.getValue()+","+y1.getValue()+","+
				x2.getValue()+","+y2.getValue()+");");
			}
		else if(def.element.getLocalName().equals("radialGradient"))
			{
			Attr cx= def.element.getAttributeNode("cx");
			Attr cy= def.element.getAttributeNode("cy");
			Attr fx= def.element.getAttributeNode("fx");
			Attr fy= def.element.getAttributeNode("fy");
			Attr r= def.element.getAttributeNode("r");
			
			if(fx==null) fx=cx;
			if(fy==null) fy=cy;
			if(cx==null || cy==null || r==null) return null;
			
			VAR_GENERATOR++;
			def.var="g"+VAR_GENERATOR;
			w.append(def.var+"=c.createRadialGradient("+
				cx.getValue()+","+cy.getValue()+",0,"+
				fx.getValue()+","+fy.getValue()+","+r.getValue()+");");
			}
		else
			{
			return null;
			}
		
		for(Node c=def.element.getFirstChild();c!=null;c=c.getNextSibling())
			{
			if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
			Element e=Element.class.cast(c);
			if(!e.getLocalName().equals("stop")) continue;
			Attr stop_color= e.getAttributeNode("stop-color");
			if(stop_color==null) continue;
			Attr offset= e.getAttributeNode("offset");
			if(offset==null) continue;
			String percent=offset.getValue().trim();
			if(!percent.endsWith("%")) continue;
			Integer val=Cast.Integer.cast(percent.substring(0,percent.length()-1));
			if(val==null) continue;
			w.append(def.var+".addColorStop("+(val/100.0)+",\'"+C.escape(stop_color.getValue())+"\');");
			}
		return def.var;
		}
	
	/**
	 * generateCode
	 */
	private void generateCode(State state, Map<Selector,String> current)
		{
		if(state.tr!=null)
			{
			double f[]=new double[6];
			state.getTransform().getMatrix(f);
			print("c.setTransform("+
					f[0]+","+f[1]+","+
					f[2]+","+f[3]+","+
					f[4]+","+f[5]+");"
					);
			}
		
		String fill= state.get(Selector.FILL);
		boolean do_fill=!fill.equals("none");
		String stroke= state.get(Selector.STROKE);
		boolean do_stroke=!stroke.equals("none");
		
		
		
		if(!fill.equals(current.get(Selector.FILL)))
			{
			if(!fill.equals("none"))
				{
				print("c.fillStyle=\""+fill+"\";");
				}
			current.put(Selector.FILL, fill);
			}
		
		if(!stroke.equals(current.get(Selector.STROKE)))
			{
			if(!stroke.equals("none")) print("c.strokeStyle=\""+stroke+"\";");
			current.put(Selector.STROKE, stroke);
			}
		
		String opacity= String.valueOf(state.getOpacity());
		
		if(!opacity.equals(current.get(Selector.OPACITY)))
			{
			this.print("c.globalAlpha=\""+opacity+"\";");
			current.put(Selector.OPACITY, opacity);
			}		
		
		String strokeWidth= state.get(Selector.STROKE_WIDTH);
		if(!strokeWidth.equals(current.get(Selector.STROKE_WIDTH)))
			{
			this.print("c.lineWidth=\""+unit(strokeWidth)+"\";");
			current.put(Selector.STROKE_WIDTH, strokeWidth);
			}
		
		String lineCap = state.get(Selector.STROKE_LINECAP);
		if(!lineCap.equals(current.get(Selector.STROKE_LINECAP)))
			{
			this.print("c.lineCap=\""+lineCap+"\";");
			current.put(Selector.STROKE_LINECAP, lineCap);
			}
		
		String lineJoin = state.get(Selector.STROKE_LINEJOIN);
		if(!lineJoin.equals(current.get(Selector.STROKE_LINEJOIN)))
			{
			this.print("c.lineJoin=\""+lineJoin+"\";");
			current.put(Selector.STROKE_LINEJOIN, lineJoin);
			}
		
		String mitterLimit = state.get(Selector.STROKE_MITERLIMIT);
		if(!mitterLimit.equals(current.get(Selector.STROKE_MITERLIMIT)))
			{
			this.print("c.mitterLimit=\""+mitterLimit+"\";");
			current.put(Selector.STROKE_MITERLIMIT, mitterLimit);
			}
		
		boolean font_changed=false;
		String fontSize = state.get(Selector.FONT_SIZE);
		if(!fontSize.equals(current.get(Selector.FONT_SIZE)))
			{
			font_changed=true;
			current.put(Selector.FONT_SIZE, fontSize);
			}
		
		String fontWeight = state.get(Selector.FONT_WEIGHT);
		if(!fontWeight.equals(current.get(Selector.FONT_WEIGHT)))
			{
			font_changed=true;
			current.put(Selector.FONT_WEIGHT, fontWeight);
			}
		
		String fontStyle = state.get(Selector.FONT_STYLE);
		if(!fontStyle.equals(current.get(Selector.FONT_STYLE)))
			{
			font_changed=true;
			current.put(Selector.FONT_STYLE, fontStyle);
			}
		
		String fontFamily = state.get(Selector.FONT_FAMILY);
		if(!fontFamily.equals(current.get(Selector.FONT_FAMILY)))
			{
			font_changed=true;
			current.put(Selector.FONT_FAMILY, fontFamily);
			}
		
		String textAnchor = state.get(Selector.TEXT_ANCHOR);
		if(!textAnchor.equals(current.get(Selector.TEXT_ANCHOR)))
			{
			current.put(Selector.TEXT_ANCHOR, textAnchor);
			}
		
		//textalign
		if(font_changed)
			{
			this.print("c.font=\""+fontStyle+" "+fontWeight+" "+fontSize+" "+fontFamily+"\";");
			}
		
		if(state.image!=null)
			{
			VAR_GENERATOR++;
			this.print("var i"+VAR_GENERATOR+"=new Image();");
			String imagesrc="i"+VAR_GENERATOR+".src=\'"+C.escape(state.image.url)+"\';";
			if(this.useBase64ForImages)
				{
				LOG.info("loading image "+state.image.url);
				try
					{
					BufferedImage img= ImageIO.read(new URL(state.image.url));
					ByteArrayOutputStream baos=new ByteArrayOutputStream();
					
					//guess image format
					String s=state.image.url;
					int i=s.indexOf('?');
					if(i!=-1) s=s.substring(0,i);
					i=s.indexOf('#');
					if(i!=-1) s=s.substring(0,i);
					String formatName=null;
					i=s.lastIndexOf('.');
					if(i!=-1) formatName=s.substring(i+1).toLowerCase();
					if(StringUtils.isBlank(formatName))
						{
						formatName="jpeg";
						}
					
					ImageIO.write(img, formatName, baos);
					baos.flush();
					baos.close();
					imagesrc="i"+VAR_GENERATOR+".src=\'data:image/"+
						formatName.toLowerCase()+
						";base64,"+C.escape(Base64.encode(baos.toByteArray()).replace("\r"," ").replace("\n"," ").replace(" ", ""))+"\';";
					}
				catch (IOException e)
					{
					System.err.println("Cannot use Base64 for "+state.image.url+" "+e.getMessage());
					}
				
				}
			this.print(imagesrc);
			if(state.image.size==null)
				{
				this.print("c.drawImage(i"+VAR_GENERATOR+","+
						fmt(state.image.top.getX())+","+
						fmt(state.image.top.getY())+
						");");
				}
			else
				{
				this.print("c.drawImage(i"+VAR_GENERATOR+","+
						fmt(state.image.top.getX())+","+
						fmt(state.image.top.getY())+","+
						fmt(state.image.size.getWidth())+","+
						fmt(state.image.size.getHeight())+
						");");
				}
			}
		
		if(state.text!=null && (do_fill || do_stroke))
			{
			
			String qStr="\""+ C.escape(state.text.text)+"\"";
			String x_pos=String.valueOf(state.text.x);
			String anchor= current.get(Selector.TEXT_ANCHOR);
			if(anchor==null || anchor.equalsIgnoreCase("start"))
				{
				//nothing
				}
			else if(anchor.equalsIgnoreCase("middle"))
				{
				VAR_GENERATOR++;
				this.print("var t"+VAR_GENERATOR+"="+qStr+";");
				this.print("var L"+VAR_GENERATOR+"=c.measureText(t"+VAR_GENERATOR+").width;");
				qStr="t"+VAR_GENERATOR;
				x_pos+="-0.5*L"+VAR_GENERATOR;
				}
			else if(anchor.equalsIgnoreCase("end"))
				{
				VAR_GENERATOR++;
				this.print("var t"+VAR_GENERATOR+"="+qStr+";");
				this.print("var L"+VAR_GENERATOR+"=c.measureText(t"+VAR_GENERATOR+").width;");
				qStr="t"+VAR_GENERATOR;
				x_pos+="-L"+VAR_GENERATOR;
				}
			if(do_stroke)
				{
				this.print("c.strokeText("+qStr+","+
					x_pos+","+state.text.y+");");
				}
			
			if(do_fill)
				{
				this.print("c.fillText("+qStr+","+
					x_pos+","+state.text.y+");");
				}
			}
			
		if(state.shape!=null)
			{
			if(!do_fill && !do_stroke)
				{
				//nothing
				}
			else if(state.shape instanceof Rectangle2D)
				{
				Rectangle2D r=Rectangle2D.class.cast(state.shape);
				String tmp=""+
						(int)r.getX()+","+
						(int)r.getY()+","+
						(int)r.getWidth()+","+
						(int)r.getHeight();
				if(do_fill) this.print("c.fillRect("+tmp+");");
				if(do_stroke) this.print("c.strokeRect("+tmp+");");
				}
			else
				{
				this.print("c.beginPath();");
				PathIterator iter= state.shape.getPathIterator(null);
				float coords[]=new float[6];
				while(!iter.isDone())
					{
					switch(iter.currentSegment(coords))
						{
						case PathIterator.SEG_MOVETO:
							{
							this.print("c.moveTo("+fmt(coords[0])+","+fmt(coords[1])+");");
							break;
							}
						case PathIterator.SEG_LINETO:
							{
							this.print("c.lineTo("+fmt(coords[0])+","+fmt(coords[1])+");");
							break;
							}
						case PathIterator.SEG_QUADTO:
							{
							this.print(
								"c.quadraticCurveTo("+
								fmt(coords[0])+","+fmt(coords[1])+","+
								fmt(coords[2])+","+fmt(coords[3])+");"
								);
							break;
							}
						case PathIterator.SEG_CUBICTO:
							{
							this.print(
								"c.bezierCurveTo("+
								fmt(coords[0])+","+fmt(coords[1])+","+
								fmt(coords[2])+","+fmt(coords[3])+","+
								fmt(coords[4])+","+fmt(coords[5])+
								");"
								);
							break;
							}
						case PathIterator.SEG_CLOSE:
							{
							this.print("c.closePath();");
							break;
							}
						}
					
					iter.next();
					}
				if(do_fill) this.print("c.fill();");
				if(do_stroke) this.print("c.stroke();");
				
				}
			}
		
		
		for(State c: state.children)
			{
			generateCode(c,current);
			}
		
		if(state.tr!=null && state.prev!=null)
			{
			double f[]=new double[6];
			AffineTransform tr=new AffineTransform(state.prev.getTransform());
			/*try
				{
				tr.invert();
				}
			catch (NoninvertibleTransformException e)
				{
				throw new RuntimeException(e);
				}*/
			tr.getMatrix(f);
			this.print("c.setTransform("+
					f[0]+","+f[1]+","+
					f[2]+","+f[3]+","+
					f[4]+","+f[5]+");"
					);
			}
		
		}
	
	
	
	private String fmt(final double f)
		{
		StringBuilder sb = new StringBuilder();
		Formatter formatter= new Formatter(sb);
		formatter.format("%."+this.precision+"f", f);
		String s= sb.toString();
		if(s.endsWith(".00")) return s.substring(0,s.length()-3);
		return s;
		}
	
	private void paintDocument(Document dom)
		{
		VAR_GENERATOR=0;//reset
		this.id2definition.clear();

		
		Element root=dom.getDocumentElement();
		if(root==null) throw new XMLException(dom,"no root");
		if(!XMLUtilities.isA(root, SVG.NS, "svg")) throw new XMLException(root,"not a SVG root");
		
		State init= new State();
		init.selector.put(Selector.FILL, "white");
		init.selector.put(Selector.STROKE, "black");
		init.selector.put(Selector.OPACITY, "1.0");
		init.selector.put(Selector.STROKE_WIDTH, "1");
		init.selector.put(Selector.STROKE_LINEJOIN, "round");
		init.selector.put(Selector.STROKE_LINECAP, "butt");
		init.selector.put(Selector.STROKE_MITERLIMIT, "1");
		init.selector.put(Selector.FONT_FAMILY, "Courier");
		init.selector.put(Selector.FONT_SIZE, "12");
		init.selector.put(Selector.FONT_STYLE, "normal");
		init.selector.put(Selector.FONT_WEIGHT, "normal");
		init.selector.put(Selector.TEXT_ANCHOR, "start");
		
		
		
		parse(init,root);
		
		
		Dimension2D size=SVGUtils.getSize(root);
		Dimension2D newsize=new Dimension2D(size);
		String initialTransform="";
		if(this.overrideWidth!=null
			&& (int)size.getWidth()!=this.overrideWidth)
			{
			double ratio= this.overrideWidth/size.getWidth();
			newsize.setSize(
				size.getWidth()*ratio,
				size.getHeight()*ratio
				);
			initialTransform="c.scale("+ratio+","+ratio+");";
			init.tr=AffineTransform.getScaleInstance(ratio, ratio);
			}
		
		
		long id=(++ID_GENERATOR);
		this.print("<div " +
				"style='text-align:center;' "+
				"width='100%' " +
				"height='"+(int)(2+newsize.getHeight())+"'"+
				">");
		this.print(
			"<canvas id='ctx"+id+"' " +
				"width='"+(int)(newsize.getWidth())+"' " +
				"height='"+(int)(newsize.getHeight())+"'>Your browser does not support the &lt;CANVAS&gt; element !</canvas>");
		this.print("<script>/* generated with svg2canvas by Pierre Lindenbaum http://plindenbaum.blogspot.com plindenbaum@yahoo.fr */");
		
		this.print("function paint"+id+"(){" +
			"var canvas=document.getElementById('ctx"+id+"');" +
			"if (!canvas.getContext) return;"+
			"var c=canvas.getContext('2d');"+
			initialTransform
			);
				
		
		
		generateCode(init,new HashMap<Selector, String>());
		
		this.print("}paint"+id+"();</script>");
		
		this.print("</div>\n");
		
		//cleanup
		this.id2definition.clear();
		}
	
	@Override
	protected void usage(PrintStream out)
		{
		out.println("SVG2Canvas 2009."+Me.FIRST_NAME+" "+Me.LAST_NAME+" "+Me.LAST_NAME+" "+Me.WWW);
		out.println("usage:\n\tsvg2canvas [options] (stdin| <svg files>+ )");
		out.println("options:");
		out.println(" -o <fileout>");
		out.println(" -b use base64 for images rather than the URL (default:"+this.useBase64ForImages+")");
		out.println(" -p <integer> precision default:"+this.precision);
		out.println(" -w width [optional]");
		super.usage(out);
		}
	
	public static void transform(Document dom,PrintStream out)
		throws IOException
		{
		try
			{
			SVGToCanvas app= new SVGToCanvas();
			app.output=out;
			app.paintDocument(dom);
			out.flush();
			}
		catch (Exception e)
			{
			throw new IOException(e);
			}
		}
	
	@Override
	protected int processArgs(String[] args)
		{
		int optind=super.processArgs(args);
		
		try {
			DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
			f.setCoalescing(true);
			f.setNamespaceAware(true);
			f.setValidating(false);
			f.setExpandEntityReferences(true);
			f.setIgnoringComments(false);
			f.setIgnoringElementContentWhitespace(true);
			DocumentBuilder docBuilder= f.newDocumentBuilder();
			
			if(fileout!=null)
				{
				this.output=new PrintStream(this.fileout);
				}
			
			startHTML();
			
	            if(optind==args.length)
                        {
                        Document dom=docBuilder.parse(System.in);
                        paintDocument(dom);
                        }
                else
                        {
                        while(optind< args.length)
                            {
                            String fname=args[optind++];
                          	InputStream in=IOUtils.openInputStream(fname);
                          	Document dom=docBuilder.parse(in);
                          	paintDocument(dom);
                          	in.close();
                            }
                        }
			
			endHTML();
			this.output.print("\n");
			this.output.flush();
			
			if(fileout!=null)
				{
				this.output.close();
				}
			this.output=System.out;
		} catch (Exception e)
			{
			e.printStackTrace();
			throw new RuntimeException(e);
			}

		
		
		return optind;
		}
	
	public static void main(String[] args)
		{
		LOG.setLevel(Level.OFF);
		try
			{
			new SVGToCanvas().processArgs(args);
			} 
		catch(Throwable err)
			{
			err.printStackTrace();
			}
		}
	
}
