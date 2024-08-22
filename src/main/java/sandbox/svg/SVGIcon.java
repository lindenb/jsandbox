package sandbox.svg;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.Icon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import sandbox.io.RuntimeIOException;

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
			Attr att1 = root.getAttributeNode("width");
			Attr att2 = root.getAttributeNode("height");
			if(att1!=null && att2!=null) {
				w=(int)SVGUtils.castUnit(att1.getValue()).orElse(w);
				h=(int)SVGUtils.castUnit(att2.getValue()).orElse(h);
				}
			else
				{
				Attr att = root.getAttributeNode("viewBox");
				String[] tokens=att.getValue().trim().split("[ \t]+");
				if(tokens.length>=4) {
					 w=(int)SVGUtils.castUnit(tokens[2]).orElse(w);
					 h=(int)SVGUtils.castUnit(tokens[3]).orElse(h);
					}
				}
			}
		this.width=w;
		this.height=h;
		System.err.println(""+getIconWidth()+" "+getIconHeight());
		}
	
	private static Document loadSVGDoc(Path p) {
		try {
		DocumentBuilderFactory dbf=DocumentBuilderFactory.newDefaultInstance();	
		dbf.setNamespaceAware(true);
		DocumentBuilder db=dbf.newDocumentBuilder();
		try(InputStream in=Files.newInputStream(p)) {
			return db.parse(in);
		}
		} catch(Throwable err) {
			throw new RuntimeIOException(err);
		}
		
	}
	
	public SVGIcon(final Path p) {
		this(loadSVGDoc(p));
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
