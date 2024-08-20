package sandbox.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.beust.jcommander.Parameter;

import sandbox.Logger;
import sandbox.StringUtils;

/**
 

```

 */

public class UrlSurveyServer extends AbstractJettyAppication {
	private static final Logger LOG = Logger.builder(UrlSurveyServer.class).build();
	@Parameter(names={"--database"},description="Database file.",required=true)
	private Path datbaseFile=null;

	private final List<WebSite> webSites = Collections.synchronizedList(new ArrayList<>());
	
	private static class WebSite {
		String url;
		Date date;
		String md5() {
			return StringUtils.md5(url);
			}
		}
	
	
	private class SurveyHandler  extends AbstractHandler {
		@Override
		public void handle(String target, Request baseRequest,
				final HttpServletRequest httpReq,
				final HttpServletResponse httpResp)
				throws IOException, ServletException {
			try {
				
				final String md5= httpReq.getParameter("md5");
				if(!StringUtils.isBlank(md5)) {
					httpResp.setContentType("text/plain");
					Optional<WebSite> site=  webSites.stream().filter(W->W.md5().equals(md5)).findFirst();
					if(site.isPresent())
						{
						LOG.info("got md5 for "+site.get().url);
						site.get().date=new Date();
						Files.write(datbaseFile,
									webSites.stream().
									map(W->W.url+"\t"+W.date.getTime()).
									collect(Collectors.toList()),
								StandardOpenOption.CREATE,
								StandardOpenOption.TRUNCATE_EXISTING
								);
						
						httpResp.setStatus(HttpStatus.SC_OK);
						try(PrintWriter pw=httpResp.getWriter()) {
							pw.println("OK");
							}
						}
					else
						{
						LOG.info("cannot get md5 for "+md5);
						httpResp.sendError(HttpStatus.SC_BAD_REQUEST,"cannot find md5="+md5);
						}
					}
				else
					{
					httpResp.setContentType("text/html");
					httpResp.setStatus(HttpStatus.SC_OK);
					try(PrintWriter pw=httpResp.getWriter()) {
						XMLOutputFactory xof=XMLOutputFactory.newFactory();
						XMLStreamWriter w= xof.createXMLStreamWriter(pw);
						w.writeStartElement("html");
						w.writeStartElement("head");
						w.writeStartElement("script");
						w.writeCharacters("function run(id,url,h) {"
								+ "var node=document.getElementById(id);node.parentNode.removeChild(node);"
								+ "var r = new XMLHttpRequest();r.open('GET', document.location+'/?md5='+h, true); r.send();"
								+ "window.open(url);"
								+ "}");
						w.writeEndElement();
						w.writeStartElement("title");
						w.writeCharacters(UrlSurveyServer.class.getName());
						w.writeEndElement();
						w.writeEndElement();

						w.writeStartElement("body");
						int id=0;
						for(final WebSite ws: webSites) {
							w.writeStartElement("a");
							w.writeAttribute("id","a"+(++id));
							w.writeAttribute("class","r"+(id%2));
							w.writeAttribute("alt", ws.url);
							w.writeAttribute("title", ws.url);
							w.writeAttribute("href","javascript:run('a"+id+"','"+ ws.url+"','"+ ws.md5() +"');");
							w.writeCharacters(ws.url);
							w.writeEndElement();//a
							w.writeCharacters(" ");
							w.writeCharacters(ws.date.toString());
							w.writeEmptyElement("br");
						}
						
						w.writeEndElement();
						w.writeEndElement();
						w.flush();
						pw.flush();
						}
					}
			} catch (final IOException e) {
				LOG.error(e);
				throw e;
			}  catch (final Throwable e) {
				LOG.error(e);
				throw new ServletException(e);
			} finally {
			}
		}
	}
	
	@Override
	protected org.eclipse.jetty.server.Handler createHandler() {
		return new SurveyHandler();
		}
	
	@Override
	public int doWork(List<String> args) {
		try(BufferedReader br=Files.newBufferedReader(this.datbaseFile)) {
			this.webSites.addAll( br.lines().
				filter(L->!(L.startsWith("#") || StringUtils.isBlank(L))).
				map(L->{
					String tokens[]=L.split("[\t]");
					WebSite w=new WebSite();
					w.url=tokens[0];
					w.date = new Date(tokens.length>1?Long.parseLong(tokens[1]):0L);
					return w;
				}).
				sorted((A,B)->B.date.compareTo(A.date)).
				collect(Collectors.toList()));
			}
		catch(Exception err) {
			LOG.error(err);
			return -1;
			}
		return super.doWork(args);
		}
	
	public static void main(final String[] args) {
		new UrlSurveyServer().instanceMainWithExit(args);
	}

}
