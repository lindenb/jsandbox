package sandbox.tools.comicsbuilder;

import java.awt.Polygon;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.util.DefaultTreeNode;
import sandbox.util.stream.MyCollectors;
import sandbox.xml.DOMWrapper;
import sandbox.xml.XmlUtils;

public class ComicsBuilder extends Launcher {
	private static final Logger LOG = Logger.builder(ComicsBuilder.class).build();
	private long generate_id = 0L;
	@Parameter(names = "-o",description = "output directory")
	private File outDir=null;
	
	
	private interface NodeFactory {
		Class<? extends AbstractNode> getNodeClass();
		AbstractNode make(Element node);
		public String getNodeName();
		public default String getRefId() {
			return getNodeName()+"-id";
		}
		
	}
	
	private final List<NodeFactory> nodeFactories = new ArrayList<>();
	private ComicsRoot comicsRoot = null;
	
	
	private NodeFactory findNodeFactoryByName(final String name) {
		return this.nodeFactories.stream().filter(NF->name.equals(NF.getNodeName())).findFirst().get();
	}
	
	
	
	private class AbstractNode extends DefaultTreeNode<AbstractNode, Element> {
		AbstractNode(Element root) {
			super(root);
			}	
		}
	
	private class Style extends AbstractNode {
		Style(Element root) {
			super(root);
			}
		}
	
	
	private abstract class LiteralNode extends AbstractNode {
		LiteralNode(Element root) {
			super(root);
			}
		}
	private class DoubleNode extends LiteralNode implements DoubleSupplier{
		DoubleNode(Element root) {
			super(root);
			}
		@Override
		public double getAsDouble() {
			return 0;
			}
		}
	private class IntegerNode extends LiteralNode implements IntSupplier{
		IntegerNode(Element root) {
			super(root);
			}
		@Override
		public int getAsInt() {
			return 0;
			}
		}
	private class StringNode extends LiteralNode implements Supplier<String>{
		StringNode(Element root) {
			super(root);
			}
		@Override
		public String get() {
			return "";
			}
		}
	
	private abstract class Reference<X extends AbstractNode>implements Supplier<X> {
		protected Reference() {
			}
		@Override
		public abstract X get();
		}
	private class SoftReference<X extends AbstractNode> extends Reference<X> {
		private final String id;
		protected final Class<X> clazz;
		private AbstractNode target = null;
		SoftReference(Class<X> clazz,String id) {
			this.clazz = clazz;
			this.id = id;
			}
		@Override
		public X get() {
			if(this.target==null) {
				if(comicsRoot.defid2node.containsKey(this.id)) {
					throw new IllegalArgumentException("no definition found for @id="+id);
					}
				this.target = comicsRoot.defid2node.get(this.id);
				}
			return clazz.cast(this.target);
			}
		}
	
	private class HardReference<X extends AbstractNode> extends Reference<X> {
		private X target;
		HardReference(X target) {
			this.target = target;
			}
		@Override
		public X get() {
			return target;
			}
		}
	
	private class ComicsRoot extends AbstractNode {
		private Map<String,AbstractNode> defid2node = new HashMap<>();
		ComicsRoot(Element root) {
			super(root);
			for(Node c1=root.getFirstChild();c1!=null;c1=c1.getNextSibling()) {
				if(c1.getNodeType()!=Node.ELEMENT_NODE) continue;
				final Element E1 = Element.class.cast(c1);
				if(E1.getLocalName().equals("defs"))
					{
					for(Node c2=E1.getFirstChild();c2!=null;c2=c2.getNextSibling()) {
						if(c2.getNodeType()!=Node.ELEMENT_NODE) continue;
						final Element E2 = Element.class.cast(c1);
						Attr idnode = E2.getAttributeNode("id");
						if(idnode==null) {
							LOG.warning("No id for "+XmlUtils.getNodePath(E2));
							continue;
							}
						if(defid2node.containsKey(idnode.getValue())) {
							LOG.warning("duplicate id for "+XmlUtils.getNodePath(E2));
							continue;
							}
						}
					}
				else if(E1.getLocalName().equals("pages"))
					{
					for(Node c2=E1.getFirstChild();c2!=null;c2=c2.getNextSibling()) {
						if(c2.getNodeType()!=Node.ELEMENT_NODE) continue;
						final Element E2 = Element.class.cast(c1);
						if(E2.getLocalName().equals("page"))
							{
							appendChild((Page)makeNode(E1));
							}
						}
					}
				else if(E1.getLocalName().equals("layouts"))
					{
					for(Node c2=E1.getFirstChild();c2!=null;c2=c2.getNextSibling()) {
						if(c2.getNodeType()!=Node.ELEMENT_NODE) continue;
						final Element E2 = Element.class.cast(c1);
						if(E2.getLocalName().equals("layout"))
							{
							appendChild((PageLayout)makeNode(E1));
							}
						}
					}
				else
					{
					LOG.warning("unprocessed node "+XmlUtils.getNodePath(E1));
					}
				}
			}
		
