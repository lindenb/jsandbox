package sandbox.tools.dusuffix;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.jcommander.FileSizeConverter;
import sandbox.jcommander.NoSplitter;
import sandbox.lang.Counter;
import sandbox.lang.StringUtils;
import sandbox.tools.central.ProgramDescriptor;
import sandbox.tools.treemap.TreeMapMaker;

public class DiskUsageBySuffix extends Launcher{
	private static final Logger LOG = Logger.builder(DiskUsageBySuffix.class).build();
	
	@Parameter(names= {"-hidden","--hidden"},description="Ignore hidden files or directory")
	private boolean ignore_hidden=false;
	@Parameter(names= {"-M","--max-size"},description="Max File size (or negative to ignore). "+FileSizeConverter.OPT_DESC,converter=FileSizeConverter.class,splitter=NoSplitter.class)
	private long max_length = -1L;
	@Parameter(names= {"-m","--min-size"},description="Min File size. "+FileSizeConverter.OPT_DESC,converter=FileSizeConverter.class,splitter=NoSplitter.class)
	private long min_length = 0L;
	@Parameter(names= {"-o","--output"},description=OUTPUT_OR_STANDOUT)
	private Path outputPath = null;

	private static final Pattern slurm_out_regex = Pattern.compile(".*\\.o[0-9]+");
	private static final Pattern slurm_err_regex = Pattern.compile(".*\\.e[0-9]+");

	private String getSuffix(String fname) {
		if(slurm_out_regex.matcher(fname).matches()) return "(sge-o)";
		if(slurm_err_regex.matcher(fname).matches()) return "(sge-e)";
		if(fname.endsWith(".gz")) {
			return getSuffix(fname.substring(0, fname.length()-3))+".gz";
			}
		if(fname.endsWith(".bgz")) {
			return getSuffix(fname.substring(0, fname.length()-4))+".bgz";
			}
		if(fname.endsWith(".gzip")) {
			return getSuffix(fname.substring(0, fname.length()-5))+".gzip";
			}
		int idx = fname.lastIndexOf('.');
		if(idx<0) return "";
		String suffix= fname.substring(idx);
		return suffix;
		}

	private void scanDirectory(XMLStreamWriter w,Path directory) throws XMLStreamException,IOException {
		if(!Files.exists(directory)) return;
		if(!Files.isDirectory(directory)) return;
		if(Files.isHidden(directory) && ignore_hidden) return; 
		final Counter<String> suffixes= new Counter<>();
		Files.walkFileTree(directory,new SimpleFileVisitor<Path>() {
			 	@Override
			 	public FileVisitResult visitFile(Path F, BasicFileAttributes attrs) throws IOException {
			 		if(!attrs.isRegularFile()) return FileVisitResult.CONTINUE;
			 		if(attrs.isSymbolicLink()) return FileVisitResult.CONTINUE;
			 		try {
			 			
						if(!Files.isReadable(F)) return  FileVisitResult.CONTINUE;
						if(Files.isHidden(F) && ignore_hidden) return FileVisitResult.CONTINUE; 
						final long length= attrs.size();
						if(length< min_length) return FileVisitResult.CONTINUE;
						if(max_length!=-1L && length> max_length) return FileVisitResult.CONTINUE;
						String suffix = getSuffix(F.getFileName().toString());
						if(StringUtils.isBlank(suffix)) {
							suffixes.increment("(no suffix)",length);
							}
						else
							{
							suffixes.increment(suffix,length);
							}
						}
					catch(final IOException err) {
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
	
		if(suffixes.isEmpty()) return;
		if(suffixes.entrySet().stream().noneMatch(KV->KV.getValue()>0L)) return;
		w.writeStartElement(TreeMapMaker.NODE_NAME);
		w.writeAttribute(TreeMapMaker.LABEL_ATTRIBUTE,directory.toString());
		for(final String suffix: suffixes.keySet()) {
			if(suffixes.count(suffix)==0L) continue;
			w.writeEmptyElement(TreeMapMaker.NODE_NAME);
			w.writeAttribute(TreeMapMaker.LABEL_ATTRIBUTE, suffix);
			w.writeAttribute(TreeMapMaker.WEIGHT_ATTRIBUTE, String.valueOf(suffixes.count(suffix)));
			}
		w.writeEndElement();
		}

@Override
public int doWork(final List<String> args) {
	try {		
		
		if(args.isEmpty()) {
			LOG.error("directory are missing");
			return -1;
		}
		
		final XMLOutputFactory xof = XMLOutputFactory.newFactory();
		try(PrintWriter pw = super.openPathAsPrintWriter(null)) {
			XMLStreamWriter sw = xof.createXMLStreamWriter(pw);
			sw.writeStartDocument("1.0");
			sw.writeStartElement(TreeMapMaker.NODE_NAME);
			for(final String fname:args) {
				scanDirectory(sw,Paths.get(fname));
				}
			sw.writeEndElement();
			sw.writeEndDocument();
			sw.close();
			pw.flush();
			}
		
		
		return 0;
		}
	catch(final Throwable err) {
		LOG.error(err);
		return -1;
		}
	}


public static ProgramDescriptor getProgramDescriptor() {
	return new ProgramDescriptor() {
		@Override
		public String getName() {
			return "dubysuffix";
			}
		};
	}


public static void main(String[] args) {
	new DiskUsageBySuffix().instanceMainWithExit(args);
	}
}
