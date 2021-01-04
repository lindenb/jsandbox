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
import java.util.List;

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
import org.w3c.tidy.Tidy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import sandbox.io.IOUtils;



public class XslHandler extends AbstractHandler
	{
	private static final String JSON_NS="http://www.ibm.com/xmlns/prod/2009/jsonx";

	private static final Logger LOG = LoggerFactory.getLogger("jsandbox");
	private String URL_PARAM="url";
	private String XSL_PARAM="xsl";
	private String ID_PARAM="id";
	private File beanXmlFile=null;
	
	public static class XslConfig
		{
		private String id="";
		private String url="";
		private String xsl="";
		private String inlineXsl;
		private String inputType="html";
		private String contentType=ContentType.APPLICATION_ATOM_XML.getMimeType();
		
		public XslConfig(){
		}
		
		public void setId(String id) {
			this.id = id;
		}
		public String getId() {
			return id;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public String getUrl() {
			return url;
		}
		
		public void setInputType(String inputType) {
			this.inputType = inputType;
		}
		
		public String getInputType() {
			return inputType;
		}
		
		public void setXsl(String xsl) {
			this.xsl = xsl;
		}
		
		public String getXsl() {
			return xsl;
		}
		
		public void setInlineXsl(String inlineXsl) {
			this.inlineXsl = inlineXsl;
		}
		public String getInlineXsl() {
			return inlineXsl;
		}
		
		public void setContentType(String contentType) {
			this.contentType = contentType;
		}
		
		public String getContentType() {
			return contentType;
		}
		
		public Source getStylesheet()
		{
		     if(this.getXsl().startsWith("http://") ||
		    		 this.getXsl().startsWith("https://") ||
		    		 this.getXsl().startsWith("ftp://") ||
		    		 this.getXsl().startsWith("file:"))
		     	{
					return new StreamSource(this.getXsl());
				} 
		     else
		     	{
					return new StreamSource(new File(this.getXsl()));
				}
		}
		
		
		
		private Document fetchJson() throws IOException
			{
			CloseableHttpClient httpclient=null;
			CloseableHttpResponse httpResponse=null;
			InputStream is=null;
			HttpEntity httpEntity;
			try {
				httpclient = HttpClients.createDefault();
				final HttpGet httpget = new HttpGet(this.getUrl());				
				httpResponse = httpclient.execute(httpget);
				httpEntity = httpResponse.getEntity();
				is=httpEntity.getContent();
				final Document dom = new Json2Dom().parse(is);				
				EntityUtils.consumeQuietly(httpEntity);
				return dom;
			} catch (Exception e) {
				e.printStackTrace();
				throw new IOException(e);
			} finally
				{
				IOUtils.close(is,httpResponse,httpclient);
				}
			}
		
		private Document fetchHtml() throws IOException
			{
			CloseableHttpClient httpclient=null;
			CloseableHttpResponse httpResponse=null;
			InputStream is=null;
			HttpEntity httpEntity;
			try {
				httpclient = HttpClients.createDefault();
				final HttpGet httpget = new HttpGet(this.getUrl());				
				httpResponse = httpclient.execute(httpget);
				httpEntity = httpResponse.getEntity();
				is=httpEntity.getContent();
				final Tidy tidy = new Tidy();
				tidy.setXmlOut(true);
				tidy.setShowErrors(0);
				tidy.setShowWarnings(false);
				final Document dom = tidy.parseDOM(is, null);
				
				/*final TransformerFactory trf = TransformerFactory.newInstance();
				final Transformer tr = trf.newTransformer();
				tr.setOutputProperty(OutputKeys.INDENT,"yes");
				tr.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
				tr.transform(new DOMSource(dom),
						new StreamResult(System.err)
						);*/
				
				EntityUtils.consumeQuietly(httpEntity);
				return dom;
			} catch (Exception e) {
				e.printStackTrace();
				throw new IOException(e);
			} finally
				{
				IOUtils.close(is,httpResponse,httpclient);
				}
			}

		private Document fetchXml() throws IOException
			{
			try {
				DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				DocumentBuilder db= dbf.newDocumentBuilder();
				return db.parse(getUrl());
			} catch (Exception e) {
				e.printStackTrace();
				throw new IOException(e);
			} finally
				{
				}
			}
		
		Document fetchDocument() throws IOException {
			if(this.inputType==null) inputType="html";
			if(this.inputType.equalsIgnoreCase("json")) return this.fetchJson();
			if(this.inputType.equalsIgnoreCase("xml")) return this.fetchXml();
			return this.fetchHtml();
			}
		
		
		
		}
	
	public void setBeanXmlFile(File beanXmlFile) {
		this.beanXmlFile = beanXmlFile;
	}
	
	
	
	@Override
	public void handle(
			final String target,
			final Request baseRequest, 
			final HttpServletRequest request,
			final HttpServletResponse response)
			throws IOException, ServletException
		{
		XslConfig config= null;
		final String configId=request.getParameter(ID_PARAM);
		if(configId!=null && !configId.isEmpty() && beanXmlFile!=null)
			{
			try {
				ApplicationContext beanFactory = new FileSystemXmlApplicationContext(
						this.beanXmlFile.toURI().toASCIIString());
				config = XslConfig.class.cast(beanFactory.getBean(configId));
			} catch(Exception err) {
				config= null;
			}
			if( config == null)
				{
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Cannot get "+configId);
				return;
				}
			}
		else
			{
			config = new XslConfig();
			
			final String urlIn = request.getParameter(URL_PARAM);
			if(urlIn==null || urlIn.trim().isEmpty())
				{
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,URL_PARAM+" param is missing.");
				return;
				}
			config.setUrl(urlIn);
			
			 String xslParam = request.getParameter(XSL_PARAM);
			if(xslParam==null || xslParam.trim().isEmpty())
				{
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,XSL_PARAM+" param is missing.");
				return;
				}
			config.setXsl(xslParam);
			}
		
		try {
			Document dom = config.fetchDocument();
			
			
		     final TransformerFactory factory = TransformerFactory.newInstance();

			final Transformer  transformer = factory.newTransformer(config.getStylesheet());
	        
			
			final Source domSource = new DOMSource(dom);
	        response.setContentType(config.getContentType());
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
		XslHandler xslHandler=new XslHandler();
		int port=8080;
		if(cmd.hasOption("p")) port=Integer.parseInt(cmd.getOptionValue("p"));
		if(cmd.hasOption("f"))
			{
			xslHandler.setBeanXmlFile(new File(cmd.getOptionValue("f")));
			
			}
	    final Server server = new Server(port);
        ContextHandler context = new ContextHandler();
        context.setContextPath("/xsl2atom");
        context.setHandler(xslHandler);
        server.setHandler(context);
	    server.start();
	    server.join();
	}
}
