package sandbox.tools.mastodongraph;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.beust.jcommander.Parameter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.gexf.Gexf;
import sandbox.io.IOUtils;

public class MastodonGraph extends Launcher {
	private static final Logger LOG= Logger.builder(MastodonGraph.class).build();
	private HttpClientBuilder builder = null;
	@Parameter(names={"--output","-o"},description="output")
	private File output = null;

	@Parameter(names={"--instance"},description="instance")
	private String instance="https://genomic.social/";
	@Parameter(names={"--config","--xml"},description="XML config",required = true)
	private File xmlConfig = null;
	@Parameter(names={"--max-depth","-D"},description="max depth")
	private int max_depth = 2;

	
	private final Map<String,Instance> url2instance = new HashMap<>();
	
	private class User {
		String id;
		String username;
		String display_name;
		String acct;
		int following_count = -1;
		int followers_count = -1;
		int statuses_count = -1;
		boolean processed_flag=false;
		int depth = 99999999;
		
		@Override
		public int hashCode() {
			return id.hashCode();
			}
		@Override
		public boolean equals(Object obj) {
			if(obj==this) return true;
			return User.class.cast(obj).id.equals(this.id);
			}
		@Override
		public String toString() {
			return "id:"+id+" username:"+username+" acct:"+acct;
			}
		}
	
	private class Link {
		final User u1;
		final User u2;
		boolean reverse=false;
		Link(User u1,User u2) {
			this.u1 = u1;
			this.u2 = u2;
			}
		boolean match(final User u1,final User u2) {
			return (this.u1.equals(u1) && this.u2.equals(u2)) ||
				   (this.u1.equals(u2) && this.u2.equals(u1));
			}
		}
	
	private class Instance {
		private String url;
		String client_id;
		String client_secret;
		String oauth_token=null;
		String oauth_token_type=null;
		String oauth_rediect = "urn:ietf:wg:oauth:2.0:oob";
		private boolean _initialized = false;
		int x_rate_limit_remaining=1_000;

		private Map<String,User> id2user = new HashMap<>();
		private List<Link> links = new ArrayList<>(100_000);
		
		void initOAuthToken() throws IOException {
			if(this._initialized) return;
			this._initialized=true;
			final HttpPost post = new HttpPost(this.url+"oauth/token");
			post.setEntity(createFormEntity(
					"client_id",this.client_id,
					"client_secret",this.client_secret,
					"redirect_uri",this.oauth_rediect,
					"grant_type","client_credentials"));
			final Response resp = wget(post);
			LOG.info(resp.content);
			this.oauth_token = resp.getJsonObject().get("access_token").getAsString();
			if(StringUtils.isBlank(this.oauth_token)) {
				throw new IllegalStateException("cannot get access_token from "+resp);
				}
			this.oauth_token_type  = resp.getJsonObject().get("token_type").getAsString();
			if(StringUtils.isBlank(this.oauth_token_type)) {
				throw new IllegalStateException("cannot get oauth_token_type from "+resp);
				}
			}
		
