package sandbox.tools.flickr;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.beust.jcommander.Parameter;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import sandbox.IOUtils;
import sandbox.Launcher;
import sandbox.flickr.FlickrExtractor;
import sandbox.http.CookieStoreUtils;
import sandbox.jcommander.DurationConverter;
import sandbox.jcommander.NoSplitter;


public class FlickrToAtom extends Launcher
	{
	private static final sandbox.Logger LOG = sandbox.Logger.builder(FlickrToAtom.class).build();
	
    @Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private File cookieStoreFile  = null;
    @Parameter(names={"-wait","--wait"},description="wait 's' seconds between each call. "+DurationConverter.OPT_DESC,converter = DurationConverter.class,splitter = NoSplitter.class)
	private Duration seconds = Duration.ofSeconds(5);
    @Parameter(names={"-html","--html"},description="output html")
	private boolean html_out = false;

	
	private abstract class AbstractImageWriter {
		XMLStreamWriter w ;
		AbstractImageWriter() throws XMLStreamException{
			final XMLOutputFactory xof = XMLOutputFactory.newInstance();
			this.w = xof.createXMLStreamWriter(System.out, "UTF-8");
			}
		abstract void writeStartElement() throws XMLStreamException;
		abstract void writeImages(String title,Collection<FlickrExtractor.Image> entries) throws XMLStreamException;
		abstract void writeEndElement() throws XMLStreamException;
		void writeHtmlEntry(FlickrExtractor.Image entry) throws XMLStreamException {
			w.writeStartElement("a");
			w.writeAttribute("id",entry.getId());
			w.writeAttribute("target", "_blank");
			w.writeAttribute("href",entry.getPageUrl());
		
			w.writeEmptyElement("img");
       		w.writeAttribute("alt", entry.getTitle());
       		w.writeAttribute("src", entry.getImageSrc());
       		w.writeAttribute("width",String.valueOf(entry.getWidth()));
       		w.writeAttribute("height",String.valueOf(entry.getHeight()));
			
			w.writeEndElement();//a
			}
		}
	
	private class AtomWriter extends AbstractImageWriter {
		private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		AtomWriter() throws XMLStreamException {
			
			}
		@Override void writeStartElement() throws XMLStreamException {
			w.writeStartDocument("UTF-8", "1.0");
			w.writeStartElement("feed");
			w.writeAttribute("xmlns", "http://www.w3.org/2005/Atom");
		
			w.writeStartElement("title");
			w.writeCharacters("Flickr2Atom");
			w.writeEndElement();
		
			w.writeStartElement("updated");
			w.writeCharacters(this.dateFormatter.format(new Date()));
			w.writeEndElement();
			}
		@Override void writeImages(String title,Collection<FlickrExtractor.Image> entries) throws XMLStreamException {
			w.writeStartElement("entry");
			
			w.writeStartElement("title");
				w.writeCharacters(title);
			w.writeEndElement();

			w.writeStartElement("id");
			w.writeCharacters(title);
			w.writeEndElement();

			
			w.writeEmptyElement("link");
			w.writeAttribute("href", title );
			
			w.writeStartElement("updated");
				w.writeCharacters(this.dateFormatter.format(new Date()));
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
			
			for(final FlickrExtractor.Image entry: entries ) {
				if(entry.getLicense().equals("1")) w.writeCharacters("[");
				writeHtmlEntry(entry);
				if(entry.getLicense().equals("1")) w.writeCharacters("]");
				}
			
			w.writeEndElement();//p
			w.writeEndElement();//div
			w.writeEndElement();//content
			
			w.writeEndElement();//entry
			
			}
		@Override
		void writeEndElement() throws XMLStreamException {
			w.writeEndElement();
			w.writeEndDocument();
			w.flush();
			w.close();
			}
		}

	
	private class HtmlWriter extends AbstractImageWriter {
		HtmlWriter() throws XMLStreamException {
			
			}
		@Override void writeStartElement() throws XMLStreamException {
			w.writeStartElement("html");
			w.writeStartElement("body");
			w.writeStartElement("dl");
			}
		@Override void writeImages(String title,Collection<FlickrExtractor.Image> entries) throws XMLStreamException {
			w.writeStartElement("dt");
			w.writeCharacters(title);
			w.writeEndElement();
			
			w.writeStartElement("dd");
			
			for(final FlickrExtractor.Image entry: entries ) {
				w.writeStartElement("span");
				writeHtmlEntry(entry);
				w.writeEndElement();//span
				}
			
			w.writeEndElement();//dd
			}
		@Override
		void writeEndElement() throws XMLStreamException {
			w.writeEndElement();
			w.writeEndElement();
			w.writeEndElement();
			w.flush();
			w.close();
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
					final BasicCookieStore cookies =  sandbox.http.CookieStoreUtils.readTsv(this.cookieStoreFile);
					builder.setDefaultCookieStore(cookies);
					}
					
				
				client = builder.build();
				
				AbstractImageWriter w = (html_out?new HtmlWriter():new AtomWriter());
				w.writeStartElement();
				
			
				for(int idx=0;idx< args.size();++idx) {
					final Set<FlickrExtractor.Image> entries = new HashSet<>();
					if(idx>0) Thread.sleep(seconds.toSeconds() * 1_000);
					final String arg = args.get(idx);
					final String content;
					if(IOUtils.isURL(arg)) {
						final HttpGet httpGet = new HttpGet(arg);
						httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
						httpGet.setHeader("Connection", "keep-alive");
						httpGet.setHeader("Accept-Language", "en-US,en;q=0.5");
						httpGet.setHeader("Referer", "https://www.flickr.com/");
						final ResponseHandler<String> responseHandler = new BasicResponseHandler();
						try {
							content = client.execute(httpGet,responseHandler);
							}
						catch(Throwable err) {
							LOG.warning(err);
							continue;
							}
						} else {
							content = Files.readString(Paths.get(arg));
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
							entries.addAll(new FlickrExtractor().apply(root));
							}
						catch(Throwable err) {
							err.printStackTrace();
							}
						break;
						}
					
					if(!found || entries.isEmpty()) {System.err.println("Not found in "+ arg); continue;}
					
					w.writeImages(arg, entries);
					}
				w.writeEndElement();
				client.close();client=null;
			
		
				
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
