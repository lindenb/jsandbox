package sandbox.awt;

public interface HasDimension {
	/** get width for this object */
	public double getWidth();
	/** get height for this object */
	public double getHeight();
	/** return true if width or height <= 0 */
	public default boolean isEmptyArea() {
		return getWidth()<=0 || getHeight()<=0;
		}
	}
