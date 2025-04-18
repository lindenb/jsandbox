package sandbox.tools.fdup;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.bigsort.ExternalSortFactory;
import sandbox.bigsort.ExternalSortFactory.ExternalSort;
import sandbox.iterator.EqualRangeIterator;
import sandbox.jcommander.FileSizeConverter;
import sandbox.jcommander.NoSplitter;
import sandbox.lang.StringUtils;
import sandbox.tools.central.ProgramDescriptor;

public class FindDuplicateFiles extends Launcher{
	private static final Logger LOG = Logger.builder(FindDuplicateFiles.class).build();
	
	@Parameter(names= {"-hidden","--hidden"},description="Ignore hidden files or directory")
	private boolean ignore_hidden=false;
	@Parameter(names= {"-M","--max-size"},description="Max File size (or negative to ignore). "+FileSizeConverter.OPT_DESC,converter=FileSizeConverter.class,splitter=NoSplitter.class)
	private long max_length = -1L;
	@Parameter(names= {"-m","--min-size"},description="Min File size. "+FileSizeConverter.OPT_DESC,converter=FileSizeConverter.class,splitter=NoSplitter.class)
	private long min_length = 0L;
	@Parameter(names= {"--max-records-in-ram"},description="Max records in RAM")
	private int max_records_in_ram = 1_000_000;
	@Parameter(names= {"-b","--buffer-size"},description="Buffer Size for BufferedInputStream. ")
	private int buff_size = 1_000_000;
	@Parameter(names= {"-f","--fast"},description="Just compare the xx first bytes. ignore if <=0. "+FileSizeConverter.OPT_DESC,converter=FileSizeConverter.class,splitter=NoSplitter.class)
	private long fast_size = -1L;
	@Parameter(names= {"-x","--extensions"},description="comma/space/semicolon separated list of extension. Empty=All files.")
	private String extensions_str="";
	@Parameter(names= {"-t","--threads"},description="Number of threads.")
	private int num_threads = 1;
	@Parameter(names= {"--too-many"},description="Ignore if there is more than 'x' files with the same size. probably a common file/format generated at multiple places . -1 == ignore")
	private int too_many_treshold = -1;


	private final Set<String> extensions = new HashSet<>();
	private enum CompareResult { not_same,suspected,same};
	

private static class FileSize {
	final String path;
	final long length;
	FileSize(final String path,final long length) {
		this.path=path;
		this.length = length;
	}
	Path toPath() {
		return Paths.get(this.path);
	}
	@Override
	public boolean equals(Object o) {
		try
			{
			return Files.isSameFile(FileSize.class.cast(o).toPath(),this.toPath());
			}
		catch(IOException err) {
			throw new RuntimeException(err);
			}
		}
	
	@Override
	public int hashCode() {
		return path.hashCode();
		}
	
