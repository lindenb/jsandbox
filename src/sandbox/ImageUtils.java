package sandbox;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class ImageUtils
	{
	public final List<String> SUFFIXES = Arrays.asList(".jpg",".jpeg",".JPG",".JPEG",".png",".PNG");
	
	private ImageUtils() {
		}
	public static ImageUtils getInstance() {
		return new ImageUtils();
		}
	
	public Optional<Dimension> getDimension(final String pathOrUrl) {
		if(StringUtils.isBlank(pathOrUrl)) return Optional.empty();
		InputStream in0 = null;
		ImageInputStream in = null;
		try
			{
			if(IOUtils.isURL(pathOrUrl)) {
				in0 = new URL(pathOrUrl).openStream();
				}
			else
				{
				in0 = Files.newInputStream(Paths.get(pathOrUrl));
				}
			in = ImageIO.createImageInputStream(in0);
		    final Iterator<ImageReader> readers = ImageIO.getImageReaders(in0);
		    if (readers.hasNext()) {
		        final ImageReader reader = readers.next();
		        try {
		            reader.setInput(in);
		           return Optional.of(new Dimension(reader.getWidth(0), reader.getHeight(0)));
		        } finally {
		            reader.dispose();
			        }
			    }
		    return Optional.empty();
			} 			
		catch(final Exception err) {
			return Optional.empty();
			}
		finally {
			IOUtils.close(in);
			IOUtils.close(in0);
			}
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
	public static String toBase64(final RenderedImage im,final String fmt) throws IOException {
		try(final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			ImageIO.write(im,fmt, os);
			os.flush();
			return Base64.getEncoder().encodeToString(os.toByteArray());
			}
		}
	public static String toBase64(final RenderedImage im) throws IOException {
		return toBase64(im,"PNG");
		}
	
	public static boolean isPng(final Path path) throws IOException {
		try(InputStream fin=Files.newInputStream(path)) {
			byte[] array = new byte[4];
			if(fin.read(array)!=array.length) return false;
			return array[0]==0x89 && 
				   array[1]==0x50 &&
				   array[2]==0x4e &&
				   array[2]==0x47;
			}
		}
	
	}
