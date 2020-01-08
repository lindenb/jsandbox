package sandbox.bigsort;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import sandbox.iterator.AbstractIterator;
import sandbox.iterator.CloseableIterator;
import sandbox.iterator.MergingIterator;

public class ExternalSortFactory<T> {
	
private Path tmpDir =  Paths.get(System.getProperty("java. io. tmpdir"));
private int maxRecordsInRam= 100_000;
private 	Comparator<T> comparator;
private 	EntryWriter<T> entryWriter;
private 	EntryReader<T> entryReader;
	
public interface BigSorter<T> extends AutoCloseable
	{
	public void add(final T t);
	public CloseableIterator<T> iterator();
	public void close();
	}

@FunctionalInterface
public static interface EntryWriter<T> {
	public void write(final T rec,final DataOutputStream dos) throws IOException;
}
@FunctionalInterface
public static interface EntryReader<T> {
	public T read(final DataInputStream dis) throws IOException;
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

public BigSorter<T> make() {
	if(maxRecordsInRam<1) throw new IllegalArgumentException();
	final BigSorterImpl sorter = new BigSorterImpl();
	sorter.tmpDir = tmpDir;
	sorter.comparator = Objects.requireNonNull(comparator,"undefined comparator");
	sorter.maxRecordsInRam = maxRecordsInRam;
	sorter.entryReader = Objects.requireNonNull(entryReader,"undefined reader");
	sorter.entryWriter = Objects.requireNonNull(entryWriter,"undefined writer");
	return sorter;
}


private static class StoredFileIterator<T> extends AbstractIterator<T> {
	Path path;
	int num = 0;
	DataInputStream dis= null;
	int nRead= 0;
	EntryReader<T> entryReader;

	@Override
	protected T advance() {
		if(nRead>=num) {
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
		if(dis!=null) try{dis.close();} catch(IOException err) {}
		dis=null;
		try{Files.deleteIfExists(this.path);} catch(IOException err) {}
		}
	}



private class BigSorterImpl implements BigSorter<T>
	{
	final List<T> ramRecords = new ArrayList<>();
	final List<StoredFileIterator<T>> storedFiles = new ArrayList<>();
	Path tmpDir = null;
	int maxRecordsInRam=100_000;
	Comparator<T> comparator;
	EntryWriter<T> entryWriter;
	EntryReader<T> entryReader;
	boolean iterating_flag=false;
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
		if(iterating_flag) throw new IllegalStateException();
		iterating_flag = true;
		List<Iterator<T>> iterators = new ArrayList<>();
		if(!this.ramRecords.isEmpty()) iterators.add(this.ramRecords.iterator());
		for(final StoredFileIterator<T> st: this.storedFiles) {
			try {st.dis = new DataInputStream(Files.newInputStream(st.path));
			st.entryReader = entryReader;
			iterators.add(st);}
			catch(final IOException err) {
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
		Collections.sort(this.ramRecords, this.comparator);
		final StoredFileIterator<T> sf = new StoredFileIterator<T>();
		sf.path = Files.createTempFile(this.tmpDir,"tmp.", ".dat");
		sf.num = this.ramRecords.size();
		try(DataOutputStream dos=new DataOutputStream(Files.newOutputStream(sf.path))) {
			for(final T rec:this.ramRecords) {
				this.entryWriter.write(rec, dos);
				}
			dos.flush();
			}
		this.storedFiles.add(sf);
		this.ramRecords.clear();
		}
	}


}
