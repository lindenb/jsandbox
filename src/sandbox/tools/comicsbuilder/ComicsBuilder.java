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
import sandbox.xml.XMLException;
import sandbox.xml.XmlUtils;

public class ComicsBuilder extends Launcher {
	private static final Logger LOG = Logger.builder(ComicsBuilder.class).build();
	@Parameter(names = "-o",description = "output directory")
	private File outDir=null;
	
	private final Map<String,Element> id2node = new HashMap<>();
	private final Map<String,PageLayout> id2layout = new HashMap<>();
	
	
	private void createDefaultLayouts(Document dom) {
		createKirbyLayout(dom, "3p1","0,0,1,1 0,1,1,1 0,2,1,1");
		createKirbyLayout(dom, "3p2","0,0,2,1 0,1,1,2 1,1,1,2");
		createKirbyLayout(dom, "3p3","0,0,1,2 1,0,1,1 1,1,1,1");
		createKirbyLayout(dom, "3p4","0,0,1,1 1,0,1,1 0,1,2,2");
		
		createKirbyLayout(dom, "4p1","0,0,1,1 0,1,1,1 0,2,1,1 0,3,1,1");
		createKirbyLayout(dom, "4p2","0,0,2,1 0,1,1,1 1,1,1,1 0,2,2,1");
		createKirbyLayout(dom, "4p3","0,0,1,1 1,0,1,1 0,1,1,1 1,1,1,1");
		createKirbyLayout(dom, "4p4","0,0,1,1 1,0,1,1 0,1,1,2 1,1,1,2");
		createKirbyLayout(dom, "4p5","0,0,1,3 1,0,1,1 1,1,1,1 1,2,1,1");
		createKirbyLayout(dom, "4p6","0,0,1,2 1,0,1,2 2,0,1,2 0,2,3,1");
		
		createKirbyLayout(dom, "5p1","0,0,2,1 0,1,1,1 1,1,1,1 0,2,1,1 1,2,1,1");
		createKirbyLayout(dom, "5p2","0,0,1,1 1,0,1,1 0,1,1,1 1,1,1,1 0,2,2,1");
		createKirbyLayout(dom, "5p3","0,0,1,1 1,0,1,1 2,0,1,1 0,1,3,1 0,2,3,1");
		createKirbyLayout(dom, "5p4","0,0,2,1 2,0,2,1 4,0,2,1 0,1,3,2 3,1,3,2");
		createKirbyLayout(dom, "5p5","0,0,3,1 0,1,1,1 1,1,1,1 2,1,1,1 0,2,3,1");
		
		createKirbyLayout(dom, "6p1","0,0,1,1 1,0,1,1 0,1,1,1 1,1,1,1 0,2,1,1 1,2,1,1");
		createKirbyLayout(dom, "6p2","0,0,1,1 1,0,1,1 2,0,1,1 0,1,1,2 1,1,2,1 1,2,2,1");

		createKirbyLayout(dom, "7p1","0,0,3,1 3,0,3,1 0,1,2,1 2,1,2,1 4,1,2,1 0,2,3,1 3,2,3,1");
		
		createKirbyLayout(dom, "8p1","0,0,1,1 1,0,1,1 0,1,1,1 1,1,1,1 0,2,1,1 1,2,1,1 0,3,1,1 1,3,1,1");
		createKirbyLayout(dom, "9p1","0,0,1,1 1,0,1,1 2,0,1,1 0,1,1,1 1,1,1,1 2,1,1,1 0,2,1,1 1,2,1,1 2,2,1,1");
	}
	
	
	private void createKirbyLayout(
			final Document dom,
			final String name,
			final String def
			)
		{
		final int A4_width = 2480;
		final int A4_height = 3508;
		final Element root = dom.createElement("layout");
		root.setAttribute("id", name);
		root.setAttribute("width",String.valueOf(A4_width));
		root.setAttribute("height",String.valueOf(A4_height));
		
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
			final Element E2 = dom.createElement("pane");
			root.appendChild(E2);
			E2.setAttribute("id",name+"."+(1+i));
			E2.setAttribute("x", String.valueOf(A4_width  * Double.parseDouble(tokens2[0])/maxx));
			E2.setAttribute("y", String.valueOf(A4_height  * Double.parseDouble(tokens2[1])/maxy));
			E2.setAttribute("width", String.valueOf(A4_width  * Double.parseDouble(tokens2[2])/maxx));
			E2.setAttribute("height", String.valueOf(A4_height  * Double.parseDouble(tokens2[3])/maxy));
			}
		final PageLayout layout = new PageLayout(this,root);
		if(id2layout.containsKey(layout.getId())) {
			throw new XMLException(root, "duplicate layout id "+layout.getId());
			}
		this.id2layout.put(layout.getId(), layout);
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
			final Element root=dom.getDocumentElement();
			if(root==null) {
				LOG.error("root missing");
				return -1;
				}
			if(root.getLocalName().equals("comics")) {
				throw new XMLException(root,"Expected root to be comics but got "+root.getLocalName());
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
