package sandbox.tools.ffcache;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import com.beust.jcommander.Parameter;

import sandbox.IOUtils;
import sandbox.ImageUtils;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.image.ImageResizer;
import sandbox.jcommander.DurationConverter;
import sandbox.jcommander.FileSizeConverter;
import sandbox.jcommander.NoSplitter;

public class FirefoxCache extends Launcher {
	private static final Logger LOG = Logger.builder(FirefoxCache.class).build();

	@Parameter(names= {"-o","--out"},description=OUTPUT_OR_STANDOUT)
	private Path output = null;
	@Parameter(names= {"-since","--since"},description=DurationConverter.OPT_DESC,converter=DurationConverter.class,splitter=NoSplitter.class)
	private Duration since = null;
	@Parameter(names= {"-min-size","--min-size"},description=FileSizeConverter.OPT_DESC,converter=FileSizeConverter.class,splitter=NoSplitter.class)
	private long min_size = 0L;
	@Parameter(names= {"-icon-size","--icon-size"},description="Icon size")
	private int icon_size = 32;
	@Parameter(names= {"-min-dim","--min-dim"},description="Min dimension either width or height.")
	private int min_dimension=100;

	
	private void recurse(final XMLStreamWriter w,final Path dir) throws IOException {
		if(!Files.exists(dir)) return;

		Files.walkFileTree(dir,new SimpleFileVisitor<Path>() {
			 	@Override
			 	public FileVisitResult visitFile(Path F, BasicFileAttributes attrs) throws IOException {
			 		if(!attrs.isRegularFile()) return FileVisitResult.CONTINUE;
			 		if(attrs.isSymbolicLink()) return FileVisitResult.CONTINUE;
			 		try {			 			
						if(!Files.isReadable(F)) return FileVisitResult.CONTINUE;
						if(since!=null) {
							final FileTime fileTime = attrs.creationTime();
							Duration dd =  Duration.ofMillis(System.currentTimeMillis()-fileTime.toMillis());
							if(since.compareTo(dd)<0) return FileVisitResult.CONTINUE;
							
							final long fsize = attrs.size();
							if(fsize < min_size) return FileVisitResult.CONTINUE;
							}
						try {
							BufferedImage img = ImageIO.read(F.toFile());
							if(img==null || (img.getWidth()< min_dimension && img.getHeight()< min_dimension ) ) return FileVisitResult.CONTINUE;
							ImageResizer resizer = new ImageResizer();
							resizer.setSize( icon_size);
							
							img = resizer.apply(img);
							String base64=ImageUtils.toBase64(img);
							w.writeStartElement("span");
							w.writeStartElement("a");
							w.writeAttribute("title",F.getFileName().toString());
							w.writeAttribute("href","file://"+ F.toString());
							w.writeAttribute("target","_blank");
							
							
							w.writeEmptyElement("img");
							w.writeAttribute("alt",F.getFileName().toString());
							w.writeAttribute("title",F.getFileName().toString());
							w.writeAttribute("src","data:image/png;base64,"+base64);
							w.writeAttribute("width",String.valueOf(img.getWidth()));
							w.writeAttribute("height",String.valueOf(img.getHeight()));
							w.writeEndElement();
							w.writeEndElement();
							w.writeCharacters("\n");
							}
						catch(Throwable err) {
							LOG.error(err);
							return FileVisitResult.CONTINUE;
							}
						}
					catch(final Throwable err) {
						LOG.error(err);
						}
			 		return FileVisitResult.CONTINUE;
			 		}
				@Override
			    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			        if (exc instanceof AccessDeniedException) {
			            return FileVisitResult.SKIP_SUBTREE;
			        }

			        return super.visitFileFailed(file, exc);
			    }
			});
		}

	
	@Override
	public int doWork(final List<String> args) {
		final String base = oneAndOnlyOneFile(args);
		XMLStreamWriter w;
		try {
			XMLOutputFactory xof = XMLOutputFactory.newFactory();
			w=xof.createXMLStreamWriter(IOUtils.openPathAsWriter(this.output));
			w.writeStartDocument("UTF-8", "1.0");
			w.writeStartElement("html");
			w.writeStartElement("body");
			recurse(w,Paths.get(base));
			w.writeEndElement();
			w.writeEndElement();			
			return 0;
			}
		catch(final Throwable err) {
			LOG.error(err);
			return -1;
			}
		}
	
	public static void main(String[] args) {
		new FirefoxCache().instanceMainWithExit(args);
		}
}
