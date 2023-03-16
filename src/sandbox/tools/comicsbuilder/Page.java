package sandbox.tools.comicsbuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import sandbox.xml.XMLException;
import sandbox.xml.XmlUtils;

public class Page extends AbstractNode {
	final Supplier<PageLayout> pageLayout;
	private List<AbstractPageComponent> components = new ArrayList<>();
	private final Pages pages;
	Page(Pages pages,Element root) {
		super(root);
		this.pages = pages;
		for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling()) {
			if(XmlUtils.isNotElement(n)) continue;
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
	
	double getWidth() {
		if(getElement().hasAttribute("width")) {
			return Double.parseDouble(getElement().getAttribute("width"));
			}
		else
			{
			getPageLayout().getWidth();
			}
		}
	
	Supplier<PageLayout> createDefaultLayout() {
	
		}
	
	PageLayout getPageLayout() {
		final Attr att = getElement().getAttributeNode("layout-id");
		if(att==null) throw new XMLException(getElement(), "@layout-id missing");
		
		return this.pageLayout.get();
		}
	}
