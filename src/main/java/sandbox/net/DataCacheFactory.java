package sandbox.net;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import sandbox.Logger;
import sandbox.io.IOUtils;
import sandbox.lang.StringUtils;
import sandbox.net.cache.DataCache;

public class DataCacheFactory {
static private final Logger LOG=Logger.builder(DataCacheFactory.class).build();

private Path directory = null;
private BiPredicate<Path,BasicFileAttributes> eraser = null;
private URLInputStreamProvider urlInputStreamProvider;
private boolean writeManifest=false;

public DataCacheFactory() {
	this.urlInputStreamProvider = new BasicURLInputStreamProvider();
	this.eraser = new BiPredicate<Path,BasicFileAttributes>() {
		@Override
		public boolean test(Path t,BasicFileAttributes attrs) {
			Duration day = Duration.ofDays(1L);
			final FileTime fileTime = attrs.creationTime();
			Duration dd =  Duration.ofMillis(System.currentTimeMillis()-fileTime.toMillis());
			if(day.compareTo(dd)<0) return true;
			return false;
		}
	};
}

public DataCacheFactory setCleanFunction(Predicate<Path> eraser) {
	return this;
	}

public DataCacheFactory setUrlInputStreamProvider(URLInputStreamProvider urlInputStreamProvider) {
	this.urlInputStreamProvider = urlInputStreamProvider;
	return this;
	}

public DataCacheFactory setDirectory(final Path directory) {
	this.directory = directory;
	return this;
	}

public DataCache make() {
	if(directory==null) {
		final NoCacheImpl cache = new NoCacheImpl();
		cache.urlStreamProvider  = this.urlInputStreamProvider;
		return cache;
		}
	else
		{
		final DataCacheImpl cache  = new DataCacheImpl();
		cache.urlStreamProvider  = this.urlInputStreamProvider;
		cache.directory = this.directory;
		cache.eraser = this.eraser;
		cache.writeManifest = writeManifest;
		return cache;
		}
	}


private static abstract class AbstractDataCache implements DataCache {
	URLInputStreamProvider urlStreamProvider;
	@Override
	public void close() {
		}
}


private static class NoCacheImpl extends AbstractDataCache  {
	@Override
	public String getUrl(String url) throws IOException {
		return url;
		}
	@Override
	public InputStream openUrl(String url) throws IOException {
		return this.urlStreamProvider.openStream(url);
		}
	}


private static class DataCacheImpl extends AbstractDataCache {
	Path directory;
	boolean writeManifest;
	private BiPredicate<Path,BasicFileAttributes> eraser = null;

	private Path getPath(final String url) throws IOException {
		cleanup();
		final String md5 = StringUtils.sha1(url);
		final String dir1 = md5.substring(0,2);
		final String dir2 = md5.substring(2);
		final Path p = this.directory.resolve(dir1);
		if(!Files.exists(p)) Files.createDirectory(p);
		final Path f = p.resolve(dir2);
		if(!Files.exists(f)) {
			LOG.info("download "+url+" to "+f);
			try(InputStream is = urlStreamProvider.openStream(url)) {
				IOUtils.copyTo(is, f);
				}
			if(writeManifest) {
				final Path manifest = this.directory.resolve("manifest.txt");
				try(BufferedWriter w= Files.newBufferedWriter(manifest, StandardOpenOption.APPEND,StandardOpenOption.CREATE)) {
					w.write(url+"\t"+f+"\n");
					w.flush();
					}
				}
			}
		return f;
		}
	
	private void cleanup() {
		try {
		if(this.eraser==null) return;
		if(!Files.exists(this.directory)) return;
		Files.walkFileTree(this.directory,new SimpleFileVisitor<Path>() {
			 	@Override
			 	public FileVisitResult visitFile(Path F, BasicFileAttributes attrs) throws IOException {
			 		if(!attrs.isRegularFile()) return FileVisitResult.CONTINUE;
			 		if(attrs.isSymbolicLink()) return FileVisitResult.CONTINUE;
			 		try {			 			
						if(!Files.isReadable(F)) return FileVisitResult.CONTINUE;
						if(eraser.test(F,attrs)) {
							LOG.info("Deleting cache "+F);
							Files.delete(F);
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
		} catch(final IOException err) {
			LOG.error(err);
			}
		}

	@Override
	public void close() {
		cleanup();
		}
	
	
	@Override
	public String getUrl(String url) throws IOException {
		final Path f = getPath(url);
		return f.toString();
		}
	
	@Override
	public InputStream openUrl(final String url) throws IOException {
		final Path f = getPath(url);
		return Files.newInputStream(f);
		}
	}
}
