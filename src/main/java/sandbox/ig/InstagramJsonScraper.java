package sandbox.ig;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import sandbox.Logger;
import sandbox.io.IOUtils;

public class InstagramJsonScraper implements Function<String,Optional<JsonObject>> {
	private static final Logger LOG = Logger.builder(InstagramJsonScraper.class).build();


	public static JsonElement find(final JsonElement root,final String  key) {
		JsonElement v = findRecursive(root,key);
		return v;
		}
	
	private static JsonElement findRecursive(final JsonElement root,final String  key) {
		if(root==null || root.isJsonNull() || root.isJsonPrimitive()) {
			return null;
			}
		else if(root.isJsonObject()) {
			final JsonObject obj = root.getAsJsonObject();
			if(obj.has(key)) {
				return obj.get(key);
			}
			for(final Map.Entry<String,JsonElement> kv:obj.entrySet()) {
				final JsonElement v1= kv.getValue();
				final JsonElement v2 =  findRecursive(v1,key);
				if(v2!=null) return v2;
				}
			}
		else if(root.isJsonArray()) {
			final JsonArray array = root.getAsJsonArray();
			for(int i=0;i< array.size();i++) {
				final JsonElement v1= array.get(i);
				final JsonElement v2 =  findRecursive(v1,key);
				if(v2!=null) return v2;
				}
			}
		return null;
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
	@Override
	public  Optional<JsonObject> apply(String html) {
		final String windowSharedData = "window._sharedData";
		final String endObject="};";

		int i= html.indexOf(windowSharedData);
		if(i==-1) {
			LOG.error("cannot find "+windowSharedData+" in url");
			return Optional.empty();
			}
		html=html.substring(i+windowSharedData.length());
		i= html.indexOf("{");
		if(i==-1)
			{
			LOG.error("cannot find '{' after "+windowSharedData+" in url after "+html);
			return Optional.empty();
			}
		html=html.substring(i);
		i = html.indexOf(endObject);
		if(i==-1)
			{
			LOG.error("cannot find  "+endObject+" in url");
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
			LOG.error("root is not json object");
			return Optional.empty();
			}
		cleanup(root);
		return Optional.of(root.getAsJsonObject());
		}
	
	}
