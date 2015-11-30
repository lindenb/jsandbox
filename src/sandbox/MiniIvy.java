package sandbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MiniIvy
	{
	private XPath xpath=null;
	private Map<Dependency,Dependency> dep2deps = new HashMap<Dependency,Dependency>();
	private Map<String, Set<Dependency>> target2dependencies = new HashMap<String, Set<Dependency>>();
	private static final String MAVEN4_NS="http://maven.apache.org/POM/4.0.0";
	
	private class Dependency
		{
		String group;
		String artifactId;
		String revision;
		Set<Dependency> dependencies = null;
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + group.hashCode();
			result = prime * result + artifactId.hashCode();
			result = prime * result + revision.hashCode();
			return result;
			}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Dependency other = (Dependency) obj;
			if (!artifactId.equals(other.artifactId))
				return false;
			if (!group.equals(other.group))
				return false;
			if (!revision.equals(other.revision))
				return false;
			return true;
			}
		
		private String getBaseUrl()
		{
		return "http://central.maven.org/maven2/"+
				this.group.replace('.', '/') +"/"+
				this.artifactId;
				
		}
		
		private String getUrl()
			{
			return  getBaseUrl()+"/"+revision+"/"+artifactId+"-"+
					this.revision
					;
			}
		private String getMetaDataUrl()
			{
			return  getBaseUrl()+"/maven-metadata.xml"
					;
			}
		
		
		@Override
		public String toString() {
			return this.group+":"+this.artifactId+":"+this.revision;
			}
		public String getPomUrl()
			{
			return getUrl()+".pom";
			}
		
		
		public String getFile()
			{
			return "$(lib.dir)/"+ this.group.replace('.', '/') +"/"+
					this.artifactId+"/"+revision+"/"+artifactId+"-"+
					this.revision+".jar";
			}
		
		private Node _fixns(Document dom,Node root)
			{
			if(root.getNodeType()==Node.ELEMENT_NODE)
				{
				Element e=dom.createElementNS(MAVEN4_NS, root.getLocalName());
				if(e.hasAttributes())
					{
					NamedNodeMap nn= e.getAttributes();
					for(int i=0;i< nn.getLength();++i)
						{
						e.setAttributeNode((Attr)nn.item(i));
						}
					}
				Node c=root.getFirstChild();
				while(c!=null)
					{
					Node next=c.getNextSibling();
					e.appendChild(_fixns(dom,c));
					c=next;
					}
				return e;
				}
			
			return root;
			}

		
		private Document fixNamespace(Document dom)
			{
			Element root=dom.getDocumentElement();
			if(!MAVEN4_NS.equals(root.getNamespaceURI()))
				{
				dom.removeChild(root);
				dom.appendChild(_fixns(dom,root));
				}
			return dom;
			}
		
		private Set<Dependency> resolve() throws IOException
			{
			try {
				if(this.dependencies!=null) return this.dependencies;
				this.dependencies=new HashSet<>();
				this.dependencies.add(this);
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				DocumentBuilder db= dbf.newDocumentBuilder();
				System.err.println("resolving "+getPomUrl());
				Document dom = fixNamespace(db.parse(getPomUrl()));
				NodeList nl=(NodeList)xpath.evaluate("/pom:project/pom:dependencies/pom:dependency", dom, XPathConstants.NODESET);
				for(int i=0;i< nl.getLength();++i)
					{
					Element E=(Element)nl.item(i);
					if("test".equals(xpath.evaluate("pom:scope/text()", E, XPathConstants.STRING)))
						continue;
					if("provided".equals(xpath.evaluate("pom:scope/text()", E, XPathConstants.STRING)))
						continue;
					if("true".equals(xpath.evaluate("pom:optional/text()", E, XPathConstants.STRING)))
						continue;
					
					Dependency dep = new Dependency();
					dep.group = (String)xpath.evaluate("pom:groupId/text()", E, XPathConstants.STRING);
					dep.artifactId = (String)xpath.evaluate("pom:artifactId/text()", E, XPathConstants.STRING);
					dep.revision = (String)xpath.evaluate("pom:version/text()", E, XPathConstants.STRING);
				
					if(dep.group.equals("${project.groupId}"))
						{
						dep.group =  (String)xpath.evaluate("/pom:project/pom:groupId/text()", E, XPathConstants.STRING);
						if(dep.group.isEmpty()) dep.group = "${project.parent.groupId}";
						}

					if(dep.group.equals("${project.parent.groupId}"))
						{
						dep.group =  (String)xpath.evaluate("/pom:project/pom:parent/pom:groupId/text()", E, XPathConstants.STRING);
						}
					
					if(dep.revision.equals("${project.version}"))
						{
						dep.revision = (String)xpath.evaluate("/pom:project/pom:version/text()", E, XPathConstants.STRING);
						dep.revision=dep.revision.trim();
						if(dep.revision.isEmpty()) dep.revision = "${project.parent.version}";
						}
					
					if(dep.revision.equals("${project.parent.version}"))
						{
						dep.revision = (String)xpath.evaluate("/pom:project/pom:parent/pom:version/text()", E, XPathConstants.STRING);
						dep.revision = dep.revision.trim();
						}
					

					if(dep.revision!=null && dep.revision.startsWith("${") && dep.revision.endsWith("}"))
						{/*
						String key=dep.revision.substring(2,dep.revision.length()-2);
						
						System.err.println("BOUM "+dep.revision);
						dbf = DocumentBuilderFactory.newInstance();
						dbf.setNamespaceAware(false);
						System.err.println("getting "+dep.getMetaDataUrl());
						Document dom2 = db.parse(dep.getMetaDataUrl());*/
						dep.revision=null;
						}
					
					if(dep.revision==null || dep.revision.trim().isEmpty())
						{
						dbf = DocumentBuilderFactory.newInstance();
						dbf.setNamespaceAware(false);
						System.err.println("getting "+dep.getMetaDataUrl());
						Document dom2 = db.parse(dep.getMetaDataUrl());
						dep.revision =(String)xpath.evaluate("/metadata/versioning/release/text()", dom2, XPathConstants.STRING);
						if(dep.revision==null || dep.revision.trim().isEmpty())
						   dep.revision =(String)xpath.evaluate("/metadata/versioning/versions/version/text()", dom2, XPathConstants.STRING);
						}
					
					if(dep.revision.equals("${project.version}"))
						{
						dep.revision  = (String)xpath.evaluate("/pom:project/pom:parent/pom:version/text()", dom, XPathConstants.STRING);
						}
					
					if(MiniIvy.this.dep2deps.containsKey(dep))
						{
						dep = dep2deps.get(dep);
						}
					else
						{
						dep2deps.put(dep,dep);
						}
					this.dependencies.addAll(dep.resolve());
					}	
				return this.dependencies;
			} catch (Exception e) {
				throw new RuntimeException(e);
				}
			}
		
		}

	

	private MiniIvy() 
		{
		}


	public int instanceMain(String[] args)
		{
		int optind=0;
		BufferedReader r = null;
		try {
			if(args.length==optind)
				{
				r = new BufferedReader(new InputStreamReader(System.in));
				}
			else if(optind+1==args.length)
				{
				r = new BufferedReader(new FileReader(new File(args[optind])));
				}
			else
				{
				System.err.println("Illegal number of arguments.");
				return -1;
				}
			
			this.xpath = XPathFactory.newInstance().newXPath();
			this.xpath.setNamespaceContext(new NamespaceContext() {
				
				@Override
				public Iterator<?> getPrefixes(String namespaceURI)
					{
					return Collections.singleton("pom").iterator();
					}
				
				@Override
				public String getPrefix(String namespaceURI)
					{
					if(namespaceURI.equals("http://maven.apache.org/POM/4.0.0")) return "pom";
					return null;
				}
				
				@Override
				public String getNamespaceURI(String prefix) {
					if(prefix.equals("pom")) return MAVEN4_NS;
					return null;
				}
			});
			
			Pattern ws=Pattern.compile("[\\s]+");
			String line;
			while((line=r.readLine())!=null)
				{
				if(line.isEmpty() || line.startsWith("#")) continue;
				String tokens[]= ws.split(line);
				if(tokens.length<=1) continue;
				if(target2dependencies.containsKey(tokens[0]))
					{
					System.err.println("Duplicate key:"+tokens[0]);
					return -1;
					}
				Set<Dependency> deps= new HashSet<>();
				for(int i=1;i< tokens.length;++i)
					{
					Pattern colon=Pattern.compile("[\\:]");
					String token2s[] =colon.split(tokens[i]);
					if(token2s.length<2) throw new IllegalArgumentException("Expected 3 tokens in "+tokens[i]);
					Dependency dep = new Dependency();
					dep.group = token2s[0];
					dep.artifactId = token2s[1];
					dep.revision = token2s[2];
					
					if(this.dep2deps.containsKey(dep))
						{
						dep = dep2deps.get(dep);
						}
					else
						{
						dep2deps.put(dep,dep);
						}
					deps.addAll(dep.resolve());
					
					}
				this.target2dependencies.put(tokens[0], deps);
				}
			r.close();
			int n=0;

			
			for(String k : this.target2dependencies.keySet())
				{
				System.out.print(k);
				System.out.print("  = ");
				for(Dependency dep: this.target2dependencies.get(k))
					{
					System.out.print(n>1?" \\\n\t":" "); 
					System.out.print(dep.getFile());
					n++;
					}
				System.out.println();
				}
			System.out.print("all_maven_jars = $(sort ");
			for(String k : this.target2dependencies.keySet())
				{
				System.out.print(" ${"+k+"}");
				}
			System.out.println(")");
			
			

			for(String k : this.target2dependencies.keySet())
				{
				System.out.print("${"+k+"} ");
				}
			System.out.println(" : ");		
			System.out.println("\tmkdir -p $(dir $@) && wget -O \"$@\" \"http://central.maven.org/maven2/$(patsubst ${lib.dir}/%,%,$@)\"");
			return 0;
			} 
		catch (Exception e)
			{
			e.printStackTrace();
			return -1;
			}
		}
	
	
	public static void main(String[] args)
		{
		int ret=new MiniIvy().instanceMain(args);
		System.exit(ret);
		}
	}
