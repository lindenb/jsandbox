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
 * Compilation:
 *        cd jsandbox; ant twittermosaic
 */
package sandbox;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;



public class TwitterGraph
	{
	private static final Logger LOG=Logger.getLogger("sandbox.TwitterMosaic");
	private XMLInputFactory xmlInputFactory;
	
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
		
		private int lower_bound(int first,int last,Link L)
			{
            int len = last - first;
            while (len > 0)
                    {
                    int half = len / 2;
                    int middle = first + half;


                    if ( links.get(middle).compareTo(L) < 0  )
                            {
                            first = middle + 1;
                            len -= half + 1;
                            }
                    else
                            {
                            len = half;
                            }
                    }
            return first;
			}
		
		public int lowerBound(Link L)
			{
			return lower_bound(0,links.size(),L);
			}
		
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
			w.writeAttribute("class","nodes");
			gexfAtt(w,"name","string",null);
			gexfAtt(w,"screenName","string",null);
			gexfAtt(w,"imageUrl","anyURI","http://a3.twimg.com/sticky/default_profile_images/default_profile_1_reasonably_small.png");
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
		for(int i=0;i< 3;++i)
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
							LOG.info("Waiting for 10 minutes");
							try { Thread.sleep(10*60*1000);}
							catch(Throwable err2) {}
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
			String uri="http://api.twitter.com/1/friends/ids.xml?user_id="+
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
	
	private SocialGraph run(BigInteger userId)
		throws Exception
		{
		SocialGraph g=new SocialGraph();
		g.owner=userId;
		g.id2user.put(userId, getProfile(userId));
		
		Set<BigInteger> friendIds=listFriends(userId);
		for(BigInteger id:friendIds)
			{
			g.links.add(new Link(userId,id));
			}
		Collections.sort(g.links);
		
		for(BigInteger id: friendIds)
			{
			g.id2user.put(id, getProfile(id));
			Set<BigInteger> hisFriends=listFriends(id);
			hisFriends.retainAll(friendIds);
			for(BigInteger id2: hisFriends)
				{
				Link L=new Link(id, id2);
				int index= g.lowerBound(L);
				if(index==g.links.size())
					{
					g.links.add(L);
					continue;
					}
				Link L2= g.links.get(index);
				if(L2.compareTo(L)==0)
					{
					L2.one2two |= L.one2two ;
					L2.two2one |= L.two2one ;
					}
				else
					{
					g.links.add(index, L);
					}
				}
			}
		
		return g;
		}
	
	public static void main(String[] args)
		{
		//id: 7431072
		//args=new String[]{"-o","/home/pierre/jeter.dot","7431072"};
		File fileout=null;
		TwitterGraph app=null;
		try
			{
			app=new TwitterGraph();
			int optind=0;
			while(optind< args.length)
				{
				if(args[optind].equals("-h") ||
				   args[optind].equals("-help") ||
				   args[optind].equals("--help"))
					{
					System.err.println("Options:");
					System.err.println(" -h help; This screen.");
					System.err.println(" -o <fileout> suffix required: png or jpg or jpeg");
					System.err.println("[screen-id]");
					return;
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
			
			BigInteger userId= new BigInteger(args[optind++]);
			SocialGraph g=app.run(userId);
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
