package sandbox.net.cache;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface DataCache extends Closeable{
public static final String OPT_DESC="";
/** open stream to remote URL, URL might be cached */
public InputStream openUrl(URL url) throws IOException;
}
