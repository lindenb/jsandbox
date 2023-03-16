package sandbox.tools.comicsbuilder;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import sandbox.awt.HasDimension;
import sandbox.xml.XMLException;
import sandbox.xml.XmlUtils;

public class PageLayout extends AbstractNode implements HasDimension {
	private final List<PaneLayout> panes = new ArrayList<>();
	private final ComicsBuilder owner;
	PageLayout(ComicsBuilder owner,final Element root) {
		super(root);
		this.owner = owner;
		for(Node n1=root.getFirstChild();n1!=null;n1=n1.getNextSibling()) {
			if(XmlUtils.isNotElement(n1)) continue;
			final Element e1 = (Element)n1;
			if(e1.getLocalName().equals("pane")) {
				this.panes.add(new PaneLayout(this,e1));
				}
			else
				{
				
				}
			}
		if(panes.isEmpty()) throw new XMLException(root,"No pane defined");
		}
	
	public String getId() {
		return getRequiredAttribute("id");
		}
	
	public Style getStyle() {
		return new Style();
		}
	
	private double getSize(final String key) {
		try {
			return Double.parseDouble(	getRequiredAttribute(key) );
			}
		catch(NumberFormatException err) {
			throw new IllegalArgumentException("bad value for @"+key+":"+ XmlUtils.getNodePath(getElement()));
			}
		}
	
	@Override
	public double getWidth() {
		return getSize("width");
		}
	@Override
	public double getHeight() {
		return getSize("height");
		}
	
	public List<PaneLayout> getPanes() {
		return panes;
		}
	}
