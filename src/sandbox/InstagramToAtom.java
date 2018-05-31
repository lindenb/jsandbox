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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.beust.jcommander.Parameter;
/**
```
$ crontab -e

0 0-23 * * * /home/lindenb/bin/insta2atom.sh

$ cat /home/lindenb/bin/insta2atom.sh

```bash
#!/bin/bash

java -jar ${HOME}/src/jsandbox/dist/insta2atom.jar |\
	xmllint --format --output  "${HOME}/public_html/feed/instagram.xml" -
```


 */
public class InstagramToAtom extends Launcher {
	private static final Logger LOG = Logger.builder(InstagramToAtom.class).build();
	
	@Parameter(names={"-t","--tumb-size"},description="Thumb size.")
	private int thumb_size =256;
	@Parameter(names={"-f","--force"},description="Force print only new items, discard the non-updated.")
	private boolean force_print_new_only = false;
	@Parameter(names={"-s","--seconds"},description="Sleep s seconds between each calls.")
	private int sleep_seconds = 5;
	@Parameter(names={"-d","--directory"},description="Cache directory. default: ${HOME}/.insta2atom ")
	private File cacheDirectory  = null;
	@Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private File cookieStoreFile  = null;
	@Parameter(names={"-g","--group"},description="group items per query")
	private boolean group_flag =false;

	
	
	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private CloseableHttpClient client = null;
	
	private static class Image implements Comparable<Image>
		{
		final String url;
		final String shortcode;
		
		Image(final String url,final String shortcode) {
			this.url = url;
			this.shortcode = shortcode.trim();
			}
		Image(final String url) {
			final int pipe = url.indexOf("|");
			if(pipe==-1) {
				this.url = url;
				this.shortcode = "";
				}
			else
				{
				this.url = url.substring(0,pipe);
				this.shortcode = url.substring(pipe+1).trim();
				}
			}
		@Override
		public int compareTo(final Image o)
			{
			return this.url.compareTo(o.url);
			}
		@Override
		public int hashCode()
			{
			return this.url.hashCode();
			}
		
		public String getPageHref() {
			return "https://www.instagram.com/p/"+this.shortcode+"/";
		}
		
		@Override
		public boolean equals(Object obj)
			{
			if(obj==this) return true;
			if(obj==null || !(this instanceof Image)) return false;
			return compareTo(Image.class.cast(obj))==0;
			}
		@Override
		public String toString()
			{
			return this.url;
			}
		}
	
	private class Query
		{
		String query = "";
		Date date = new Date();
		String md5 = "";
		final Set<Image> old_images_urls = new TreeSet<>();
		final Set<Image> new_images_urls = new TreeSet<>();
		
		String getUrl() {
			return "https://www.instagram.com"+
					(this.query.startsWith("/")?"":"/") + 
					this.query +
					(this.query.endsWith("/")?"":"/");
			}
		}
	
	private String md5(final String s) {
		 MessageDigest md;
		 try {
			 md = MessageDigest.getInstance("MD5");
		 } catch (final Exception err) {
			throw new RuntimeException(err);
		 	}
		md.update(s.getBytes());
		return new BigInteger(1,md.digest()).toString(16);
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
	
	private boolean  query(final Query q)
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
			return false;
			}
		finally
			{
			IOUtils.close(httpIn);
			IOUtils.close(response);
			}
		final String thumbnail_src= "\"thumbnail_src\":\"";
		final String shortcode_src= "\"shortcode\":\"";
		for(;;)
			{
			int i= html.indexOf(thumbnail_src);
			
			if(i==-1) {
				break;
				}
			int h=i;
			i+= thumbnail_src.length();
			final int j=  html.indexOf("\"",i);
			if(j!=-1) {
				String shortcode="";
				while(h>=0 && h+shortcode_src.length() < html.length()) {
					if(html.substring(h,h+shortcode_src.length()).equals(shortcode_src)) {
						h+=shortcode_src.length();
						final int k = html.indexOf("\"",h);
						if(k!=-1) {
							shortcode =  html.substring(h, k);;
							}
						break;
						}
					--h;
					}
				
				final String image_url = html.substring(i, j);
				if(image_url.startsWith("https://") &&
					(image_url.endsWith(".png") || image_url.endsWith(".jpg")))
					{
					q.new_images_urls.add(new Image(image_url,shortcode));
					}
				html = html.substring(j);
				}
			else
				{
				html = html.substring(i);
				}
			}
		
