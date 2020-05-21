package sandbox.tools.ig;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.beust.jcommander.Parameter;

import sandbox.IOUtils;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.iterator.BufferedReaderIterator;

public class IgToTable extends Launcher
	{
	private static final Logger LOG = Logger.builder(IgToTable.class).build();

	@Parameter(names= {"-o","--out"},description=OUTPUT_OR_STANDOUT)
	private Path output = null;
	@Parameter(names= {"-s","--sleep"},description="sleep (seconds) between each call")
	private int sleep = 0;
	@Parameter(names= {"--vertical"},description="vertical output")
	private boolean vertical = false;

	
   private String month2num(final String s) {
   if(s.equalsIgnoreCase("January")) return "01";
   if(s.equalsIgnoreCase("February")) return "02";
   if(s.equalsIgnoreCase("March")) return "03";
   if(s.equalsIgnoreCase("April")) return "04";
   if(s.equalsIgnoreCase("May")) return "05";
   if(s.equalsIgnoreCase("June")) return "06";
   if(s.equalsIgnoreCase("July")) return "07";
   if(s.equalsIgnoreCase("August")) return "08";
   if(s.equalsIgnoreCase("September")) return "09";
   if(s.equalsIgnoreCase("October")) return "10";
   if(s.equalsIgnoreCase("November")) return "11";
   if(s.equalsIgnoreCase("December")) return "12";
   return s;
   }
	
	private void fetch(final PrintWriter out,final CloseableHttpClient client,final String url) throws Exception {
		LOG.info("fetch "+url);
		CloseableHttpResponse resp=null;
		InputStream in=null;
		try {
			resp = client.execute(new HttpGet(url));
			if(resp.getStatusLine().getStatusCode()!=200) {
				LOG.error("cannot fetch "+url+" "+resp.getStatusLine());
				return;
				}
			in = resp.getEntity().getContent();
			final String htmlDoc =  IOUtils.readStreamContent(in);
			String needle = "property=\"og:description\"";
			int i= htmlDoc.indexOf(needle);
			if(i==-1) {
				LOG.error("Cannot find "+ needle+" in "+url );
				return;
				}
			i+=needle.length();
			needle = "content=\"";
			i= htmlDoc.indexOf(needle,i);
			if(i==-1) {
				LOG.error("Cannot find "+ needle+" in "+url );
				return;
				}
			i+=needle.length();
			needle = "\"";
			int j= htmlDoc.indexOf(needle,i);
			if(j==-1) {
				LOG.error("Cannot find "+ needle+" in "+url );
				return;
				}
			final String content = htmlDoc.substring(i,j);
			String tokens[] = content.split("[, \\-]+");
			String likes = null;
			String comments = null;
			String inspir = null;
			String account1 = null;
			String account2 = null;
			String date=null;
			String og_image = null;
			
			for(i=0;i< tokens.length;i++) {
				if(likes==null && i+1<tokens.length && (tokens[i+1].equalsIgnoreCase("Likes") || tokens[i+1].equalsIgnoreCase("Like"))) {
					likes = tokens[i];
					}
				else if(comments==null && i+1<tokens.length &&  (tokens[i+1].equalsIgnoreCase("Comments") || tokens[i+1].equalsIgnoreCase("Comment"))) {
					comments = tokens[i];
					}
				else if(inspir==null && IOUtils.isURL(tokens[i])) {
					inspir = tokens[i];
					}
				else if(account1==null && tokens[i].startsWith("@")) {
					account1 = tokens[i];
					}
				else if(account2==null && tokens[i].startsWith("@")) {
					account2 = tokens[i];
					}
				}
			
			needle= "\"accessibility_caption\":\"";
			i= htmlDoc.indexOf(needle);
			final List<String> tagged_in_caption= new ArrayList<>();
			if(i!=-1) {
				i+=needle.length();
				j=  htmlDoc.indexOf("\"",i);
				if(j!=-1) {
					tokens = htmlDoc.substring(i,j).split("[, \t\n\\.)\u2026\u201D]+");
					boolean in_tagging =false;
					for(i=0;i+4<tokens.length;i++) {
						if(tokens[i].equals("tagging")) in_tagging=true;
						if(!in_tagging) continue;
						if(tokens[i].startsWith("@")) tagged_in_caption.add(tokens[i]);
						}
					for(i=0;i+4<tokens.length;i++) {
						if(tokens[i].equalsIgnoreCase("on") &&
							tokens[i+2].matches("\\d{2}") && tokens[i+3].matches("\\d{4}")) {
							date= tokens[i+3]+"-"+month2num(tokens[i+1])+"-"+tokens[i+2];
							}
						}
					}
				}
			
			if(account1==null && tagged_in_caption.size()>0)  {
				account1 = tagged_in_caption.get(0);
				}
			if(account2==null && tagged_in_caption.size()>1)  {
				account2 = tagged_in_caption.get(1);
				}
			if(tagged_in_caption.size()>2) {
				LOG.info("> 2 persons in "+url);
			}
			needle= "\"display_url\":\""; 
			i= htmlDoc.indexOf(needle);
			if(i!=-1) {
				i+=needle.length();
				needle = "\"";
				j= htmlDoc.indexOf(needle,i);
				if(j>i) {
					og_image=StringUtils.unescapeUnicode(htmlDoc.substring(i,j));
					}
				}
				
			
			if(this.vertical) {
				if(inspir!=null) out.println("URL: "+inspir);
				if(date!=null) out.println("date: "+date);
				if(account1!=null) out.println("depicted: "+account1);
				if(account2!=null) out.println("photo: "+account2);
				if(likes!=null && !likes.equals("0")) out.println("likes: "+likes);
				if(comments!=null && !comments.equals("0")) out.println("comments: "+comments);
				if(og_image!=null) out.println("og-image: "+og_image);
				out.println("ig: "+url);
				out.println();
				} 
			else  {
				out.print(url);
				out.print("\t");
				out.print(likes==null?".":likes);
				out.print("\t");
				out.print(comments==null?".":comments);
				out.print("\t");
				out.print(date==null?".":date);
				out.print("\t");
				out.print(inspir==null?".":inspir);
				out.print("\t");
				out.print(account1==null?".":account1);
				out.print("\t");
				out.print(account2==null?".":account2);
				out.print("\t");
				out.print(og_image==null?".":og_image);
				out.println();
				}
			}
		catch(final IOException err) {
			LOG.error(err);
			}
		finally
			{
			IOUtils.close(in);
			IOUtils.close(resp);
			}
		}
	
	@Override
	public int doWork(List<String> args)
		{
		try {
			final HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setUserAgent(IOUtils.getUserAgent());
			
			
			try(CloseableHttpClient client = builder.build();
				PrintWriter out = IOUtils.openPathAsPrintWriter(this.output);
				BufferedReaderIterator iter= new BufferedReaderIterator(super.openBufferedReader(args))) {
				while(iter.hasNext()) {
					String line = iter.next();
					if(line.startsWith("#") || StringUtils.isBlank(line)) continue;
					line= line.trim();
					if(!IOUtils.isURL(line)) {
						LOG.warning("not a url?"+line);
						continue;
						}
					fetch(out,client,line);
					Thread.sleep(this.sleep*1000);
					}
				out.flush();
				}

			return 0;
			}
		catch(Exception err) {
			LOG.error(err);
			return -1;
			}
		}
	
	public static void main(String[] args)
		{
		new IgToTable().instanceMainWithExit(args);
		}

	}
