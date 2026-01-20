package sandbox.svg;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import sandbox.awt.Colors;
import sandbox.lang.StringUtils;

public class SVGGraphics2DRenderer
	{
	private class Context {
		Context parent = null;
		Element root;
		Graphics2D g;
		AffineTransform tr;
		String font_family="Courier";
		int font_style= Font.PLAIN;
		double font_size=10;
		Color stroke = Color.BLACK;
		Color fill = null;
		double fill_opacity=1.0;
		double stroke_opacity=1.0;
		double stroke_width=1.0;
		int stroke_cap=BasicStroke.CAP_ROUND;
		int stroke_join=BasicStroke.JOIN_MITER;
		
		Map<String,String> style = new HashMap<>();
		Context(Context parent,Element root) {
			this.parent = parent;
			if(parent!=null) {
				this.g=parent.g;
				this.font_family=parent.font_family;
				this.font_size=parent.font_size;
				this.stroke = parent.stroke;
				this.fill = parent.fill;
				this.stroke_opacity = parent.stroke_opacity;
				this.fill_opacity = parent.fill_opacity;
				this.stroke_width = parent.stroke_width;
				this.stroke_cap = parent.stroke_cap;
				this.stroke_join = parent.stroke_join;
				this.style.putAll(parent.style);
				this.tr=parent.tr;

				}
			this.root = root;
			if(root.hasAttribute("style")) {
				for(String token:  root.getAttribute("style").split("[;]")) {
					this.style.put(
							StringUtils.subStringBefore(token,":"),
							StringUtils.subStringAfter(token,":")
							);
					}
				}

			
			
			Optional<String> att= getAttr("stroke");
			if(att.isPresent()) {
				stroke = att.get().equals("none")?null:Colors.getInstance().parse(att.get()).orElse(stroke);
				}
			
			if((att=getAttr("fill")).isPresent()) {
				fill = att.get().equals("none")?null:Colors.getInstance().parse(att.get()).orElse(fill);
				}

			
			
			att= getAttr("stroke-linejoin");
			if(att.isPresent()) {
				this.stroke_join = SVGUtils.toBasicStrokeJoin(att.get()).orElse(this.stroke_join);
				}
			att= getAttr("stroke-linecap");
			if(att.isPresent()) {
				this.stroke_cap = SVGUtils.toBasicStrokeJoin(att.get()).orElse(this.stroke_cap);
				}
			
			if((att=getAttr("font-family")).isPresent()) {
				this.font_family = att.get();
				}
			if((att=getAttr("font-weight")).isPresent()) {
				if(att.get().equals("normal")) this.font_size=Font.PLAIN;
				else if(att.get().equals("bold")) this.font_size=Font.BOLD;
				}
			if((att=getAttr("font-style")).isPresent()) {
				if(att.get().equals("normal")) this.font_size=Font.PLAIN;
				else if(att.get().equals("bold")) this.font_size=Font.BOLD;
				else if(att.get().equals("italic")) this.font_size=Font.ITALIC;
				}
			
			
			
			this.font_size = getAttrAsDouble("font-size").orElse(this.font_size);
			
			this.stroke_width = getAttrAsDouble("stroke-width").orElse(this.stroke_width);

			
			if((att=getAttr("transform")).isPresent()) {
				AffineTransform atr = parent==null?new AffineTransform():new AffineTransform(parent.tr);
				atr.concatenate(SVGUtils.svgToaffineTransform(att.get()));
				this.tr=atr;
				}
			
			this.fill_opacity= getAttrAsDouble("fill-opacity").orElse( getAttrAsDouble("opacity").orElse(this.fill_opacity));
			this.stroke_opacity= getAttrAsDouble("stroke-opacity").orElse( getAttrAsDouble("opacity").orElse(this.stroke_opacity));
			}
		
		
		private Optional<String> getAttr(String key) {
			final Attr att= this.root.getAttributeNode(key);
			if(att!=null) return Optional.of(att.getValue());
			if(this.style.containsKey(key)) return Optional.of(this.style.get(key));
			return Optional.empty();
			}
		
		
		OptionalDouble getAttrAsDouble(String name) {
			if(root.hasAttribute(name)) return OptionalDouble.empty();
			try {
				return OptionalDouble.of(Double.parseDouble(root.getAttribute(name)));
				}
			catch(Throwable err) {
				return OptionalDouble.empty();
				}
			}
		
		OptionalDouble getAttrAsUnit(String name) {
			if(root.hasAttribute(name)) return OptionalDouble.empty();
			try {
				return SVGUtils.castUnit(root.getAttribute(name));
				}
			catch(Throwable err) {
				return OptionalDouble.empty();
				}
			}
		
		private void recurse() {
			final String name=root.getLocalName();
			if(name.equals("rect")) {
				OptionalDouble x = getAttrAsUnit("x");
				OptionalDouble y = getAttrAsUnit("y");
				OptionalDouble w = getAttrAsUnit("width");
				OptionalDouble h = getAttrAsUnit("height");
				if(x.isPresent() && y.isPresent() &&
					w.isPresent() && h.isPresent()) {
					paint(new Rectangle2D.Double(x.getAsDouble(), y.getAsDouble(), w.getAsDouble(), h.getAsDouble()));
					}
				}
			else if(name.equals("line")) {
				OptionalDouble x1 = getAttrAsUnit("x1");
				OptionalDouble x2 = getAttrAsUnit("x2");
				OptionalDouble y1 = getAttrAsUnit("y1");
				OptionalDouble y2 = getAttrAsUnit("y2");
				if(x1.isPresent() && x2.isPresent() &&
					y1.isPresent() && y2.isPresent()) {
					paint(new Line2D.Double(
							x1.getAsDouble(),
							y1.getAsDouble(),
							x2.getAsDouble(),
							y2.getAsDouble())
							);
					}
				}
			else if(name.equals("circle")) {
				OptionalDouble x = getAttrAsUnit("cx");
				OptionalDouble y = getAttrAsUnit("cy");
				OptionalDouble r = getAttrAsUnit("r");
				if(x.isPresent() && y.isPresent() && r.isPresent()) {
					double rr= r.getAsDouble();
					paint(new Ellipse2D.Double(
							x.getAsDouble() -rr ,
							y.getAsDouble() -rr,
							rr*2,
							rr*2
							));
					}
				}
			else if(name.equals("ellipse")) {
				OptionalDouble x = getAttrAsUnit("cx");
				OptionalDouble y = getAttrAsUnit("cy");
				OptionalDouble rx = getAttrAsUnit("rx");
				OptionalDouble ry = getAttrAsUnit("ry");
				if(x.isPresent() && y.isPresent() && rx.isPresent() && ry.isPresent()) {
					double rrx= rx.getAsDouble();
					double rry= ry.getAsDouble();
					paint(new Ellipse2D.Double(
							x.getAsDouble() -rrx ,
							y.getAsDouble() -rry,
							rrx*2,
							rry*2
							));
					}
				}
			else if(name.equals("text")) {
				OptionalDouble x = getAttrAsUnit("x");
				OptionalDouble y = getAttrAsUnit("y");
				String s=root.getTextContent();
				if(x.isPresent() && y.isPresent() &&
					!StringUtils.isBlank(s)) {
					drawString(x.getAsDouble(),y.getAsDouble(),s);
					}
				}
			else if(name.equals("polyline")) {
				Attr points = root.getAttributeNode("points");
				if(points!=null) {
					paint(SVGUtils.polylineToShape(points.getValue()));
					}
				}
			else if(name.equals("polygon")) {
				Attr points = root.getAttributeNode("points");
				if(points!=null) {
					paint(SVGUtils.polygonToShape(points.getValue()));
					}
				}
			else if(name.equals("path")) {
				Attr d = root.getAttributeNode("d");
				if(d!=null) {
					paint(SVGUtils.pathToShape(d.getValue()));
					}
				}
			else {
				recurse_children();
				}
			}
		private void recurse_children() {
			for(Node c=root.getFirstChild();c!=null;c=c.getNextSibling()) {
				if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
				Context ctx = new Context(this, Element.class.cast(c));
				ctx.recurse();
				}
			}
		
		protected boolean beforeFill() {
			if(this.fill==null || this.fill_opacity<=0) return false;
			this.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)this.fill_opacity));
			this.g.setColor(this.fill);
			return true;
			}
		
		protected boolean beforeStroke() {
			if(this.stroke==null || this.stroke_opacity<=0) return false;
			this.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(float)this.stroke_opacity));
			this.g.setStroke(new BasicStroke((float) stroke_width, stroke_cap, stroke_join));
			this.g.setColor(this.stroke);
			return true;
			}
		
		private void paint(Shape shape) {
			AffineTransform tr=g.getTransform();
			g.setTransform(this.tr);
			if(beforeFill()) {
				this.g.fill(shape);
				}
			if(beforeStroke()) {
				this.g.draw(shape);	
				}
			g.setTransform(tr);
			}
		
		private void drawString(double x,double y,String s) {
			this.g.setFont(new Font(this.font_family,this.font_style,(int)this.font_size));
			if(beforeFill()) {
				this.g.drawString(s, (float)x, (float)y);
				}
			if(beforeStroke()) {
				this.g.drawString(s, (float)x, (float)y);
				}
			}
		}
	
	
	
	public void paint(Graphics2D g,final Document dom) {
		if(dom==null) return;
		final Element root = dom.getDocumentElement();
		if(root==null) return;
		final Context ctx= new Context(null,root);
		ctx.g = g;
		ctx.tr = ctx.g.getTransform();
		ctx.recurse();
		g.setTransform(ctx.tr);
		}

	
	


	}
