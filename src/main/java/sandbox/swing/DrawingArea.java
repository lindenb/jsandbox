package sandbox.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import sandbox.awt.Dimension2D;

@SuppressWarnings("serial")
public abstract class DrawingArea extends JPanel{
public DrawingArea() {
	super(null,true);
	setOpaque(true);
	setBackground(Color.WHITE);
	}
@Override
protected final void paintComponent(Graphics g) {
	paintDrawingArea(Graphics2D.class.cast(g));
	}

public abstract void paintDrawingArea(Graphics2D g);

public Dimension2D getDimension() {
	return new Dimension2D(getSize());
	}

public BufferedImage getScreenshot() {
	final BufferedImage img = new BufferedImage(
		this.getWidth(),
		this.getHeight(),
		BufferedImage.TYPE_INT_ARGB_PRE
		);
	final Graphics  g = img.getGraphics();
	paintComponent(g);
	g.dispose();
	return img;
	}

}
