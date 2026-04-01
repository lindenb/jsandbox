package sandbox.image;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import sandbox.io.IOUtils;

public class ImageUtils {
	private static class ImageMetaDataImpl implements ImageMetaData {
		String format;
		int width;
		int height;
		@Override
		public String getFormat() {
			return format;
		}

		@Override
		public int getWidth() {
			return width;
		}

		@Override
		public int getHeight() {
			return height;
		}
		
		}
	
	
	public static Optional<ImageMetaData> getMetaData(File f) {
		return getMetaData(f.toPath());
		}
	public static Optional<ImageMetaData> getMetaData(Path p) {
		try(InputStream in= Files.newInputStream(p)) {
			return getMetaData(in);
			}
		catch(Throwable err) {
			return Optional.empty();
			}
		}
	
	public static Optional<ImageMetaData> getMetaData(URL u) {
		try(InputStream in= u.openStream()) {
			return getMetaData(in);
			}
		catch(Throwable err) {
			return Optional.empty();
			}
		}
	
	public static Optional<ImageMetaData> getMetaData(String uri) {
		if(IOUtils.isURL(uri)) {
			try {
				return getMetaData(new URL(uri));
				}
			catch(MalformedURLException e) {
				return Optional.empty();
				}
			}
		else
			{
			return getMetaData(Paths.get(uri));
			}
		}
	
	private static Optional<ImageMetaData> getMetaData(InputStream in0) {
		try(ImageInputStream in = ImageIO.createImageInputStream(in0)) {
		    final Iterator<ImageReader> readers = ImageIO.getImageReaders(in0);
		    if (readers.hasNext()) {
		        final ImageReader reader = readers.next();
		        try {
		            reader.setInput(in);
		            final ImageMetaDataImpl m=new ImageMetaDataImpl();
		            m.format = reader.getFormatName();
		            m.width = reader.getWidth(0);
		            m.height = reader.getHeight(0);
		           return Optional.of(m);
		        } finally {
		            reader.dispose();
			        }
			    }
		    return Optional.empty();
			} 			
		catch(final Exception err) {
			return Optional.empty();
			}
		}
}
