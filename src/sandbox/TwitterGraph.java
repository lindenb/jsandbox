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
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;


import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;



public class TwitterGraph
	{
	private static final Logger LOG=Logger.getLogger("sandbox.TwitterMosaic");

	private static class User
		{
		BigInteger id;
		String name;
		String screenName;
		String imageUrl;
		}
	
	private static class Link
		{
		BigInteger id1;
		BigInteger id2;
		Link(BigInteger id1,BigInteger id2)
			{
			this.id1=id1;
			this.id2=id2;
			}
		@Override
		public int hashCode()
			{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id1 == null) ? 0 : id1.hashCode());
			result = prime * result + ((id2 == null) ? 0 : id2.hashCode());
			return result;
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
		List<User> friends;
		Set<Link> links;
		
		public void toDot(PrintWriter out)
			{
			out.println("digraph G {");
			for(Link L:links)
				{
				out.println("n"+L.id1+" -> n"+L.id2+";");
				}
			out.println("}");
			}
		}
	
	private TwitterGraph()
		{
		
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
		InputStream in=null;
		for(int i=0;i< 3;++i)
			{
			try
				{
				URL url=new URL(uri);
				URLConnection con=url.openConnection();
				con.setConnectTimeout(10*1000);
				in=url.openStream();
				return in;
				}
			catch(IOException err)
				{
				System.err.println("Trying... "+err.getClass().getCanonicalName()+":"+err.getMessage());
				in=null;
				try { Thread.sleep(5*1000);}
				catch(Throwable err2) {}
				}
			}
		return null;
		}
	private List<User> friends(BigInteger userId)
		throws Exception
		{			
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
		xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
		xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
		 List<User> friends=new ArrayList<User>();
		
		BigInteger cursor=BigInteger.ONE.negate();
		for(;;)
			{
			String uri="http://api.twitter.com/1/statuses/friends.xml?user_id="+
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
				if(localName.equals("user"))
					{
					friends.add(parseUser(reader));
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
		LOG.info("count friends:"+friends.size());
		return friends;
		}
	
	private SocialGraph run(BigInteger userId)
		throws Exception
		{
		SocialGraph g=new SocialGraph();
		g.friends= friends(userId);
		Set<BigInteger> friendIds=new HashSet<BigInteger>(g.friends.size()+1);
		g.links=new HashSet<Link>(10000);
		
		friendIds.add(userId);
		for(User f:g.friends)
			{
			g.links.add(new Link(userId,f.id));
			friendIds.add(f.id);
			}
		
		for(User f:g.friends)
			{
			List<User> friendOfAFriend= friends(f.id);
			for(User f2:friendOfAFriend)
				{
				if(!friendIds.contains(f2.id)) continue;
				g.links.add(new Link(f.id,f2.id));
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
