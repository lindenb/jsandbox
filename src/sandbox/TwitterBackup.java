package sandbox;


import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.google.gson.*;

/** TwitterBackup */
public class TwitterBackup
	extends AbstractTwitterApplication
	{
	private String screen_name=null;
	private String user_id=null;
	private File fileout;
	private Map<BigInteger, Element> id2tweet=new TreeMap<BigInteger, Element>();
	private Document dom;
	private TwitterBackup()
		{	
		try {
			DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
			f.setNamespaceAware(false);
			f.setValidating(false);
			f.setExpandEntityReferences(true);
			f.setIgnoringComments(false);
			DocumentBuilder docBuilder= f.newDocumentBuilder();
			this.dom=docBuilder.newDocument();
		} catch (ParserConfigurationException e)
			{
			throw new RuntimeException(e);
			}
		}
	
	private void save() throws Exception
		{
		TransformerFactory factory=TransformerFactory.newInstance();
		Transformer transformer=factory.newTransformer();
		//transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		if(fileout==null  || !this.fileout.exists())
			{
			Element root=dom.createElement("tweets");
			this.dom.appendChild(root);
			root.appendChild(dom.createTextNode("\n"));
			for(Element e:this.id2tweet.values())
				{
				root.appendChild(e);
				root.appendChild(dom.createTextNode("\n"));
				}
			
			Result result=null;
			result=(fileout==null?
					new StreamResult(System.out):
					new StreamResult(fileout)
					);
			
			transformer.transform(new DOMSource(this.dom),result);
			}
		else
			{
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			
			File tmpOut=File.createTempFile("tmp.", ".tweets",this.fileout.getParentFile());
			FileWriter fileWriter=new FileWriter(tmpOut);
			XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
			XMLEventWriter w= xmlfactory.createXMLEventWriter(fileWriter);
			

			
			XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
			xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
			xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
			xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
			FileReader fileReader=new FileReader(this.fileout);
			XMLEventReader reader= xmlInputFactory.createXMLEventReader(fileReader);
			while(reader.hasNext())
				{
				XMLEvent evt=reader.nextEvent();
				if(evt.isStartElement())
					{
					StartElement E=evt.asStartElement();
					if(E.getName().getLocalPart().equals("tweet"))
						{
						Attribute att=E.getAttributeByName(new QName("id_str"));
						if(att==null) att=E.getAttributeByName(new QName("id"));
						BigInteger tweet_id=new BigInteger(att.getValue());
						//update tweet
						if(this.id2tweet.containsKey(tweet_id))
							{
							int depth=1;
							//skip that element
							while(reader.hasNext())
								{
								evt=reader.nextEvent();
								if(evt.isStartElement()) { depth++;}
								else if(evt.isEndElement()) { depth--; if(depth==0) break;}
								else if(evt.isEndDocument()) throw new IllegalStateException("problem at id_str:"+tweet_id);
								}
							w.flush();
							fileWriter.flush();
							transformer.transform(
								new DOMSource(this.id2tweet.get(tweet_id)),
								new StreamResult(fileWriter)	
								);							
							fileWriter.flush();
							
							this.id2tweet.remove(tweet_id);
							continue;
							}
						
						}
					}
				else if(evt.isEndElement())
					{
					EndElement E=evt.asEndElement();
					if(E.getName().getLocalPart().equals("tweets"))
						{
						w.flush();
						fileWriter.flush();
						for(Element tweet:this.id2tweet.values())
							{
							transformer.transform(
								new DOMSource(tweet),
								new StreamResult(fileWriter)	
								);
							
							fileWriter.write('\n');
							}
						fileWriter.flush();
						}
					
					}
				w.add(evt);
				}
			fileReader.close();
			w.flush();
			w.close();
			fileWriter.flush();
			fileWriter.close();
			this.fileout.delete();
			if(!tmpOut.renameTo(this.fileout))
				{
				System.err.println("ERROR: cannot rename "+tmpOut+"/"+fileout);
				System.exit(-1);
				}
			}
		}
		
	
	
	private void appendAttributes(Element root,JsonObject object,String...atts)
		{
		for(String attName:atts)
			{
			JsonElement E=object.get(attName);
			if(E==null || E.isJsonNull() || !E.isJsonPrimitive()) continue;
			root.setAttribute(attName, E.getAsString());
			}
		}
	
	
	private Element createGeo(JsonObject o)
		{
		if(o==null || o.get("coordinates")==null) return null;
		Element geo=this.dom.createElement("geo");
		/*  we are currently representing it as a latitude then a longitude). */
		JsonArray coordinates=o.getAsJsonArray("coordinates");
		double latitude=coordinates.get(0).getAsDouble();
		double longitute=coordinates.get(1).getAsDouble();
		geo.setAttribute("lat", String.valueOf(latitude));
		geo.setAttribute("lon", String.valueOf(longitute));
		return geo;
		}
	private Element createPlace(JsonObject o)
		{
		if(o==null || o.get("place")==null) return null;
		Element geo=this.dom.createElement("place");
		appendAttributes(geo,o,"country","country_code","full_name","name","place_type");
		return geo;
		}

	
	private Element createUser(JsonObject o)
		{
		Element user=this.dom.createElement("user");
		appendAttributes(user,o,"name","id_str","screen_name");
		if(this.screen_name!=null && this.screen_name.equals(user.getAttribute("screen_name"))) return null;
		if(this.user_id!=null && this.user_id.equals(user.getAttribute("id_str"))) return null;
		return user;
		}
	
	private void fillText(Element root,String text,List<Entity> entities)
		{
		Element curr=root;
		int index=0;
		while(index < text.length())
			{
			for(Entity entity:entities)
				{
				if(index==entity.begin)
					{
					if(entity.getType().equals("hashtag"))
						{
						curr=this.dom.createElement("hashtag");
						root.appendChild(curr);
						}
					else if(entity.getType().equals("url"))
						{
						curr=this.dom.createElement("a");
						curr.setAttribute("href", ((UrlEntity)entity).expanded_url);
						root.appendChild(curr);
						}
					else if(entity.getType().equals("user"))
						{
						curr=this.dom.createElement("user");
						curr.setAttribute("id_str", ((UserEntity)entity).id_str);
						curr.setAttribute("name", ((UserEntity)entity).name);
						curr.setAttribute("screen_name", ((UserEntity)entity).screen_name);
						root.appendChild(curr);
						}
					else if(entity.getType().equals("media"))
						{
						MediaEntity me=(MediaEntity)entity;
						curr=this.dom.createElement("media");
						curr.setAttribute("id_str", ((MediaEntity)entity).id_str);
						curr.setAttribute("type", ((MediaEntity)entity).type);
						curr.setAttribute("url", ((MediaEntity)entity).expanded_url);
						if(me.source_status_id_str!=null)
							{
							curr.setAttribute("source_id_str", me.source_status_id_str);
							}
						root.appendChild(curr);
						}
					}
				}
			
			curr.appendChild(dom.createTextNode(text.substring(index,index+1)));
			++index;
			for(Entity entity:entities)
				{
				if(index==entity.end)
					{
					curr=root;
					}
				}
			
			}
		}
	
	private void addTweet(JsonObject o)
		{
		if(o.get("id_str")==null) return;
		Element tweet=this.dom.createElement("tweet");
		appendAttributes(tweet,o,
				"created_at",
				"id_str",
				"favorited",
				"retweeted",
				"in_reply_to_status_id_str",
				"in_reply_to_user_id"
				);
		if( o.get("user")!=null &&
			o.get("user").isJsonObject()
			)
			{
			Element user=createUser(o.get("user").getAsJsonObject());
			if(user!=null) tweet.appendChild(user);
			}
		if( o.get("geo")!=null &&
			o.get("geo").isJsonObject()
			)
			{
			Element geo=createGeo(o.get("geo").getAsJsonObject());
			if(geo!=null) tweet.appendChild(geo);
			}
		if( o.get("place")!=null &&
				o.get("place").isJsonObject()
				)
			{
			Element place=createPlace(o.get("place").getAsJsonObject());
			if(place!=null) tweet.appendChild(place);
			}

		/* build text */
		String text=o.get("text").getAsString();
		Element textE=this.dom.createElement("text");
		tweet.appendChild(textE);
		
		/* entities */
		JsonObject entities=o.getAsJsonObject("entities");
		List<Entity> L=new ArrayList<AbstractTwitterApplication.Entity>();
		if(entities!=null)
			{
			JsonArray array=entities.getAsJsonArray("hashtags");
			for(int i=0;array!=null && i<array.size();++i)
				{
				L.add(new HashTagEntity(array.get(i).getAsJsonObject()));
				}
			array=entities.getAsJsonArray("urls");
			for(int i=0;array!=null && i<array.size();++i)
				{
				L.add(new UrlEntity(array.get(i).getAsJsonObject()));
				}
			array=entities.getAsJsonArray("user_mentions");
			for(int i=0;array!=null && i<array.size();++i)
				{
				L.add(new UserEntity(array.get(i).getAsJsonObject()));
				}
			array=entities.getAsJsonArray("media");
			for(int i=0;array!=null && i<array.size();++i)
				{
				L.add(new MediaEntity(array.get(i).getAsJsonObject()));
				}
			}
		fillText(textE,text,L);
		String id=o.get("id_str").getAsString();
		this.id2tweet.put(new BigInteger(id), tweet);
		}
	
	@Override
	protected int parseArgument(String[] args,int optind) throws Exception
		{
		int i=super.parseArgument(args, optind);
		if(i!=-1) return i;
		if(args[optind].equals("-o") && optind+1<args.length)
			{
			this.fileout=new File(args[++optind]);
			return optind;
			}
		return -1;
		}
	
	@Override
	protected void usage()
		{
		System.err.println("java -jar twitterbackup [options] screen_name <screen_name>");
		System.err.println("java -jar twitterbackup [options] user_id <user_id>");
		System.err.println("java -jar twitterbackup [options] tweet <id1> <id2> <url1> ...");
		super.usage();
		return;
		}
	
	private JsonElement getJsonResponse(OAuthRequest request) throws IOException
		{
	    getService().signRequest(getAccessToken(), request);
	    Response response = request.send();
	    Reader  in=new InputStreamReader(new LogInputStream(response.getStream(),
	    		!Level.OFF.equals(LOG.getLevel())?System.err:null));
	    JsonParser parser=new JsonParser();
	    JsonElement jsonResponse=parser.parse(in);
	    in.close();
	    return jsonResponse;
		}
	
	
	public int run(String[] args) throws Exception
		{
		int optind=parseArguments(args);
		if(optind==args.length)
			{
			usage();
			System.exit(-1);
			}
		else if(
				(args[optind].equals("screen_name") || args[optind].equals("user_id")) && optind+1<args.length)
			{
			if(args[optind].equals("screen_name"))
				{
				this.screen_name=args[++optind];
				}
			else
				{
				this.user_id=args[++optind];
				}
			
		    connect();
		    
			
			for(String verb:new String[]{
					"/statuses/user_timeline.json",
					"/favorites/list.json"
					})
				{
				String url=getBaseURL()+verb;
			    OAuthRequest request = new OAuthRequest(Verb.GET, url);
			    if(this.user_id!=null)
			    	{
				    request.addQuerystringParameter("user_id", this.user_id);
			    	}
			    if(this.screen_name!=null)
			    	{
				    request.addQuerystringParameter("screen_name", this.screen_name);
			    	}
			    
			    request.addQuerystringParameter("exclude_replies","false");
			    JsonArray array= getJsonResponse(request).getAsJsonArray();
				for(int i=0;i< array.size();++i)
					{
					addTweet(array.get(i).getAsJsonObject());
					}
			    
				}
			}
		else if(args[optind].equals("tweet") && optind+1<args.length)
			{
			++optind;
			 connect();
			while(optind< args.length)
				{
				String tweet_id=args[optind++];
				int j=tweet_id.indexOf("/status/");
				if(j!=-1)
					{
					tweet_id=tweet_id.substring(j+8);
					j=tweet_id.indexOf("/");
					if(j!=-1)
						{
						tweet_id=tweet_id.substring(0,j);
						}
					}
				OAuthRequest request = new OAuthRequest(Verb.GET, getBaseURL()+"/statuses/show.json");
				request.addQuerystringParameter("id",tweet_id);
				addTweet(getJsonResponse(request).getAsJsonObject());
				}
			}
		else if(args[optind].equals("search") && optind+1<args.length)
			{
			++optind;
			connect();
			StringBuilder q=new StringBuilder();
			while(optind< args.length)
					{
					q.append(" ").append(args[optind++]);
					}
			OAuthRequest request = new OAuthRequest(Verb.GET, getBaseURL()+"/search/tweets.json");
			request.addQuerystringParameter("q",q.toString());

		    JsonArray array= getJsonResponse(request).getAsJsonArray();
			for(int i=0;i< array.size();++i)
				{
				addTweet(array.get(i).getAsJsonObject());
				}
			}
		else
			{
			System.err.println("Illegal number of arguments.");
			System.exit(-1);
			}
		
		savePreferences();
		
		save();
	    return 0;
		}
	    
	public static void main(String[] args) throws Exception
		{
		new TwitterBackup().run(args);
		}
	}
