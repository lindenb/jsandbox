package sandbox.drawing;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

public enum ShapeFactory {
square,
circle;
public Shape create(double cx,double cy,double radius,double radian) {
	switch(this)
		{
		case circle:
			return new Ellipse2D.Double(cx-radius, cy - radius, radius*2.0, radius*2.0);
		case square:
			if(radian==0.0) return new Rectangle2D.Double(cx-radius, cy - radius, radius*2.0, radius*2.0);
			GeneralPath g=new GeneralPath();
			for(int i=0;i< 4;i++)
				{
				double px = cx + radius * Math.cos(radian);
				double py = cy + radius * Math.sin(radian);
				if(i==0) 
					{
					g.moveTo(px, py);
					}
				else
					{
					g.lineTo(px, py);
					}
				radian+= Math.PI/2.0;
				}
			g.closePath();
			return g;
		default: throw new IllegalStateException(this.name());
		}
	}
public Shape create(double cx,double cy,double radius) {
	return create(cx,cy,radius,0.0);
	}

}
