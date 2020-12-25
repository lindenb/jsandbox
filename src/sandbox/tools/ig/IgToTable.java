package sandbox.tools.ig;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.beust.jcommander.Parameter;
import com.google.gson.JsonElement;

import sandbox.IOUtils;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.http.CookieStoreUtils;
import sandbox.ig.InstagramJsonScraper;
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
	@Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private Path cookieStoreFile  = null;

	private final InstagramJsonScraper scraper = new InstagramJsonScraper();
	
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
	private String fetchHtml(final CloseableHttpClient client,final String url) {
	LOG.info("fetch "+url);
	CloseableHttpResponse resp=null;
	InputStream in=null;
	try {
		resp = client.execute(new HttpGet(url));
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
   
   
	private void fetch(final PrintWriter out,final CloseableHttpClient client,final String url) throws Exception {
		if(url.contains("/explore/tags/")) {
			fetchExploreTags(out,client,url);
			}
		if(url.contains("/p/") ) {
			fetchPost(out,client,url);
			}
		else
			{
			fetchProfile(out,client,url);
			}
		}
	
	private String findBetween(final String htmlDoc,String start,String end) {
		int i= htmlDoc.indexOf(start);
		if(i==-1) {return null;}
		i+=start.length();
		int j= htmlDoc.indexOf(end,i);
		if(j==-1) return null;
		return htmlDoc.substring(i,j);
		}
	private String toString(JsonElement e) {
		if(e==null || e.isJsonNull()) return null;
		if(!e.isJsonPrimitive()) return null;
		return e.getAsJsonPrimitive().getAsString();
		}
	private String getCount(JsonElement root,final String key) {
		JsonElement e=InstagramJsonScraper.find(root,key);
		if(e==null) return null;
		e= InstagramJsonScraper.find(e,"count");
		return toString(e);
		}
	
	private void fetchExploreTags(final PrintWriter out,final CloseableHttpClient client,final String url) throws Exception {
		final String htmlDoc =  fetchHtml(client,url);
		if(htmlDoc!=null) {
			JsonElement root = scraper.apply(htmlDoc).orElse(null);
			if(root!=null) {
				}
			}
		}
	
	private void fetchProfile(final PrintWriter out,final CloseableHttpClient client,final String url) throws Exception {
		final String htmlDoc =  fetchHtml(client,url);
		String profile_url = null;
		String count_followers = null;
		String count_following = null;
		String full_name = null;
		String username = null;
		String count_posts = null;
		String isprivate=null;
		boolean deleted = true;
		if(htmlDoc!=null) {
			JsonElement root = scraper.apply(htmlDoc).orElse(null);
			if(root!=null) {
				final JsonElement requested_by_viewer = InstagramJsonScraper.find(root,"ProfilePage");
				if(requested_by_viewer==null) System.err.println("xx="+htmlDoc.indexOf("requested_by_viewer"));
				if(requested_by_viewer!=null) root = requested_by_viewer;
				profile_url = toString(InstagramJsonScraper.find(root, "profile_pic_url"));
				if(profile_url == null) toString(InstagramJsonScraper.find(root, "display_url")); 
				//if(profile_url!=null) profile_url= StringUtils.unescapeUnicode(profile_url);
				count_followers = getCount(root, "edge_followed_by");
				count_following = getCount(root, "edge_follow"); 
				full_name = toString(InstagramJsonScraper.find(root, "full_name"));
				username =  toString(InstagramJsonScraper.find(root, "username"));
				count_posts =   getCount(root, "edge_owner_to_timeline_media");
				isprivate = toString(InstagramJsonScraper.find(root,"is_private"));
				}
			else
				{
				LOG.warning("cannot find json in "+url);
				}
			if(profile_url==null) profile_url= findBetween(htmlDoc, "property=\"og:image\" content=\"","\"");

			deleted=false;
			if(profile_url==null) profile_url= findBetween(htmlDoc, "property=\"og:image\" content=\"","\"");
			
			}
		if(this.vertical) {
			out.println("ig:"+url);
			if(deleted) out.println("deleted:"+true);
			if(username!=null) out.println("user:"+username);
			if(full_name!=null) out.println("name:"+full_name);
			if(profile_url!=null) out.println("img:"+profile_url);
			if(count_followers!=null) out.println("followers:"+count_followers);
			if(count_following!=null) out.println("following:"+count_following);
			if(count_posts!=null) out.println("posts:"+count_posts);
			if(isprivate!=null && isprivate.equals("true")) out.println("private:true");
			out.println();
			} else {
			out.print(url);
			out.print("\t");
			out.print(username==null?".":username);
			out.print("\t");
			out.print(full_name==null?".":full_name);
			out.print("\t");
			out.print(profile_url==null?".":profile_url);
			out.print("\t");
			out.print(count_followers==null?".":count_followers);
			out.print("\t");
			out.print(count_following==null?".":count_following);
			out.print("\t");
			out.print(count_posts==null?".":count_posts);
			out.print("\t");
			out.print(isprivate!=null && isprivate.equals("true")?"PRIVATE":".");
			out.println();
			}
		}
	
	
	private void fetchPost(final PrintWriter out,final CloseableHttpClient client,final String url) throws Exception {
		LOG.info("fetch "+url);
		try {
			final String htmlDoc =  fetchHtml(client,url);
			if(htmlDoc==null) return;
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
		catch(final Throwable err) {
			LOG.error(err);
			}
		}
	
	@Override
	public int doWork(List<String> args)
		{
		try {
			final HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setUserAgent(IOUtils.getUserAgent());
			if(this.cookieStoreFile!=null) {
				final BasicCookieStore cookies = CookieStoreUtils.readTsv(this.cookieStoreFile);
				builder.setDefaultCookieStore(cookies);
				}
			
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
