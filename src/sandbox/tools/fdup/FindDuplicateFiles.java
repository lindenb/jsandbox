package sandbox.tools.fdup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.bigsort.ExternalSortFactory;
import sandbox.bigsort.ExternalSortFactory.ExternalSort;
import sandbox.iterator.CloseableIterator;
import sandbox.iterator.EqualRangeIterator;

public class FindDuplicateFiles extends Launcher{
	private static final Logger LOG = Logger.builder(FindDuplicateFiles.class).build();

private boolean ignore_hidden=false;
private long max_length = -1L;
private long min_length = 0L;
private ExternalSort<FileSize> sorter;

private static class FileSize {
	final String path;
	final long length;
	FileSize(final String path,final long length) {
		this.path=path;
		this.length = length;
	}
	@Override
	public String toString() {
		return path+"("+length+")";
		}
}

void recurse(Path f) throws IOException {
	if(!Files.exists(f)) return;
	if(Files.isHidden(f) && ignore_hidden) return; 

	if(Files.isDirectory(f)) {
		for(Iterator<Path> iter=Files.list(f).iterator();iter.hasNext();){
			recurse(iter.next());
			}
		return;
		}
	if(!Files.isRegularFile(f)) return;
	
	long length=Files.size(f);
	if(length< min_length) return;
	if(max_length!=-1L && length> max_length) return;
	LOG.info("ADDD"+f);
	sorter.add(new FileSize(f.toString(), length));
	}

@Override
public int doWork(final List<String> args) {
	try {
		final Comparator<FileSize> comparator= (A,B)->Long.compare(A.length, B.length);
		this.sorter = new ExternalSortFactory<FileSize>().
				setComparator(comparator).
				setEntryReader(IS->{
					long l= IS.readLong();
					String p = IS.readUTF();
					return new FileSize(p,l);
				}).
				setEntryWriter((E,OS)->{
					OS.writeLong(E.length);
					OS.writeUTF(E.path);
				}).make();
		for(final String fname:args) {
			recurse(Paths.get(fname));
			}
		try(CloseableIterator<FileSize> iter1= this.sorter.iterator()) {
			EqualRangeIterator<FileSize> iter2= new EqualRangeIterator<>(comparator, iter1);
			while(iter2.hasNext()) {
				final List<FileSize> row= iter2.next();
				System.err.println("##### "+ row.get(0).length+" "+row.size());
				for(FileSize a:row) {
					System.err.println(a.path);
				}
			iter2.close();
			}
		}
		this.sorter.close();
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