		public List<Page> getPages() {
			return stream(Page.class).collect(Collectors.toList());
			}
		}
	
	
	
	
	private class Defs extends AbstractNode {
		Defs() {
			super(null);
		}
	}
	
	private class PageLayout extends AbstractNode {
		final List<Supplier<PaneLayout>> paneLayouts = new ArrayList<>();
		PageLayout(Element root) {
			super(root);
			for(Node c=root.getFirstChild();c!=null;c=c.getNextSibling()) {
				if(notElement(c)) continue;
				Element E1=Element.class.cast(c);
				if(E1.getLocalName().equals("pane")) {
					
					}
				}
			}
		private DoubleSupplier getSize(final String attName) {
			return ()->{
				return 1000.0;
				};
			}
		public DoubleSupplier getWidth() {
			return getSize("width");
			}
		public DoubleSupplier getHeight() {
			return getSize("height");
			}
	}
	
	private class PaneLayout extends AbstractNode {
		PaneLayout(Element root) {
			super(root);
			}
		}
	
	private class Page extends AbstractNode {
		final Supplier<PageLayout> pageLayout;
		private List<AbstractPageComponent> components = new ArrayList<>();
		Page(Element root) {
			super(root);
			//layout
			Attr att = root.getAttributeNode("layout-id");
			if(att!=null) {
				pageLayout = new SoftReference<>(PageLayout.class,att.getValue());
				}
			else
				{
				final Element x = XmlUtils.stream(root).
					filter(N->N.getNodeType()==Node.ELEMENT_NODE && N.getLocalName().equals("layout")).
					map(N->Element.class.cast(N)).
					collect(MyCollectors.oneOrNone()).
					orElse(null);
				if(x!=null) {
					pageLayout = new HardReference<>((PageLayout)makeNode(x));
					}
				else
					{
					pageLayout = createDefaultLayout();
					}
				}
			for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling()) {
				if(n.getNodeType()!=Node.ELEMENT_NODE) continue;
				Element E1=Element.class.cast(n);
				AbstractPageComponent component;
				if(E1.getLocalName().equals("text")) {
					component = new Text(this,E1);
					}
				else if(E1.getLocalName().equals("pane")) {
					component = new Pane(this,E1);
					}
				else
					{
					continue;
					}
				this.components.add(component);
				}
 			}
		
		DoubleSupplier getWidth() {
			return ()->{
				if(getUserData().hasAttribute("width")) {
					return Double.parseDouble(getUserData().getAttribute("width"));
					}
				else
					{
					pageLayout.get().getWidth();
					}
				};
			}
		
		
		
		Supplier<PageLayout> createDefaultLayout() {
		
			}
		
