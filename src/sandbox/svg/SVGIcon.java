package sandbox.svg;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SVGIcon implements Icon
	{
	private final Document dom;
	private final int width;
	private final int height;
	
	public SVGIcon(final Document dom,int width,int height) {
		this.dom = dom;
		this.width = width;
		this.height = width;
		}
	
	public SVGIcon(final Document dom) {
		this.dom = dom;
		int w=0,h=0;
		final Element root= dom.getDocumentElement();
		if(root!=null) {
			Attr att = root.getAttributeNode("width");
			if(att!=null) w=(int)Double.parseDouble(att.getValue());
			att = root.getAttributeNode("height");
			if(att!=null) h=(int)Double.parseDouble(att.getValue());
			}
		this.width=w;
		this.height=h;
		}
	
	@Override
	public int getIconHeight()
		{
		return this.height;
		}

	@Override
	public int getIconWidth()
		{
		return this.width;
		}
	
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y)
		{
		final SVGGraphics2DRenderer renderer = new SVGGraphics2DRenderer();
		renderer.paint(Graphics2D.class.cast(g),this.dom);
		}


	
	}
