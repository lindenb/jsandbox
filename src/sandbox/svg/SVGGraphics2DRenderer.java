package sandbox.svg;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class SVGGraphics2DRenderer
	{
	private class Context {
		Context parent = null;
		Graphics2D g;
		AffineTransform tr;
		Context() {
			}
		Context(Context parent) {
			this.parent = parent;
			}
		}
	public void paint(Graphics2D g,final Document dom) {
		if(dom==null) return;
		Element root = dom.getDocumentElement();
		if(root==null) return;
		Context ctx= new Context();
		ctx.g = g;
		ctx.tr = ctx.g.getTransform();
		recurse(ctx,root);
		g.setTransform(ctx.tr);
		}
	private void recurse(Context ctx,Node node) {
		if(node==null) return;
		if(node.getNodeType()!=Node.ELEMENT_NODE) return;
		final Element root= Element.class.cast(node);
		final String name=root.getLocalName();
		if(name.equals("rect")) {
			
			}
		}
	}
