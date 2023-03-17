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

import sandbox.awt.HasDimension;
import sandbox.xml.XMLException;
import sandbox.xml.XmlUtils;

public class Page extends AbstractNode implements HasDimension {
	private final List<AbstractNode> components = new ArrayList<>();
	private final Pages pages;
	Page(Pages pages,Element root) {
		super(root);
		this.pages = pages;
		for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling()) {
			if(XmlUtils.isNotElement(n)) continue;
			Element E1=Element.class.cast(n);
			AbstractNode component;
			if(E1.getLocalName().equals("text")) {
				component = new TextArea(this,E1);
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
	@Override
	public double getWidth() {
		if(getElement().hasAttribute("width")) {
			return Double.parseDouble(getElement().getAttribute("width"));
			}
		else
			{
			return this.pages.getWidth();
			}
		}
	
	@Override
	public double getHeight() {
		if(getElement().hasAttribute("width")) {
			return Double.parseDouble(getElement().getAttribute("width"));
			}
		else
			{
			return this.pages.getHeight();
			}
		}
	}