	@Override
	public String toString() {
		return path+"("+length+")";
		}
	
	
	}
//https://stackoverflow.com/questions/22818590
private CompareResult areSameBinary(Path f1,Path f2, final long length) throws IOException {
	
	int buffer = this.buff_size;
	if(fast_size>0 && this.fast_size< (long)this.buff_size) {
		buffer = (int)this.fast_size;
	}
	System.err.print(String.valueOf(f1));
	System.err.print(" ");
	System.err.print(String.valueOf(f2));
	System.err.print(" ");
	System.err.println(length);
	 try(   InputStream in1 =new BufferedInputStream(Files.newInputStream(f1),buffer);
			InputStream in2 =new BufferedInputStream(Files.newInputStream(f2),buffer);
			 ){
		 		  long nReads = 0L;
			      int value1,value2;
			      do{
			           //since we're buffered read() isn't expensive
			           value1 = in1.read();
			           value2 = in2.read();
			           if(value1 !=value2){
				           return CompareResult.not_same;
				           }
			           nReads++;
			           if(this.fast_size>0 && nReads>=this.fast_size) return CompareResult.suspected;
			      }while(value1 >=0);

			 //since we already checked that the file sizes are equal 
			 //if we're here we reached the end of both files without a mismatch
			 return CompareResult.same;
			}
	}

private void recurse(final  ExternalSort<FileSize> sorter,final Path dir) throws IOException {
	if(!Files.exists(dir)) return;

	if(Files.isHidden(dir) && ignore_hidden) return; 

	Files.walkFileTree(dir,new SimpleFileVisitor<Path>() {
		 	@Override
		 	public FileVisitResult visitFile(Path F, BasicFileAttributes attrs) throws IOException {
		 		if(!attrs.isRegularFile()) return FileVisitResult.CONTINUE;
		 		if(attrs.isSymbolicLink()) return FileVisitResult.CONTINUE;
		 		if(!extensions.isEmpty() && extensions.stream().noneMatch(EX->F.getFileName().toString().endsWith(EX))) return FileVisitResult.CONTINUE;
		 		try {
		 			
					if(!Files.isReadable(F)) return  FileVisitResult.CONTINUE;
					if(Files.isHidden(F) && ignore_hidden) return FileVisitResult.CONTINUE; 
					final long length= attrs.size();
					if(length< min_length) return FileVisitResult.CONTINUE;
					if(max_length!=-1L && length> max_length) return FileVisitResult.CONTINUE;
					sorter.add(new FileSize(F.toString(), length));
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
	/*
	
		if(!Files.isRegularFile(F)) return;
		
		});*/
	}

@Override
public int doWork(final List<String> args) {
	ExternalSort<FileSize> sorter = null;
	try {		
		

		final Comparator<FileSize> comparator= (A,B)->Long.compare(A.length, B.length);

		this.extensions.addAll(Arrays.
				stream(this.extensions_str.split("[\\|\\s;,]+")).
				filter(S->!StringUtils.isBlank(S)).
				collect(Collectors.toSet()));
		
		sorter = new ExternalSortFactory<FileSize>().
				setComparator(comparator).
				setMaxRecordsInRam(max_records_in_ram).
				setDebug(true).
				setEntryReader(IS->{
					final long l= IS.readLong();
					final String p = IS.readUTF();
					return new FileSize(p,l);
				}).
				setEntryWriter((OS,E)->{
					OS.writeLong(E.length);
					OS.writeUTF(E.path);
				}).make();
		
		for(final String fname:args) {
			recurse(sorter,Paths.get(fname));
			}
		Iterator<FileSize> iter1=  sorter.iterator();
		SimpleDateFormat df = new SimpleDateFormat("YYYY-MM-DD HH:mm:ss");
        
		final Semaphore lock = (this.num_threads==1?null:new Semaphore(this.num_threads));
		
		final EqualRangeIterator<Set<FileSize>,FileSize> itereq= new EqualRangeIterator<>(comparator, iter1,HashSet::new);
		while(itereq.hasNext()) {
			final List<FileSize> row= new ArrayList<>(itereq.next());
			if(row.size()==1) continue;
			if(this.too_many_treshold>0 && row.size()> this.too_many_treshold) continue;
			
			for(int x=0;x+1< row.size();++x) {
				
				final Path p1 = row.get(x).toPath();
				if(!Files.exists(p1)) continue;
 				for(int y=x+1;y< row.size();++y) {
					final Path p2 = row.get(y).toPath();
					if(!Files.exists(p2)) continue;
					final long length = Files.size(p2);
					if(Files.size(p1)!= length) continue;
					
					
					
					final Runnable runner =()->{
						try  {
						final CompareResult cmp = areSameBinary(p1, p2, length);
						if(cmp.equals(CompareResult.not_same)) return;
						
						final StringBuilder sb=new StringBuilder();
						sb.append(length);
						for(int k=0;k<2;++k) {
							final Path p=(k==0?p1:p2);
							sb.append("\t");
							sb.append(p.toString());
							sb.append("\t");
							sb.append(df.format(Files.getLastModifiedTime(p).toMillis()));
							}
						sb.append("\t").append(cmp.name());
						System.out.println(sb.toString());
						
						} catch(IOException err) {
							LOG.error(err);
							}
						finally
							{
							if(lock!=null) lock.release();
							}
						};
						
					if(lock!=null) {
						final Thread thread=new Thread(runner);
						thread.start();
						}
					else
						{
						runner.run();
						}
 				
					}
				}
			}
		itereq.close();
		sorter.close();
		
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
			return "finddupfiles";
			}
		};
	}


public static void main(String[] args) {
	new FindDuplicateFiles().instanceMainWithExit(args);
	}
}
