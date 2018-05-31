package sandbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.beust.jcommander.Parameter;
/**

 */
public class InstagramToAtom extends Launcher {
	private static final Logger LOG = Logger.builder(InstagramToAtom.class).build();
	
	@Parameter(names={"-t","--tumb-size"},description="Thumb size.")
	private int thumb_size =128;
	@Parameter(names={"-f","--force"},description="Force print every items, even if they're not new.")
	private boolean force_print_flag=true;
	@Parameter(names={"-s","--seconds"},description="Sleep s seconds between each calls.")
	private int sleep_seconds = 5;
	@Parameter(names={"-c","--cache"},description="Cache directory. default: ${HOME}/.insta2atom ")
	private File cacheDirectory  = null;
	
	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	CloseableHttpClient client = null;
	
	
	
	private class Query
		{
		String query = "";
		Date date = new Date();
		String md5 = "";
		final Set<String> images_urls = new LinkedHashSet<>();
		boolean changed_flag = false;
		
		String getUrl() {
			return "https://www.instagram.com"+
					(this.query.startsWith("/")?"":"/") + 
					this.query +
					(this.query.endsWith("/")?"":"/");
			}
		}
	
	private File getPreferenceDir() {
		if(this.cacheDirectory==null) {
			final File userDir = new File(System.getProperty("user.home","."));
			this.cacheDirectory =  new File(userDir,".insta2atom");
			}
		return this.cacheDirectory;
		}
	private File getCacheFile() {
	
		return new File(getPreferenceDir(),"cache.tsv");
		}
	
	private void  query(Query q)
		{
		
		String html;
		
		CloseableHttpResponse response = null;
		InputStream httpIn = null;
		try
			{
			response = this.client.execute(new HttpGet( q.getUrl()));
			httpIn = response.getEntity().getContent();
			html = IOUtils.readStreamContent(httpIn);
			}
		catch(final IOException err)
			{
			LOG.error(err);
			q.changed_flag=false;
			return;
			}
		finally
			{
			IOUtils.close(httpIn);
			IOUtils.close(response);
			}
		
		final String thumbnail_src= "\"thumbnail_src\":\"";
		for(;;)
			{
			int i= html.indexOf(thumbnail_src);
			if(i==-1) break;
			i+= thumbnail_src.length();
			int j=  html.indexOf("\"",i);
			if(j!=-1) {
				final String image_url = html.substring(i, j);
				if(image_url.startsWith("https://") &&
					(image_url.endsWith(".png") || image_url.endsWith(".jpg")))
					{
					q.images_urls.add(image_url);
					}
				html = html.substring(j);
				}
			else
				{
				html = html.substring(i);
				}
			}
		 MessageDigest md;
		 try {
			 md = MessageDigest.getInstance("MD5");
		 } catch (Exception err) {
			LOG.error(err);
			throw new RuntimeException(err);
		 	}
		 md.update(String.join(" ", q.images_urls).getBytes());
		 final String new_md5 = new BigInteger(1,md.digest()).toString(16);
		if(q.images_urls.isEmpty() || new_md5.equals(q.md5)) {
			q.changed_flag =false;
		} 
		else
		{
			q.changed_flag =true;
			q.md5 = new_md5;
			q.date = new Date();
		}
		
		}
	
	
	private void saveCache(final List<Query> cache) {
		final File cacheFile = getCacheFile();
		if(!cacheFile.getParentFile().exists()) {
			LOG.info("creating "+cacheFile.getParent());
			if(!cacheFile.getParentFile().mkdir()) {
				LOG.error("Cannot create "+cacheFile.getParentFile());
				return;
				}
			}
		try(final PrintWriter pw = new PrintWriter(cacheFile))
			{
			cache.stream().forEach(Q->pw.println(Q.query+"\t"+dateFormatter.format(Q.date)+"\t"+Q.md5+"\t"+String.join(" ", Q.images_urls)));
			pw.flush();
			}
		catch(final IOException err) {
			LOG.error(err);
			}
		}
	