		private Response wget(final HttpRequestBase method)  throws IOException {
			if(this.x_rate_limit_remaining<=1) {
				try {
					LOG.info("SLEEP !");
					Thread.sleep(60*10*1000);
					}
				catch(Exception err) {
					LOG.error(err);
					}
				}
			initOAuthToken();
					for(;;) /* loop if too many requests */ {
					LOG.info(method);
					
					final Response response = new Response();
					response.request=method;
					if(this.oauth_token!=null) {
						LOG.info("setting Authorization as "+this.oauth_token_type+" "+this.oauth_token);
						method.addHeader("Authorization",this.oauth_token_type+" "+this.oauth_token);
						}
					try(CloseableHttpClient client =  builder.build()) {
						CloseableHttpResponse resp=null;
						InputStream in=null;
						try {
							resp = client.execute(method);
							for(Header header: resp.getAllHeaders()) {
								//LOG.info(header.getName()+":"+header.getValue());
								if(header.getName().equals("X-RateLimit-Remaining")) {
									this.x_rate_limit_remaining = Integer.parseInt(header.getValue());
									}
								else if(header.getName().equals("Date")) {
								    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");
								    try {
								    	dateFormat.parse(header.getValue());
								    	}
								    catch(ParseException err) {
								    	//LOG.error(err);
								    	}
									}
								// 2022-11-10T12:25:00.219345Z
								else if(header.getName().equals("X-RateLimit-Reset")) {
								    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");
								    try {
								    	dateFormat.parse(header.getValue());
								    	}
								    catch(ParseException err) {
								    	//LOG.error(err);
								    	}
									}
								// e.g: Link:<https://genomic.social/api/v1/accounts/xx/following?limit=2&max_id=50848>; rel="next", <https://genomic.social/api/v1/accounts/xxx/following?limit=2&since_id=51004>; rel="prev"
								else if(header.getName().equals("Link")) {
									for(String kv1: header.getValue().split(",")) {
										//LOG.info(kv1);
										String[] kv2 = kv1.split(";");
										if(kv2.length!=2) continue;
										//LOG.info(kv2[0]);
										//LOG.info(kv2[1]);
										if(!kv2[1].trim().equals("rel=\"next\"")) continue;
										response.next_url = kv2[0].substring(1,kv2[0].length()-1).trim();
										//LOG.info("next url:"+response.next_url);
										}
									
									}
								}
							response.httpStatus  = resp.getStatusLine().getStatusCode();
							if(response.httpStatus==429) { // TOO MANY REQUESTS
								LOG.info("wait ...");
								try {
									Thread.sleep(60*10*1000);//10 minutes
									}
								catch(Throwable err) {
									LOG.info(err);
									}
								continue;
								}
							if(response.httpStatus!=200) {
								LOG.error("Error ("+response.httpStatus+") cannot fetch "+method.getMethod()+":"+method.getURI()+" "+resp.getStatusLine());
								return response;
								}
							in = resp.getEntity().getContent();
							response.content = IOUtils.readStreamContent(in);
							}
						finally {
							IOUtils.close(in);
							IOUtils.close(resp);
							}
						return response;
						}
					}
				}
		
		private String searchUserIdByName(String id) throws IOException {
			String url = this.url+"api/v2/search"; //+"/followers"
			try {
				final HttpGet get = new HttpGet(
					new URIBuilder(url).
					setParameter("q", id).
					setParameter("type","accounts").
					setParameter("resolve","true").
					setParameter("limit","5").
					build());
				Response resp = wget(get);
				LOG.info(resp.content);
				}
			catch(final Throwable err) {
				LOG.error(err);
				return "";
				}
			return "";
			}
		
		private User getUserFromJson(final JsonObject obj) {
			final String id = obj.get("id").getAsString();
			User user = this.id2user.get(id);
			if(user!=null) return user;
			user = new User();
			user.id = id;
			user.username = obj.get("username").getAsString();
			user.display_name = obj.get("display_name").getAsString();
			user.acct = obj.get("acct").getAsString();
			user.statuses_count = obj.get("statuses_count").getAsInt();
			
			if(obj.has("following_count")) {
				user.following_count = obj.get("following_count").getAsInt();
			}
			if(obj.has("followers_count")) {
				user.followers_count = obj.get("followers_count").getAsInt();
			}
			this.id2user.put(id, user);
			LOG.info("users : "+id2user.size());
			return user;
			}
		
		private void links(final User user,String method) throws IOException {
			String url = this.url+"api/v1/accounts/"+user.id+"/"+method;
			while(!StringUtils.isBlank(url)) {
				final HttpGet get = new HttpGet(url);
				final Response resp = wget(get);
				
				//LOG.info(resp.content);
				for(JsonElement f: resp.getJsonArray()) {
					final User user2 = getUserFromJson(f.getAsJsonObject());
					user2.depth = Math.min(user2.depth, user.depth+1);
					
					Link link = links.stream().filter(L->L.match(user, user2)).findAny().orElse(null);
					if(link==null) {
						link = new Link(user, user2);
						links.add(link);
						}
					else if(link.u1.equals(user2) && link.u2.equals(user))
						{
						link.reverse=true;
						}
					}
				url = resp.next_url;
				}
			}
		
