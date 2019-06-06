package sandbox.flickr;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import sandbox.IOUtils;
import sandbox.Launcher;

public class FlickrToAtom extends Launcher
	{
	@Override
	public int doWork(List<String> args)
		{
		CloseableHttpClient client = null;
			try 
			{
				
				client = HttpClientBuilder.create().
						setUserAgent(IOUtils.getUserAgent()).
						build();
			
				for(final String arg:args) {
					HttpGet httpGet = new HttpGet(arg);
					ResponseHandler<String> responseHandler = new BasicResponseHandler();
					final String content = client.execute(httpGet,responseHandler);
					
					}
				
				client.close();client=null;
			return 0;
			}	 
			catch(Exception err) {
				err.printStackTrace();
			return -1;
			}
			finally {
				IOUtils.close(client);
			}
		}
	public static void main(String[] args)
		{
		new FlickrToAtom().instanceMainWithExit(args);

		}

	}
