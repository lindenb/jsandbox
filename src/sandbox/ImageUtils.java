package sandbox;

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
	
	public BufferedImage read(final String fileOrUrl) throws IOException {
		if(IOUtils.isURL(fileOrUrl)) {
			return ImageIO.read(new URL(fileOrUrl));
			}
		else
			{
			return ImageIO.read(new File(fileOrUrl));
			}
		}
	}
