package sandbox.ig;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import sandbox.IOUtils;
import sandbox.Logger;

class InstagramJsonScraper implements Function<String,Optional<JsonObject>>, AutoCloseable {
	private static final Logger LOG = Logger.builder(InstagramJsonScraper.class).build();

	private final CloseableHttpClient client;

	public InstagramJsonScraper(final CloseableHttpClient client) {
		this.client = client;
		}
	
	private String queryHtml(final String queryName) throws IOException {
		CloseableHttpResponse response = null;
		InputStream httpIn = null;
		try
			{
			response = this.client.execute(new HttpGet(this.fixUrl( queryName)));
			httpIn = response.getEntity().getContent();
			final String html = IOUtils.readStreamContent(httpIn);
			return html;
			}
		catch(final IOException err)
			{
			LOG.error(err);
			throw err;
			}
		finally
			{
			IOUtils.close(httpIn);
			IOUtils.close(response);
			}
		}
	
	private String fixUrl(final String queryName) {
		if(IOUtils.isURL(queryName)) return queryName;
		return "https://www.instagram.com"+
				(queryName.startsWith("/")?"":"/") + 
				queryName +
				(queryName.endsWith("/")?"":"/");
		}
	private boolean cleanup(JsonElement c) {
		if(c.isJsonObject()) {
			final JsonObject obj = c.getAsJsonObject();
			final Set<String> toRemove= new HashSet<>();
			for(final Map.Entry<String,JsonElement> kv:obj.entrySet()) {
				JsonElement v= kv.getValue();
				String k= kv.getKey();
				boolean key_is_int;
				try {
					Integer.parseInt(k);
					key_is_int = true;
					}
				catch(NumberFormatException err) {
					key_is_int= false;
					}
				
				if((key_is_int ||  k.equals("qex")) && v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean()) {
					toRemove.add(k);
					}
				else if(k.equals("g") && v.isJsonPrimitive() && v.getAsJsonPrimitive().isString() && v.getAsString().isEmpty()) {
					toRemove.add(k);
					}
				else if((k.equals("p") ||key_is_int) && v.isJsonPrimitive() && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) {
					toRemove.add(k);
					}
				else if((v.isJsonObject() || v.isJsonArray()) && cleanup(v)) {
					toRemove.add(k);
					}
				}
			toRemove.stream().forEach(K->obj.remove(K));
			return (obj.entrySet().isEmpty());
			}
		else if(c.isJsonArray())
			{
			JsonArray a = c.getAsJsonArray();
			int i=0;
			while(i< a.size()) {
				if(cleanup(a.get(i))) {
					a.remove(i);
					}
				else
					{
					i++;
					}
				}
			return a.size()==0;
			}
		return false;
		}
	
	public  Optional<JsonObject> apply(String queryName) {
		final String windowSharedData = "window._sharedData";
		final String endObject="};";
		String html;
		try {
			html = queryHtml(queryName);
			}
		catch(Exception err) {
			LOG.error(err);
			return Optional.empty();
			}
		int i= html.indexOf(windowSharedData);
		if(i==-1) {
			LOG.error("cannot find "+windowSharedData+" in "+fixUrl(queryName));
			return Optional.empty();
			}
		html=html.substring(i+windowSharedData.length());
		i= html.indexOf("{");
		if(i==-1)
			{
			LOG.error("cannot find '{' after "+windowSharedData+" in "+fixUrl(queryName)+" after "+html);
			return Optional.empty();
			}
		html=html.substring(i);
		i = html.indexOf(endObject);
		if(i==-1)
			{
			LOG.error("cannot find  "+endObject+" in "+fixUrl(queryName));
			return Optional.empty();
			}
		html=html.substring(0, i+1);
		final JsonReader jsr = new JsonReader(new StringReader(html));
		jsr.setLenient(true);
		final JsonParser parser = new JsonParser();
		JsonElement root;
		try {
			root = parser.parse(jsr);
			}
		catch(final JsonSyntaxException err) {
			LOG.error(err);
			return Optional.empty();
			}
		finally
			{
			try {jsr.close(); } catch(IOException err) { }
			}
		if(!root.isJsonObject())
			{
			LOG.error("root is not json object in "+fixUrl(queryName));
			return Optional.empty();
			}
		cleanup(root);
		return Optional.of(root.getAsJsonObject());
		}
	public void close() throws IOException {
		this.client.close();
		}
	}
