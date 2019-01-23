package sandbox;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import java.awt.Dimension;

public class DimensionConverter
	implements Function<String,Dimension>
	{
	@Override
	public Dimension apply(final String dimStr) {
		if(dimStr.toLowerCase().matches("\\d+x\\d+")) {
			final int x_symbol = dimStr.toLowerCase().indexOf("x");
			return new Dimension(
					Integer.parseInt(dimStr.substring(0,x_symbol)),
					Integer.parseInt(dimStr.substring(1+x_symbol))
					);
			}
	
		final File f= new File(dimStr);
		if(!f.exists() || !f.isFile()) {
			throw new IllegalArgumentException("not an existing file: "+f);
			}
		if(f.getName().endsWith(".xcf"))
				{
				try(FileInputStream fis=new FileInputStream(f))
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
			
			try(ImageInputStream in = ImageIO.createImageInputStream(f)){
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
			catch(final IOException err) {
				throw new IllegalArgumentException(err);
				}
			}
		}
	}
