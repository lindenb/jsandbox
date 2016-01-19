package sandbox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

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
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

import sun.util.logging.resources.logging;


public class TidyXslHandler extends AbstractHandler
	{
	private String URL_PARAM="url";
	private String XSL_PARAM="xsl";
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
			tidy.setXmlOut(true);
			tidy.setShowErrors(0);
			tidy.setQuiet(true);
			tidy.setErrout(null);
			tidy.setXmlTags(true);
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
			throw err;
			}
		catch(Exception err)
			{
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
		System.setProperty("http.proxyHost", "cache.ha.univ-nantes.fr");
		System.setProperty("https.proxyHost", "cache.ha.univ-nantes.fr");
		System.setProperty("http.proxyPort", "3128");
		System.setProperty("https.proxyPort", "3128");

	    final Server server = new Server(8080);
	    server.setHandler(new TidyXslHandler());
        ContextHandler context = new ContextHandler();
        context.setContextPath("/tidyxsl");
        context.setHandler(new TidyXslHandler());
        server.setHandler(context);
	    server.start();
	    server.join();
	}
}
