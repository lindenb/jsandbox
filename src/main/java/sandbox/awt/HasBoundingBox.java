package sandbox.awt;


public interface HasBoundingBox extends HasXY,HasDimension {
	public default boolean containsXY(HasXY pt) {
		return containsXY(pt.getX(),pt.getY());
		}
	
	public default boolean containsXY(double x, double y) {
        final double x0 = getX();
        final double y0 = getY();
        return (x >= x0 &&
                y >= y0 &&
                x < x0 + getWidth() &&
                y < y0 + getHeight());
    	}
}
