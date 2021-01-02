package sandbox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.beust.jcommander.Parameter;

import sandbox.http.CookieStoreUtils;
import sandbox.io.IOUtils;

public class GrandComicsDatabaseScraper extends Launcher {
	private static final Logger LOG = Logger.builder(GrandComicsDatabaseScraper.class).build();
	
	@Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private File cookieStoreFile  = null;
	// https://www.comics.org/issue/177/
	@Parameter(names={"-issues","--issues"},description="Issues-id")
	private String issuesIdStr="";
	
	private CloseableHttpClient client = null;
	private XPath xpath;
	

	
	private String fetchHtml(final String url) throws IOException {
		LOG.info("fetch "+url);
		CloseableHttpResponse resp=null;
		InputStream in=null;
		try {
			resp = this.client.execute(new HttpGet(url));
			if(resp.getStatusLine().getStatusCode()!=200) {
				LOG.error("cannot fetch "+url+" "+resp.getStatusLine());
				return null;
			}
			in = resp.getEntity().getContent();
			return IOUtils.readStreamContent(in);
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
	
	
	private int fetchIssue(final XmlStreamWriter w,int id) throws Exception {
			final String htmlDoc = fetchHtml("https://www.comics.org/issue/"+id+"/");
			if(htmlDoc==null ||htmlDoc.isEmpty()) return -1;
			Document dom = new HtmlParser().parseDom(htmlDoc);
			if(dom==null) return -1;
			w.writeStartElement("issue");
			w.writeAttribute("id", String.valueOf(id));
			final List<Element> elements = XmlUtils.allNodes(dom).
					stream().
					filter(XmlUtils.isElement).
					map(N->Element.class.cast(N)).
					collect(Collectors.toList())
					;
			
			w.writeStartElement("series");
			w.writeCharacters((NodeList)this.xpath.evaluate("//span[@id='series_name'][1]", dom, XPathConstants.STRING));
			w.writeEndElement();
			
			
			w.writeEndElement();
			return -1;
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
			
			final XMLOutputFactory xof = XMLOutputFactory.newInstance();
			final XmlStreamWriter out= XmlStreamWriter.wrap(xof.createXMLStreamWriter(System.out, "UTF-8"));
			out.writeStartDocument("UTF-8", "1.0");
			out.writeStartElement("gcd");
			for(final String serieId: issuesIdStr.split("[ ,]+")){
				if(serieId.trim().isEmpty()) continue;
				int serie_id;
				try {
					serie_id = Integer.parseInt(serieId);
					}
				catch(final NumberFormatException err) {
					LOG.error(err);
					continue;
					}
				while(serie_id>0)
					{
					serie_id = fetchIssue(out,serie_id);
					}
				out.writeEndElement();
				out.writeEndDocument();
				out.flush();
				out.close();
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
			}
		
		}	
	public static void main(final String[] args) {
		new GrandComicsDatabaseScraper().instanceMainWithExit(args);

	}

}
