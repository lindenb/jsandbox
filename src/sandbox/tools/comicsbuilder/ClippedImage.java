package sandbox.tools.comicsbuilder;

import java.awt.Shape;
import java.util.function.Supplier;


import sandbox.awt.HasDimension;


class ClippedImage implements HasDimension{
	public Supplier<RemoteImage> delegate;
	private Supplier<Shape> shapeSupplier;

	private Shape getShape() {
		return this.shapeSupplier.get();
	}
	
	@Override
	public double getWidth() {
		return getShape().getBounds2D().getWidth();
		}
	@Override
	public double getHeight() {
		return getShape().getBounds2D().getHeight();
		}
	
	}
