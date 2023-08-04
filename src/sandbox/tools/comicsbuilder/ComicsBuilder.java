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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.svg.SVG;
import sandbox.xml.DefaultNamespaceContext;
import sandbox.xml.XMLException;
import sandbox.xml.XmlUtils;

public class ComicsBuilder extends Launcher {
	
	private static final Logger LOG = Logger.builder(ComicsBuilder.class).build();
	private static final String NS="uri:comics";
	
	@Parameter(names = "-o",description = "output directory")
	private File outDir=null;
	private Path layoutDir;
	private final XPath xpath;
	private final Map<String,Element> id2node = new HashMap<>();
	private final Map<String,Document> id2layout = new HashMap<>();
	private static int ID_GENERATOR = 0;
	
	
	
	ComicsBuilder() {
		final XPathFactory xpf = XPathFactory.newInstance();
		this.xpath = xpf.newXPath();
		this.xpath.setNamespaceContext(new DefaultNamespaceContext().
				put("c",NS).
				put("svg", SVG.NS)
				);
		}
	
	
	
	private void createDefaultLayouts(Document dom) {
		createKirbyLayout("3p1","0,0,1,1 0,1,1,1 0,2,1,1");
		createKirbyLayout("3p2","0,0,2,1 0,1,1,2 1,1,1,2");
		createKirbyLayout("3p3","0,0,1,2 1,0,1,1 1,1,1,1");
		createKirbyLayout("3p4","0,0,1,1 1,0,1,1 0,1,2,2");
		
		createKirbyLayout("4p1","0,0,1,1 0,1,1,1 0,2,1,1 0,3,1,1");
		createKirbyLayout("4p2","0,0,2,1 0,1,1,1 1,1,1,1 0,2,2,1");
		createKirbyLayout("4p3","0,0,1,1 1,0,1,1 0,1,1,1 1,1,1,1");
		createKirbyLayout("4p4","0,0,1,1 1,0,1,1 0,1,1,2 1,1,1,2");
		createKirbyLayout("4p5","0,0,1,3 1,0,1,1 1,1,1,1 1,2,1,1");
		createKirbyLayout("4p6","0,0,1,2 1,0,1,2 2,0,1,2 0,2,3,1");
		
		createKirbyLayout("5p1","0,0,2,1 0,1,1,1 1,1,1,1 0,2,1,1 1,2,1,1");
		createKirbyLayout("5p2","0,0,1,1 1,0,1,1 0,1,1,1 1,1,1,1 0,2,2,1");
		createKirbyLayout("5p3","0,0,1,1 1,0,1,1 2,0,1,1 0,1,3,1 0,2,3,1");
		createKirbyLayout("5p4","0,0,2,1 2,0,2,1 4,0,2,1 0,1,3,2 3,1,3,2");
		createKirbyLayout("5p5","0,0,3,1 0,1,1,1 1,1,1,1 2,1,1,1 0,2,3,1");
		
		createKirbyLayout("6p1","0,0,1,1 1,0,1,1 0,1,1,1 1,1,1,1 0,2,1,1 1,2,1,1");
		createKirbyLayout("6p2","0,0,1,1 1,0,1,1 2,0,1,1 0,1,1,2 1,1,2,1 1,2,2,1");

		createKirbyLayout("7p1","0,0,3,1 3,0,3,1 0,1,2,1 2,1,2,1 4,1,2,1 0,2,3,1 3,2,3,1");
		
		createKirbyLayout("8p1","0,0,1,1 1,0,1,1 0,1,1,1 1,1,1,1 0,2,1,1 1,2,1,1 0,3,1,1 1,3,1,1");
		createKirbyLayout("9p1","0,0,1,1 1,0,1,1 2,0,1,1 0,1,1,1 1,1,1,1 2,1,1,1 0,2,1,1 1,2,1,1 2,2,1,1");
	}
	
	
	private void createKirbyLayout(
			final String name,
			final String def
			)
		{
		final int A4_width = 2480;
		final int A4_height = 3508;
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		final Document dom = dbf.newDocumentBuilder().newDocument();
		final Element root = dom.createElementNS(SVG.NS,"svg:svg");
		root.setAttribute("id", name);
		root.setAttribute("width",String.valueOf(A4_width));
		root.setAttribute("height",String.valueOf(A4_height));
		final Element groot = dom.createElementNS(SVG.NS,"g");
		root.appendChild(groot);
		
		
		final String[] tokens = def.split("[ ]");
		double maxx=0.0;
		double maxy=0.0;
		for(int i=0;i< tokens.length;i++) {
			final String[] tokens2 =  tokens[i].split("[,]");
			maxx = Math.max(maxx,  Double.parseDouble(tokens2[0])+ Double.parseDouble(tokens2[2]));
			maxy = Math.max(maxy,  Double.parseDouble(tokens2[1])+ Double.parseDouble(tokens2[3]));
			}
		
		
		for(int i=0;i< tokens.length;i++) {
			final String[] tokens2 =  tokens[i].split("[,]");
			final Element E2 =dom.createElementNS(SVG.NS,"svg:rect");
			groot.appendChild(E2);
			E2.setAttribute("id",name+"."+(1+i));
			E2.setAttribute("x", String.valueOf(A4_width  * Double.parseDouble(tokens2[0])/maxx));
			E2.setAttribute("y", String.valueOf(A4_height  * Double.parseDouble(tokens2[1])/maxy));
			E2.setAttribute("width", String.valueOf(A4_width  * Double.parseDouble(tokens2[2])/maxx));
			E2.setAttribute("height", String.valueOf(A4_height  * Double.parseDouble(tokens2[3])/maxy));
			}
		if(id2layout.containsKey(name)) {
			throw new XMLException(root, "duplicate layout id "+name);
			}
		this.id2layout.put(name, dom);
		}
	
