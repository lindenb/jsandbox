package sandbox.net.cache;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Function;

import org.apache.commons.lang3.function.FailableFunction;

public interface DataCache extends Closeable{
public static final String OPT_DESC="";
/** open stream to remote URL, URL might be cached */
public InputStream openUrl(URL url) throws IOException;
/** open stream to remote URL, URL might be cached */
public default FailableFunction<URL,InputStream,IOException> getDownloader() {
	return U->U.openStream();
	};
}
