package sandbox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

public class DcComicsScraper extends Launcher {
	private static final Logger LOG = Logger.builder(DcComicsScraper.class).build();
	
	@Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private File cookieStoreFile  = null;
	@Parameter(names={"-B","--bdb-home"},description="BerkeleyDB home",required=true)
	private File bdbHome=null;
	// https://www.dccomics.com/comics?seriesid=394486#browse
	@Parameter(names={"-series","--series"},description="Series-id")
	private String seriesIdsStr="";
	
	private Environment env = null;
	private Database nodesDb = null;
	private Transaction txn=null;
	private CloseableHttpClient client = null;

	private class JsonBinding extends TupleBinding<JsonElement> {
		private final JsonParser parser =new JsonParser();
		private final Gson gson = new GsonBuilder().create();
		private JsonBinding() {
		}
		
		@Override
		public JsonElement entryToObject(final TupleInput in) {
			final int n=in.readInt();
			JsonReader r=new JsonReader(new StringReader(in.readChars(n)));
			return this.parser.parse(r);
			}
		@Override
		public void objectToEntry(JsonElement e, TupleOutput out) {		     
			final String s= this.gson.toJson(e);
		      final char array[]=s.toCharArray();
		      out.writeInt(array.length);
		      out.writeChars(array);
			}
		}
	
	private final JsonBinding jsonBinding = new JsonBinding();
	
	private void openBdb() throws IOException{
		LOG.info("open bdb");
		final EnvironmentConfig envcfg = new EnvironmentConfig();
		envcfg.setAllowCreate(true);
		envcfg.setReadOnly(false);
		this.env =new Environment(bdbHome, envcfg);
		final DatabaseConfig cfg = new DatabaseConfig();
		cfg.setAllowCreate(true);
		cfg.setReadOnly(false);
		this.nodesDb = this.env.openDatabase(txn, "nodes", cfg);
		}
	private void closeBdb() {
		LOG.info("close bdb");
		if(this.nodesDb!=null)this.nodesDb.close();
		this.nodesDb=null;
		if(this.env!=null){
			this.env.cleanLog();
			this.env.close();
		}
		this.env=null;
	}
	

	
	private JsonElement fetchJson(final String url) throws IOException {
		LOG.info("fetch "+url);
		CloseableHttpResponse resp=null;
		InputStream in=null;
		try {
			resp = this.client.execute(new HttpGet(url));
			if(resp.getStatusLine().getStatusCode()!=200) return null;
			in = resp.getEntity().getContent();
			final JsonReader jsr = new JsonReader(new InputStreamReader(in, "UTF-8"));
			jsr.setLenient(true);
			final JsonParser parser = new JsonParser();
			final JsonElement root=parser.parse(jsr);
			return root;
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
	
	private void insertComics(final JsonObject root) throws IOException {
		final String nid = root.get("nid").getAsString();
		final String type = root.get("type").getAsString();
		final String keystr = type+"~"+nid;
		final DatabaseEntry key = new DatabaseEntry();
		final DatabaseEntry data = new DatabaseEntry();
		StringBinding.stringToEntry(keystr, key);
		if(this.nodesDb.get(this.txn, key, data,LockMode.DEFAULT )==OperationStatus.SUCCESS) {
			LOG.info("already inserted "+ keystr);
		}
		
		JsonObject obj = new JsonObject();
		obj.addProperty("id", nid);
		obj.addProperty("title", root.get("dc_solr_sortable_title").getAsString());
		obj.addProperty("date", root.get("dc_solr_relevant_date").getAsString());
		
		JsonArray characters=root.get("field_related_generic_character").getAsJsonArray();

		obj.add("characters", characters);
		
		for(int i=0;i< characters.size();++i) {
			
		}
		jsonBinding.objectToEntry(root, data);
		if(this.nodesDb.put(this.txn, key, data)!=OperationStatus.SUCCESS) {
			LOG.info("cannot insert "+ root);
		}

		
	}
	
	private final int MAX_PAGE_PER_SERIES=3;
	private void fetchSerie(int id) throws IOException {
		for(int i=1;i<=MAX_PAGE_PER_SERIES;++i) {
			final JsonElement root = fetchJson("https://www.dccomics.com/proxy/search?page="+i+"&type=comic%7Cgraphic_novel&seriesid="+id);
			if(root==null || !root.isJsonObject()) continue;
			final JsonObject object = root.getAsJsonObject();
			if(!object.has("results") ) continue;
			final JsonElement e = object.get("results");
			if(e==null ||!e.isJsonObject()) continue;
			final JsonObject results = e.getAsJsonObject();
			final List<JsonObject> comics = results.
				entrySet().stream().
				map(KV->KV.getValue()).
				filter(E->E!=null && E.isJsonObject()).
				map(E->E.getAsJsonObject()).
				filter(E->E.has("fields")).
				map(E->E.get("fields").getAsJsonObject()).
				collect(Collectors.toList());
			for(final JsonObject c:comics) {
				insertComics(c);
			}
			}
		}
	
	@Override
	public int doWork(final List<String> args) {
		try
			{
			openBdb();
			
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
			
			this.client = builder.build();

			for(final String serieId: seriesIdsStr.split("[ ,]+")){
				if(serieId.trim().isEmpty()) continue;
				final int serie_id;
				try {
					serie_id = Integer.parseInt(serieId);
					}
				catch(NumberFormatException err) {
					LOG.error(err);
					continue;
					}
				fetchSerie(serie_id);
				}
			
			return 0;
			}
		catch(final Exception err)
			{
			LOG.error(err);
			return -1;
			}
		finally
			{
			IOUtils.close(this.client);
			closeBdb();
			}
		
		}	
	public static void main(final String[] args) {
		new DcComicsScraper().instanceMainWithExit(args);

	}

}