	private List<Query> readCache() {
		final List<Query> cache = new ArrayList<>();
		final File cacheFile = getCacheFile();
		if(!cacheFile.exists()) {
			LOG.warning("Cannot find cache "+cacheFile);
			return cache;
			}
		try(final FileReader fr=new FileReader(cacheFile)) {
			cache.addAll(new BufferedReader(fr).lines().
				filter(L->!(L.trim().isEmpty() || L.startsWith("#"))).
				map(L->L.split("[\t]")).
				map(A->{
					final Query q = new Query();
					q.query=A[0];
					if(A.length>2) { 
						try {
							q.date = dateFormatter.parse(A[1]);
							}
						catch(final ParseException err) {
							LOG.error(err);
							return null;
							}
						q.md5 = A[2];
						if(A.length>3 && !A[3].trim().isEmpty()) {
							q.images_urls.clear();
							q.images_urls.addAll(Arrays.asList(A[3].split("[ ]")));
							}
						}
					return q;
				}).
				filter(Q->Q!=null).
				collect(Collectors.toList())
				);
			}
		catch(final IOException err) {
			LOG.error(err);
			}
		return cache;
		}
	
	@Override
	public int doWork(final List<String> args) {
		
		XMLStreamWriter w = null;
		try
			{
			HttpHost proxy=new HttpHost("cache.ha.univ-nantes.fr", 3128);
			this.client = HttpClientBuilder.create().
					setProxy(proxy).
					setUserAgent(IOUtils.getUserAgent()).
					build();
			
			final XMLOutputFactory xof = XMLOutputFactory.newInstance();
			w = xof.createXMLStreamWriter(System.out, "UTF-8");
			w.writeStartDocument("UTF-8", "1.0");
			w.writeStartElement("feed");
			w.writeAttribute("xmlns", "http://www.w3.org/2005/Atom");
			
			w.writeStartElement("title");
			w.writeCharacters(getClass().getSimpleName());
			w.writeEndElement();
			
			w.writeStartElement("updated");
			w.writeCharacters(this.dateFormatter.format(new Date()));
			w.writeEndElement();
			
			final List<Query> cache = readCache();
			for(final Query q:cache) {
				w.writeComment(q.query);
				query(q);
				if(q.images_urls.isEmpty()) continue;
				if(!this.force_print_flag &&  !q.changed_flag) continue;
				
				w.writeStartElement("entry");
				
				w.writeStartElement("title");
					w.writeCharacters(q.query);
				w.writeEndElement();

				w.writeStartElement("id");
				w.writeCharacters(q.md5);
				w.writeEndElement();

				
				w.writeEmptyElement("link");
				w.writeAttribute("href", q.getUrl());
				
				w.writeStartElement("updated");
					w.writeCharacters(dateFormatter.format(q.date));
				w.writeEndElement();
				
				w.writeStartElement("author");
					w.writeStartElement("name");
						w.writeCharacters(q.query);
					w.writeEndElement();
				w.writeEndElement();
				
				w.writeStartElement("summary"); 
				w.writeAttribute("type","html");
				w.writeCharacters("<div><p>");
				for(final String thumb: q.images_urls) {
					w.writeCharacters(
						"<a href=\""+q.getUrl()+"\"><img src=\"" +
								thumb+
						"\" width=\""+thumb_size+"\" height=\""+this.thumb_size+"\"/></a>"
						);
					}
				w.writeCharacters("</p></div>");
				w.writeEndElement();
				
				w.writeEndElement();//entry
				
				Thread.sleep(this.sleep_seconds*1000);
				}
			
			w.writeEndElement();
			w.writeEndDocument();
			w.flush();
			w.close();
			saveCache(cache);
			this.client.close();this.client=null;
			return 0;
			}
		catch(final Exception err)
			{
			LOG.error(err);
			return -1;
			}
		finally
			{
			IOUtils.close(w);
			IOUtils.close(this.client);
			}
		}
	
	public static void main(final String[] args) {
		new InstagramToAtom().instanceMainWithExit(args);
		}

	
}
