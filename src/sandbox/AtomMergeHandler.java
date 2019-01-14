package sandbox;

/* <?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xmlns:util="http://www.springframework.org/schema/util"
xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
    http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util-2.5.xsd">


<util:list id="myList">
<bean id="mainBean" class="sandbox.XslHandler$XslConfig">
	<property name="id"   value="1"/>
	<property name="url"  value="http://www.ncbi.nlm.nih.gov/pubmed/trending/"/>
        <property name="xsl"> 
		<value>https://raw.githubusercontent.com/lindenb/xslt-sandbox/master/stylesheets/bio/ncbi/pubmedtrending2rss.xsl</value>
        </property>
</bean>

</util:list>
</beans>
*/

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.resource.PathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;



public class AtomMergeHandler extends AbstractHandler
	{

	private static final Logger LOG = LoggerFactory.getLogger("jsandbox");
	
	
	
	
	@Override
	public void handle(
			final String target,
			final Request baseRequest, 
			final HttpServletRequest request,
			final HttpServletResponse response)
			throws IOException, ServletException
		{
		try {
			final XPath xpath = XPathFactory.newInstance().newXPath();
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			
		    final Set<String> urls = new HashSet<>();
		    final Set<String> entryIds = new HashSet<>();
		    Document dom=null;
		    for(final String url: urls)
		    	{
		    	if( dom == null )
		    		{
		    		dom = db.parse(url);
		    		final NodeList nl = (NodeList)xpath.evaluate("/a:feed/a:entry", dom, XPathConstants.NODESET);
		    		for(int i=0;i< nl.getLength();++i)
		    			{
		    			entryIds.add((String)xpath.evaluate("a:id/text()", nl.item(i), XPathConstants.STRING));
		    			}
		    		}
		    	else
		    		{
		    		Document dom2 = db.parse(url);
		    		final NodeList nl = (NodeList)xpath.evaluate("/a:feed/a:entry", dom2, XPathConstants.NODESET);
		    		for(int i=0;i< nl.getLength();++i)
		    			{
		    			String entryId =(String)xpath.evaluate("a:id/text()",  nl.item(i), XPathConstants.STRING);
		    			if(entryIds.contains(entryId)) continue;
		    			entryIds.add(entryId);
		    			dom.getDocumentElement().appendChild(dom.importNode( nl.item(i),true));
		    			}
		    		}
		    	}
			
			final TransformerFactory factory = TransformerFactory.newInstance();
			final Transformer  transformer = factory.newTransformer();
	        
			
			final Source domSource = new DOMSource(dom);
	        response.setContentType(ContentType.APPLICATION_ATOM_XML.getMimeType());
	        response.setStatus(HttpServletResponse.SC_OK);
	        baseRequest.setHandled(true);
	        
	        final OutputStream out = response.getOutputStream();
	        transformer.transform(
	        		domSource,
	        		new StreamResult( out)
	        		);
	        out.flush();
	        out.close();
			}
		catch(IOException err)
			{
			LOG.error("Error",err);
			throw err;
			}
		catch(Exception err)
			{
			LOG.error("Error",err);
			throw new ServletException(err);
			}
		finally
			{
			
			}
		}
	public static void main(String[] args) throws Exception {
		final CommandLineParser parser = new DefaultParser();
		final Options options = new Options();
		CommandLine cmd = null;
		try
			{
			
			options.addOption(Option.builder("p").hasArg().desc("port").argName("PORT").build());
			options.addOption(Option.builder("f").hasArg().desc("bean-factory.xml").argName("XML").build());
			cmd = parser.parse(options, args);
			}
		catch(ParseException err)
			{
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("xslhandler", options);
			System.err.println(err.getMessage());
			System.exit(-1);
			}
		AtomMergeHandler xslHandler=new AtomMergeHandler();
		int port=8080;
		if(cmd.hasOption("p")) port=Integer.parseInt(cmd.getOptionValue("p"));
		if(cmd.hasOption("f"))
			{
			xslHandler.setBeanXmlFile(new File(cmd.getOptionValue("f")));
			
			}
	    final Server server = new Server(port);
        ContextHandler context = new ContextHandler();
        context.setContextPath("/atommerge");
        context.setHandler(xslHandler);
        server.setHandler(context);
	    server.start();
	    server.join();
	}
}
