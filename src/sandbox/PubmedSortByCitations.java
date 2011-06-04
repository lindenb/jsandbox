/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	Jun-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * Reference:
 *   
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Wiki
 *  
 * Motivation:
 * 	Sort Pubmed records by number of citations
 * Compilation:
 *       ant pubmedsortbycitations
 *       java -jar dist/pubmedsortbycitations.jar -c -L ALL -e '"Nucleic Acids Res"[JOUR] "Database issue"[ISS] 2005[PDAT]' > sorted.xml
 *       java -jar dist/pubmedsortbycitations.jar -c -L ALL pubmed_result.txt > sorted.xml
 */
package sandbox;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
/**
 * 
 * PubmedSortByCitations
 *
 */
public class PubmedSortByCitations
	{
	static private final Logger LOG=Logger.getLogger("pubmed.sort.by.citations");
	private DocumentBuilder builder;
	private XPath xpath;
	private boolean createNodes=false;
	private String email="me@nowhere.com";
	private static class ArticleNode
		implements Comparable<ArticleNode>
		{
		Element node;
		Set<Integer> cited=new TreeSet<Integer>();
		@Override
		public int compareTo(ArticleNode o)
			{
			return o.cited.size()-this.cited.size();
			}
		}
	
	private PubmedSortByCitations() throws Exception
		{
		XPathFactory xpathFactory=XPathFactory.newInstance();
		this.xpath=xpathFactory.newXPath();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		factory.setCoalescing(true);
		factory.setIgnoringComments(true);
		factory.setValidating(false);
		factory.setIgnoringElementContentWhitespace(true);
        this.builder = factory.newDocumentBuilder();
        this.builder.setEntityResolver(new EntityResolver()
			{
			@Override
			public InputSource resolveEntity(String publicId, String systemId)
					throws SAXException, IOException
				{
				return new InputSource(new StringReader(""));
				}
			});


		}
	
	private void countCitations(ArticleNode article)  throws Exception
		{
		String pmid=(String)this.xpath.evaluate("MedlineCitation/PMID",article.node,XPathConstants.STRING);
		if(pmid==null || pmid.isEmpty()) throw new IllegalArgumentException("Cannot find <PMID> in "+article.node);
		String uri="http://eutils.ncbi.nlm.nih.gov/entrez/eutils/elink.fcgi?" +
			"retmode=xml"+ 
			"&dbfrom=pubmed"+
			"&tool="+URLEncoder.encode(this.getClass().getSimpleName(), "UTF-8")+
			"&id="+URLEncoder.encode(pmid, "UTF-8")+
			"&cmd=neighbor" +
			"&email="+URLEncoder.encode(email, "UTF-8");
		LOG.info("parsing "+uri);
		Document dom=this.builder.parse(uri);
		NodeList L=(NodeList)this.xpath.evaluate("/eLinkResult/LinkSet/LinkSetDb[LinkName='pubmed_pubmed_citedin' and DbTo='pubmed']/Link/Id", dom, XPathConstants.NODESET);
		
		for(int i=0;i< L.getLength();++i)
			{
			article.cited.add(Integer.parseInt(L.item(i).getTextContent()));
			}
		if(LOG.getLevel()!=Level.OFF)
			{
			LOG.info(pmid+" cited in "+article.cited);
			}
		}
	
	private void run(Document dom) throws Exception
		{
		if(dom==null) throw new NullPointerException("no dom");
		Element root=dom.getDocumentElement();
		if(root==null) throw new NullPointerException("no root");
		if(!root.getNodeName().equals("PubmedArticleSet")) throw new IllegalArgumentException("not a <PubmedArticleSet> node");
		List<ArticleNode> articles=new ArrayList<ArticleNode>();
		for(Node n1=root.getFirstChild();n1!=null;n1=n1.getNextSibling())
			{
			if(n1.getNodeType()!=Node.ELEMENT_NODE) continue;
			Element e1=Element.class.cast(n1);
			if(e1.getNodeName().equals("PubmedArticle"))
				{
				ArticleNode article=new ArticleNode();
				article.node=e1;
				articles.add(article);
				}
			}
		for(ArticleNode article:articles)
			{
			root.removeChild(article.node);
			countCitations(article);
			}
		Collections.sort(articles);
		for(ArticleNode article:articles)
			{
			if(this.createNodes)
				{
				Element CitedBy=dom.createElement("CitedBy");
				CitedBy.setAttribute("count", String.valueOf(article.cited.size()));
				for(Integer pmid:article.cited)
					{
					Element PMID=dom.createElement("PMID");
					PMID.appendChild(dom.createTextNode( String.valueOf(pmid)));
					CitedBy.appendChild(PMID);
					}
				article.node.appendChild(CitedBy);
				}
			root.appendChild(article.node);
			}
		//echo result
		
		TransformerFactory factory=TransformerFactory.newInstance();
		Transformer transformer=factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT,"yes");
		transformer.transform(new DOMSource(dom), new StreamResult(System.out));
		}
	public static void main(String[] args) {
		try
			{
			String query=null;
			LOG.setLevel(Level.OFF);
			PubmedSortByCitations app=new PubmedSortByCitations();
			int optind=0;
			while(optind< args.length)
				{
				if(args[optind].equals("-h") ||
				   args[optind].equals("-help") ||
				   args[optind].equals("--help"))
					{
					System.err.println("Pierre Lindenbaum PhD. 2011");
					System.err.println("Options:");
					System.err.println(" -m <email> default:"+app.email);
					System.err.println(" -e <query> (optional)");
					System.err.println(" -c create some XML nodes containing the pmids citing the article");
					System.err.println(" -L <level: one of java.util.logging.Level> (optional)");
					System.err.println(" -h help; This screen.");
					System.err.println("(stdin|file|uri) pubmed xml document (if query is not defined).");
					return;
					}
				else if(args[optind].equals("-m"))
					{
					app.email=args[++optind];
					}
				else if(args[optind].equals("-e"))
					{
					query=args[++optind];
					}
				else if(args[optind].equals("-c"))
					{
					app.createNodes=!app.createNodes;
					}
				else if(args[optind].equals("-L"))
					{
					LOG.setLevel(Level.parse(args[++optind]));
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
			if(args.length==optind && query!=null)
				{
				String uri="http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term="+
					URLEncoder.encode(query, "UTF-8")+	
					"&retstart=0" +
					"&retmax=0" +
					"&usehistory=y" +
					"&retmode=xml" +
					"&email=" +URLEncoder.encode(app.email,"UTF-8")+
					"&tool="+URLEncoder.encode(PubmedSortByCitations.class.getName(),"UTF-8")
					;
				LOG.info(uri);
				Document dom=app.builder.parse(uri);
				String QueryKey=app.xpath.evaluate("/eSearchResult/QueryKey", dom);
				String WebEnv=app.xpath.evaluate("/eSearchResult/WebEnv", dom);
				int count=Integer.parseInt(app.xpath.evaluate("/eSearchResult/Count", dom));
				
				uri="http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed" +
					"&WebEnv="+URLEncoder.encode(WebEnv,"UTF-8")+
					"&query_key="+URLEncoder.encode(QueryKey,"UTF-8")+
					"&retmode=xml&retmax="+count+
					"&email="+URLEncoder.encode(app.email,"UTF-8")+
					"&tool="+URLEncoder.encode(PubmedSortByCitations.class.getName(),"UTF-8")
					;
				LOG.info(uri);
				app.run(app.builder.parse(uri));
				}
			else if(args.length!=optind && query!=null)
				{
				System.err.println("Query and file both defined.");
				}
			else if(args.length==optind)
				{
				app.run(app.builder.parse(System.in));
				}
			else if(optind+1==args.length)
				{
				String uri=args[optind++];
				LOG.info("parsing "+uri);
				if(	uri.startsWith("http://") ||
					uri.startsWith("https://") ||
					uri.startsWith("ftp://"))
					{
					app.run(app.builder.parse(uri));
					}
				else
					{
					app.run(app.builder.parse(new File(uri)));
					}
				}
			else
				{
				System.err.println("Illegal number of arguments.");
				}
			} 
		catch(Throwable err)
			{
			err.printStackTrace();
			}
		}
	}
