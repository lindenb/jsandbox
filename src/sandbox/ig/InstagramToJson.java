package sandbox.ig;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.http.HttpHost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.beust.jcommander.Parameter;
import com.google.gson.JsonObject;

import sandbox.IOUtils;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.http.CookieStoreUtils;
/**


 */
public class InstagramToJson extends Launcher {
	private static final Logger LOG = Logger.builder(InstagramToJson.class).build();

	@Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private Path cookieStoreFile  = null;

	
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
			try(InstagramJsonScraper scraper = new InstagramJsonScraper(client)) {
				boolean first=true;
				for(int i=0;i< args.size();i++) {
					Optional<JsonObject> optJ = scraper.apply(args.get(i));
					if(!optJ.isPresent()) continue;
					if(!first) jsW.print(",");
					first=false;
					jsW.print(optJ.get().toString());
					}
				}
			jsW.println("]");
			jsW.flush();
			jsW.close();
			
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
