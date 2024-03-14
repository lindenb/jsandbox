package sandbox.tools.xml2xsd;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	March-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * Reference:
 *   
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Motivation:
 * 	transform a XML document to XSD
 * Compilation & Run:
 *        cd jsandbox
 *        ant xml2xsd
 *        java -jar dist/xml2xsd.jar file.xml
 */
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.io.IOUtils;

public class XmlToXsd extends Launcher {
	private static final String XSD=XMLConstants.W3C_XML_SCHEMA_NS_URI;
	private static final String JXB="http://java.sun.com/xml/ns/jaxb";
	
	@Parameter(names= {"-jaxb","--jaxb"},description="add JAXB support")
	private boolean supportingJaxb=false;
	@Parameter(names= {"-package","--package"},description="package name for jaxb/java")
	private String packageName="generated";
	
	@Parameter(names= {"-java","--java"},description="export java code to this file",hidden=true)
	private Path exportJavaFile = null;

	
	private static class XsdNode
		{
		/** parent node */
		XsdNode parent;
		/** node name */
		String name;
		/** all the DOM elements for this kind of node */
		final List<Element> elements=new ArrayList<Element>();
		/** child nodes */
		final List<XsdNode> children=new ArrayList<XsdNode>();
		/** all the attributes for the elements */
		final Map<String,List<Attr>> attributes=new HashMap<String,List<Attr> >();
		
		/** finds a node by is tagName */
		XsdNode findNodeByName(String tagName)
			{
			for(int i=0;i< children.size();++i)
				{
				if(children.get(i).name.equals(tagName))
					{
					return children.get(i);
					}
				}
			return null;
			}
		
		/** main recursive function */
		void recurse(Element root)
			{
			NamedNodeMap atts=root.getAttributes();
			for(int i=0;i< atts.getLength();++i)
				{
				Attr att=(Attr)atts.item(i);
				if(att.getNamespaceURI()!=null || att.getNodeName().contains(":")) continue;
				List<Attr> L=this.attributes.get(att.getName());
				if(L==null)
					{
					L=new ArrayList<Attr>();
					this.attributes.put(att.getName(),L);
					}
				L.add(att);
				}
			
			
			for(Node n1=root.getFirstChild();n1!=null;n1=n1.getNextSibling())
				{
				if(n1.getNodeType()!=Node.ELEMENT_NODE) continue;
				Element e1=Element.class.cast(n1);
				String tagName=e1.getTagName();
				
				XsdNode childNode=findNodeByName(tagName);
				if(childNode==null)
					{
					childNode=new XsdNode();
					childNode.parent=this;
					childNode.name=tagName;
					this.children.add(childNode);
					}
				childNode.elements.add(e1);
				childNode.recurse(e1);
				}
			}
		
		private String getJavaType(List<?> domNodes) {
			String s= getXsdType(domNodes);
			if(s.equals("xs:string")) return "String";
			return "todo";
			}
		
		/** guess xsd type for a set of Nodes */
		private String getXsdType(List<?> domNodes)
			{
			boolean is_bigdecimal=true;
			boolean is_biginteger=true;
			boolean is_double=true;
			boolean is_float=true;
			boolean is_long=true;
			boolean is_int=true;
			boolean is_boolean=true;
			for(Object o:domNodes)
				{
				final String text=((Node)o).getTextContent();
				if(text==null) return null;
				if(is_double)
					{
					try {
						Double.parseDouble(text);
						} 
					catch (Exception e2) {
						is_double=false;
						}
					}
				if(is_float)
					{
					try {
						Float.parseFloat(text);
						} 
					catch (Exception e2) {
						is_float=false;
						}
					}
				if(is_long)
					{
					try {
						Long.parseLong(text);
						} 
					catch (Exception e2) {
						is_long=false;
						}
					}
				if(is_int)
					{
					try {
						Integer.parseInt(text);
						} 
					catch (Exception e2) {
						is_int=false;
						}
					}
				if(is_boolean && !(text.equals("true") || text.equals("false")))
					{
					is_boolean=false;
					}
				if(is_bigdecimal)
					{
					try {
						new BigDecimal(text);
						} 
					catch (Exception e2) {
						is_bigdecimal=false;
						}
					}
				if(is_biginteger)
					{
					try {
						new BigInteger(text);
						} 
					catch (Exception e2) {
						is_biginteger=false;
						}
					}
				}
			if(is_int) return "xsd:int";
			if(is_long) return "xsd:long";
			if(is_biginteger) return "xsd:integer";
			if(is_float) return "xsd:float";
			if(is_double) return "xsd:double";
			if(is_bigdecimal) return "xsd:decimal";
			if(is_boolean) return "xsd:bool";
			return "xsd:string";
			}
		
		String getJavaName() {
			return this.name;
			}
		
