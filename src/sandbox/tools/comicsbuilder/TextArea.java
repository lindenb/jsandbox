package sandbox.tools.comicsbuilder;

import org.w3c.dom.Element;

public class TextArea extends AbstractNode implements HasRectangularShape {
	private final AbstractNode parent;
	TextArea(AbstractNode parent,Element root) {
		super(root);
		this.parent = parent;
		}
	public Style getStyle() {
		return null;
		}
	@Override
	public double getX() {
		return 0.0;
		}
	@Override
	public double getY() {
		return 0.0;
		}
	@Override
	public double getWidth() {
		return 0.0;
		}
	@Override
	public double getHeight() {
		return 0.0;
		}
	}
