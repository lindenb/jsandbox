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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TwitterMosaic
	{
	private static final int ICON_SIZE=48;
	private static final Logger LOG=Logger.getLogger("sandbox.TwitterMosaic");
	private String what="friends";

	private static class User
		{
		@SuppressWarnings("unused")
		BigInteger id;
		@SuppressWarnings("unused")
		String name;
		String screenName;
		String imageUrl;
		@Override
		public String toString()
			{
			return screenName;
			}
		}

	private List<User> users=new ArrayList<User>();

	private TwitterMosaic()
		{

		}

	private BufferedImage createImage()
		throws IOException
		{
		int cols=(int)Math.ceil(Math.sqrt(this.users.size()));
		int rows=(int)Math.ceil(this.users.size()/(double)cols);
		BufferedImage img=new BufferedImage(
				cols*ICON_SIZE,
				rows*ICON_SIZE,
				BufferedImage.TYPE_INT_RGB);
		int x=0;
		int y=0;
		Graphics2D g=Graphics2D.class.cast(img.getGraphics());
		for(User u:this.users)
			{
			LOG.info(u.imageUrl);
			try
				{
				BufferedImage icn=ImageIO.read(new URL(u.imageUrl));
				g.drawImage(icn, x*ICON_SIZE, y*ICON_SIZE, null);
				}
			catch(java.awt.color.CMMException err)
				{
				LOG.severe(err.getMessage());
				}
			catch(javax.imageio.IIOException err)
				{
				LOG.severe(err.getMessage());
				}

			x++;
			if(x==cols)
				{
				x=0;
				y++;
				}
			}
		g.dispose();
		return img;
		}

	private void run(BigInteger userId)
		throws Exception
		{
		DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
		f.setCoalescing(true);
		f.setNamespaceAware(false);
		f.setValidating(false);
		f.setExpandEntityReferences(true);
		f.setIgnoringComments(false);
		f.setIgnoringElementContentWhitespace(true);
		DocumentBuilder docBuilder= f.newDocumentBuilder();
		XPathFactory xpathFactory=XPathFactory.newInstance();
		XPath xpath=xpathFactory.newXPath();
		XPathExpression usersXpath=xpath.compile("/users_list/users/user");
		XPathExpression nextCursorXPath=xpath.compile("/users_list/next_cursor");
		BigInteger cursor=BigInteger.ONE.negate();
		for(;;)
			{
			String uri="http://api.twitter.com/1/statuses/"+this.what+".xml?user_id="+
						userId+"&cursor="+cursor;
			LOG.info(uri);
			Document dom=docBuilder.parse(
				uri
				);
			NodeList nodeList=(NodeList)usersXpath.evaluate(dom, XPathConstants.NODESET);
			for(int i=0;i< nodeList.getLength();++i)
				{
				Element e1=(Element)nodeList.item(i);
				User user=new User();
				user.id=new BigInteger(xpath.evaluate("id", e1));
				user.name=xpath.evaluate("name", e1);
				user.screenName=xpath.evaluate("screen_name", e1);
				user.imageUrl=xpath.evaluate("profile_image_url", e1);

				this.users.add(user);
				LOG.info(user.screenName+"("+this.users.size()+")");
				}
			String s=(String)nextCursorXPath.evaluate(dom, XPathConstants.STRING);
			if(s==null || s.equals("0"))
				{
				break;
				}
			cursor=new BigInteger(s);
			}
		}

	public static void main(String[] args)
		{
		try
			{
			File fileout=null;
			TwitterMosaic app=new TwitterMosaic();
			int optind=0;
			while(optind< args.length)
				{
				if(args[optind].equals("-h") ||
				   args[optind].equals("-help") ||
				   args[optind].equals("--help"))
					{
					System.err.println("Options:");
					System.err.println(" -h help; This screen.");
					System.err.println(" -d look for friends (default).");
					System.err.println(" -w look for followers.");
					System.err.println(" -o <fileout> suffix required: png or jpg or jpeg");
					System.err.println("[user-id]");
					return;
					}
				else if(args[optind].equals("-o"))
					{
					fileout=new File(args[++optind]);
					}
				else if(args[optind].equals("-d"))
					{
					app.what="friends";
					}
				else if(args[optind].equals("-w"))
					{
					app.what="followers";
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
				System.err.println("User ID missing");
				System.exit(-1);
				}
			if(fileout==null)
				{
				System.err.println("option -o <fileout> missing");
				System.exit(-1);
				}

			BigInteger userId=new BigInteger(args[optind++]);
			app.run(userId);

			if(fileout.getName().toLowerCase().endsWith(".png"))
				{
				ImageIO.write(app.createImage(), "PNG", fileout);
				}
			else if(fileout.getName().toLowerCase().endsWith(".jpeg") ||
			   fileout.getName().toLowerCase().endsWith(".jpg"))
				{
				ImageIO.write(app.createImage(), "JPG", fileout);
				}
			else
				{
				System.err.println("Unknown file format");
				}
			}
		catch(Throwable err)
			{
			err.printStackTrace();
			}
		}
	}
