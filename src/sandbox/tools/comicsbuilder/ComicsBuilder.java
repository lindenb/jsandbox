package sandbox.tools.comicsbuilder;

import java.awt.Polygon;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.xml.XMLException;

public class ComicsBuilder extends Launcher {
	private long generate_id = 0L;
	@Parameter(names = "-o",description = "output directory")
	private File outDir=null;
	private int number_of_pages = 0;
	
	
	private static interface HasDimension {
		public double getWidth();
		public double getHeight();
		}
	private static interface HasRectangularShape extends HasDimension {
		public double getX();
		public double getY();
		}
	
	private static class RemoteImage implements HasDimension {
		public String uri;
		private int width;
		private int height;

		@Override
		public double getWidth() {
			return width;
			}
		@Override
		public double getHeight() {
			return height;
			}
		}
	
	private static class CroppedImage implements HasDimension {
		public RemoteImage delegate;
		private Polygon polygon;

		public Rectangle2D getBounds() {
			return polygon.getBounds2D();
			}
		
		@Override
		public double getWidth() {
			return getBounds().getWidth();
			}
		@Override
		public double getHeight() {
			return getBounds().getHeight();
			}
		}
	
	private static class PageLayout extends AbstractList<PanelLayout> implements HasDimension {
		private String id;
		private double width;
		private double height;
		private final List<PanelLayout> panels = new ArrayList<>();
		public String getId() {
			return this.id;
			}
		@Override
		public double getWidth() {
			return width;
			}
		@Override
		public double getHeight() {
			return height;
			}
		@Override
		public PanelLayout get(int idx) {
			return this.panels.get(idx);
			}
		@Override
		public int size() {
			return this.panels.size();
			}
		@Override
		public PageLayout clone() {
			final PageLayout cp = new PageLayout();
			cp.id=id;
			cp.width = getWidth();
			cp.height = getHeight();
			for(PanelLayout p:this.panels) {
				PanelLayout p2 = p.clone();
				p2.owner = cp;
				cp.panels.add(p2);
				}
			return cp;
			}	
		}
	private static class PanelLayout implements HasRectangularShape {
		private PageLayout owner;
		private String id;
		private double fx,fy,fw,fh;
		public String getId() {
			return this.id;
			}
		public Rectangle2D.Double getBounds() {
			return null;
			}
		@Override
		public double getX() {
			return getBounds().getX();
			}
		@Override
		public double getY() {
			return getBounds().getY();
			}
		@Override
		public double getWidth() {
			return getBounds().getWidth();
			}
		@Override
		public double getHeight() {
			return getBounds().getHeight();
			}
		@Override
		public PanelLayout clone() {
			PanelLayout cp = new PanelLayout();
			cp.owner = owner;
			cp.id = id;
			cp.fx = fx;
			cp.fy = fy;
			cp.fw = fw;
			cp.fh = fh;
			return cp;
			}
		}
	
	
	
	public String nextId() {
		return "n"+(++generate_id);
		}
	
	private void makeRemoveImage(final Document dom,final Element root) {
		
		}
	
	
	private void makeLayout(final Document dom,final Element root) {
		/* find layout */
		Attr att= root.getAttributeNode("id");
		if(att==null) throw new XMLException(root,"@id missing.");
		final String layoutId = att.getValue();
		if(findPageLayoutByName(dom,layoutId)!=null) throw new XMLException(att,"Duplicate layout with id ="+ layoutId+".");
		/* use a copy of this layout */
		PageLayout layout = new PageLayout();
		layout.id = layoutId;
		/* override width ?*/
		att= root.getAttributeNode("width");
		if(att!=null) layout.width = parseDistance(att.getValue());
		/* override height ?*/
		att= root.getAttributeNode("height");
		if(att!=null) layout.height = parseDistance(att.getValue());
		for(Node c0 = root.getFirstChild();c0!=null;c0=c0.getNextSibling()) {
			if(c0.getNodeType()!=Node.ELEMENT_NODE) continue;
			final Element e1 = Element.class.cast(c0);
			if(e1.getLocalName().equals("pane")) {
				makePanelLayout(layout,e1);
				}
			}
		}
	private void makePanelLayout(final PageLayout layout,final Element root) {
		PanelLayout pane= new PanelLayout();
		pane.owner = layout;
		/* override width ?*/
		Attr att= root.getAttributeNode("width");
		if(att!=null) layout.width = parseDistance(att.getValue());
		layout.panels.add(pane);
		}
	
	private void makePage(final Document dom,final Element page) {
		this.number_of_pages++;
		/* find layout */
		Attr att= page.getAttributeNode("layout-id");
		if(att==null) throw new XMLException(page,"@layout-id missing.");
		final String layoutId = att.getValue();
		PageLayout layout = findPageLayoutByName(dom,layoutId);
		if(layout==null) throw new XMLException(att,"Cannot find layout with id ="+ layoutId+".");
		/* use a copy of this layout */
		layout = layout.clone();
		/* override width ?*/
		att= page.getAttributeNode("width");
		if(att!=null) layout.width = parseDistance(att.getValue());
		/* override height ?*/
		att= page.getAttributeNode("height");
		if(att!=null) layout.height = parseDistance(att.getValue());
		
		
		
		
		}
	
	
	
	PageLayout findPageLayoutByName(final Document dom,final String id) {
		return null;
	}
	
	private double parseDistance(final String s) {
		return Double.parseDouble(s);
	}
	
	@Override
	public int doWork(List<String> args) {
		try {
			final String input = oneAndOnlyOneFile(args);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			final Document dom= db.parse(input);
			final Element root=dom.getDocumentElement();
			for(Node c0 = root.getFirstChild();c0!=null;c0=c0.getNextSibling()) {
				if(c0.getNodeType()!=Node.ELEMENT_NODE) continue;
				final Element e1 = Element.class.cast(c0);
				if(e1.getLocalName().equals("page")) {
					makePage(dom,e1);
					}
				}
			return 0;
			}
		catch(Throwable err) {
			err.printStackTrace();
			return -1;
			}
		}
	
	public static void main(String[] args) {
		new ComicsBuilder().instanceMainWithExit(args);
		}
	
	}
