package sandbox.net;

import java.io.IOException;
import java.io.InputStream;

public interface DataCache extends AutoCloseable {
public static final String OPT_DESC="";
/** get path to cached file or URL if file is not cached */
public String getUrl(String url) throws IOException;
/** open stream to remote URL, URL might be cached */
public InputStream openUrl(String url) throws IOException;
}
