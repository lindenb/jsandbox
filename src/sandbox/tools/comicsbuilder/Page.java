package sandbox.tools.comicsbuilder;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import sandbox.awt.HasDimension;
import sandbox.xml.XmlUtils;

public class Page extends AbstractNode implements HasDimension {
	private final List<Element> components = new ArrayList<>();
	private final Pages pages;
	Page(Pages pages,Element root) {
		super(root);
		this.pages = pages;
		for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling()) {
			if(XmlUtils.isNotElement(n)) continue;
			this.components.add(Element.class.cast(n));
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
