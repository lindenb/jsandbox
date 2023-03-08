package sandbox.awt;

import java.awt.Dimension;

public class Dimension2D implements HasDimension {
	private final double width;
	private final double height;
	
	public Dimension2D(final Dimension d) {
		this(d.getWidth(),d.getHeight());
		}
	public Dimension2D(final Dimension2D d) {
		this(d.getWidth(),d.getHeight());
		}
	
	public Dimension2D(double width,double height) {
		this.width = width;
		this.height = height;
		}
	
	@Override
	public double getWidth() {
		return width;
		}
	@Override
	public double getHeight() {
		return height;
		}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==this) return true;
		if(obj==null || !(obj instanceof Dimension2D)) return false;
		final Dimension2D d = Dimension2D.class.cast(obj);
		return getWidth()==d.getWidth() && getHeight()==d.getHeight();
		}
	
	@Override
	public int hashCode() {
		return Double.hashCode(getWidth())*31 + Double.hashCode(getHeight());
		}
	
	@Override
	public String toString() {
		return "Dimension(w="+getWidth()+",h="+getHeight()+")";
		}
	}
