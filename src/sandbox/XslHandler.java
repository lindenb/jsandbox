package sandbox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamWriter;
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



public class XslHandler extends AbstractHandler
	{
	private static final String JSON_NS="http://www.ibm.com/xmlns/prod/2009/jsonx";

	private static final Logger LOG = LoggerFactory.getLogger("jsandbox");
	private String URL_PARAM="url";
	private String XSL_PARAM="xsl";
	
	public static class XslConfig
		{
		private String id="";
		private String url="";
		private String inputType="html";
		
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
		
		
		private void _jsonparseObject(final Node root,final String label,JsonReader r) throws Exception
			{
			final Element E = root.getOwnerDocument().createElementNS(JSON_NS, "jsonx:object");
			if(label!=null) E.setAttribute("name",label);
			for(;;)
				{
				if(r.peek()==JsonToken.END_OBJECT) break;
				if(r.peek()!=JsonToken.NAME) throw new IllegalStateException(r.peek().name());
				final String s=r.nextName();
				_jsonParse(E,s,r);
				}
			}
		
		private void _jsonParseArray(final Node root,final String label,JsonReader r) throws Exception
			{
			final Element E =  root.getOwnerDocument().createElementNS(JSON_NS, "jsonx:array");
			if(label!=null) E.setAttribute("name",label);
			for(;;)
				{
				if(r.peek()==JsonToken.END_ARRAY) break;
				_jsonParse(E,null,r);
				}
			}
	
	
		private void _jsonParse(final Node root,final String label,JsonReader r) throws Exception
			{
			if(!r.hasNext()) return ;
			final Document owner=root.getOwnerDocument();
			    JsonToken token=r.peek();
			    switch(token)
			    	{
			    	case NAME: break;
			    	case BEGIN_OBJECT:
			    		{
			    		r.beginObject();
			    		_jsonparseObject(root,label,r);	
			    		break;
			    		}
			    	case END_OBJECT:
			    		{
			    		break;
			    		}
			    	case BEGIN_ARRAY:
			    		{
			    		r.beginArray();
			    		_jsonParseArray(root,label,r);
			    		break;
			    		}
			    	case END_ARRAY:
			    		{
			    		break;
			    		}
			    	case NULL:
			    		{
			    		r.nextNull();
			    		final Element E = owner.createElementNS(JSON_NS, "jsonx:null");
			    		if(label!=null) E.setAttribute("name",label);
			    		root.appendChild(E);
			    		break;
			    		}
			    	case STRING:
			    		{
			    		final Element E = owner.createElementNS(JSON_NS, "jsonx:string");
			    		if(label!=null) E.setAttribute("name",label);
			    		E.appendChild(owner.createTextNode(r.nextString()));
			    		root.appendChild(E);
			    		break;
			    		}
			    	case NUMBER:
			    		{
				    	final Element E = owner.createElementNS(JSON_NS, "jsonx:number");
				    	root.appendChild(E);
			    		if(label!=null) E.setAttribute("name",label);
			    		String s;
			    		try
			    			{
			    			s= String.valueOf(r.nextLong());
			    			}
			    		catch(Exception err)
			    			{
			    			s= String.valueOf(r.nextDouble());
			    			}
	
			    		E.appendChild(owner.createTextNode(s));
			    		break;
			    		}
			    	case BOOLEAN:
			    		{
					    final Element E = owner.createElementNS(JSON_NS, "jsonx:boolean");
			    		if(label!=null) E.setAttribute("name",label);
			    		E.appendChild(owner.createTextNode(String.valueOf(r.nextBoolean())));
			    		root.appendChild(E);
			    		break;
			    		}
			    	case END_DOCUMENT:
			    		{
			    		break;
			    		}
			    	default: throw new IllegalStateException(token.name());
			    	}
			}

		
		private Document fetchJson() throws IOException
			{
			CloseableHttpResponse httpResponse;
			HttpEntity httpEntity;
			try {
				CloseableHttpClient httpclient = HttpClients.createDefault();
				final HttpGet httpget = new HttpGet(this.url);				
				httpResponse = httpclient.execute(httpget);
				httpEntity = httpResponse.getEntity();
				InputStream is=httpEntity.getContent();
				JsonReader reader = new JsonReader(new InputStreamReader(is));
				reader.setLenient(true);

				
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				Document dom = dbf.newDocumentBuilder().newDocument();
				_jsonParse(dom,null,reader);
				
				EntityUtils.consume(httpEntity);
				httpResponse.close();
				httpclient.close();
				
				return dom;
				 
			} catch (Exception e) {
				throw new IOException(e);
			}
			
			
			
			}
		
		Document fetchDocument() {
			if(this.inputType==null) inputType="html";
			if(this.inputType.equalsIgnoreCase("json")) ;
			return null;
			}
		
		
		
		}
	
	@Override
	public void handle(
			final String target,
			final Request baseRequest, 
			final HttpServletRequest request,
			final HttpServletResponse response)
			throws IOException, ServletException
		{
		final String urlIn = request.getParameter(URL_PARAM);
		if(urlIn==null || urlIn.trim().isEmpty())
			{
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,URL_PARAM+" param is missing.");
			return;
			}
		final String xslParam = request.getParameter(XSL_PARAM);
		if(xslParam==null || xslParam.trim().isEmpty())
			{
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,XSL_PARAM+" param is missing.");
			return;
			}
		CloseableHttpClient httpclient = null;
		CloseableHttpResponse httpResponse = null;
		InputStream in = null;
		try {
			httpclient = HttpClients.createDefault();
			
			final HttpGet httpget = new HttpGet(urlIn);
			
			 httpResponse = httpclient.execute(httpget);
			
			HttpEntity httpEntity = httpResponse.getEntity();
			in=httpEntity.getContent();
			final Tidy tidy = new Tidy(); 
			tidy.setOutputEncoding("UTF-8");
			tidy.setShowErrors(0);
			tidy.setQuiet(true);
			tidy.setErrout(null);
			tidy.setXmlTags(true);
			tidy.setXHTML(true);
			final Document dom =tidy.parseDOM(in, null);
			EntityUtils.consume(httpEntity);
			httpResponse.close();
			httpclient.close();
			
		     final TransformerFactory factory = TransformerFactory.newInstance();
		     Source xslt=null ;
		     if(xslParam.startsWith("http://") || xslParam.startsWith("https://") || xslParam.startsWith("ftp://") || xslParam.startsWith("file:"))
		     	{
					xslt = new StreamSource(xslParam);
				} 
		     else
		     	{
					xslt = new StreamSource(new File(xslParam));
				}

			final Transformer  transformer = factory.newTransformer(xslt);
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
			if(in!=null) in.close();
			if(httpResponse!=null) httpResponse.close();
			if(httpclient!=null) httpclient.close();
			
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
			try {
				ApplicationContext beanFactory = new FileSystemXmlApplicationContext(
						new File(cmd.getOptionValue("f")).toURI().toASCIIString());
				beanFactory.getBean("");

			} catch(Exception err) {
				System.err.println(err.getMessage());
				System.exit(-1);
			}
			}
	    final Server server = new Server(port);
        ContextHandler context = new ContextHandler();
        context.setContextPath("/tidyxsl");
        context.setHandler(xslHandler);
        server.setHandler(context);
	    server.start();
	    server.join();
	}
}
