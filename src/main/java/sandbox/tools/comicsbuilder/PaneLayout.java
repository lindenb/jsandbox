package sandbox.tools.comicsbuilder;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import sandbox.xml.XmlUtils;

class PaneLayout extends Node{
	private final PageLayout owner;
	PaneLayout(PageLayout owner,Element root) {
		super(root);
		this.owner = owner;
	}
	
	public PageLayout getPageLayout() {
		return owner;
		}
	
	public Style getStyle() {
		Style style= getPageLayout().getStyle();
		
		return style;
		}
	
	private double parseRatio(String s) {
		double factor = 0.01;
		return Double.parseDouble(s) * factor;
	}
	
	public double getX() {
		return getPageLayout().getWidth() * parseRatio("");
	}
	public double getY() {
		return getPageLayout().getHeight() * parseRatio("");
	}
	public double getWidth() {
		Attr att = getElement().getAttributeNode("width");
		if(att!=null) {
			double factor = parseRatio(att.getValue());
			return getPageLayout().getWidth() * factor;
			}
		att = getElement().getAttributeNode("right");
		if(att!=null) {
			double factor = parseRatio(att.getValue());
			return getPageLayout().getWidth() * factor - getX();
			}
		throw new IllegalArgumentException("no @width or @right in "+XmlUtils.getNodePath(getElement()));
		}
	
	public double getHeight() {
		Attr att = getElement().getAttributeNode("height");
		if(att!=null) {
			double factor = parseRatio(att.getValue());
			return getPageLayout().getHeight() * factor;
			}
		att = getElement().getAttributeNode("bottom");
		if(att!=null) {
			double factor = parseRatio(att.getValue());
			return getPageLayout().getHeight() * factor - getX();
			}
		throw new IllegalArgumentException("no @height or @bottom in "+XmlUtils.getNodePath(getElement()));
	}
}
