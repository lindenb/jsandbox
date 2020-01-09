package sandbox.bigsort;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import sandbox.IOUtils;
import sandbox.Logger;
import sandbox.io.EntryReader;
import sandbox.io.EntryWriter;
import sandbox.iterator.AbstractIterator;
import sandbox.iterator.CloseableIterator;
import sandbox.iterator.MergingIterator;

public class ExternalSortFactory<T> {
private static final Logger LOG = Logger.builder(ExternalSortFactory.class).build();

private Path tmpDir =  IOUtils.getDefaultTmpDirPath();
private int maxRecordsInRam= 100_000;
private 	Comparator<T> comparator;
private 	EntryWriter<T> entryWriter;
private 	EntryReader<T> entryReader;
private boolean do_debug=false;
	
public interface ExternalSort<T> extends AutoCloseable
	{
	public void add(final T t);
	public CloseableIterator<T> iterator();
	public void close();
	}


public ExternalSortFactory<T> setTmpDir(final Path tmpDir) {
	this.tmpDir = tmpDir;
	return this;
}

public ExternalSortFactory<T> setComparator(final Comparator<T> comparator) {
	this.comparator = comparator;
	return this;
}
public ExternalSortFactory<T> setEntryReader(final EntryReader<T> entryReader) {
	this.entryReader = entryReader;
	return this;
}
public ExternalSortFactory<T> setEntryWriter(final EntryWriter<T> entryWriter) {
	this.entryWriter = entryWriter;
	return this;
}
public ExternalSortFactory<T> setMaxRecordsInRam(final int maxRecordsInRam) {
	this.maxRecordsInRam = maxRecordsInRam;
	return this;
}

public ExternalSortFactory<T> setDebug(boolean do_debug) {
	this.do_debug = do_debug;
	return this;
	}

public ExternalSort<T> make() {
	if(maxRecordsInRam<1) throw new IllegalArgumentException("maxRecordsInRam:"+maxRecordsInRam);
	final BigSorterImpl sorter = new BigSorterImpl();
	sorter.tmpDir = tmpDir;
	sorter.comparator = Objects.requireNonNull(comparator,"undefined comparator");
	sorter.maxRecordsInRam = maxRecordsInRam;
	sorter.entryReader = Objects.requireNonNull(entryReader,"undefined reader");
	sorter.entryWriter = Objects.requireNonNull(entryWriter,"undefined writer");
	sorter.do_debug = this.do_debug;
	return sorter;
}


private static class StoredFileIterator<T> extends AbstractIterator<T> {
	Path path;
	int total = 0;
	DataInputStream dis= null;
	int nRead= 0;
	EntryReader<T> entryReader;

	@Override
	protected T advance() {
		if(this.nRead>=this.total) {
			close();
			return null;
			}
		try  {
			final T rec = this.entryReader.read(this.dis);
			nRead++;
			return rec;
			}
		catch(final IOException err) {
			throw new RuntimeException(err);
			}
		}
	
	@Override
	public void close() {
		IOUtils.close(this.dis);
		dis=null;
		try{Files.deleteIfExists(this.path);} catch(IOException err) {}
		}
	}



private class BigSorterImpl implements ExternalSort<T>
	{
	final List<T> ramRecords = new ArrayList<>();
	final List<StoredFileIterator<T>> storedFiles = new ArrayList<>();
	Path tmpDir = null;
	int maxRecordsInRam=100_000;
	Comparator<T> comparator;
	EntryWriter<T> entryWriter;
	EntryReader<T> entryReader;
	boolean iterating_flag=false;
	boolean do_debug=false;
	@Override
	public void add(final T t) {
		if(iterating_flag) throw new IllegalStateException();
		if(this.ramRecords.size() >= this.maxRecordsInRam) {
			try { saveRamToDisk();}
			catch(final IOException err) {
				throw new RuntimeException(err);
				}
			}
		this.ramRecords.add(t);
		}
	
	public CloseableIterator<T> iterator()  {
		if(this.iterating_flag) throw new IllegalStateException();
		this.iterating_flag = true;
		final List<Iterator<T>> iterators = new ArrayList<>(this.storedFiles.size()+1);
		if(!this.ramRecords.isEmpty()) {
			this.sortRam();
			iterators.add(this.ramRecords.iterator());
			}
		for(final StoredFileIterator<T> st: this.storedFiles) {
			try {
				st.dis = new DataInputStream(Files.newInputStream(st.path));
				st.entryReader = entryReader;
				iterators.add(st);
				}
			catch(final IOException err) {
				LOG.error(err);
				throw new RuntimeException(err);
				}
			}
		
		return new MergingIterator<>(this.comparator, iterators);
		
		}
	
	public void close() {
		storedFiles.stream().forEach(SF->SF.close());
	}
	
	private void saveRamToDisk() throws IOException {
		if(this.ramRecords.isEmpty()) return;
		this.sortRam();
		
		final StoredFileIterator<T> sf = new StoredFileIterator<T>();
		sf.path = Files.createTempFile(this.tmpDir,"tmp.", ".dat");
		sf.total = this.ramRecords.size();
		try(DataOutputStream dos=new DataOutputStream(Files.newOutputStream(sf.path))) {
			for(final T rec:this.ramRecords) {
				this.entryWriter.write(dos,rec);
				}
			dos.flush();
			}
		this.storedFiles.add(sf);
		if(do_debug) LOG.debug("saved "+this.ramRecords.size()+" on disk "+sf.path);
		this.ramRecords.clear();
		}
	
	private void sortRam() {
		if(do_debug) LOG.debug("sorting "+this.ramRecords.size()+" items");
		Collections.sort(this.ramRecords, this.comparator);
	}
	
	
	}


}