		void exportJava(PrintWriter w) {
			w.println("public class "+getJavaName()+"{");
			for(String attName:this.attributes.keySet())
				{
				w.println("private "+ getJavaType(this.attributes.get(attName))+" "+attName+";");
				}
			
			w.println("private void parse(XmlStreamReader r,StartElement startE) {");

			
			for(String attName:this.attributes.keySet())
				{
				
				}
			w.println("}");
			
			w.println("}");
			}
		
		private int[] getOccurrence() {
			int[] occ=new int[] {Integer.MAX_VALUE,1};
			if(parent!=null) {
				for(Element self:parent.elements)
					{
					int count=0;
					for(Node n1=self.getFirstChild();n1!=null;n1=n1.getNextSibling())
						{
						if(n1.getNodeType()!=Node.ELEMENT_NODE) continue;
						final Element e1=Element.class.cast(n1);
						if(!e1.getTagName().equals(this.name)) continue;
						++count;
						}
					occ[0] = Math.max(occ[0], count);
					occ[1] = Math.min(occ[1], count);
					}
				}
			return occ;
			}
		
		/** write to XSD */
		public void write(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeStartElement("xsd","element",XSD);
			w.writeAttribute("name", name);
			if(parent!=null)
				{
				final int[] occ = getOccurrence();
				if(occ[0]==0) w.writeAttribute("minOccurs", "0");
				if(occ[1]!=1) w.writeAttribute("maxOccurs", "unbounded");
				}
			
			if(this.children.isEmpty() && this.attributes.isEmpty())
				{
				w.writeAttribute("type", getXsdType(this.elements));
				}
			else
				{
				
				w.writeStartElement("xsd","complexType",XSD);
				if(!this.children.isEmpty())
					{
					w.writeStartElement("xsd","sequence",XSD);
					for(XsdNode n: children)
						{
						n.write(w);
						}
					w.writeEndElement();
					}
				
				
				for(String attName:this.attributes.keySet())
					{
					int required=1;
					for(Element self:this.elements)
						{
						Attr att= self.getAttributeNode(attName);
						if(att==null)
							{
							required=0;
							continue;
							}
						}
					w.writeStartElement("xsd","attribute",XSD);
					w.writeAttribute("name",attName);
					if(required==1) w.writeAttribute("use","required");
					w.writeAttribute("type",getXsdType(this.attributes.get(attName)));
					w.writeEndElement();
					}
				w.writeEndElement();
				}
			w.writeEndElement();
			w.writeCharacters("\n");
			}
		}

	public void parse(final Document dom) throws XMLStreamException,IOException
		{
		
		final XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
		final XMLStreamWriter w= xmlfactory.createXMLStreamWriter(System.out,"UTF-8");
		w.writeStartDocument("UTF-8","1.0");
		w.writeStartElement("xsd","schema",XSD);
		w.writeAttribute("xmlns", XMLConstants.XML_NS_URI, "xsd",XSD);
		if(supportingJaxb)
			{
			w.writeAttribute("xmlns", XMLConstants.XML_NS_URI, "jxb",JXB);
			w.writeAttribute("jxb", JXB, "version","2.0");
			}
		
		w.writeStartElement("xsd","annotation",XSD);
		w.writeStartElement("xsd","appinfo",XSD);
	    
		if(supportingJaxb)
			{
			w.writeEmptyElement("jxb","globalBindings",JXB);
			w.writeAttribute("collectionType", "java.util.ArrayList");
	
			w.writeStartElement("jxb","schemaBindings",JXB);
			w.writeEmptyElement("jxb","package",JXB);
			w.writeAttribute("name",this.packageName);
			w.writeEndElement();
			}
		
	    w.writeEndElement();
	    w.writeEndElement();
		
	    final Element root=dom.getDocumentElement();
	    final XsdNode node=new XsdNode();
		node.name=root.getTagName();
		node.elements.add(root);
		node.recurse(root);
		node.write(w);
		
		w.writeEndElement();
		w.writeEndDocument();
		w.flush();
		
		if(exportJavaFile!=null) {
			try(PrintWriter pw = IOUtils.openPathAsPrintWriter(this.exportJavaFile)) {
				pw.println("package "+this.packageName+";");
				pw.println("import javax.xml.namespace.QName;");
				pw.println("import javax.xml.stream.*;");
				node.exportJava(pw);
				pw.flush();
				}
			}
		
		}

	@Override
	public int doWork(final List<String> args) {
		try {	
			final DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
			factory.setCoalescing(true);
			factory.setExpandEntityReferences(true);
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			factory.setIgnoringElementContentWhitespace(true);
			final DocumentBuilder builder=factory.newDocumentBuilder();
			final Document dom;
			final String input = oneFileOrNull(args);
			if(input==null)
				{
				dom=builder.parse(System.in);
				}
			else
				{
				dom=builder.parse(new File(input));
				}
			this.parse(dom);
			return 0;
			}
		catch(Throwable error)
			{
			error.printStackTrace();
			return -1;
			}
		}

	public static void main(String[] args)
		{
		new XmlToXsd().instanceMainWithExit(args);
		}
	}
