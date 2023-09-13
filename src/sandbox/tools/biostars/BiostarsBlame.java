package sandbox.tools.biostars;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.html.TidyToDom;
import sandbox.io.IOUtils;
import sandbox.xml.XmlUtils;

public class BiostarsBlame extends Launcher {
	private static final Logger LOG= Logger.builder(BiostarsBlame.class).build();
	private int userId;
	private HttpClientBuilder builder = null;
	private final Set<Long> seen_posts = new HashSet<>();
	private final List<Post> posts = new ArrayList<>();
	private class Post {
		long id;
		String title;
		Set<String> atts = Collections.emptySet();
		@Override
		public String toString() {
			return ""+id+" "+atts+" "+title;
			}
		}
	
	
	private DocumentFragment fetch(String url) throws IOException {
	LOG.info(url);
	try(CloseableHttpClient client =  builder.build()) {
		CloseableHttpResponse resp=null;
		InputStream in=null;
		try {
			final HttpGet method = new HttpGet(url);
			resp = client.execute(method);
			int httpStatus  = resp.getStatusLine().getStatusCode();
			if(httpStatus!=200) {
				LOG.error("Error ("+httpStatus+") cannot fetch "+method.getMethod()+":"+method.getURI()+" "+resp.getStatusLine());
				return null;
				}
			in = resp.getEntity().getContent();
			final TidyToDom tidyToDom = new TidyToDom();
			final String html = IOUtils.readStreamContent(in);
			return tidyToDom.importString(html);
			}
		finally {
			IOUtils.close(in);
			IOUtils.close(resp);
			}
		}
	}

	private final Set<String> clazz(final String s) {
		if(StringUtils.isBlank(s)) return Collections.emptySet();
		return Arrays.stream(s.split("[ \t]+")).filter(S->!StringUtils.isBlank(s)).collect(Collectors.toSet());
	}
	
	private final Set<String> clazz(final Node n) {
		if(n==null || n.getNodeType()!=Node.ELEMENT_NODE) return Collections.emptySet();
		Element E = Element.class.cast(n);
		if(!E.hasAttribute("class")) return Collections.emptySet();
		return clazz(E.getAttribute("class"));
		}
	
	private void searchPost(Node root) {
		if(root==null) return ;
		final Element div = root.hasAttributes() && XmlUtils.isA(root, "div")?Element.class.cast(root):null;
		if(div!=null && 
				div.hasAttribute("class") &&
				div.hasAttribute("data-value") &&
				clazz(div).contains("post")
				)
			{
			//LOG.info(XmlUtils.toString(div));
			final Post post =new Post();
			post.id = Long.parseLong(div.getAttribute("data-value"));
			if(this.seen_posts.contains(post.id)) return ;
			post.atts =  clazz(div);
			for(Node n1=div.getFirstChild();n1!=null;n1=n1.getNextSibling()) {
				if(clazz(n1).isEmpty()) continue;
				post.atts.addAll(clazz(n1));
				for(Node n2=n1.getFirstChild();n2!=null;n2=n2.getNextSibling()) {
					if(! XmlUtils.isA(n2, "div")) continue;
					post.atts.addAll(clazz(n2));
					if(clazz(n2).contains("title")) {
						post.title = n2.getTextContent();
						}
					}
				}
			if(post.title==null) return;
			if(post.title.startsWith("Comment: ")) return;
			if(post.title.startsWith("Answer: ")) return;
			if(post.atts.contains("accepted")) return;
			if(!post.atts.contains("has_answers")) return;
			posts.add(post);
			this.seen_posts.add(post.id);
			}
		else {
			for(Node n1=root.getFirstChild();n1!=null;n1=n1.getNextSibling()) {
				searchPost(n1);
				}
			}
		}
	
	
	private boolean fetchPost(int page) throws IOException {
		final String url = "https://www.biostars.org/u/"+this.userId+"/?page="+page+"&active=posts";
		DocumentFragment dom = fetch(url);
		if(dom==null) return false;
		int n=this.seen_posts.size();
		searchPost(dom);
		return this.seen_posts.size()!=n;
		}
	
	@Override
	public int doWork(List<String> args) {
		
		try {
			this.userId = Integer.parseInt(oneAndOnlyOneFile(args));
			this.builder = HttpClientBuilder.create();

			int page=1;
			for(;;) {
				if(!fetchPost(page)) break;
				page++;
				}
			for(Post post:this.posts) {
				System.out.print("https://www.biostars.org/p/"+post.id);
				System.out.print(" ");
				}
			System.out.println();
			return 0;
			}
		catch(Throwable err) {
			err.printStackTrace();
			return -1;
			}
		}
	
	
	public static void main(String[] args) {
		new BiostarsBlame().instanceMainWithExit(args);
	}

}
