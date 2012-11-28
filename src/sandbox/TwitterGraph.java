/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	Jan-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Wiki
 *  https://github.com/lindenb/jsandbox/wiki/JSandbox-Wiki
 * Motivation:
 *  make a graph of a twitter account
 */

package sandbox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.hp.hpl.jena.reasoner.IllegalParameterException;



public class TwitterGraph
	extends AbstractTwitterApplication
	{
	private Connection connection=null;
	
	private static class User
		{
		BigInteger id;
		String name=null;
		String screenName=null;
		String imageUrl=null;
		String location=null;
		String description=null;
		boolean protectedProfile=false;
		int friends=-1;
		int followers=-1;
		int listed=-1;
		int utc_offset=-1;
		int statuses_count=-1;
		
		private void gexfAtt(XMLStreamWriter w,String key,String value)
			throws XMLStreamException
			{
			if(value==null) return;
			w.writeStartElement("attvalue");
			w.writeAttribute("for", key);
			w.writeAttribute("value", value);
			w.writeEndElement();
			}
		
		void toGexf(XMLStreamWriter w)
			throws XMLStreamException
			{
			w.writeStartElement("node");
			w.writeAttribute("id", String.valueOf(this.id));
			w.writeAttribute("label", this.screenName);
			
			w.writeStartElement("attvalues");
			gexfAtt(w,"name",this.name);
			gexfAtt(w,"screenName",this.screenName);
			gexfAtt(w,"imageUrl",this.imageUrl);
			gexfAtt(w,"location",this.location);
			gexfAtt(w,"description",this.description);
			gexfAtt(w,"protectedProfile",String.valueOf(protectedProfile));
			gexfAtt(w,"friends",String.valueOf(friends));
			gexfAtt(w,"followers",String.valueOf(followers));
			gexfAtt(w,"listed",String.valueOf(listed));
			gexfAtt(w,"utc_offset",String.valueOf(utc_offset));
			gexfAtt(w,"statuses_count",String.valueOf(statuses_count));
			
			w.writeEndElement();
			
			w.writeEndElement();
			}
		}
	/**
	 * Link
	 */
	private static class Link
		implements Comparable<Link>
		{
		BigInteger id1;
		BigInteger id2;
		boolean one2two=false;
		boolean two2one=false;
		
		Link(BigInteger id1,BigInteger id2)
			{
			if(id1.compareTo(id2)<0)
				{
				this.id1=id1;
				this.id2=id2;
				this.one2two=true;
				}
			else
				{
				this.id1=id2;
				this.id2=id1;
				this.two2one=true;
				}
			}
		@Override
		public int compareTo(Link o)
			{
			int i=id1.compareTo(o.id1);
			if(i!=0) return i;
			return id2.compareTo(o.id2);
			}
		@Override
		public boolean equals(Object obj)
			{
			if (this == obj) { return true; }
			if (obj == null) { return false; }
			Link other = (Link) obj;
			return id1.equals(other.id1) && id2.equals(other.id2);
			}
		
		public void toGexf(int index,XMLStreamWriter w)
		throws XMLStreamException
			{
			w.writeEmptyElement("edge");
			w.writeAttribute("id", "E"+(index+1));
			if(one2two && two2one)
				{
				w.writeAttribute("type", "mutual");
				w.writeAttribute("source", String.valueOf(id1));
				w.writeAttribute("target", String.valueOf(id2));
				}
			else if(one2two)
				{
				w.writeAttribute("type", "directed");
				w.writeAttribute("source", String.valueOf(id1));
				w.writeAttribute("target", String.valueOf(id2));
				}
			else
				{
				w.writeAttribute("type", "directed");
				w.writeAttribute("source", String.valueOf(id2));
				w.writeAttribute("target", String.valueOf(id1));
				}
			}
		
		@Override
		public String toString()
			{
			return id1.toString()+" -> "+id2;
			}
		}
	
	private static class SocialGraph
		{
		BigInteger owner;
		Map<BigInteger,User> id2user=new HashMap<BigInteger,User>();
		List<Link> links=new ArrayList<Link>(1000);
		
		
		public void toDot(PrintWriter out)
			{
			out.println("digraph G {");
			for(User u:id2user.values())
				{
				out.println("n"+u.id+"[label='"+u.screenName+"'];");
				}
			for(Link L:links)
				{
				out.println("n"+L.id1+" -> n"+L.id2+";");
				}
			out.println("}");
			}
		
		public void toTSV(PrintWriter out)
			{
			for(Link L:this.links)
				{
				User u=this.id2user.get(L.id1);
				out.print(u==null?"":u.screenName);
				out.print("\t");
				u=this.id2user.get(L.id2);
				out.print(u==null?"":u.screenName);
				out.println();
				}
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
		
		public void toGexf(OutputStream out)
		throws XMLStreamException
			{
			XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
			XMLStreamWriter w= xmlfactory.createXMLStreamWriter(out,"UTF-8");
			
			w.writeStartDocument("UTF-8","1.0");
			w.writeStartElement("gexf");
			w.writeAttribute("xmlns", "http://www.gexf.net/1.2draft");
			w.writeAttribute("version", "1.2");
			
			
			/* meta */
			w.writeStartElement("meta");
				w.writeStartElement("creator");
				  w.writeCharacters(TwitterGraph.class.getCanonicalName());
				w.writeEndElement();
				w.writeStartElement("description");
				  w.writeCharacters("Graph of twitter user id. "+this.owner);
				w.writeEndElement();
			w.writeEndElement();
			
			/* graph */
			w.writeStartElement("graph");
			w.writeAttribute("mode", "static");
			w.writeAttribute("defaultedgetype", "directed");
			
			
			
			/* attributes */
			w.writeStartElement("attributes");
			w.writeAttribute("class","node");
			w.writeAttribute("mode","static");
			gexfAtt(w,"name","string",null);
			gexfAtt(w,"screenName","string",null);
			gexfAtt(w,"imageUrl","string","http://a3.twimg.com/sticky/default_profile_images/default_profile_1_reasonably_small.png");
			gexfAtt(w,"location","string",null);
			gexfAtt(w,"description","string",null);
			gexfAtt(w,"protectedProfile","boolean",null);
			gexfAtt(w,"friends","integer",null);
			gexfAtt(w,"followers","integer",null);
			gexfAtt(w,"listed","integer",null);
			gexfAtt(w,"utc_offset","integer",null);
			gexfAtt(w,"statuses_count","integer",null);
			w.writeEndElement();//attributes
			
			/* nodes */
			w.writeStartElement("nodes");
			for(User u:id2user.values())
				{
				u.toGexf(w);
				}
			w.writeEndElement();//nodes
			
			/* edges */
			w.writeStartElement("edges");
			for(int i=0;i< links.size();++i)
				{
				links.get(i).toGexf(i,w);
				}
			w.writeEndElement();//edges
	
			w.writeEndElement();//graph
			
			w.writeEndElement();//gexf
			w.writeEndDocument();
			w.flush();
			}
		
		}
	
	private TwitterGraph()
		{
		}
	
	
	private static int parseInt(XMLEventReader r)
		{
		try
			{
			String s=null;
			while(r.hasNext())
				{
				XMLEvent evt=r.nextEvent();
				if(evt.isEndElement()) break;
				if(s==null) s="";
				s+=evt.asCharacters().getData();
				}
			if(s==null) return -1;
			return Integer.parseInt(s);
			}
		catch (Exception e)
			{
			return -1;
			}
		}
	
	private User parseUser(XMLEventReader reader)throws XMLStreamException
		{
		int depth=1;
		User u=new User();
		while(reader.hasNext())
			{
			XMLEvent evt=reader.nextEvent();
			if(evt.isStartElement())
				{
				String localPart=evt.asStartElement().getName().getLocalPart();
				if(depth==1)
					{
					if(localPart.equals("id"))
						{
						u.id=new BigInteger(reader.getElementText());
						}
					else if(localPart.equals("profile_image_url"))
						{
						u.imageUrl=reader.getElementText();
						}
					else if(localPart.equals("name"))
						{
						u.name=reader.getElementText();
						}
					else if(localPart.equals("screen_name"))
						{
						u.screenName=reader.getElementText();
						}
					else if(localPart.equals("location"))
						{
						u.location=reader.getElementText();
						}
					else if(localPart.equals("description"))
						{
						u.description=reader.getElementText();
						}
					else if(localPart.equals("followers_count"))
						{
						u.followers=parseInt(reader);
						}
					else if(localPart.equals("friends_count"))
						{
						u.friends=parseInt(reader);
						}
					else if(localPart.equals("protected"))
						{
						u.protectedProfile=Boolean.parseBoolean(reader.getElementText());
						}
					else if(localPart.equals("listed_count"))
						{
						u.listed=parseInt(reader);
						}
					else if(localPart.equals("statuses_count"))
						{
						u.statuses_count=parseInt(reader);
						}
					else if(localPart.equals("utc_offset"))
						{
						u.utc_offset=parseInt(reader);
						}
					else
						{
						depth++;
						}
					}
				else
					{
					depth++;
					}
				}
			else if(evt.isEndElement())
				{
				if(depth==1 && evt.asEndElement().getName().getLocalPart().equals("user"))
					{
					break;
					}
				depth--;
				}
			}
		return u;
		}	
	private InputStream tryOpen(String uri) throws IOException
		{
		URLConnection con=null;
		InputStream in=null;
		for(int i=0;i< 100;++i)
			{
			try
				{
				URL url=new URL(uri);
				con=url.openConnection();
				con.setConnectTimeout(5*1000);
				in=url.openStream();
				return in;
				}
			catch(IOException err)
				{
				if(con!=null
					&& (con instanceof HttpURLConnection)
					)
					{
					int code=HttpURLConnection.class.cast(con).getResponseCode();
					switch(code)
						{
						case 403: return null;
						case 401: return null;
						case 400:
							{
							LOG.info("I don't use the OAuth API. I'm waiting for 10 minutes ! :-P");
							try { Thread.sleep(10*60*1000);}
							catch(Throwable err2) {}
							break;
							}
						default:
							{
							LOG.info("Http Error code:"+code);
							break;
							}
						}
					}
				System.err.println("Trying... "+err.getClass().getCanonicalName()+":"+err.getMessage());
				in=null;
				try { Thread.sleep(5*1000);}
				catch(Throwable err2) {}
				}
			}
		return null;
		}
	
	
	private Set<BigInteger> listFriends(
			BigInteger userId,
			String screen_name,
			Set<BigInteger> retains
			)
			throws Exception
			{		
			JsonParser parser=new JsonParser();
			Set<BigInteger> friends=new HashSet<BigInteger>();
			BigInteger cursor=BigInteger.ONE.negate();
			String url=getBaseURL()+"/get/friends/ids";

			for(;;)
				{
				OAuthRequest request = new OAuthRequest(Verb.GET, url);
			    if(userId!=null) request.addQuerystringParameter("user_id", userId.toString());
			    else if(screen_name!=null) request.addQuerystringParameter("screen_name", screen_name);
			    else throw new IllegalParameterException("user id/screen_name ?");
			    request.addQuerystringParameter("cursor", cursor.toString());
			    getService().signRequest(getAccessToken(), request);
			    JsonElement jsonResponse=null;
			    for(;;)
			    	{
			    	try
			    		{
					    Response response = request.send();
					    Reader  in=new InputStreamReader(new LogInputStream(response.getStream(),
					    		!Level.OFF.equals(LOG.getLevel())?System.err:null));
					    jsonResponse=parser.parse(in);
					    in.close();
						break;
			    		}
			    	catch (Exception e)
			    		{
						e.printStackTrace();
						Thread.sleep(5*60*1000);//5minutes
						}
			    	}
			    JsonArray array=jsonResponse.getAsJsonObject().get("ids").getAsJsonArray();
			    for(int i=0;i< array.size();++i)
			    	{
			    	BigInteger friendid=array.get(i).getAsBigInteger();
			    	if(retains!=null && !retains.contains(friendid))
			    		{
			    		continue;
			    		}
			    	friends.add(friendid);
			    	}
			    
			    cursor=null;
			    if(jsonResponse.getAsJsonObject().get("next_cursor")!=null)
			    	{
			    	cursor=jsonResponse.getAsJsonObject().get("next_cursor").getAsBigInteger();
			    	}
			    if(cursor==null || cursor.equals(BigInteger.ZERO)) break;
				}
			LOG.info("count friends: of "+userId+"="+friends.size());
			return friends;
			}	
	
	private JsonObject sqlUserById(BigInteger id)
		throws SQLException
		{
		JsonObject o=null;
		JsonParser parser=new JsonParser();
		PreparedStatement pstmt=connection.prepareStatement(
				"select json from user where id=?");
		pstmt.setString(1, id.toString());
		ResultSet row=pstmt.executeQuery();
		while(row.next())
			{
			o=parser.parse(row.getString(1)).getAsJsonObject();
			}
		pstmt.close();
		return o;
		}
	
	private static String toString(JsonElement e)
		{
		return new GsonBuilder().create().toJson(e);
		}
	
	private BigInteger fetchUserInfo(BigInteger user_id,String screen_name)
		throws Exception
		{
		JsonParser parser=new JsonParser();
		JsonObject user=null;
		if(user_id!=null)user=sqlUserById(user_id);
		if(user!=null)
			{
			return user.getAsJsonObject().get("id_str").getAsBigInteger();
			}
		
		//https://dev.twitter.com/docs/api/1.1/get/users/show
		String url=getBaseURL()+"/get/users/show";

		OAuthRequest request = new OAuthRequest(Verb.GET, url);
	    if(user_id!=null) request.addQuerystringParameter("user_id", user_id.toString());
	    else if(screen_name!=null) request.addQuerystringParameter("screen_name", screen_name);
	    else throw new IllegalParameterException("user id/screen_name ?");

	    getService().signRequest(getAccessToken(), request);
	    JsonElement jsonResponse=null;
	    for(;;)
	    	{
	    	try
	    		{
			    Response response = request.send();
			    Reader  in=new InputStreamReader(new LogInputStream(response.getStream(),
			    		!Level.OFF.equals(LOG.getLevel())?System.err:null));
			    jsonResponse=parser.parse(in);
			    in.close();
				break;
	    		}
	    	catch (Exception e)
	    		{
				e.printStackTrace();
				Thread.sleep(5*60*1000);//5minutes
				}
	    	}
	    user_id=jsonResponse.getAsJsonObject().get("id_str").getAsBigInteger();
	    PreparedStatement pstmt=connection.prepareStatement(
	    	"insert into user(id,json) values(?,?)");
	    pstmt.setString(1,user_id.toString() );
		pstmt.setString(2, toString(jsonResponse));
	    pstmt.executeUpdate();
			
			
		
		return user_id;
		}

	private void insertRelation(BigInteger user1,BigInteger user2)
		throws SQLException
		{
	    PreparedStatement pstmt=connection.prepareStatement(
		    	"insert into friend(id1,id2) values(?,?)");
		pstmt.setString(1,user1.toString() );
		pstmt.setString(2,user2.toString() );
		pstmt.executeUpdate();
		pstmt.close();
		}
	
	private void build(BigInteger user_id,String screen_name)
		throws Exception
		{
		Set<BigInteger> friends=this.listFriends(user_id, screen_name,null);
		user_id=this.fetchUserInfo(user_id,screen_name);
		for(BigInteger friend_id:friends)
			{
			this.fetchUserInfo(friend_id,null);
			Set<BigInteger> friendsOfMyFriends=this.listFriends(friend_id,null,friends);
			friendsOfMyFriends.retainAll(friends);
			
			for(BigInteger link:friendsOfMyFriends)
				{
				insertRelation(friend_id,link);
				}
			
			}
		}
	

	
	private void run(String args[]) throws Exception
		{
		//id: 7431072
		//args=new String[]{"-o","/home/pierre/jeter.dot","7431072"};
		File database=null;
		
		int optind=0;
		while(optind< args.length)
			{
			if(args[optind].equals("-h") ||
			   args[optind].equals("-help") ||
			   args[optind].equals("--help"))
				{
				System.err.println("Options:");
				System.err.println(" -h help; This screen.");
				System.err.println(" -d <database>");
				System.err.println("[screen-id]");
				return;
				}
			else if(args[optind].equals("-d"))
				{
				database=new File(args[++optind]);
				}
			else if(args[optind].equals("--"))
				{
				optind++;
				break;
				}
			else if(args[optind].startsWith("-"))
				{
				System.err.println("Unknown option "+args[optind]);
				return;
				}
			else 
				{
				break;
				}
			++optind;
			}
		
		if(database==null)
			{
			System.err.println("option -d <database> missing");
			System.exit(-1);
			}
		Class.forName("org.sqlite.JDBC");
		this.connection = DriverManager.getConnection("jdbc:sqlite:"+database);
		if( (args[optind].equals("screen_name") || args[optind].equals("user_id")) && optind+1<args.length)
			{
			
			String screen_name=null;
			BigInteger user_id=null;
			if(args[optind].equals("screen_name"))
				{
				screen_name=args[++optind];
				}
			else
				{
				user_id=new BigInteger(args[++optind]);
				}
			connect();
			build(user_id,screen_name);
			savePreferences();
			}
		this.connection.close();
		/*
			if(fileout.getName().toLowerCase().endsWith(".dot"))
				{
				PrintWriter p=new PrintWriter(fileout);
				g.toDot(p);
				p.flush();
				p.close();
				}
			else if(fileout.getName().toLowerCase().endsWith(".gexf"))
				{
				FileOutputStream p=new FileOutputStream(fileout);
				g.toGexf(p);
				p.flush();
				p.close();
				}
			else if(fileout.getName().toLowerCase().endsWith(".tsv"))
				{
				PrintWriter p=new PrintWriter(fileout);
				g.toTSV(p);
				p.flush();
				p.close();
				}
			else
				{
				System.err.println("Boum");
				}
		*/
		}
	
	public static void main(String[] args) throws Exception
		{
		new TwitterGraph().run(args);
		}
	}
