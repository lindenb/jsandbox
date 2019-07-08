package sandbox.flickr;

import java.io.File;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.beust.jcommander.Parameter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import sandbox.CookieStoreUtils;
import sandbox.IOUtils;
import sandbox.Launcher;


public class FlickrToAtom extends Launcher
	{
	private static final sandbox.Logger LOG = sandbox.Logger.builder(FlickrToAtom.class).build();
	
    @Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private File cookieStoreFile  = null;
    @Parameter(names={"-s","--seconds"},description="wait 's' seconds between each call.")
	private int seconds = 5;
	
	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private class Entry 
		{
		String id;
		String ownerNsid;
		String title;
		@SuppressWarnings("unused")
		String username;
		String displayUrl;
		String license;	
		String width;
		String height;	
					
		@Override
		public int hashCode() { return id.hashCode();}
		
		@Override
		public boolean equals(final Object o) {
			if(o==this) return true;
			return this.id.equals(Entry.class.cast(o).id);
			}

		}
	
	
	private Entry addImage(final JsonObject elt) {
		final Entry entry=new Entry();
		entry.id = elt.getAsJsonPrimitive("id").getAsString();
		entry.ownerNsid = elt.getAsJsonPrimitive("ownerNsid").getAsString();
		if(elt.get("title")!=null) {
			entry.title = elt.getAsJsonPrimitive("title").getAsString();
			}
		else
			{
			entry.title="";
			}
		entry.license = elt.getAsJsonPrimitive("license").getAsString();
		entry.username = elt.getAsJsonPrimitive("username").getAsString();
		JsonObject sizes = elt.getAsJsonObject("sizes");
		JsonObject sq = sizes.getAsJsonObject("t");
		if(sq==null) sq = sizes.getAsJsonObject("sq");
		entry.displayUrl = sq.getAsJsonPrimitive("displayUrl").getAsString();
		entry.width = sq.getAsJsonPrimitive("width").getAsString();
		entry.height = sq.getAsJsonPrimitive("height").getAsString();
		return entry;
		}
	
	private Set<Entry> searchNodes(final String name,final JsonElement elt)
		{
		final Set<Entry> set = new LinkedHashSet<>();
		_searchNodes(name,elt,set);
		return set;
		}
	
	private void _searchNodes(final String name,final JsonElement elt,final Set<Entry> set)
			{
			if(elt.isJsonArray())
				{
				final JsonArray jArray = elt.getAsJsonArray();
				if("_data".equals(name))
					{
					for(final JsonElement item:jArray) {
						if(item.isJsonObject()) set.add(addImage(item.getAsJsonObject()));
						}
					}
				else
					{
					for(final JsonElement item:jArray) {
						_searchNodes(null,item,set);
						}
					}
				}
			else if(elt.isJsonObject())
				{
				for(final Map.Entry<String, JsonElement> kv: elt.getAsJsonObject().entrySet())
					{
					//if(kv.getKey().equals("edge_hashtag_to_top_posts")) continue;
					_searchNodes(kv.getKey(),kv.getValue(),set);
					}	
				}
			}
	
	@Override
	public int doWork(final List<String> args)
		{
		CloseableHttpClient client = null;
			try 
			{
				
			final HttpClientBuilder builder =  HttpClientBuilder.create().
					setDefaultRequestConfig( org.apache.http.client.config .RequestConfig.custom().setCookieSpec(  org.apache.http.client.config.CookieSpecs.STANDARD).build()).
					setUserAgent(IOUtils.getUserAgent());
				
				if(this.cookieStoreFile!=null) {
					final BasicCookieStore cookies =  sandbox.CookieStoreUtils.readTsv(this.cookieStoreFile);
					builder.setDefaultCookieStore(cookies);
					}
					
				
				client = builder.build();
				
				
				
				final XMLOutputFactory xof = XMLOutputFactory.newInstance();
				XMLStreamWriter w = xof.createXMLStreamWriter(System.out, "UTF-8");
				w.writeStartDocument("UTF-8", "1.0");
				w.writeStartElement("feed");
				w.writeAttribute("xmlns", "http://www.w3.org/2005/Atom");
			
				w.writeStartElement("title");
				w.writeCharacters("Flickr2Atom");
				w.writeEndElement();
			
				w.writeStartElement("updated");
				w.writeCharacters(this.dateFormatter.format(new Date()));
				w.writeEndElement();
			
			
				for(int idx=0;idx< args.size();++idx) {
					final Set<Entry> entries = new HashSet<>();
					if(idx>0) Thread.sleep(seconds *  1_000);
					final String arg = args.get(idx);
					final HttpGet httpGet = new HttpGet(arg);
					httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
					httpGet.setHeader("Connection", "keep-alive");
					httpGet.setHeader("Accept-Language", "en-US,en;q=0.5");
					final ResponseHandler<String> responseHandler = new BasicResponseHandler();
					final String content;
					try {
						content = client.execute(httpGet,responseHandler);
						}
					catch(Throwable err) {
						LOG.warning(err);
						continue;
						}
					final String modelExport="modelExport:";
					boolean found=false;
					for(String line:content.split("[\n]"))
						{
						line=line.trim();
						int start= line.indexOf(modelExport);
						if(start==-1) continue;
						found = true;
						line=line.substring(start+modelExport.length());
						if(line.endsWith(",")) line=line.substring(0,line.length()-1).trim();
						try {
							final JsonReader jsr = new JsonReader(new StringReader(line));
							jsr.setLenient(true);
							final JsonParser parser = new JsonParser();
							JsonElement root = parser.parse(jsr);
							if(!root.isJsonObject())
								{
								LOG.error("root is not json object in "+arg);
								}
							else
								{
								entries.addAll(searchNodes(null,root.getAsJsonObject()));
 								}
							}
						catch(Throwable err) {
							err.printStackTrace();
							}
						break;
						}
					
					if(!found || entries.isEmpty()) {System.err.println("Not found in "+ arg); continue;}
					
				w.writeStartElement("entry");
				
				w.writeStartElement("title");
					w.writeCharacters(java.util.Arrays.stream(arg.split("[\\?&]")).
						filter(S->S.startsWith("text=")).
						map(S->S.substring(5).
						replace('+',' ')).
						findFirst().orElse(arg) );
				w.writeEndElement();

				w.writeStartElement("id");
				w.writeCharacters(arg);
				w.writeEndElement();

				
				w.writeEmptyElement("link");
				w.writeAttribute("href", arg );
				
				w.writeStartElement("updated");
					w.writeCharacters(FlickrToAtom.this.dateFormatter.format(new Date()));
				w.writeEndElement();
				
				w.writeStartElement("author");
					w.writeStartElement("name");
						w.writeCharacters("flickr");
					w.writeEndElement();
				w.writeEndElement();
				
				w.writeStartElement("content"); 
				w.writeAttribute("type","xhtml");
				
				w.writeStartElement("div");
				w.writeDefaultNamespace("http://www.w3.org/1999/xhtml");
				w.writeStartElement("p");  
				
				for(final Entry entry: entries ) {
					if(entry.license.equals("1")) w.writeCharacters("[");
					w.writeStartElement("a");
					w.writeAttribute("id",entry.id);
					w.writeAttribute("target", "_blank");
					w.writeAttribute("href","https://www.flickr.com/photos/"+ entry.ownerNsid +"/"+ entry.id +"/");
				
					w.writeEmptyElement("img");
		       		w.writeAttribute("alt", entry.title);
		       		w.writeAttribute("src", entry.displayUrl);
		       		w.writeAttribute("width",String.valueOf(entry.width));
		       		w.writeAttribute("height",String.valueOf(entry.height));
					if(entry.license.equals("1")) w.writeCharacters("]");
				
					w.writeEndElement();//a
					}
				
				w.writeEndElement();//p
				w.writeEndElement();//div
				w.writeEndElement();//content
				
				w.writeEndElement();//entry
					
					}
				
				client.close();client=null;
				w.writeEndElement();
				w.writeEndDocument();
				w.flush();
				w.close();
		
				
			return 0;
			}	 
			catch(final Exception err) {
				err.printStackTrace();
			return -1;
			}
			finally {
				IOUtils.close(client);
			}
		}
	public static void main(final String[] args)
		{
		new FlickrToAtom().instanceMainWithExit(args);

		}

	}