		private void scan(String userid) throws IOException {
			String url = this.url+"api/v1/accounts/"+userid; //+"/followers"
			final HttpGet get = new HttpGet(url);
			Response resp = wget(get);
			LOG.info(resp.content);
			User user = getUserFromJson(resp.getJsonObject());
			user.depth=0;
			for(;;) {
				user = this.id2user.values().stream().
						filter(U->U.depth< MastodonGraph.this.max_depth).
						filter(U->!U.processed_flag).
						findAny().
						orElse(null);
				if(user==null) break;
				user.processed_flag=true;
				links(user,"following");
				links(user,"followers");
				LOG.info("links: "+links.size()+ " remains:"+ this.id2user.values().stream().
						filter(U->U.depth < MastodonGraph.this.max_depth).
						filter(U->!U.processed_flag).count()
						);
				}
			}
		
		private void gexfAtt(final XMLStreamWriter w,final String key,final String value)
				throws XMLStreamException
				{
				if(value==null) return;
				w.writeStartElement("attvalue");
				w.writeAttribute("for", key);
				w.writeAttribute("value", value);
				w.writeEndElement();
				}

		
		private void gexfAtt(XMLStreamWriter w,
				String key,
				String type,
				String def
				)throws XMLStreamException
				{
				if(def==null)
					{
					w.writeEmptyElement("attribute");
					}
				else
					{
					w.writeStartElement("attribute");
					}
				
				w.writeAttribute("id", key);
				w.writeAttribute("title", key.replace('_', ' '));
				w.writeAttribute("type", type);
				
				if(def!=null)
					{
					w.writeStartElement("default");
					w.writeCharacters(def);
					w.writeEndElement();
					
					w.writeEndElement();
					}
				}

		void toGexf(OutputStream out) throws XMLStreamException
				{
				XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
				XMLStreamWriter w= xmlfactory.createXMLStreamWriter(out,"UTF-8");
				
				w.writeStartDocument("UTF-8","1.0");
				w.writeStartElement("gexf");
				w.writeAttribute("version",Gexf.VERSION);
				w.writeAttribute("xmlns",Gexf.XMLNS);
				w.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
				w.writeAttribute("xsi:schemaLocation",Gexf.SCHEMA_LOCATION);

				
				
				/* meta */
				w.writeStartElement("meta");
					w.writeStartElement("creator");
					  w.writeCharacters(MastodonGraph.class.getCanonicalName());
					w.writeEndElement();
					w.writeStartElement("description");
					  w.writeCharacters("Mastodon Graph");
					w.writeEndElement();
				w.writeEndElement();
				
				/* graph */
				w.writeStartElement("graph");
				w.writeAttribute("mode", "static");
				//w.writeAttribute("defaultedgetype", "directed");
				
				
				
				/* attributes */
				w.writeStartElement("attributes");
				w.writeAttribute("class","node");
				w.writeAttribute("mode","static");
				gexfAtt(w,"name","string",null);
				gexfAtt(w,"display_name","string",null);
				gexfAtt(w,"acct","string",null);
				gexfAtt(w,"following","integer",null);
				gexfAtt(w,"followers","integer",null);
				gexfAtt(w,"statuses_count","integer",null);
				w.writeEndElement();//attributes
				
				/* nodes */
				w.writeStartElement("nodes");
				
				for(User user: this.id2user.values()) {
					userToGexf(w,user);
					}
				w.writeEndElement();//nodes
				
				/* edges */
				w.writeStartElement("edges");
				int relid=0;
				for(Link link:this.links) {
					w.writeEmptyElement("edge");
					w.writeAttribute("id", "E"+(++relid));
					w.writeAttribute("type", (link.reverse?"undirected":"directed"));
					w.writeAttribute("source","u"+link.u1.id);
					w.writeAttribute("target","u"+link.u2.id);
					}
				w.writeEndElement();//edges

				w.writeEndElement();//graph
				
				w.writeEndElement();//gexf
				w.writeEndDocument();
				w.flush();
				}
				