	@Override
	public int doWork(List<String> args) {
		try {
			Pages pages = null;
			final String input = oneAndOnlyOneFile(args);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			final Document dom= db.parse(input);
			final Element root = dom.getDocumentElement();
			if(root==null) {
				LOG.error("root missing");
				return -1;
				}
			NodeList gList =(NodeList)this.xpath.evaluate("/svg:svg/svg:g", dom, XPathConstants.NODESET);
			if(gList.getLength()==0) {
				throw new XMLException(root,"Cannot find /svg:svg/svg:g");
				}
			
			
			if(gList.getLength()>1) {
				throw new XMLException(root,"multiple /svg:svg/svg:g");
				}
			final Element gRoot =(Element)gList.item(0);
			final List<Element> pagesList = XmlUtils.asList(Element.class,(NodeList)this.xpath.evaluate("c:page", gRoot, XPathConstants.NODESET));
			if(pagesList.isEmpty()) {
				LOG.warning("no <c:page> under /svg/g");
				}
			// remove all child from parents
			for(Element E: pagesList) {
				gRoot.removeChild(E);
				}
			
			
			// process each page
			for(int i=0; i< pagesList.size();i++) {
				final Element page = pagesList.get(i);
				String title = "page"+(i+1);
				
				// get Layout for this page
				Attr att=page.getAttributeNode("layout-id");
				if(att==null) throw new XMLException(page,"missing layout-id");
				final String layoutId = att.getValue();
				if(StringUtils.isBlank(layoutId)) throw new XMLException(page,"empty layout-id");
				final Document pageLayout= this.id2layout.get(layoutId);
				if(pageLayout==null) throw new XMLException(page,"cannot find layout-id="+layoutId);
				final List<Element> panelRects = XmlUtils.asList(Element.class,
						(NodeList)this.xpath.evaluate("/svg:svg/svg:g/svg:rect",
								pageLayout,
								XPathConstants.NODESET)
						);
				
				// <defs> for this pages
				final Element defs = dom.createElementNS(SVG.NS, "defs");
				gRoot.appendChild(defs);
				for(Element E1 :panelRects) {
					final Element E = (Element)dom.importNode(E1, true);
					E.setAttribute("id", String.valueOf(++ID_GENERATOR));
					defs.appendChild(E);
				}
				//adjust page width
				if(!page.hasAttribute("width")) {
					page.setAttribute("width", this.xpath.evaluate("/svg:svg/@width",pageLayout));
					}
				//adjust page width
				if(!page.hasAttribute("height")) {
					page.setAttribute("height", this.xpath.evaluate("/svg:svg/@height",pageLayout));
					}
				// adjust main document width
				root.setAttribute("width", page.getAttribute("width"));
				root.setAttribute("height", page.getAttribute("height"));
				
				//clean up root
				while(gRoot.hasChildNodes()) {
					gRoot.removeChild(gRoot.getFirstChild());
					}
				}
			
			
			
			if(root.getLocalName().equals("svg")) {
				throw new XMLException(root,"Expected root to be svg but got "+root.getLocalName());
				}
			if(SVG.NS.equals(root.getNamespaceURI())) {
				throw new XMLException(root,"Expected xmnns to be "+SVG.NS+" but got "+root.getNamespaceURI());
				}

			createDefaultLayouts(dom);
			
			for(Node c1=root.getFirstChild();c1!=null;c1=c1.getNextSibling()) {
				if(XmlUtils.isNotElement(c1)) continue;
				final Element E1 = Element.class.cast(c1);
				if(E1.getLocalName().equals("defs"))
					{
					for(Node c2=E1.getFirstChild();c2!=null;c2=c2.getNextSibling()) {
						if(XmlUtils.isNotElement(c2)) continue;
						final Element E2 = Element.class.cast(c1);
						Attr idnode = E2.getAttributeNode("id");
						if(idnode==null) {
							LOG.warning("No id for "+XmlUtils.getNodePath(E2));
							continue;
							}
						if(id2node.containsKey(idnode.getValue())) {
							LOG.warning("duplicate id for "+XmlUtils.getNodePath(E2));
							continue;
							}
						this.id2node.put(idnode.getValue(), E2);
						}
					}
				else if(E1.getLocalName().equals("layouts"))
					{
					for(Node c2=E1.getFirstChild();c2!=null;c2=c2.getNextSibling()) {
						if(XmlUtils.isNotElement(c2)) continue;
						final Element E2 = Element.class.cast(c1);
						if(E2.getLocalName().equals("layout"))
							{
							PageLayout layout = new PageLayout(this, E2);
							if(id2layout.containsKey(layout.getId())) {
								throw new XMLException(E2, "duplicate layout id "+layout.getId());
								}
							}
						}
					}
				else if(E1.getLocalName().equals("pages"))
					{
					if(pages!=null) throw new XMLException(E1,"duplicate pages");
					pages= new Pages(this, E1);
					
					}
				else
					{
					LOG.warning("unprocessed node "+XmlUtils.getNodePath(E1));
					}
				}
			
			if(pages==null) throw new XMLException(root,"<pages> missing.");
			for(Page page:pages.getPages()) {
				
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
