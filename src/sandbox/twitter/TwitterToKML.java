/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	April-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Wiki
 *  https://github.com/lindenb/jsandbox/wiki/JSandbox-Wiki
 * Reference
 *   http://plindenbaum.blogspot.com/2011/04/mapping-people-im-following-on-twitter.html
 * Motivation:
 *  make a KML file of my following/followers
 * Compilation:
 *        cd jsandbox; ant twitterkml
 * Execution:
 *        java -jar dist/twitterkml.jar -g (geonames-id) -o file (twitter-id)
 */

package sandbox.twitter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;




public class TwitterToKML
	{
	private static final Logger LOG=Logger.getLogger("sandbox.TwitterKML");
	private XMLInputFactory xmlInputFactory;
	private String geonamesId=null;
	private BigInteger owner=null;
	private Map<BigInteger,User> id2user=new HashMap<BigInteger,User>();
	private List<GeoLocation> geoLocations=new ArrayList<TwitterToKML.GeoLocation>();
	private boolean use_followers=false;
	private class GeoLocation
		{
		String location=null;
		Double longitude=null;
		Double latitude=null;
		@SuppressWarnings("unused")
		String countryName=null;
		String toponymName=null;
		Set<User> users=new HashSet<TwitterToKML.User>();
		void parse(XMLEventReader reader) throws XMLStreamException
			{
			while(reader.hasNext())
				{
				XMLEvent evt=reader.nextEvent();
				if(evt.isEndElement())
					{
					String localName=evt.asEndElement().getName().getLocalPart();
					if(localName.equals("geoname"))
						{
						return;
						}
					continue;
					}
				if(!evt.isStartElement()) continue;
				StartElement e=evt.asStartElement();
				String localName=e.getName().getLocalPart();
				if(localName.equals("lat"))
					{
					this.latitude=new Double(reader.getElementText());
					}
				else if(localName.equals("lng"))
					{
					this.longitude=new Double(reader.getElementText());
					}
				else if(localName.equals("countryName"))
					{
					this.countryName=reader.getElementText();
					}
				else if(localName.equals("toponymName"))
					{
					this.toponymName=reader.getElementText();
					}
				}
			}
		
		/*private String escapeXML(String s)
			{
			StringBuilder b=new StringBuilder(s.length());
			for(int i=0;i<s.length();++i)
				{
				switch(s.charAt(i))
					{
					case '<': b.append("&lt;"); break;
					case '>': b.append("&gt;"); break;
					case '&': b.append("&amp;"); break;
					case '\'': b.append("&apos;"); break;
					case '\"': b.append("&quote;"); break;
					default:b.append(s.charAt(i));break;
					}	
				}
			return b.toString();
			}*/
		void toKML(XMLStreamWriter w)
		throws XMLStreamException
			{
			w.writeStartElement("Placemark");
			
			w.writeStartElement("name");
			w.writeCharacters(String.valueOf(this.toponymName));
			w.writeEndElement();//
			
			StringWriter description=new StringWriter();
			XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
			XMLStreamWriter w2= xmlfactory.createXMLStreamWriter(description);
			w2.writeStartElement("div");
			w2.writeAttribute("style","max-height:200px;overflow:auto;");
			w2.writeStartElement("ul");
			for(User u:this.users)
				{
				w2.writeStartElement("li");
				u.toKML(w2);
				w2.writeEndElement();
				}
			w2.writeEndElement();
			w2.writeEndElement();
			w2.flush();
			
			w.writeStartElement("description");
			w.writeCData(description.toString());
			w.writeEndElement();//
			
			w.writeStartElement("Point");
			w.writeStartElement("coordinates");
			w.writeCharacters(String.valueOf(this.longitude)+","+this.latitude);
			w.writeEndElement();//coordinates
			w.writeEndElement();//Point
			
			w.writeEndElement();//Placemark
			}


		@Override
		public boolean equals(Object obj)
			{
			if (this == obj) { return true; }
			if (obj == null) { return false; }
			if (getClass() != obj.getClass()) { return false; }
			GeoLocation other = (GeoLocation) obj;
			
			return 	latitude.equals(other.latitude) &&
					longitude.equals(other.longitude);
			}

		
		
		
		}
	@SuppressWarnings("unused")
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
		
		void toKML(XMLStreamWriter w)
		throws XMLStreamException
			{
			w.writeStartElement("div");
			if(imageUrl!=null && !imageUrl.isEmpty())
				{
				w.writeEmptyElement("img");
				w.writeAttribute("src",imageUrl);
				w.writeAttribute("width","48");
				w.writeAttribute("alt",screenName);
				w.writeEmptyElement("br");
				}
			w.writeStartElement("a");
			w.writeAttribute("href", "http://twitter.com/#!/"+screenName);
			w.writeCharacters("@"+String.valueOf(screenName));
			w.writeEndElement();
			
			if(name!=null && !name.isEmpty())
				{
				w.writeEmptyElement("br");
				w.writeStartElement("span");
				w.writeCharacters(name);
				w.writeEndElement();
				}
			if(description!=null && !description.isEmpty())
				{
				w.writeEmptyElement("br");
				w.writeStartElement("span");
				w.writeCharacters(description);
				w.writeEndElement();
				}
			w.writeEndElement();
			}


		@Override
		public int hashCode()
			{
			final int prime = 31;
			int result = 1;
			result = prime * result + id.hashCode();
			return result;
			}


		@Override
		public boolean equals(Object obj)
			{
			if (this == obj) { return true; }
			if (obj == null) { return false; }
			if (getClass() != obj.getClass()) { return false; }
			User other = (User) obj;
			return id.equals(other.id);
			}
		
		
		
		}
	
	

		
		public void toKML(OutputStream out)
		throws XMLStreamException
			{
			XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
			XMLStreamWriter w= xmlfactory.createXMLStreamWriter(out,"UTF-8");
			
			w.writeStartDocument("UTF-8","1.0");
			w.writeStartElement("kml");
			w.writeAttribute("xmlns", "http://www.opengis.net/kml/2.2");
			
			w.writeStartElement("Document");
			w.writeStartElement("name");
			w.writeCharacters("Twitter: 'following' for user_id: "+this.owner+" date:"+new java.sql.Timestamp(System.currentTimeMillis()));
			w.writeEndElement();//name
			
			for(GeoLocation loc: this.geoLocations)
				{
				loc.toKML(w);
				}
			
			w.writeEndElement();//
			
			w.writeEndElement();//kml
			w.writeEndDocument();
			w.flush();
			}
		
	
	
	private TwitterToKML()
		{
		this.xmlInputFactory = XMLInputFactory.newInstance();
		xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
		xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
		xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
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
	
	private GeoLocation getGeolocation(String location)
		throws Exception
		{
		
		if(location==null || location.trim().isEmpty()) return null;
		for(GeoLocation loc: this.geoLocations)
			{
			if(loc.location.equalsIgnoreCase(location)) return loc;
			}
		String uri="http://api.geonames.org/search?type=xml&maxRows=1&q="+
				URLEncoder.encode(location, "UTF-8")+"&username="+
				URLEncoder.encode(geonamesId, "UTF-8");
		LOG.info(uri);
		InputStream in=null;
		try
			{
			in=tryOpen(uri);
			
			XMLEventReader reader= xmlInputFactory.createXMLEventReader(in);
			while(reader.hasNext())
				{
				XMLEvent evt=reader.nextEvent();
				
				if(!evt.isStartElement()) continue;
				StartElement e=evt.asStartElement();
				String localName=e.getName().getLocalPart();
				if(localName.equals("geoname"))
					{
					GeoLocation loc=new GeoLocation();
					loc.location=location;
					loc.parse(reader);
					if(loc.latitude==null || loc.longitude==null) return null;
					
					for(GeoLocation loc2: this.geoLocations)
						{
						if(loc2.equals(loc)) return loc2;
						}
					this.geoLocations.add(loc);
					return loc;
					}
				else if(localName.equals("status"))
					{
					Attribute att=e.getAttributeByName(new QName("message"));
					LOG.info(uri+" "+att.getValue());
					return null;
					}
				}
			}
		finally
			{
			if(in!=null) in.close();
			}
		return null;
		}
	
	private User getProfile(BigInteger userId)
		throws Exception
		{
		User user=null;
		String uri="http://api.twitter.com/1/users/show.xml?user_id="+
				userId;
		LOG.info(uri);
		InputStream in=tryOpen(uri);
		if(in==null)
			{
			user=new User();
			user.id=userId;
			user.imageUrl=null;
			user.name=userId.toString();
			user.screenName=userId.toString();
			return user;
			}
		XMLEventReader reader= xmlInputFactory.createXMLEventReader(in);
		while(reader.hasNext())
			{
			XMLEvent evt=reader.nextEvent();
			if(!evt.isStartElement()) continue;
			StartElement e=evt.asStartElement();
			String localName=e.getName().getLocalPart();
			if(!localName.equals("user")) continue;
			user= parseUser(reader);
			break;
			}
		reader.close();
		in.close();
		return user;
		}
	
	private Set<BigInteger> listFriends(BigInteger userId)
		throws Exception
		{			
		Set<BigInteger> friends=new HashSet<BigInteger>();
		BigInteger cursor=BigInteger.ONE.negate();
		for(;;)
			{
			String uri="http://api.twitter.com/1/"+(use_followers?"followers":"friends")+"/ids.xml?user_id="+
			userId+"&cursor="+cursor;
			LOG.info(uri);
			InputStream in=tryOpen(uri);
			if(in==null) break;
			XMLEventReader reader= xmlInputFactory.createXMLEventReader(in);
			String next_cursor=null;
			while(reader.hasNext())
				{
				XMLEvent evt=reader.nextEvent();
				if(evt.isEndDocument()) break;
				if(!evt.isStartElement()) continue;
				StartElement e=evt.asStartElement();
				String localName=e.getName().getLocalPart();
				if(localName.equals("id"))
					{
					friends.add(new BigInteger(reader.getElementText()));
					}
				else if(localName.equals("next_cursor"))
					{
					next_cursor=reader.getElementText();
					break;
					}
				}
			reader.close();
			in.close();
			
			if(next_cursor==null || next_cursor.isEmpty() || next_cursor.equals("0"))
				{
				break;
				}
			cursor=new BigInteger(next_cursor);
			}
		LOG.info("count friends: of "+userId+"="+friends.size());
		return friends;
		}	
	
	private void run()
		throws Exception
		{
		this.id2user.put(owner, getProfile(owner));
		
		Set<BigInteger> friendIds=listFriends(owner);
		
		
		for(BigInteger id: friendIds)
			{
			User user=getProfile(id);
			this.id2user.put(id,user);
			}
		Iterator<BigInteger> r=id2user.keySet().iterator();
		while(r.hasNext())
			{
			User u=id2user.get(r.next());
			if(u.location==null || u.location.trim().isEmpty())
				{
				r.remove();
				continue;
				}
			GeoLocation geoloc= getGeolocation(u.location);
			if(geoloc==null)
				{
				r.remove();
				continue;
				}
			geoloc.users.add(u);
			
			}
		
		}
	
	public static void main(String[] args)
		{
		//id: 7431072
		//args=new String[]{"-o","/home/pierre/jeter.dot","7431072"};
		File fileout=null;
		TwitterToKML app=null;
		try
			{
			app=new TwitterToKML();
			int optind=0;
			while(optind< args.length)
				{
				if(args[optind].equals("-h") ||
				   args[optind].equals("-help") ||
				   args[optind].equals("--help"))
					{
					System.err.println("Options:");
					System.err.println(" -h help; This screen.");
					System.err.println(" -o <fileout>.kml");
					System.err.println(" -f use followers instead of following.");
					System.err.println(" -g <genonames-id> id on geonames.org");
					System.err.println("[screen-id]");
					return;
					}
				else if(args[optind].equals("-g"))
					{
					app.geonamesId=args[++optind];
					}
				else if(args[optind].equals("-f"))
					{
					app.use_followers=true;
					}
				else if(args[optind].equals("-o"))
					{
					fileout=new File(args[++optind]);
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
			if(app.geonamesId==null)
				{
				System.err.println("GeoNames-Id missing");
				System.exit(-1);
				}
			if(optind+1!=args.length)
				{
				System.err.println("Screen-Name missing");
				System.exit(-1);
				}
			
			
			if(fileout==null)
				{
				System.err.println("option -o <fileout> missing");
				System.exit(-1);
				}
			
			app.owner=new BigInteger(args[optind++]);
			app.run();
			
			
			FileOutputStream p=new FileOutputStream(fileout);
			app.toKML(p);
			p.flush();
			p.close();
				
			} 
		catch(Throwable err)
			{
			err.printStackTrace();
			}
		finally
			{
			if(app!=null) app=null;//was app.close();
			}
		}
	}
