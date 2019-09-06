package sandbox.ig;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpHost;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.beust.jcommander.Parameter;

import sandbox.HtmlParser;
import sandbox.IOUtils;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.http.CookieStoreUtils;
/**

e.g: https://stalkfest.com/account/emrata/1999824/friends/

 */
public class InstagramGraph extends Launcher {
	private static final Logger LOG = Logger.builder(InstagramGraph.class).build();

	@Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private Path cookieStoreFile  = null;

	private CloseableHttpClient client = null;
	private XPath xpath;
	private int id_generator=0;
	
	private interface Handler {
		public Set<String> getFriends(final Document dom) throws Exception;
		public String getName(final String url);
		}
	
	private class MySocialMateHandler implements Handler {
		@Override
		public Set<String> getFriends(final Document dom) throws Exception {
			if(dom==null || dom.getDocumentElement()==null) {
				LOG.warning("dom is null/empty");
				return Collections.emptySet();
				}

			final NodeList anchors=(NodeList)InstagramGraph.this.xpath.evaluate("//a[@class='author-thumb']", dom, XPathConstants.NODESET);
			if(anchors==null || anchors.getLength()<2)  {
				LOG.warning("xpath1 return null");
				return Collections.emptySet();
				};
			final Set<String> friends = new HashSet<>(anchors.getLength());
			
			for(int i=0;i< anchors.getLength();i++) {
				final Element a =(Element)anchors.item(i);
				if(a==null) continue;
				String href=a.getAttribute("href")+"/following";
				friends.add(href);
				}
			return friends;
			}
	
		@Override
		public String getName(final String url) {
			final String tokens[]=url.split("[/]");
			for(int i=0;i+1< tokens.length;i++) {
				if(tokens[i].equals("u")) return tokens[i+1];
				}
			return url;
			}
		}
	
	private class StalkfestHandler implements Handler {
		@Override
		public Set<String> getFriends(final Document dom) throws Exception{
			if(dom==null || dom.getDocumentElement()==null) {
				LOG.warning("dom is null/empty");
				return Collections.emptySet();
				}
			
			final NodeList divs=(NodeList)InstagramGraph.this.xpath.evaluate("//div[contains(@class,'some-fellowsMedium')]", dom, XPathConstants.NODESET);
			if(divs==null || divs.getLength()<2)  {
				LOG.warning("xpath1 return null");
				return Collections.emptySet();
				};
			final NodeList anchors = (NodeList)InstagramGraph.this.xpath.evaluate("//a[@href and contains(@class,'some-item-user')]", divs.item(1), XPathConstants.NODESET);
			if(anchors==null || anchors.getLength()==0)  {
				LOG.warning("xpath2 return null");
				return Collections.emptySet();
				};
			
			final Set<String> friends = new HashSet<>(anchors.getLength());
			
			for(int i=0;i< anchors.getLength();i++) {
				final Element a =(Element)anchors.item(i);
				if(a==null) continue;
				String href="https://stalkfest.com"+a.getAttribute("href")+"friends";
				friends.add(href);
				}
			return friends;
			}
	
		@Override
		public String getName(final String url) {
			final String tokens[]=url.split("[/]");
			for(int i=0;i+1< tokens.length;i++) {
				if(tokens[i].equals("account")) return tokens[i+1];
				}
			return url;
			}
		}
	
	private final Map<String, User> url2user = new HashMap<>();
	private Handler handler = null;
	
	
	private class User {
		final String url;
		final int id;
		final Set<String> friends= new HashSet<>();
		User(String url) {
			this.url = url;
			this.id=(++id_generator);
			}
		
		String getName() {
			return InstagramGraph.this.handler.getName(this.url);
			}
		
		@Override
		public boolean equals(Object obj) {
			if(obj==this) return true;
			if(obj==null || !(obj instanceof User)) return false;
			return this.url.equals(User.class.cast(obj).url);
			}
		
		@Override
		public int hashCode() {
			return this.url.hashCode();
			}
	}
	
	private void scan(final String url,int depth)  {
		try {
			User u = this.url2user.get(url);
			if(u!=null) {
				return;
				}
			LOG.info(url+" "+depth);
			
			u=new User(url);
			this.url2user.put(url, u);
			
			if(depth>2) return;
			
			final HttpGet httpGet=new HttpGet(url);
			this.client.execute(httpGet);
			httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			httpGet.setHeader("Connection", "keep-alive");
			httpGet.setHeader("Accept-Language", "en-US,en;q=0.5");
			final ResponseHandler<String> responseHandler = new BasicResponseHandler();
			final String content;
			try {
				content = client.execute(httpGet,responseHandler);
				Thread.sleep(1*1000);
				}
			catch(Throwable err) {
				LOG.warning(err);
				return;
				}
			LOG.info("fetched.");
			final Document dom=new HtmlParser().setNamespaceAware(false).parseDom(content);
			if(dom==null || dom.getDocumentElement()==null) {
				LOG.warning("dom is null/empty");
				return;
			}

			
			final Set<String> friends = this.handler.getFriends(dom);		
			u.friends.addAll(friends);
	
			for(final String u2:friends) {
				if(url2user.containsKey(u2)) continue;
				scan(u2, depth+1);
				}
			}
		catch(Exception err) {
			LOG.error(err);
		}
		finally {
			
		}
	}
	
	@Override
	public int doWork(final List<String> args) {
		try
			{	
			this.xpath = XPathFactory.newInstance().newXPath();
			
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
			
			for(final String s:args)
				{
				this.handler = new MySocialMateHandler();
				scan(s,0);
				}
			
			PrintStream out=System.out;
			out.println("digraph G {");
			for(final User u: this.url2user.values()) {
				out.println("n"+u.id+";");
				}
			for(final User u: this.url2user.values()) {
				for(final String f:u.friends) {
					User u2=this.url2user.get(f);
					if(u2==null || u2.getName().compareTo(u.getName())<=0) continue;
					out.println("n"+u.id+" -> n"+u2.id+";");
					}
				}
			out.println("}");
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
			}
		
		}
	
	public static void main(final String[] args) {
		new InstagramGraph().instanceMainWithExit(args);
		}

	
}
