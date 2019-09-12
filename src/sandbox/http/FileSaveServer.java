package sandbox.http;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.StringUtil;

import com.beust.jcommander.Parameter;

import sandbox.Logger;

/**
 
 note to self:

TODO: handle CORS
```
var textContent = document.documentElement.outerHTML; // x.innerHTML instead if you only want the contents

var http = new XMLHttpRequest();
var url = 'http://localhost:8080/filesaveserver/';
var params = 'file='+ encodeURIComponent('*')+ '&content=' + encodeURIComponent(textContent);
console.log(params);
http.open('POST', url, true);

//Send the proper header information along with the request

http.onreadystatechange = function() {//Call a function when the state changes.
    if(http.readyState == 4 && http.status == 200) {
        alert(http.responseText);
    }
}
http.send(params);
```

 */

public class FileSaveServer extends AbstractJettyAppication {
	private static final Logger LOG = Logger.builder(FileSaveServer.class).build();
	@Parameter(names={"--directory"},description="output save directory.",required=true)
	private Path saveToDirectory=null;
	@Parameter(names={"--size"},description="max content size")
	private long max_content_size=1_000_000L;
	@Parameter(names={"--max-files"},description="max files in save directory")
	private long max_files = 1_000;

	
	private class FileSaveHandler  extends AbstractHandler {
		@Override
		public void handle(String target, Request baseRequest,
				final HttpServletRequest httpReq,
				final HttpServletResponse httpResp)
				throws IOException, ServletException {
			try {
				final String content = httpReq.getParameter("content");
				httpResp.setContentType("text/plain");
				
				if(StringUtil.isBlank(content) || content.length() > max_content_size) {
					httpResp.sendError(HttpStatus.SC_BAD_REQUEST,"empty content");
					return;
					}
				String file = httpReq.getParameter("file");
				if("*".equals(file)) {
					int i=0;
					for(;;) {
						i++;
						file= String.format("file.%04d.txt", i);
						final Path p = saveToDirectory.resolve(file);
						if(!Files.exists(p))break;
						}
					}
				
				if(StringUtil.isBlank(file) ||file.length()>255 || !file.matches("[A-Za-z_][A-Za-z_0-9]*(\\.[a-z]+)?")) {
					LOG.warning("bad file name");
					httpResp.sendError(HttpStatus.SC_BAD_REQUEST,"Bad file name");
					return;
					}
				if(!Files.exists(saveToDirectory) || !Files.isDirectory(saveToDirectory))
					{
					httpResp.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR,"cannot find save path " +saveToDirectory);
					return;
					}
				
				if(Files.list(saveToDirectory).count()>max_files) {
					httpResp.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR,"too many files under " +saveToDirectory);
					return;
					}
				
				final Path fileOut = saveToDirectory.resolve(file);
				if(Files.exists(fileOut))
					{
					httpResp.sendError(HttpStatus.SC_BAD_REQUEST,"file exists "+fileOut);
					return;
					}
				try(BufferedWriter w=Files.newBufferedWriter(fileOut)) {
					w.write(content);
					w.flush();
					}
				catch(IOException err) {
					Files.delete(fileOut);
					httpResp.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR,"IO Error "+fileOut+" "+err.getMessage());
					return;
					}
				httpResp.setStatus(HttpStatus.SC_OK);
				try(PrintWriter w=httpResp.getWriter()) {
					w.println(fileOut);
					w.flush();
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
		return new FileSaveHandler();
		}
	
	
	public static void main(String[] args) {
		new FileSaveServer().instanceMainWithExit(args);
	}

}
