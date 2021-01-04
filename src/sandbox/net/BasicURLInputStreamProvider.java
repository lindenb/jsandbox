package sandbox.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import sandbox.io.IOUtils;

public class BasicURLInputStreamProvider implements URLInputStreamProvider {
@Override
public InputStream openStream(String urlstr) throws IOException {
	if(!IOUtils.isURL(urlstr)) {
		return Files.newInputStream(Paths.get(urlstr));
		}
	final URL url = new URL(urlstr);
	return url.openStream();
	}
}