		if(q.new_images_urls.isEmpty()) {
			LOG.warning("No image found for "+q.query);
		}
		
		final String new_md5 = md5(q.new_images_urls.stream().
				map(I->I.url).
				collect(Collectors.joining(" ")));
		if(!new_md5.equals(q.md5)) {
			q.md5 = new_md5;
			q.date = new Date();
			}
		return true;
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
			pw.println("#Date: "+dateFormatter.format(new Date()));
			cache.stream().forEach(Q->pw.println(
					Q.query+"\t"+dateFormatter.format(Q.date)+"\t"+Q.md5+"\t"+
					Q.new_images_urls.
					stream().
					map(I->I.url+(I.shortcode.isEmpty()?"":"|"+I.shortcode)).
					collect(Collectors.joining(" "))));
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
					q.query=A[0].trim();
					if(q.query.isEmpty()) return null;
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
							q.old_images_urls.clear();
							q.old_images_urls.addAll(
									Arrays.stream(A[3].split("[ ]")).map(S->new Image(S)).collect(Collectors.toSet()));
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
			for(int idx=0;idx < cache.size();++idx) {
				final Query q=cache.get(idx);
				w.writeComment(q.query);
				if(!query(q)) continue;
				
				final Set<Image> images_to_print = new TreeSet<>(q.new_images_urls);
				
				if(this.force_print_new_only)
					{
					images_to_print.removeAll(q.old_images_urls);
					}
			
				if(images_to_print.isEmpty()) continue;
				
				if(group_flag)
					{
					w.writeStartElement("entry");
					
					w.writeStartElement("title");
						w.writeCharacters(q.query);
					w.writeEndElement();
	
					w.writeStartElement("id");
					w.writeCharacters(q.md5);
					w.writeEndElement();
	
					
					w.writeEmptyElement("link");
					w.writeAttribute("href", q.getUrl()+"?m="+q.md5);
					
					w.writeStartElement("updated");
						w.writeCharacters(this.dateFormatter.format(q.date));
					w.writeEndElement();
					
					w.writeStartElement("author");
						w.writeStartElement("name");
							w.writeCharacters(q.query);
						w.writeEndElement();
					w.writeEndElement();
					
					w.writeStartElement("content"); 
					w.writeAttribute("type","html");
					w.writeCharacters("<div><p>");
					for(final Image image_url:images_to_print) {
						w.writeCharacters("<a target=\"_blank\" href=\""+
								(image_url.shortcode.isEmpty()?q.getUrl():image_url.getPageHref())+
								"\"><img src=\"" +
								image_url.url +
						"\" width=\""+this.thumb_size+"\" height=\""+this.thumb_size+"\"/></a>"
						);
					}
					w.writeCharacters("</p></div>");
					w.writeEndElement();//content
					
					w.writeEndElement();//entry
					}
				else
					{
					for(final Image image_url : images_to_print) {
						w.writeStartElement("entry");
						
						w.writeStartElement("title");
							w.writeCharacters(q.query);
						w.writeEndElement();
		
						w.writeStartElement("id");
						w.writeCharacters(md5(image_url.url));
						w.writeEndElement();
		
						
						w.writeEmptyElement("link");
						w.writeAttribute("href", 
								image_url.shortcode.isEmpty()?
								q.getUrl()+"?m="+md5(image_url.url):
								image_url.getPageHref()
								);
						
						w.writeStartElement("updated");
							w.writeCharacters(this.dateFormatter.format(q.date));
						w.writeEndElement();
						
						w.writeStartElement("author");
							w.writeStartElement("name");
								w.writeCharacters(q.query);
							w.writeEndElement();
						w.writeEndElement();
						
						w.writeStartElement("content"); 
						w.writeAttribute("type","html");
						w.writeCharacters("<div><p>" +
							"<a target=\"_blank\" href=\""+
									(image_url.shortcode.isEmpty()?
									q.getUrl()+"?m="+md5(image_url.url):
									image_url.getPageHref()
									)+"\"><img src=\"" +
									image_url.url +
							"\" width=\""+this.thumb_size+"\" height=\""+this.thumb_size+"\"/></a>" +
							"</p></div>"
							);
						w.writeEndElement();//content
						
						w.writeEndElement();//entry
						}
					}
				
				if(idx>0) Thread.sleep(this.sleep_seconds*1000);
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
