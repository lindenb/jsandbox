package sandbox.flickr;

import java.text.SimpleDateFormat;

import com.beust.jcommander.Parameter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.BasicCookieStore;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import sandbox.IOUtils;
import sandbox.Launcher;
import sandbox.CookieStoreUtils;

public class FlickrToAtom extends Launcher
	{
	private static final sandbox.Logger LOG = sandbox.Logger.builder(FlickrToAtom.class).build();
	
    @Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private File cookieStoreFile  = null;
	
	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private class Entry 
		{
		String id;
		String ownerNsid;
		String title;
		String username;
		String displayUrl;
		
					
		@Override
		public int hashCode() { return id.hashCode();}
		
		@Override
		public boolean equals(Object o) {
			if(o==this) return true;
			return this.id.equals(Entry.class.cast(o).id);
			}
			
		void write( final XMLStreamWriter w) throws XMLStreamException {
			
				w.writeStartElement("entry");
				
				w.writeStartElement("title");
					w.writeCharacters(this.title);
				w.writeEndElement();

				w.writeStartElement("id");
				w.writeCharacters(this.id);
				w.writeEndElement();

				
				w.writeEmptyElement("link");
				w.writeAttribute("href", 
						"https://www.flickr.com/photos/"+ ownerNsid +"/"+ id +"/"
						);
				
				w.writeStartElement("updated");
					w.writeCharacters(FlickrToAtom.this.dateFormatter.format(new Date()));
				w.writeEndElement();
				
				w.writeStartElement("author");
					w.writeStartElement("name");
						w.writeCharacters(username);
					w.writeEndElement();
				w.writeEndElement();
				
				w.writeStartElement("content"); 
				w.writeAttribute("type","xhtml");
				
				w.writeStartElement("div");
				w.writeDefaultNamespace("http://www.w3.org/1999/xhtml");
				w.writeStartElement("p");  
				
				w.writeStartElement("a");
				w.writeAttribute("id",id);
				w.writeAttribute("target", "_blank");
				w.writeAttribute("href","https://www.flickr.com/photos/"+ ownerNsid +"/"+ id +"/");
				
				 w.writeEmptyElement("img");
           		w.writeAttribute("alt", title);
           		w.writeAttribute("src", displayUrl);
           		w.writeAttribute("width",String.valueOf(75));
				
				w.writeEndElement();//a
				
				w.writeEndElement();//p
				w.writeEndElement();//div
				w.writeEndElement();//content
				
				w.writeEndElement();//entry
				}	
			
		}
	final Set<Entry> entries = new HashSet<>();
	
	private void addImage(final JsonObject elt) {
		final Entry entry=new Entry();
		entry.id = elt.getAsJsonPrimitive("id").getAsString();
		entry.ownerNsid = elt.getAsJsonPrimitive("ownerNsid").getAsString();
		entry.title = elt.getAsJsonPrimitive("title").getAsString();
		entry.username = elt.getAsJsonPrimitive("username").getAsString();
		JsonObject sizes = elt.getAsJsonObject("sizes");
		JsonObject sq = sizes.getAsJsonObject("sq");
		entry.displayUrl = sq.getAsJsonPrimitive("displayUrl").getAsString();
		
		this.entries.add(entry);
		}
	
	private void searchNodes(final String name,final JsonElement elt)
			{
			if(elt.isJsonArray())
				{
				final JsonArray jArray = elt.getAsJsonArray();
				if("_data".equals(name))
					{
					for(final JsonElement item:jArray) {
						if(item.isJsonObject()) addImage(item.getAsJsonObject());
						}
					}
				else
					{
					for(final JsonElement item:jArray) {
						searchNodes(null,item);
						}
					}
				}
			else if(elt.isJsonObject())
				{
				for(final Map.Entry<String, JsonElement> kv: elt.getAsJsonObject().entrySet())
					{
					//if(kv.getKey().equals("edge_hashtag_to_top_posts")) continue;
					searchNodes(kv.getKey(),kv.getValue());
					}	
				}
			}
	
	@Override
	public int doWork(List<String> args)
		{
		CloseableHttpClient client = null;
			try 
			{
				
				HttpClientBuilder builder =  HttpClientBuilder.create().setUserAgent(IOUtils.getUserAgent());
				
				if(this.cookieStoreFile!=null) {
					final BasicCookieStore cookies =  sandbox.CookieStoreUtils.readTsv(this.cookieStoreFile);
					builder.setDefaultCookieStore(cookies);
					}
					
				
				client = builder.build();
			
				for(final String arg:args) {
					HttpGet httpGet = new HttpGet(arg);
					ResponseHandler<String> responseHandler = new BasicResponseHandler();
					final String content = client.execute(httpGet,responseHandler);
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
								searchNodes(null,root.getAsJsonObject());
								}
							}
						catch(Throwable err) {
							err.printStackTrace();
							}
						break;
						}
					
					if(!found) System.err.println("Not found in "+ arg);
					}
				
				client.close();client=null;
				
			
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
			
			for(Entry e:entries) e.write(w);
			
			w.writeEndElement();
			w.writeEndDocument();
			w.flush();
			w.close();
				
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
