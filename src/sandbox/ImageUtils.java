package sandbox;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

public class ImageUtils
	{
	private ImageUtils() {
		}
	public static ImageUtils getInstance() {
		return new ImageUtils();
		}
	
	public String formatForFile(String s) {
		if(s!=null && s.toLowerCase().endsWith(".png")) return "PNG";
		return "JPG";
	}
	
	public BufferedImage read(final String nullOrfileOrUrl) throws IOException {
		if(nullOrfileOrUrl==null) {
			return ImageIO.read(System.in);
			}
		else if(IOUtils.isURL(nullOrfileOrUrl)) {
			return ImageIO.read(new URL(nullOrfileOrUrl));
			}
		else
			{
			return ImageIO.read(new File(nullOrfileOrUrl));
			}
		}
	
	public Graphics2D createGraphics(final BufferedImage img) {
		return img.createGraphics();
	}
	
	public BufferedImage scaleForHeight(final BufferedImage img,int height) {
		if(img.getHeight()==height) return img;
		int width = (int)(img.getWidth()*(height/(double)img.getHeight()));
		final BufferedImage dest = new BufferedImage(width, height, img.getType());
		final Graphics2D g = createGraphics(dest);
		g.drawImage(img, 0, 0, width, height, null);
		g.dispose();
		return dest;
		}
	
	}
