package sandbox.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.UnaryOperator;

public class ImageResizer implements UnaryOperator<BufferedImage> {
	private int size = 64;
	private Color background = Color.BLACK;
	public void setSize(int size) {
		this.size = size;
		}
	public void setBackground(Color background) {
		this.background = background;
	}
	
	@Override
	public BufferedImage apply(final BufferedImage img) {
		float width = img.getWidth();
		float height = img.getHeight();
		int w2;
		int h2;
		if(width >= height ) {
        	w2 = this.size;
        	h2 = (int)(height * (this.size/width));
        	}
		else
			{
        	h2 = this.size;
        	w2 = (int)(width * (this.size/height));
			}
		BufferedImage img2=new BufferedImage(
    			size,
    			size,
    			img.getType()
    			);
    	final Graphics2D g=(Graphics2D)img2.getGraphics();
    	g.setColor(background);
    	g.fillRect(0,0,size,size);
    	g.drawImage(img,
    			(size-w2)/2,
    			(size-h2)/2,
    			(size-w2)/2 + w2,
				(size-h2)/2 + h2,
				0,0,
    			img.getWidth(),
    			img.getHeight(),
    			null);
    	g.dispose();
		return img2;
	}

}
