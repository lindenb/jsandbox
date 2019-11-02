package sandbox.feed;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import com.beust.jcommander.Parameter;

import sandbox.IOUtils;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.date.DateParser;



public class AtomMerger extends Launcher
	{
	private static final Logger LOG=Logger.builder(AtomMerger.class).build();

	
	@Parameter(names={"-o","--out"},description="output file")
	private File output;
	@Parameter(names={"-L","--limit"},description="limit number of items (-1: no limit)")
	private int limitEntryCount=-1;
	@Parameter(names={"--24"},description="limit to last 24 hours")
	private boolean lessThan24H00Flag=false;

	
	private static class DateExtractor implements Function<Node, Date> {
		private final DateParser dateParser = new DateParser();

		@Override
		public Date apply(Node o1)
			{
			String updated=null;

			for (Node c = o1.getFirstChild(); c != null; c = c.getNextSibling()) {
				if (c.getNodeType() == Node.ELEMENT_NODE && c.getLocalName().equals("updated")) {
					updated = c.getTextContent().replaceAll(" PST","");
					break;
					}
				}
				
			if(updated==null) {
				LOG.info("Cannot get date in this entry");
				return null;
				}
			
			final Optional<Date> optDate = this.dateParser.apply(updated);
			if(optDate.isPresent()) return optDate.get();
			LOG.info("bad date format : "+updated);
			return null;
			}
		}
	
	private static class EntrySorter implements Comparator<Node>
		{
		private final DateExtractor dateExtractor = new DateExtractor();
		private final Date defaultDate= new Date();
		EntrySorter() {
			}
		private Date getDate(final Node o1) {
			final Date updated= this.dateExtractor.apply(o1);
			return updated==null?this.defaultDate:updated;
			}
		@Override
		public int compare(final Node o1,final Node o2) {
			return getDate(o2).compareTo(getDate(o1));
			}
		}
	
	private boolean ignoreErrors=false;
	
		
	private static boolean isAtom(final Document dom)
		{
		final Element root= dom.getDocumentElement();
		return root!=null && root.getLocalName().equals("feed") && root.getNamespaceURI().equals(Atom.NS);
		}

	private static String getId(final Element root)
		{
		String t=null;
		for(Node c2= root.getFirstChild();c2!=null;c2=c2.getNextSibling())
           {
           if(c2.getNodeType()!=Node.ELEMENT_NODE) continue;
           if(!c2.getLocalName().equals("id")) continue;
           t  = c2.getTextContent().trim();
           break;
           }
		return t;
		}

	
	@Override
	public int doWork(final List<String> args)
		{
		if(args.isEmpty())
			{
			System.err.println("Empty input. Double colon '--' missing ?");
			return -1;
			}
		try {
			final Set<String> paths = new LinkedHashSet<>();
			paths.addAll(IOUtils.expandList(args));
			paths.remove("");
		
			final Set<String> seenids = new HashSet<>();
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			Document outdom = db.newDocument();
			Element feedRoot = outdom.createElementNS(Atom.NS,"feed");
			outdom.appendChild(feedRoot);
			
			Element c = outdom.createElementNS(Atom.NS,"title");
			c.appendChild(outdom.createTextNode("AtomMerger"));
			feedRoot.appendChild(c);
			
			c = outdom.createElementNS(Atom.NS,"updated");
			c.appendChild(outdom.createTextNode(
					new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z").format(new Date())
					));
			feedRoot.appendChild(c);
			
			List<Node> chilrens = new ArrayList<>();
			
			for(final String path:paths)
				{
				if(path.isEmpty()) continue;
				LOG.info("Parsing atom "+ path);
				Document dom = null;
				try {
					try 
						{
						dom  = (
								path.equals("-")?
								db.parse(System.in):
								db.parse(path)
								);
						}
					catch(final Exception err)
						{
						err.printStackTrace();
						dom=null;
						}
					finally
						{
						}
					if(dom==null) continue;
					
					if(!isAtom(dom))
						{
						LOG.error("Not root atom or rss for "+path);
						continue;
						}
					
					
					final Element root= dom.getDocumentElement();

					for(Node c1= root.getFirstChild();c1!=null;c1=c1.getNextSibling())
						{
						if(c1.getNodeType()!=Node.ELEMENT_NODE) continue;
						if(!c1.getLocalName().equals("entry")) continue;
						
						
						if(this.lessThan24H00Flag) {
							final DateExtractor dateExtractor = new DateExtractor();
							final Date date = dateExtractor.apply(c1);
							if( date == null ) continue;
							final Date today = new Date();
							final long diff = today.getTime() - date.getTime();
							if( diff > (24L*60L*60L*1000L) ) {
								continue;
								}
							}
						
						final String id = getId(Element.class.cast(c1));
						
						if(id==null || seenids.contains(id)) continue;	
						
						
						seenids.add(id);
						
						chilrens.add(outdom.importNode(c1, true) );
						if(this.limitEntryCount!=-1)
							{
							Collections.sort(chilrens,new EntrySorter());
							while(chilrens.size()>this.limitEntryCount) 
								{
								chilrens.remove(chilrens.size()-1);
								}
							}
						
						}
				} catch(final Throwable err)
					{
					if(this.ignoreErrors) {
						LOG.warning("Ignore error" + err);
						}
					else
						{
						throw err;
						}
					}
				}
			
			Collections.sort(chilrens,new EntrySorter());
			if(this.limitEntryCount!=-1)
				{
				while(chilrens.size()>this.limitEntryCount) 
					{
					chilrens.remove(chilrens.size()-1);
					}
				}
			
			for(final Node n:chilrens)
				{
				feedRoot.appendChild(n);
				}
			
			final TransformerFactory trf = TransformerFactory.newInstance();
			final Transformer tr = trf.newTransformer();
			tr.setOutputProperty(OutputKeys.INDENT,"yes");
			tr.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
			tr.transform(new DOMSource(outdom),
					this.output==null?
							new StreamResult(System.out):
							new StreamResult(this.output)
					);
			return 0;
		} catch (final Throwable e) {
			e.printStackTrace();
			return -1;
		} finally {
		}
		
		}
	


	
	public static void main(String[] args)
		{
		System.setProperty("http.agent",IOUtils.getUserAgent());
		new AtomMerger().instanceMainWithExit(args);
		}
	
	
	}
