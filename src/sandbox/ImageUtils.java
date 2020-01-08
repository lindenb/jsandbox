package sandbox;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

public class ImageUtils
	{
	public final List<String> SUFFIXES = Arrays.asList(".jpg",".jpeg",".JPG",".JPEG",".png",".PNG");
	
	private ImageUtils() {
		}
	public static ImageUtils getInstance() {
		return new ImageUtils();
		}
	
	public boolean hasImageSuffix(final File f) {
		return hasImageSuffix(f.toPath());
	}
	
	public boolean hasImageSuffix(final Path f) {
		return hasImageSuffix(f.getFileName().toString());
	}
	public boolean hasImageSuffix(final String f) {
		return SUFFIXES.stream().anyMatch(FX->f.endsWith(FX));
	}
	/** return format PNG or JPG (default) */
	public String formatForFile(final String s) {
		if(s!=null && s.toLowerCase().endsWith(".png")) return "PNG";
		return "JPG";
	}
	
	public BufferedImage readPath(final Path path) throws IOException {
		try (InputStream in=Files.newInputStream(path)) {
			return ImageIO.read(in);
		}
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
			return readPath(Paths.get(nullOrfileOrUrl));
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
	
	public void saveToPathOrStandout(final RenderedImage im,final Path out) throws IOException {
		final OutputStream os = (out==null?System.out:Files.newOutputStream(out));
		ImageIO.write(im,out==null?"PNG":formatForFile(out.getFileName().toString()), os);
		os.flush();
		os.close();
		}
	
	}
