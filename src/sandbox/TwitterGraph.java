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
 *  make a mosaic of twitter friends/follower
 * Compilation:
 *        cd jsandbox; ant twittermosaic
 */
package sandbox;


import java.io.File;
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
		String name;
		String screenName;
		String imageUrl;
		}
	
	private static class Link
		implements Comparable<Link>
		{
		BigInteger id1;
		BigInteger id2;
		byte count=1;
		Link(BigInteger id1,BigInteger id2)
			{
			if(id1.compareTo(id2)<0)
				{
				this.id1=id1;
				this.id2=id2;
				}
			else
				{
				this.id1=id2;
				this.id2=id1;
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
		
		public void toGexf(OutputStream out)
			throws IOException,XMLStreamException
			{
			XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
			XMLStreamWriter w= xmlfactory.createXMLStreamWriter(out,"UTF-8");
			w.writeStartDocument("UTF-8","1.0");
			w.writeStartElement("gexf");
			w.writeAttribute("xmlns", "http://www.gexf.net/1.2draft");
			w.writeAttribute("version", "1.2");
			
			w.writeStartElement("graph");
			w.writeAttribute("mode", "static");
			w.writeAttribute("defaultedgetype", "directed");
			
			w.writeStartElement("attributes");
			w.writeAttribute("class","nodes");
			
			w.writeEmptyElement("attribute");
			w.writeAttribute("type","string");
			w.writeAttribute("title","name");
			w.writeAttribute("id","twitterName");
			
			
			w.writeEndElement();
			
			w.writeStartElement("nodes");
			
			for(User u:id2user.values())
				{
				w.writeEmptyElement("node");
				w.writeAttribute("id", u.id.toString());
				w.writeAttribute("label", u.screenName);
				}
			
			w.writeEndElement();
			
			w.writeStartElement("edges");
			for(int i=0;i< links.size();++i)
				{
				w.writeEmptyElement("edge");
				w.writeAttribute("id", "E"+(i+1));
				w.writeAttribute("source", links.get(i).id1.toString());
				w.writeAttribute("target", links.get(i).id2.toString());
				}

			w.writeEndElement();
			
			w.writeEndElement();

			w.writeEndElement();
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
				
				if(depth==1 && localPart.equals("id"))
					{
					u.id=new BigInteger(reader.getElementText());
					}
				else if(depth==1 && localPart.equals("profile_image_url"))
					{
					u.imageUrl=reader.getElementText();
					}
				else if(depth==1 && localPart.equals("name"))
					{
					u.name=reader.getElementText();
					}
				else if(depth==1 && localPart.equals("screen_name"))
					{
					u.screenName=reader.getElementText();
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
					L2.count++;
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
		args=new String[]{"-o","/home/pierre/jeter.dot","7431072"};
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
