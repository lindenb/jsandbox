package sandbox.tools.fdup;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.bigsort.ExternalSortFactory;
import sandbox.bigsort.ExternalSortFactory.ExternalSort;
import sandbox.iterator.EqualRangeIterator;
import sandbox.jcommander.FileSizeConverter;
import sandbox.jcommander.NoSplitter;

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

	@Parameter(names= {"-t","--threads"},description="Number of threads.")
	private int num_threads = 1;

	

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
private static boolean areSameBinary(Path f1,Path f2,int buff_size) throws IOException {
	 try(   InputStream in1 =new BufferedInputStream(Files.newInputStream(f1),buff_size);
			InputStream in2 =new BufferedInputStream(Files.newInputStream(f2),buff_size);
			 ){

			      int value1,value2;
			      do{
			           //since we're buffered read() isn't expensive
			           value1 = in1.read();
			           value2 = in2.read();
			           if(value1 !=value2){
			           return false;
			           }
			      }while(value1 >=0);

			 //since we already checked that the file sizes are equal 
			 //if we're here we reached the end of both files without a mismatch
			 return true;
			}
	}

private void recurse(final  ExternalSort<FileSize> sorter,final Path dir) throws IOException {
	if(!Files.exists(dir)) return;

	if(Files.isHidden(dir) && ignore_hidden) return; 

	Files.walk(dir).forEach(F->{
		if(!Files.isRegularFile(F)) return;
		try {
			if(!Files.isReadable(F)) return;
			if(Files.isHidden(F) && ignore_hidden) return; 
			final long length=Files.size(F);
			if(length< min_length) return;
			if(max_length!=-1L && length> max_length) return;
			sorter.add(new FileSize(F.toString(), length));
			}
		catch(IOException err) {
			LOG.error(err);
			
			}
		});
	}

@Override
public int doWork(final List<String> args) {
	ExternalSort<FileSize> sorter = null;
	try {		
		
		final Comparator<FileSize> comparator= (A,B)->Long.compare(B.length, A.length);
		sorter = new ExternalSortFactory<FileSize>().
				setComparator(comparator).
				setMaxRecordsInRam(max_records_in_ram).
				setDebug(true).
				setEntryReader(IS->{
					long l= IS.readLong();
					String p = IS.readUTF();
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
        
		final Semaphore lock = new Semaphore(this.num_threads);
		
		final EqualRangeIterator<Set<FileSize>,FileSize> itereq= new EqualRangeIterator<>(comparator, iter1,HashSet::new);
		while(itereq.hasNext()) {
			final List<FileSize> row= new ArrayList<>(itereq.next());
			if(row.size()==1) continue;
			
			
			for(int x=0;x+1< row.size();++x) {
				
				final Path p1 = row.get(x).toPath();
				if(!Files.exists(p1)) continue;
 				for(int y=x+1;y< row.size();++y) {
					final Path p2 = row.get(y).toPath();
					if(!Files.exists(p2)) continue;
					final long length = Files.size(p2);
					if(Files.size(p1)!= length) continue;
					
					lock.acquire();
					
					
					final Thread thread=new Thread(()->{
						try  {
						if(!areSameBinary(p1, p2,this.buff_size)) return;
						
						final StringBuilder sb=new StringBuilder();
						sb.append(length);
						for(int k=0;k<2;++k) {
							final Path p=(k==0?p1:p2);
							sb.append("\t");
							sb.append(p.toString());
							sb.append("\t");
							sb.append(df.format(Files.getLastModifiedTime(p).toMillis()));
							}
						System.out.println(sb.toString());
						
						} catch(IOException err) {
							LOG.error(err);
							}
						finally
							{
							lock.release();
							}
						});
					thread.start();
 				
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

public static void main(String[] args) {
	new FindDuplicateFiles().instanceMainWithExit(args);
}
}
