package sandbox.ig;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.beust.jcommander.Parameter;
import com.google.gson.JsonObject;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.http.CookieStoreUtils;
import sandbox.io.IOUtils;
/**


 */
public class InstagramToJson extends Launcher {
	private static final Logger LOG = Logger.builder(InstagramToJson.class).build();

	@Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private Path cookieStoreFile  = null;

	private String fetchHtml(final CloseableHttpClient client,final String url) {
	LOG.info("fetch "+url);
	CloseableHttpResponse resp=null;
	InputStream in=null;
	try {
		resp = client.execute(new HttpGet(url));
		if(resp.getStatusLine().getStatusCode()!=200) {
			LOG.error("cannot fetch "+url+" "+resp.getStatusLine());
			return null;
			}
		in = resp.getEntity().getContent();
		return IOUtils.readStreamContent(in);
		}
	catch(final IOException err) {
		LOG.error(err);
		return null;
		}
	finally
		{
		IOUtils.close(in);
		IOUtils.close(resp);
		}
	}
	
	@Override
	public int doWork(final List<String> args) {
		
		try
			{			
			final HttpClientBuilder builder = HttpClientBuilder.create();
			final String proxyH = System.getProperty("http.proxyHost");
			final String proxyP = System.getProperty("http.proxyPort");
			if(proxyH!=null && proxyP!=null && 
					!proxyH.trim().isEmpty() && 
					!proxyP.trim().isEmpty())
				{
				builder.setProxy(new HttpHost(proxyH, Integer.parseInt(proxyP)));
				}
			builder.setUserAgent(IOUtils.getUserAgent());
			
			if(this.cookieStoreFile!=null) {
				final BasicCookieStore cookies = CookieStoreUtils.readTsv(this.cookieStoreFile);
				builder.setDefaultCookieStore(cookies);
			}
			
			final CloseableHttpClient client = builder.build();
			PrintWriter jsW   = new PrintWriter(System.out);
			jsW.print("[");
			InstagramJsonScraper scraper = new InstagramJsonScraper();
			
				boolean first=true;
				for(int i=0;i< args.size();i++) {
					final String html = fetchHtml(client,args.get(i));
					if(html==null) continue;
					Optional<JsonObject> optJ = scraper.apply(html);
					if(!optJ.isPresent()) continue;
					if(!first) jsW.print(",");
					first=false;
					jsW.print(optJ.get().toString());
					}
				
			jsW.println("]");
			jsW.flush();
			jsW.close();
			client.close();
			return 0;
			}
		catch(final Exception err)
			{
			LOG.error(err);
			return -1;
			}
		finally
			{
			}
		
		}
	
	public static void main(final String[] args) {
		new InstagramToJson().instanceMainWithExit(args);
		}

	
}