		PageLayout getPageLayout() {
			return this.pageLayout.get();
			}
		}
	
	private class AbstractPageComponent extends AbstractNode {
		private Page page;
		AbstractPageComponent(Page page,Element root) {
			super(root);
			this.page = page;
			}
		}
	
	private class Pane extends AbstractPageComponent {
		Pane(Page page,Element root) {
			super(page,root);
			}
		}
	
	private class Text extends AbstractPageComponent {
		Text(Page page,Element root) {
			super(page,root);
			}
		}
	
	private static interface HasDimension {
		public double getWidth();
		public double getHeight();
		}
	private static interface HasRectangularShape extends HasDimension {
		public double getX();
		public double getY();
		}
	
	private static class GoogleFont {
		/** source uri */
		private String url;
		
		}
	
	private static class RemoteImage extends AbstractNode implements HasDimension {
		RemoteImage(Element e) {
			super(e);
			}
		
		@Override
		public double getWidth() {
			return getAttributeAsDouble("width");
			}
		@Override
		public double getHeight() {
			return getAttributeAsDouble("height");
			}
		
		public String getSrc() {
			return getAttribute("src");
			}
		void save(XMLStreamWriter out) throws XMLStreamException {
			out.writeStartElement("image");
			out.writeAttribute("src",getSrc());
			out.writeAttribute("width",String.valueOf(getWidth()));
			out.writeAttribute("height",String.valueOf(getHeight()));
			
			out.writeEndElement();
			}
		}
	
	
	
	private static class Polygon extends AbstractList<Point2D> implements HasRectangularShape {
		private final List<Point2D> points = new ArrayList<>();
		public Rectangle2D getBounds() {
			if(isEmpty()) throw new IllegalStateException("empty polygon");
			Point2D pt = get(0);
			double minx=pt.getX(),miny=pt.getY(),maxx=minx,maxy=miny;
			for(int i=1;i< size();i++) {
				pt = get(i);
				minx = Math.min(minx, pt.getX());
				miny = Math.min(miny, pt.getY());
				maxx = Math.max(maxx, pt.getX());
				maxy = Math.max(maxy, pt.getY());
				}
			return new Rectangle2D.Double(minx,miny,maxx-minx,maxy-miny);
			}
		@Override
		public Point2D get(int index) {
			return points.get(index);
			}
		@Override
		public int size() {
			return points.size();
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
		void save(XMLStreamWriter out) throws XMLStreamException {
			out.writeStartElement("polygon");
			out.writeAttribute("points",
				this.points.stream().
				map(P->String.valueOf(P.getX())+","+String.valueOf(P.getY())).
				collect(Collectors.joining(" ")));
			out.writeEndElement();
			}
		}
	
	private static class ClippedImage implements HasDimension {
		public RemoteImage delegate;
		private Polygon polygon;

		@Override
		public double getWidth() {
			return polygon.getWidth();
			}
		@Override
		public double getHeight() {
			return polygon.getHeight();
			}
		void save(XMLStreamWriter out) throws XMLStreamException {
			out.writeStartElement("clipped-image");
			out.writeEndElement();
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
	
	
	private <X extends AbstractNode> Reference<X> findRequiredRef(Node root,Class<X> clazz) {
		for(Node c1=root.getFirstChild();c1!=null;c1=c1.getNextSibling()) {
			if(c1.getNodeType()!=null)
			}
		}
	
	private void makeCroppedImage(final Document dom,final Element root) {
		
	}
	
	private void makeRemoveImage(final Document dom,final Element root) {
		
		}
	
	
	private AbstractNode makeNode(Element E) {
		return findNodeFactoryByName(E.getLocalName()).make(E);
	}
	
	private void makePolygon(final Document dom,final Element root)  {
		Attr att= root.getAttributeNode("points");
		if(att==null) throw new XMLException(root,"@points missing.");
		final String points=att.getValue();
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
		
		final Set<GoogleFont> googleFont = new HashSet<>();
		
		XMLStreamWriter w = XMLOutputFactory.newInstance().createXMLStreamWriter(System.out);
		w.writeStartElement("html");
		w.writeStartElement("head");
		w.writeStartElement("title");
		w.writeEndElement();//title
		w.writeStartElement("style");
		
		w.writeEndElement();
		w.writeEndElement();//head
		w.writeStartElement("body");
		
		w.writeStartElement("div");
		
		w.writeEndElement();//div

		
		w.writeEndElement();//html
		w.writeEndElement();//html
		}
	
	

	
	private double parseDistance(final String s) {
		return Double.parseDouble(s);
	}
	
	
	private void parseDefs(final Document dom,Element root) {
		
	}
	
	@Override
	public int doWork(List<String> args) {
		try {
			final String input = oneAndOnlyOneFile(args);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			final Document dom= db.parse(input);
			final Element root=dom.getDocumentElement();
			if(root==null) {
				LOG.error("root missing");
				return -1;
				}
			if(root.getLocalName().equals("comics")) {
				LOG.error("Expected root to be comics but got "+root.getLocalName());
				return -1;
				}
			this.comicsRoot = new ComicsRoot(root);
			for(Page page:this.comicsRoot.getPages()) {
				
				}
			
			
			return 0;
			}
		catch(Throwable err) {
			err.printStackTrace();
			return -1;
			}
		}
	
	private void loadRemoteImages(Path dir) throws IOException {
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.svg");
		Files.list(dir).filter(P->matcher.matches(P)).forEach(P->loadRemoteImage(P));
		}
	private void loadRemoteImage(Path svg) throws IOException {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			final Document dom= db.parse(svg.toFile());
			final Element root = dom.getDocumentElement();
			RemoteImage img = new RemoteImage(root);
		} catch (ParserConfigurationException | SAXException err) {
			throw new IOException(err);
			}
		}

	private static boolean notElement(Node n) {
		switch(n.getNodeType()) {
			case Node.COMMENT_NODE: return true;
			case Node.CDATA_SECTION_NODE:
			case Node.TEXT_NODE:
				{
				String s=CharacterData.class.cast(n).getData();
				if(StringUtils.isBlank(s)) return true;
				throw new IllegalArgumentException("found non blank node "+XmlUtils.getNodePath(n));
				}
			case Node.ELEMENT_NODE: return false;
			default: throw new IllegalArgumentException("cannot handle node "+XmlUtils.getNodePath(n));
			}
		}
	
	
	public static void main(String[] args) {
		new ComicsBuilder().instanceMainWithExit(args);
		}
	
	}
