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

package sandbox.twitter;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sandbox.LogInputStream;
import sandbox.Logger;

import com.beust.jcommander.Parameter;



public class TwitterGraph
	extends AbstractTwitterApplication
	{
	private static final Logger LOG=Logger.builder(TwitterGraph.class).build();

	private Connection connection=null;
	@Parameter(names={"-d","--database"},description="sqlite3 database",required=true)
	private File databaseFile=null;


	
	private TwitterGraph()
		{
		}
	
	
	private static void gexfAtt(final XMLStreamWriter w,final String key,final String value)
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

	public void toGexf(OutputStream out)
	throws XMLStreamException,SQLException
		{
		JsonParser jsonParser=new JsonParser();
		XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
		XMLStreamWriter w= xmlfactory.createXMLStreamWriter(out,"UTF-8");
		
		w.writeStartDocument("UTF-8","1.0");
		w.writeStartElement("gexf");
		w.writeAttribute("version", "1.3");
		w.writeAttribute("xmlns", "http://www.gexf.net/1.3");
		w.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		w.writeAttribute("xsi:schemaLocation", "http://www.gexf.net/1.3 http://www.gexf.net/1.3/gexf.xsd");

		
		
		/* meta */
		w.writeStartElement("meta");
			w.writeStartElement("creator");
			  w.writeCharacters(TwitterGraph.class.getCanonicalName());
			w.writeEndElement();
			w.writeStartElement("description");
			  w.writeCharacters("Twitter Graph");
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
		Statement stmt=connection.createStatement();
		ResultSet row=stmt.executeQuery("select json from user");
		while(row.next())
			{
			final JsonObject user=jsonParser.parse(row.getString(1)).getAsJsonObject();
			userToGexf(w,user);
			}
		row.close();
		stmt.close();
		w.writeEndElement();//nodes
		
		/* edges */
		w.writeStartElement("edges");
		stmt=connection.createStatement();
		row=stmt.executeQuery("select id1,id2,reverse from friend");
		int relid=0;
		while(row.next())
			{
			final BigInteger id1=new BigInteger(row.getString(1));
			final BigInteger id2=new BigInteger(row.getString(2));
			final  boolean reverse=row.getInt(3)!=0;
			w.writeEmptyElement("edge");
			w.writeAttribute("id", "E"+(++relid));
			w.writeAttribute("type", (reverse?"undirected":"directed"));
			w.writeAttribute("source",id1.toString());
			w.writeAttribute("target",id2.toString());
				
			}
		row.close();
		stmt.close();
		w.writeEndElement();//edges

		w.writeEndElement();//graph
		
		w.writeEndElement();//gexf
		w.writeEndDocument();
		w.flush();
		}
	
	private void userToGexf(final  XMLStreamWriter w,final JsonObject user)
		throws XMLStreamException
		{
		w.writeStartElement("node");
		w.writeAttribute("id", user.get("id_str").getAsString());
		w.writeAttribute("label", user.get("screen_name").getAsString());
		
		w.writeStartElement("attvalues");
		gexfAtt(w,"name",user.get("name").getAsString());
		gexfAtt(w,"screenName",user.get("screen_name").getAsString());
		gexfAtt(w,"imageUrl",user.get("screen_name").getAsString());
		gexfAtt(w,"location",user.has("location")?user.get("location").getAsString():"nowhere");
		gexfAtt(w,"description",user.get("screen_name").getAsString());
		gexfAtt(w,"protectedProfile",user.has("protected")?String.valueOf(user.get("protected").getAsBoolean()):"false");
		gexfAtt(w,"friends",user.has("friends_count")?user.get("friends_count").getAsString():"0");
		gexfAtt(w,"followers",user.has("followers_count")?user.get("followers_count").getAsString():"0");
		gexfAtt(w,"listed",user.has("listed_count")?user.get("listed_count").getAsString():"0");
		gexfAtt(w,"utc_offset",user.has("utc_offset")?user.get("utc_offset").getAsString():"0");
		gexfAtt(w,"statuses_count",user.has("statuses_count")?user.get("statuses_count").getAsString():"0");
		
		w.writeEndElement();
		
		w.writeEndElement();
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
			String url=getBaseURL()+"/friends/ids.json";
			LOG.info(url);
			for(;;)
				{
			    JsonElement jsonResponse=null;
			    JsonArray array=null;
			    for(;;)
			    	{
			    	try
			    		{
						OAuthRequest request = new OAuthRequest(Verb.GET, url);
					    if(userId!=null) request.addQuerystringParameter("user_id", userId.toString());
					    else if(screen_name!=null) request.addQuerystringParameter("screen_name", screen_name);
					    else throw new IllegalArgumentException("user id/screen_name ?");
					    request.addQuerystringParameter("cursor", cursor.toString());
					    getService().signRequest(getAccessToken(), request);

					    Response response = request.send();
					    Reader  in=new InputStreamReader(new LogInputStream(response.getStream(),System.err));
					    jsonResponse=parser.parse(in);
					    in.close();
					    array=jsonResponse.getAsJsonObject().get("ids").getAsJsonArray();
						break;
			    		}
			    	catch (Exception e)
			    		{
						sleep(e);
						}
			    	}
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
		if(user_id!=null)
			{
			user=sqlUserById(user_id);
			}
		if(user!=null)
			{
			LOG.info("user.id "+user_id+" already in database");
			return user_id;
			}
		
		//https://dev.twitter.com/docs/api/1.1/get/users/show
		String url=getBaseURL()+"/users/show.json";

		OAuthRequest request = new OAuthRequest(Verb.GET, url);
	    if(user_id!=null) request.addQuerystringParameter("user_id", user_id.toString());
	    else if(screen_name!=null) request.addQuerystringParameter("screen_name", screen_name);
	    else throw new IllegalArgumentException("user id/screen_name ?");

	    getService().signRequest(getAccessToken(), request);
	    JsonElement jsonResponse=null;
	    for(;;)
	    	{
	    	try
	    		{
			    Response response = request.send();
			    Reader  in=new InputStreamReader(new LogInputStream(response.getStream(),
			    		System.err));
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
	    	"insert  OR IGNORE into user(id,json) values(?,?)");
	    pstmt.setString(1,user_id.toString() );
		pstmt.setString(2, toString(jsonResponse));
	    pstmt.executeUpdate();
			
			
		
		return user_id;
		}

	private void insertRelation(BigInteger user1,BigInteger user2)
		throws SQLException
		{
	    PreparedStatement pstmt=connection.prepareStatement(
		    	"update friend set reverse=1 where id1=? and id2=?");
		pstmt.setString(1,user2.toString() );
		pstmt.setString(2,user1.toString() );
		int count=pstmt.executeUpdate();
		pstmt.close();
		if(count==0)
			{
			pstmt=connection.prepareStatement(
			    	"insert OR IGNORE into friend(id1,id2,reverse) values (?,?,0)"
					);
			pstmt.setString(1,user1.toString() );
			pstmt.setString(2,user2.toString() );
			pstmt.executeUpdate();
			pstmt.close();
			}
		}
	
	private void low_rate()
		{
		try {Thread.sleep(5*1000);}//5secs
		catch(Throwable err) {}
		
		}
	
	private void build(BigInteger user_id,String screen_name)
		throws Exception
		{
		LOG.info("build for user "+user_id+"/"+screen_name);
		Set<BigInteger> friends=this.listFriends(user_id, screen_name,null);
		LOG.info("friends: "+friends.size());
		
		user_id=this.fetchUserInfo(user_id,screen_name);
		low_rate();
		for(BigInteger friend_id:friends)
			{
			LOG.info("now looking at: friend_id:"+friend_id);
			insertRelation(user_id,friend_id);
			this.fetchUserInfo(friend_id,null);
			low_rate();
			Set<BigInteger> friendsOfMyFriends=this.listFriends(friend_id,null,friends);
			low_rate();
			friendsOfMyFriends.retainAll(friends);
			
			for(BigInteger link:friendsOfMyFriends)
				{
				insertRelation(friend_id,link);
				}
			
			}
		}
	
	private void openConnection() throws Exception
		{
		Class.forName("org.sqlite.JDBC");
		this.connection = DriverManager.getConnection("jdbc:sqlite:"+this.databaseFile);
		Statement stmt=this.connection.createStatement();
		stmt.executeUpdate("create table if not exists user(id VARCHAR(50) UNIQUE NOT NULL,json TEXT NOT NULL);");
		stmt.executeUpdate("create table if not exists friend(id1 VARCHAR(50)  NOT NULL,id2 VARCHAR(50) NOT NULL,reverse int,PRIMARY KEY (id1, id2));");
		stmt.close();

		}

	private void closeConnection() throws SQLException
		{
		if(this.connection!=null) this.connection.close();
		this.connection=null;
		}
	
	

	
	@Override
	public int doWork(List<String> args) {
		if(this.databaseFile==null)
			{
			LOG.error("undefined option 'd'");
			return -1;
			}
		
		try
			{
			
			if( args.size()==1 )
				{
				final String screen_name=oneFileOrNull(args);
				BigInteger user_id=null;
				
				try {
					user_id = new BigInteger(screen_name);
				} catch (Exception e) {
					user_id = null;
					}
				
				connect();
				savePreferences();
				openConnection();
				build(user_id,screen_name);
				closeConnection();
				savePreferences();
				}
			else if(args.isEmpty())
				{
				openConnection();
				this.toGexf(System.out);
				closeConnection();
				}
			else
				{
				LOG.error("Illegal Arguments.\n");
				return -1;
				}
			return 0;
			}
		catch(Exception err)
			{
			LOG.error(err);
			return -1;
			}
		
		}
	
	public static void main(String[] args) throws Exception
		{
		new TwitterGraph().instanceMainWithExit(args);
		}
	}
