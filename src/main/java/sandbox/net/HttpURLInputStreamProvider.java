package sandbox.net;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;

import sandbox.io.IOUtils;

public class HttpURLInputStreamProvider implements URLInputStreamProvider {
private final CloseableHttpClient client;
public HttpURLInputStreamProvider(final CloseableHttpClient client) {
	this.client = client;
	}
@Override
public InputStream openStream(String urlstr) throws IOException {
	if(!IOUtils.isURL(urlstr)) {
		return Files.newInputStream(Paths.get(urlstr));
		}
	final HttpUriRequest httpGet = makeUriRequest(urlstr);
	final CloseableHttpResponse resp=this.client.execute(httpGet);
	if(resp.getStatusLine().getStatusCode()!=200) {
		resp.close();
		throw new IOException("cannot fetch "+urlstr+" "+resp.getStatusLine());
		}
	return new DelegateInputStream(resp, resp.getEntity().getContent());
	}

protected HttpUriRequest makeUriRequest(final String urlstr) {
	return new HttpGet(urlstr);
	}

private static class DelegateInputStream extends FilterInputStream {
	final CloseableHttpResponse resp;
	DelegateInputStream(final CloseableHttpResponse resp,InputStream is) {
		super(is);
		this.resp = resp;
		}
	@Override
	public void close() throws IOException {
		super.close();
		resp.close();
		}
	}
}
