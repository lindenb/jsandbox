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

public class Pages extends AbstractNode {
	private final List<Page> pages = new ArrayList<>();
	private final ComicsBuilder comicsBuilder;
	Pages(final ComicsBuilder comicsBuilder,Element root) {
		super(root);
		this.comicsBuilder = comicsBuilder;
		for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling()) {
			if(XmlUtils.isNotElement(n)) continue;
			final Element E1=Element.class.cast(n);
			if(E1.getLocalName().equals("page")) {
				this.pages.add(new Page(this, E1));
				}
			}
		}
	
	double getWidth() {
		Attr  att = getElement().getAttributeNode("width");
		if(att!=null) {
			return Double.parseDouble(att.getValue());
			}
		throw new XMLException(getElement(),"missing @width");
		}
	
	double getHeight() {
		Attr  att = getElement().getAttributeNode("height");
		if(att!=null) {
			return Double.parseDouble(att.getValue());
			}
		throw new XMLException(getElement(),"missing @height");
		}
	
	}
