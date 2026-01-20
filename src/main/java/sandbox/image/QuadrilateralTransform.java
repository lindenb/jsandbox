package sandbox.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

/* https://stackoverflow.com/questions/36372692 */
public class QuadrilateralTransform {
 private final int step;
 
 public QuadrilateralTransform() {
	 this(1);
 }
 public QuadrilateralTransform(int step) {
	 this.step = step;
 }

 
 // render image to quad using current settings
public void paint(final Graphics2D ctx,
		 final BufferedImage img,
		 final Point2D corners[],
		 final Color bckg,
		 final ImageObserver imageObserver
		 ) {
	if(corners==null || corners.length!=4) throw new IllegalArgumentException("bad corners input");	
	paint(ctx, img, corners[0], corners[1],corners[2],corners[3],bckg, imageObserver);
	}
 
  // render image to quad using current settings
 public void paint(final Graphics2D ctx,
		 final BufferedImage img,
		 final Point2D corner0,
		 final Point2D corner1,
		 final Point2D corner2,
		 final Point2D corner3,
		 final Color bckg,
		 final ImageObserver imageObserver
		 ) {
	
    final int    w = img.getWidth() - 1;         // -1 to give room for the "next" points
    final int    h = img.getHeight() - 1;
    final Point2D.Double y0 = new Point2D.Double();
    final Point2D.Double y1 = new Point2D.Double();
    final Point2D.Double y2 = new Point2D.Double();
    final Point2D.Double y3 = new Point2D.Double();

    

    final Point2D.Double p0 = new Point2D.Double();
    final Point2D.Double p1 = new Point2D.Double();
    final Point2D.Double p2 = new Point2D.Double();
    final Point2D.Double p3 = new Point2D.Double();

    /**
    C0 +-----+---------------+C1
       |     |               |
    Y0 +-----+P0---+P1-------+ Y1
       |     |     |         |
    Y3 +-----+P3---+P2-------+ Y2
       |                     |
    C3 +---------------------+C2
    
    */   
    for(double y = 0; y < h; y += this.step) {
      for(double x = 0; x < w; x += this.step) {
          lerp(corner0, corner3,  y / h, y0);
          lerp(corner0, corner3,  Math.min(h,y+this.step)/h, y3);

          lerp(corner1, corner2,  y / h, y1);
          lerp(corner1, corner2,  Math.min(h,y+this.step)/h, y2);
        
        

		   lerp(y0, y1,  x / w, p0);
		   lerp(y0, y1,  Math.min(w,x+this.step) / w, p1);
		   lerp(y3, y2,  x / w, p3);
		   lerp(y3, y2,  Math.min(w,x+this.step) / w, p2);
		   
		   ctx.drawImage(img,
				  (int)p0.x,
				  (int)p0.y,
				  (int)p2.x,
				  (int)p2.y,
				  (int)x,
				  (int)y,
				  (int)Math.min(x+step,w),
				  (int)Math.min(y+step,w),
				  bckg,
				  imageObserver
				  );

      }
    }
  }
  
  
  private static void lerp(final Point2D p1, final Point2D p2, double t,final Point2D.Double dest) {
     dest.setLocation(
      p1.getX() + (p2.getX() - p1.getX()) * t, 
      p1.getY() + (p2.getY() - p1.getY()) * t
  	);
  }
}
