package sandbox.jcommander;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.beust.jcommander.IStringConverter;

import sandbox.Logger;
import sandbox.StringUtils;

public class DimensionConverter
	implements Function<String,Dimension>
	{
	private static final Logger LOG = Logger.builder(DimensionConverter.class).build();
	public static final String OPT_DESC="";
	
	public static class StringConverter implements IStringConverter<Dimension> {
		@Override
		public Dimension convert(final String dimStr) {
			return new DimensionConverter().apply(dimStr);
			}
	}
	
	
	@Override
	public Dimension apply(final String dimStr) {
		if(StringUtils.isBlank(dimStr)) throw new IllegalArgumentException("empty string");
		if(dimStr.toLowerCase().matches("\\d+x\\d+")) {
			final int x_symbol = dimStr.toLowerCase().indexOf("x");
			return new Dimension(
					Integer.parseInt(dimStr.substring(0,x_symbol)),
					Integer.parseInt(dimStr.substring(1+x_symbol))
					);
			}
	
		final Path f= Paths.get(dimStr);
		if(!Files.exists(f) || !Files.isRegularFile(f)) {
			throw new IllegalArgumentException("not an existing file: "+f);
			}
		if(f.getFileName().toString().endsWith(".xcf"))
				{
				try(InputStream fis=Files.newInputStream(f))
					{
					byte array[]=new byte[9];
					fis.read(array);
					if(!Arrays.equals(array, "gimp xcf ".getBytes()))
						{
						throw new IOException("bad gimp xcf header");
						}
					
					array=new byte[5];
					if(fis.read(array)!=array.length) {
						throw new IOException("bad gimp xcf header");
						}
					LOG.info("version "+new String(array));
					array=new byte[8];
					if(fis.read(array)!=array.length) {
						throw new IOException("bad gimp xcf header");
						}
					final ByteBuffer buf = ByteBuffer.wrap(array); // big endian by default
				    buf.put(array);
				    buf.position(0);
				    final int w= buf.getInt();
				    final int h= buf.getInt();
				    LOG.info("width of "+f+" is "+w+"x"+h);
				    return new Dimension(w,h);
					}
				catch(final IOException err) {
					throw new IllegalArgumentException(err);
					}
				}
			
			try(ImageInputStream in = ImageIO.createImageInputStream(f.toFile())){
				if(in!=null) {
				    final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
				    if (readers.hasNext()) {
				        final ImageReader reader = readers.next();
				        try {
				            reader.setInput(in);
				           return new Dimension(reader.getWidth(0), reader.getHeight(0));
				        } finally {
				            reader.dispose();
					        }
					    }
					}
				} 			
			catch(final IOException err) {
				throw new IllegalArgumentException(err);
				}
			throw new IllegalArgumentException("cannot get dimension from "+dimStr);
			}
		}
