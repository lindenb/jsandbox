package sandbox.tools.comicsbuilder;

import org.w3c.dom.Element;

class PaneLayout extends AbstractNode{
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
		return getPageLayout().getWidth() * parseRatio("");
	}
	public double getHeight() {
		return getPageLayout().getHeight() * parseRatio("");
	}
}
