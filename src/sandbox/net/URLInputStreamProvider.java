package sandbox.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

public interface URLInputStreamProvider {
public InputStream openStream(final String url) throws IOException;
}