			private void userToGexf(final  XMLStreamWriter w,final User user)
					throws XMLStreamException
				{
				w.writeStartElement("node");
				w.writeAttribute("id", "u"+user.id);
				w.writeAttribute("label", user.acct);
				
				w.writeStartElement("attvalues");
				gexfAtt(w,"name",user.username);
				gexfAtt(w,"acct",user.acct);
				gexfAtt(w,"display_name",user.display_name);
				gexfAtt(w,"following",String.valueOf(user.following_count));
				gexfAtt(w,"followers",String.valueOf(user.followers_count));
				gexfAtt(w,"statuses_count",String.valueOf(user.statuses_count));
				w.writeEndElement();
				
				w.writeEndElement();
				}

		
		@Override
		public String toString() {
			return this.url+" "+ this.client_id+" "+this.client_secret;
			}
		}
	
	
	private class Response {
		HttpRequestBase request;
		String content = null;
		int httpStatus = -1;
		private JsonElement _json = null;
		String next_url =  null;
		JsonElement getJson() {
			if(content==null) return null;
			if(_json==null) {
				final JsonParser parser = new JsonParser();
				this._json = parser.parse(this.content);
				}
			return this._json;
			}
		JsonObject getJsonObject() {
			return getJson().getAsJsonObject();
			}
		JsonArray getJsonArray() {
			return getJson().getAsJsonArray();
			}
		}
	
	
	
	
	private Instance findInstanceByName(String s) {
		for(Instance instance:this.url2instance.values()) {
			return instance;
			}
		return null;
		}
	
	

	private UrlEncodedFormEntity createFormEntity(String...array) {
		return createFormEntity(Arrays.asList(array));
		}

	private UrlEncodedFormEntity createFormEntity(final List<String> L) {
		final Map<String,String> hash = new HashMap<>(L.size()/2);
		for(int i=0;i+1< L.size();i+=2) {
			hash.put(L.get(i), L.get(i+1));
			}
		return createFormEntity(hash);
		}
	
	private UrlEncodedFormEntity createFormEntity(Map<String,String> map) {
		final List<NameValuePair> postParameters = new ArrayList<NameValuePair>(map.size());
		for(final String k:map.keySet()) {
			postParameters.add(new BasicNameValuePair(k,map.get(k)));
			}
		try {
			return new UrlEncodedFormEntity(postParameters, "UTF-8");
			}
		catch(UnsupportedEncodingException err) {
			throw new RuntimeException(err);
			}
		}
	
	@Override
	public int doWork(final List<String> args) {
		try {
			// load config
			IOUtils.assertFileExists(this.xmlConfig);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db=dbf.newDocumentBuilder();
			Document dom = db.parse(this.xmlConfig);
			Element root=dom.getDocumentElement();
			if(root==null)  {
				LOG.error("empty config");
				return -1;
				}
			for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling()) {
				if(n.getNodeType()!=Node.ELEMENT_NODE) continue;
				final Element e = Element.class.cast(n);
				if(!e.getNodeName().equals("instance")) continue;
				final Instance instance = new Instance();
				instance.url = e.getAttribute("href");
				if(StringUtils.isBlank(instance.url)) {
					LOG.error("no @href in <instance> in "+this.xmlConfig);
					return -1;
					}
				if(this.url2instance.containsKey(instance.url)) {
					LOG.error("duplicate @href "+instance.url+" for <instance> in "+this.xmlConfig);
					return -1;
					}
				
				this.url2instance.put(instance.url,instance);
				for(Node n2=e.getFirstChild();n2!=null;n2=n2.getNextSibling()) {
					if(n2.getNodeType()!=Node.ELEMENT_NODE) continue;
					final Element e2 = Element.class.cast(n2);
					if(e2.getNodeName().equals("client_id") || e2.getNodeName().equals("client_key") ) {
						instance.client_id= e2.getTextContent().trim();
						}
					else if(e2.getNodeName().equals("client_secret")) {
						instance.client_secret= e2.getTextContent().trim();
						}
					}
				LOG.info(instance);
				}
			
			this.builder = HttpClientBuilder.create();
			
			Instance ins = url2instance.values().iterator().next();
			ins.scan("109291968049706699");
			if(this.output==null) {
				ins.toGexf(System.out);
				}
			else
				{
				try(FileOutputStream fos = new FileOutputStream(this.output)) {
					ins.toGexf(fos);
					fos.flush();
					}
				}
			for(String param: args) {
				Instance instance = findInstanceByName(param);
				if(instance==null) {
					LOG.warning("cannot find an instance for "+param);
					continue;
					}
				
				}
			return 0;
		} catch(final Throwable err) {
			LOG.error(err);
			return -1;
		}
		
		
		}
	
	public static void main(String[] args) {
		new MastodonGraph().instanceMainWithExit(args);

	}

}
