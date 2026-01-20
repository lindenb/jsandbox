package sandbox.awt;

import java.awt.geom.AffineTransform;
import java.util.Arrays;

public class AffineTransformFactory {
	private AffineTransform tr = new AffineTransform();
	public AffineTransformFactory() {
		}
	
	public static AffineTransformFactory newInstance() {
		return new AffineTransformFactory();
	}
	
	private void concat(AffineTransform tr2) {
		this.tr.concatenate(tr2);
		}
	
	
	public AffineTransformFactory reset() {
		tr = new AffineTransform();
		return this;
		}
	
	public AffineTransformFactory translate(double dx,double dy) {
		concat(AffineTransform.getTranslateInstance(dx, dy));
		return this;
		}
	
	public AffineTransformFactory scale(double sx,double sy) {
		concat(AffineTransform.getScaleInstance(sx, sy));
		return this;
		}
	
	public AffineTransformFactory rotate(double theta) {
		concat(AffineTransform.getRotateInstance(theta));
		return this;
		}
	public AffineTransformFactory rotate(double theta,double dx,double dy) {
		concat(AffineTransform.getRotateInstance(theta,dx,dy));
		return this;
		}
	
	public AffineTransform make() {
		return  new AffineTransform(this.tr);
		}
	
	@Override
	public String toString() {
		final double[] m=new double[9];
		this.tr.getMatrix(m);;
		return "AffineTransformFactory("+Arrays.toString(m)+")";
		}
}
