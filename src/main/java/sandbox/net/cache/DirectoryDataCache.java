package sandbox.net.cache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import sandbox.io.IOUtils;
import sandbox.lang.StringUtils;

public class DirectoryDataCache implements DataCache {
private Path directory;
public DirectoryDataCache(Path directory) {
	this.directory=directory;
	}
public Path getPath(URL url)throws IOException {
	final String md5 = StringUtils.md5(url.toString());
	final Path f= this.directory.resolve(md5);
	if(!Files.exists(f)) {
		try(InputStream in = getDownloader().apply(url)) {
			IOUtils.copyTo(in, f);
			}
		catch(IOException err) {
			Files.deleteIfExists(f);
			throw err;
			}
		}
	return f;
	}

@Override
public InputStream openUrl(URL url) throws IOException {
	return Files.newInputStream(getPath(url));
	}	
@Override
public void close() throws IOException {
	
	}
}
