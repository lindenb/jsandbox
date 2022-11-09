package sandbox.tools.mastodongraph;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import com.beust.jcommander.Parameter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.io.IOUtils;

public class MastodonGraph extends Launcher {
	private static final Logger LOG= Logger.builder(MastodonGraph.class).build();
	private HttpClientBuilder builder = null;
	@Parameter(names={"--instance"},description="instance")
	private String instance="https://genomic.social/";
	@Parameter(names={"--api-key","-K"},description="API key")
	private String api_key=null;

	
	private String oauth_token=null;
	private String oauth_token_type=null;
	
	
	private class Response {
		HttpRequestBase request;
		String content = null;
		private JsonElement _json = null;
		JsonElement getJson() {
			if(content==null) return null;
			if(_json==null) {
				final JsonParser parser = new JsonParser();
				this._json = parser.parse(this.content);
				}
			return this._json;
			}
		JsonObject getJsonObject() {
			return getJson().getAsJsonObject();
			}
		}
	
	
	private void scan(String user) throws IOException {
		final HttpPost get = new HttpPost(this.instance+"api/v1/accounts/"+user+"/followers");
		Response resp = wget(get);
		LOG.info(resp.content);
		
		}
	
	private Response wget(HttpRequestBase method)  throws IOException {
		LOG.info(method);
		final Response response = new Response();
		response.request=method;
		if(this.oauth_token!=null) {
			method.addHeader("Authorization",this.oauth_token_type+" "+this.oauth_token);
			}
		try(CloseableHttpClient client = this.builder.build()) {
				CloseableHttpResponse resp=null;
				InputStream in=null;
				try {
					resp = client.execute(method);
					if(resp.getStatusLine().getStatusCode()!=200) {
						LOG.error("cannot fetch "+method.getMethod()+":"+method.getURI()+" "+resp.getStatusLine());
						return response;
						}
					in = resp.getEntity().getContent();
					response.content = IOUtils.readStreamContent(in);
					}
				finally {
					IOUtils.close(in);
					IOUtils.close(resp);
					}
				return response;
				}
			}
		
	

	private UrlEncodedFormEntity createParams(String...array) {
		return createParams(Arrays.asList(array));
		}

	private UrlEncodedFormEntity createParams(final List<String> L) {
		final Map<String,String> hash = new HashMap<>(L.size()/2);
		for(int i=0;i+1< L.size();i+=2) {
			hash.put(L.get(i), L.get(i+1));
			}
		return createParams(hash);
		}
	
	private UrlEncodedFormEntity createParams(Map<String,String> map) {
		final List<NameValuePair> postParameters = new ArrayList<NameValuePair>(map.size());
		for(final String k:map.keySet()) {
			postParameters.add(new BasicNameValuePair(k,map.get(k)));
			}
		try {
			return new UrlEncodedFormEntity(postParameters, "UTF-8");
			}
		catch(UnsupportedEncodingException err) {
			throw new RuntimeException(err);
			}
		}
	@Override
	public int doWork(final List<String> args) {
		try {
			this.builder = HttpClientBuilder.create();
			HttpPost post = new HttpPost(this.instance+"oauth/token");
			post.setEntity(createParams(
					"client_id","TODO",
					"client_secret","TODO",
					"redirect_uri","urn:ietf:wg:oauth:2.0:oob",
					"grant_type","client_credentials"));
			Response resp = wget(post);
			LOG.info(resp.content);
			this.oauth_token = resp.getJsonObject().get("access_token").getAsString();
			this.oauth_token_type  = resp.getJsonObject().get("token_type").getAsString();
			
			scan("yokofakun");
			return 0;
		} catch(final Throwable err) {
			LOG.error(err);
			return -1;
		}
		
		
		}
	
	public static void main(String[] args) {
		new MastodonGraph().instanceMainWithExit(args);

	}

}
