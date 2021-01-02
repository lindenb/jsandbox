package sandbox.net;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Function;

import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.io.IOUtils;

public interface DataCache {
static final Logger LOG=Logger.builder(DataCache.class).build();
public static final String OPT_DESC="";
public String getUrl(String url) throws IOException;
public InputStream openUrl(String url) throws IOException;
public static DataCache open(final Path directory,final URLInputStreamProvider urlStreamProvider) {
	if(directory==null) return noCache(urlStreamProvider);
	IOUtils.assertDirectoryExist(directory);
	return new DataCacheImpl(directory,urlStreamProvider);
	}
public static DataCache open(final Path directory) {
	return open(directory,new BasicURLInputStreamProvider());
	}

public static DataCache noCache(final URLInputStreamProvider urlStreamProvider) {
	return new NoCacheImpl(urlStreamProvider);
	}
public static DataCache noCache() {
	return noCache(new BasicURLInputStreamProvider());
	}

static class NoCacheImpl implements DataCache {
	private final URLInputStreamProvider urlStreamProvider;
	NoCacheImpl(final URLInputStreamProvider urlStreamProvider) {
		this.urlStreamProvider = urlStreamProvider;
	}
	@Override
	public String getUrl(String url) throws IOException {
		return url;
		}
	@Override
	public InputStream openUrl(String url) throws IOException {
		return this.urlStreamProvider.openStream(url);
		}
	}

static class DataCacheImpl implements DataCache {
	private final Path directory;
	private final URLInputStreamProvider urlStreamProvider;
	DataCacheImpl(final Path directory,final URLInputStreamProvider urlStreamProvider) {
		this.directory = directory;
		this.urlStreamProvider = urlStreamProvider;
		}
	private Path getPath(final String url) throws IOException {
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
			final Path manifest = this.directory.resolve("manifest.txt");
			try(BufferedWriter w= Files.newBufferedWriter(manifest, StandardOpenOption.APPEND,StandardOpenOption.CREATE)) {
				w.write(url+"\t"+f+"\n");
				w.flush();
				}
			}
		return f;
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
