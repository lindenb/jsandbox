package sandbox;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;


import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.JTextComponent;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.lindenb.njson.JSONParser;
import org.lindenb.njson.ObjectNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class RDF
	{
	public static final String NS="http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	}

class RDFS
	{
	public static final String NS="http://www.w3.org/2000/01/rdf-schema#";
	}

class OWL
	{
	public static final String NS="http://www.w3.org/2002/07/owl#";
	}

class XSD
	{
	public static final String NS="http://www.w3.org/2001/XMLSchema#";
	}

class MY
	{
	public static final String NS="urn:schema:";
	}

interface CloseableIterator<T>
	extends Iterator<T>
	{
	public void close();
	}

interface OntNode
	{
	public String getUri();
	public String getQName();
	public String getLocalName();
	public String getPrefix();
	public String getNamespaceUri();
	public String getLabel();
	public String getDescription();
	public OntModel getModel();
	}

enum PropertyType
	{
	STRING,LONG,DOUBLE,OBJECT
	}

interface OntProperty extends OntNode
	{
	public OntClass getOntClass();
	public Integer getMinCardinality();
	public Integer getMaxCardinality();
	public PropertyType getPropertyType();
	}


interface OntDataProperty extends OntProperty
	{
	public String getRangeUri();
	}
interface OntNumericProperty extends OntProperty {}

interface OntStringProperty
extends OntDataProperty
	{
	public Pattern getPattern();
	public List<String> getEnum();
	public boolean isMultiline();
	}

interface OntLongProperty
extends OntDataProperty
	{
	public  Long getMinInclusive();
	public  Long getMaxInclusive();
	}

interface OntDoubleProperty
extends OntDataProperty
	{
	public  Double getMinInclusive();
	public  Double getMaxInclusive();
	}

interface OntDateProperty
extends OntDataProperty
	{
	}


interface OntObjectProperty extends OntProperty
	{
	public Set<OntClass> getRange();
	}

interface OntClass extends OntNode
	{
	public List<OntProperty> getProperties();
	}

interface Instance
	{
	public boolean match(Pattern regex);
	}

interface InstanceOfProperty
	extends Instance
	{
	public OntProperty getOntProperty();
	public String getValue();
	public InstanceOfClass getInstance();
	}

interface InstanceOfClass
extends Instance
	{
	public String getId();
	public String getName();
	public String getDescription();
	public List<InstanceOfProperty> getProperties();
	public List<InstanceOfProperty> getProperties(OntProperty prop);
	public Store getDataStore();
	public OntClass getOntClass();
	}

interface OntModel
	{
	public List<OntClass> getOntClasses();
	public OntClass getOntClassByUri(String uri);
	public NamespaceContext getNamespaceContext();
	}


interface Store
	{
	public OntModel getOntModel();
	public InstanceOfClass getInstanceById(String id);
	public CloseableIterator<InstanceOfClass> listInstances();
	}



/** AbstractOntNode */
abstract class AbstractOntNode
	implements OntNode
	{
	private String uri;
	private String label;
	private String description;
	
	protected AbstractOntNode()
		{
		}
	
	@Override
	public String getUri()
		{
		return uri;
		}
	
	void setUri(String uri)
		{
		this.uri = uri;
		}
	public void setLabel(String label)
		{
		this.label = label;
		}
	
	@Override
	public String getLabel()
		{
		return label==null?getLocalName():this.label;
		}
	
	public void setDescription(String description)
		{
		this.description = description;
		}
	
	@Override
	public String getDescription()
		{
		return description==null?getLabel():description;
		}
	
	@Override
	public String getQName()
		{
		return getPrefix()+":"+getLocalName();
		}
	
	@Override
	public String getPrefix()
		{
		return getModel().getNamespaceContext().getPrefix(getNamespaceUri());
		}
	
	private int split()
		{
		int i=getUri().lastIndexOf('#');
		if(i==-1) i=getUri().lastIndexOf('/');
		if(i==-1) i=getUri().lastIndexOf(':');
		if(i==-1) throw new IllegalArgumentException("bad URI "+getUri());
		return i;
		}
	
	@Override
	public String getLocalName()
		{
		return getUri().substring(split()+1);
		}
	
	@Override
	public String getNamespaceUri()
		{
		return getUri().substring(0,split()+1);
		}
	
	@Override
	public boolean equals(Object obj)
		{
		if(obj==this) return true;
		if(obj==null  || obj.getClass()!=this.getClass()) return false;
		return getUri().equals(AbstractOntNode.class.cast(obj).getUri());
		}
	@Override
	public int hashCode()
		{
		return getUri().hashCode();
		}
	
	@Override
	public String toString()
		{
		return getQName();
		}
	}

/** OntClassImpl */
class OntClassImpl
	extends AbstractOntNode
	implements OntClass
	{
	private OntModel ontModel;
	private List<OntProperty> ontProperties=new ArrayList<OntProperty>();
	
	public OntClassImpl()
		{
		}
	
	@Override
	public OntModel getModel()
		{
		return this.ontModel;
		}

	void setModel(OntModel ontModel)
		{
		this.ontModel=ontModel;
		}
	
	void add(OntProperty prop)
		{
		this.ontProperties.add(prop);
		}
	
	@Override
	public List<OntProperty> getProperties()
		{
		return this.ontProperties;
		}
	}

/** OntPropertyImpl */
abstract class OntPropertyImpl
	extends AbstractOntNode
	implements OntProperty,Cloneable
	{
	private OntModel ontModel;
	private OntClass ontClass;
	private Integer minCardinality;
	private Integer maxCardinality;
	public OntPropertyImpl()
		{
		}
	
	@Override
	public OntModel getModel()
		{
		return ontModel;
		}

	@Override
	public OntClass getOntClass()
		{
		return ontClass;
		}

	public void setOntClass(OntClass ontClass)
		{
		this.ontClass = ontClass;
		}


	@Override
	public Integer getMinCardinality()
		{
		return this.minCardinality;
		}

	public void setMinCardinality(Integer minCardinality)
		{
		this.minCardinality = minCardinality;
		}
	
	@Override
	public Integer getMaxCardinality()
		{
		return this.maxCardinality;
		}
	
	public void setMaxCardinality(Integer maxCardinality)
		{
		this.maxCardinality = maxCardinality;
		}

	}


class OntStringPropertyImpl
	extends OntPropertyImpl
	implements OntStringProperty
	{
	private Pattern pattern;
	private List<String> enumeration=new ArrayList<String>();
	private boolean multiline=false;
	@Override
	public Pattern getPattern()
		{
		return pattern;
		}
	void setPattern(Pattern pattern)
		{
		this.pattern = pattern;
		}
	@Override
	public String getRangeUri()
		{
		return XSD.NS+"string";
		}
	@Override
	public List<String> getEnum()
		{
		return this.enumeration;
		}
	
	@Override
	public boolean isMultiline()
		{
		return multiline;
		}
	
	public void setMultiline(boolean multiline)
		{
		this.multiline = multiline;
		}

	void setEnumeration(List<String> enumeration)
		{
		this.enumeration.clear();
		this.enumeration.addAll(enumeration);
		}
	
	@Override
	public PropertyType getPropertyType()
		{
		return PropertyType.STRING;
		}
	}


class OntLongPropertyImpl
extends OntPropertyImpl
implements OntLongProperty
	{
	private Long minInclusive=null;
	private Long maxInclusive=null;
	@Override
	public Long getMinInclusive()
		{
		return minInclusive;
		}
	@Override
	public Long getMaxInclusive()
		{
		return maxInclusive;
		}
	void setMaxInclusive(Long maxInclusive)
		{
		this.maxInclusive = maxInclusive;
		}
	
	void setMinInclusive(Long minInclusive)
		{
		this.minInclusive = minInclusive;
		}
	@Override
	public String getRangeUri()
		{
		return XSD.NS+"int";
		}
	@Override
	public PropertyType getPropertyType()
		{
		return PropertyType.LONG;
		}
	}

/**
 * OntDoublePropertyImpl
 */
class OntDoublePropertyImpl
extends OntPropertyImpl
implements OntDoubleProperty
	{
	private Double minInclusive=null;
	private Double maxInclusive=null;
	@Override
	public Double getMinInclusive()
		{
		return minInclusive;
		}
	@Override
	public Double getMaxInclusive()
		{
		return maxInclusive;
		}
	void setMaxInclusive(Double maxInclusive)
		{
		this.maxInclusive = maxInclusive;
		}
	
	void setMinInclusive(Double minInclusive)
		{
		this.minInclusive = minInclusive;
		}
	@Override
	public String getRangeUri()
		{
		return XSD.NS+"double";
		}
	@Override
	public PropertyType getPropertyType()
		{
		return PropertyType.LONG;
		}
	}

class OntObjectPropertyImpl
extends OntPropertyImpl
implements OntObjectProperty
	{
	private Set<OntClass> range=new HashSet<OntClass>();
	@Override
	public Set<OntClass> getRange()
		{
		return this.range;
		}

	void setRange(Set<OntClass> range)
		{
		this.range.clear();
		this.range.addAll(range);
		}
	
	@Override
	public PropertyType getPropertyType()
		{
		return PropertyType.OBJECT;
		}
	}


class NamespaceContextImpl
	implements NamespaceContext
	{
	private Map<String, String> uri2prefix=new HashMap<String, String>();
	public NamespaceContextImpl()
		{
		uri2prefix.put(RDF.NS, "rdf");
		uri2prefix.put(RDFS.NS, "rdfs");
		}
	
	public Set<String> getPrefixes()
		{
		return new HashSet<String>(this.uri2prefix.values());
		}
	
	public void setPrefixNs(String prefix,String ns)
		{
		String oldP=this.uri2prefix.get(ns);
		if(oldP!=null)
			{
			if(oldP.equals(prefix)) return;
			throw new IllegalArgumentException("duplicate prefix/uri "+prefix);
			}
		this.uri2prefix.put(ns, prefix);
		}
	
	@Override
	public String getNamespaceURI(String prefix)
		{
		for(String uri:this.uri2prefix.keySet())
			{
			if(getPrefix(uri).equals(prefix)) return uri;
			}
		return null;
		}
	@Override
	public String getPrefix(String namespaceURI)
		{
		return this.uri2prefix.get(namespaceURI);
		}
	
	@Override
	public Iterator<String> getPrefixes(String namespaceURI)
		{
		return  this.uri2prefix.values().iterator();
		}
	}


/** OntModelImpl */
class OntModelImpl
	implements OntModel
	{
	private List<OntClass> ontClasses=new Vector<OntClass>();
	private NamespaceContextImpl nsCtx=new NamespaceContextImpl();
	
	public OntModelImpl()
		{
		this.nsCtx.setPrefixNs("my", MY.NS);
		this.nsCtx.setPrefixNs("owl", OWL.NS);
		this.nsCtx.setPrefixNs("xsd", XSD.NS);
		}
	
	@Override
	public NamespaceContext getNamespaceContext()
		{
		return nsCtx;
		}
	
	@Override
	public OntClass getOntClassByUri(String uri)
		{
		for(OntClass oc: getOntClasses())
			{
			if(oc.getUri().equals(uri)) return oc;
			}
		return null;
		}
	@Override
	public List<OntClass> getOntClasses()
		{
		return ontClasses;
		}
	
	public void read(File file)
		throws IOException,SAXException
		{
		try
			{
			XPathFactory xPathFactory=XPathFactory.newInstance();
			XPath xpath=xPathFactory.newXPath();
			xpath.setNamespaceContext(this.nsCtx);
			
			DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
			f.setCoalescing(true);
			f.setNamespaceAware(true);
			f.setValidating(false);
			f.setExpandEntityReferences(true);
			f.setIgnoringComments(true);
			f.setIgnoringElementContentWhitespace(true);
			DocumentBuilder docBuilder= f.newDocumentBuilder();
			Document dom=docBuilder.parse(file);
			Element root=dom.getDocumentElement();
			if(!(	root.getNamespaceURI().equals(RDF.NS) &&
					root.getLocalName().equals("RDF"))) throw new IOException("not rdf:RDF root");
			
			NodeList list=(NodeList)xpath.evaluate("//namespace::*",root,XPathConstants.NODESET);
			
			for(int i=0;i< list.getLength();++i)
				{
				Attr attr=(Attr)list.item(i);
				this.nsCtx.setPrefixNs(attr.getLocalName(),attr.getValue());
				}
			list=(NodeList)xpath.evaluate("rdfs:Class[@rdf:about]",root,XPathConstants.NODESET);
			
			for(int i=0;i< list.getLength();++i)
				{	
				Element e1=Element.class.cast(list.item(i));
				OntClassImpl ontclass=new OntClassImpl();
				Attr  att=e1.getAttributeNodeNS(RDF.NS, "about");
				
				ontclass.setUri(att.getValue());
				ontclass.setModel(this);
				if(this.getOntClassByUri(ontclass.getUri())!=null)  throw new IOException("class defined twice id="+ontclass.getUri());
				this.ontClasses.add(ontclass);
				
				Element e2=(Element)xpath.evaluate("rdfs:label",e1,XPathConstants.NODE);
				if(e2!=null) ontclass.setLabel(e2.getTextContent());
				e2=(Element)xpath.evaluate("rdfs:comment",e1,XPathConstants.NODE);
				if(e2!=null) ontclass.setDescription(e2.getTextContent());
				}
			
			list=(NodeList)xpath.evaluate("rdf:Property[@rdf:about]",root,XPathConstants.NODESET);
			for(int i=0;i< list.getLength();++i)
				{
				Element e1=Element.class.cast(list.item(i));
				
				//ontClass Domain
				Set<OntClassImpl> domains=new HashSet<OntClassImpl>();
				
				//get all the domains
				NodeList list2=(NodeList)xpath.evaluate("rdfs:domain/@rdf:resource",e1,XPathConstants.NODESET);
				for(int j=0;j< list2.getLength();++j)
					{
					Attr att=Attr.class.cast(list2.item(j));
					OntClassImpl c=OntClassImpl.class.cast(getOntClassByUri(att.getValue()));
					if(c==null) throw new IOException("cannot get OntClass "+att.getValue());
					domains.add(c);	
					}
				
				//create a property for each domain
				for(OntClassImpl domain:domains)
					{
					OntPropertyImpl ontProperty=null;
					
					String range=(String)xpath.evaluate("rdfs:range[1]/@rdf:resource",e1,XPathConstants.STRING);
					if(range==null || range.isEmpty()) range=XSD.NS+"string";
					/* RANGE IS STRING */
					if(range.equals(XSD.NS+"string"))
						{
						OntStringPropertyImpl property=new OntStringPropertyImpl();
						ontProperty=property;
						//get pattern
						String s1=(String)xpath.evaluate("my:pattern",e1,XPathConstants.STRING);
						if(s1==null)
							{
							int flags=0;
							if("true".equals((String)xpath.evaluate("my:caseInsensitive",root,XPathConstants.STRING)))
								{
								flags=Pattern.CASE_INSENSITIVE;
								}
							Pattern pattern=Pattern.compile(s1, flags);
							property.setPattern(pattern);
							}
						//get enums
						NodeList enums=(NodeList)xpath.evaluate("my:enum",e1,XPathConstants.NODESET);
						if(enums.getLength()>0)
							{
							List<String> items=new ArrayList<String>(enums.getLength());
							for(int k=0;k< enums.getLength();++k)
								{
								items.add(enums.item(k).getTextContent());
								}
							property.setEnumeration(items);
							}
						}
					/* RANGE IS INT */
					else if(range.equals(XSD.NS+"int"))
						{
						OntLongPropertyImpl property=new OntLongPropertyImpl();
						ontProperty=property;
						String s1=(String)xpath.evaluate("xsd:minInclusive",e1,XPathConstants.STRING);
						if(s1!=null)
							{
							property.setMinInclusive(Long.parseLong(s1));
							}
						s1=(String)xpath.evaluate("xsd:maxInclusive",e1,XPathConstants.STRING);
						if(s1!=null)
							{
							property.setMaxInclusive(Long.parseLong(s1));
							}
						}
					/* RANGE IS DOUBLE */
					else if(range.equals(XSD.NS+"double"))
						{
						OntDoublePropertyImpl property=new OntDoublePropertyImpl();
						ontProperty=property;
						String s1=(String)xpath.evaluate("xsd:minInclusive",e1,XPathConstants.STRING);
						if(s1!=null)
							{
							property.setMinInclusive(Double.parseDouble(s1));
							}
						s1=(String)xpath.evaluate("xsd:maxInclusive",e1,XPathConstants.STRING);
						if(s1!=null)
							{
							property.setMaxInclusive(Double.parseDouble(s1));
							}
						}
					/* RANGE IS OBJECT */
					else 
						{
						OntObjectPropertyImpl property=new OntObjectPropertyImpl();
						ontProperty=property;
						NodeList ranges=(NodeList)xpath.evaluate("rdfs:domain/@rdf:resource",e1,XPathConstants.NODESET);
						Set<OntClass> set=new HashSet<OntClass>();
						for(int k=0;k< ranges.getLength();++k)
							{
							Attr rsrc=(Attr)ranges.item(k);
							OntClass ontClass= this.getOntClassByUri(rsrc.getValue());
							if(ontClass==null) throw new IOException("cannot find ontClass:"+rsrc.getValue());
							set.add(ontClass);
							}
						if(set.isEmpty()) throw new IOException("no range");
						property.setRange(set);
						}
					
					Element e2=(Element)xpath.evaluate("rdfs:label",e1,XPathConstants.NODE);
					if(e2!=null) ontProperty.setLabel(e2.getTextContent());
					e2=(Element)xpath.evaluate("rdfs:comment",e1,XPathConstants.NODE);
					if(e2!=null) ontProperty.setDescription(e2.getTextContent());
					e2=(Element)xpath.evaluate("owl:minCardinality",e1,XPathConstants.NODE);
					if(e2!=null && !e2.getTextContent().equals("unbounded"))
						{
						ontProperty.setMinCardinality(Integer.parseInt(e2.getTextContent()));
						}
					e2=(Element)xpath.evaluate("owl:maxCardinality",e1,XPathConstants.NODE);
					if(e2!=null && !e2.getTextContent().equals("unbounded"))
						{
						ontProperty.setMaxCardinality(Integer.parseInt(e2.getTextContent()));
						}
					//finalize property
					Attr  att=e1.getAttributeNodeNS(RDF.NS, "about");
					ontProperty.setUri(att.getValue());
					ontProperty.setOntClass(domain);
					domain.add(ontProperty);
					}
				}
			}
		catch(XPathExpressionException err)
			{
			throw new IOException(err);
			}
		catch(ParserConfigurationException err)
			{
			throw new IOException(err);
			}
		}
	
	public void write(OutputStream out) throws XMLStreamException
		{
		XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
		XMLStreamWriter w= xmlfactory.createXMLStreamWriter(out,"UTF-8");
		w.writeStartDocument("UTF-8","1.0");
		w.writeStartElement("rdf", "RDF", RDF.NS);
		for(String prefix:this.nsCtx.getPrefixes())
			{
			w.writeAttribute("xmlns", XMLConstants.XML_NS_URI, prefix, this.nsCtx.getNamespaceURI(prefix));
			}
		w.writeCharacters("\n");
		for(OntClass ontClass: this.getOntClasses())
			{
			w.writeStartElement("rdfs", "Class",RDFS.NS);
			w.writeAttribute("rdf", RDF.NS, "about", ontClass.getUri());
			w.writeCharacters("\n");
			
			w.writeStartElement("rdfs", "label",RDFS.NS);
			w.writeCharacters(ontClass.getLabel());
			w.writeEndElement();
			
			w.writeStartElement("rdfs", "comment",RDFS.NS);
			w.writeCharacters(ontClass.getDescription());
			w.writeEndElement();
			
			w.writeEndElement();
			
			for(OntProperty prop:ontClass.getProperties())
				{
				w.writeStartElement("rdf", "Property",RDF.NS);
				w.writeAttribute("rdf", RDF.NS, "about", prop.getUri());
				
				w.writeStartElement("rdfs", "label",RDFS.NS);
				w.writeCharacters(prop.getLabel());
				w.writeEndElement();
				
				w.writeStartElement("rdfs", "comment",RDFS.NS);
				w.writeCharacters(prop.getDescription());
				w.writeEndElement();
				
				if(prop.getMinCardinality()!=null)
					{
					w.writeStartElement("owl", "minCardinality",OWL.NS);
					w.writeCharacters(String.valueOf(prop.getMinCardinality()));
					w.writeEndElement();
					}
				
				if(prop.getMaxCardinality()!=null)
					{
					w.writeStartElement("owl", "maxCardinality",OWL.NS);
					w.writeCharacters(String.valueOf(prop.getMaxCardinality()));
					w.writeEndElement();
					}
				
				if(prop instanceof OntStringProperty)
					{
					OntStringProperty property=(OntStringProperty)prop;
					
					w.writeEmptyElement("rdfs","domain",RDFS.NS);
					w.writeAttribute("rdf", RDF.NS, "resource", XSD.NS+"string");
					
					if(property.getPattern()!=null)
						{
						w.writeStartElement("my", "pattern",MY.NS);
						w.writeCharacters(property.getPattern().pattern());
						w.writeEndElement();
						}
					
					for(String e: ((OntStringProperty) prop).getEnum())
						{
						w.writeStartElement("my", "enum",MY.NS);
						w.writeCharacters(e);
						w.writeEndElement();
						}
					}
				else if(prop instanceof OntLongProperty)
					{
					OntLongProperty property=(OntLongProperty)prop;
					
					w.writeEmptyElement("rdfs", "domain",RDFS.NS);
					w.writeAttribute("rdf", RDF.NS, "resource", XSD.NS+"int");
					
					if(property.getMinInclusive()!=null)
						{
						w.writeStartElement("xsd", "minInclusive",XSD.NS);
						w.writeCharacters(String.valueOf(property.getMinInclusive()));
						w.writeEndElement();
						}
					if(property.getMaxInclusive()!=null)
						{
						w.writeStartElement("xsd", "maxInclusive",XSD.NS);
						w.writeCharacters(String.valueOf(property.getMaxInclusive()));
						w.writeEndElement();
						}
					}
				else if(prop instanceof OntDoubleProperty)
					{
					OntDoubleProperty property=(OntDoubleProperty)prop;
					w.writeEmptyElement("rdfs", "domain",RDFS.NS);
					w.writeAttribute("rdf", RDF.NS, "resource", XSD.NS+"double");
					if(property.getMinInclusive()!=null)
						{
						w.writeStartElement("xsd", "minInclusive",XSD.NS);
						w.writeCharacters(String.valueOf(property.getMinInclusive()));
						w.writeEndElement();
						}
					if(property.getMaxInclusive()!=null)
						{
						w.writeStartElement("xsd", "maxInclusive",XSD.NS);
						w.writeCharacters(String.valueOf(property.getMaxInclusive()));
						w.writeEndElement();
						}
					}
				else
					{
					OntObjectProperty property=(OntObjectProperty)prop;
					for(OntClass c:property.getRange())
						{
						w.writeEmptyElement("rdfs", "domain",RDFS.NS);
						w.writeAttribute("rdf", RDF.NS, "resource", c.getUri());
						}
					}
				w.writeEndElement();
				}
			
			w.writeCharacters("\n");
			}
		
		w.writeEndElement();
		w.writeEndDocument();
		w.flush();
		}
	
	}

/** DataStoreImpl */
class DataStoreImpl
	implements Store
	{
	private OntModel model;
	private List<InstanceOfClass> instances=new Vector<InstanceOfClass>();
	
	private class IterorImpl
		implements CloseableIterator<InstanceOfClass>
		{
		private Iterator<InstanceOfClass> iter;
		IterorImpl(Iterator<InstanceOfClass> iter)
			{
			this.iter=iter;
			}
		@Override
		public boolean hasNext()
			{
			return this.iter.hasNext();
			}
		@Override
		public InstanceOfClass next()
			{
			return this.iter.next();
			}
		@Override
		public void remove()
			{
			throw new UnsupportedOperationException();
			}
		
		@Override
		public void close()
			{
			
			}
		}
	
	public DataStoreImpl(OntModel model)
		{
		this.model=model;
		}
	
	@Override
	public CloseableIterator<InstanceOfClass> listInstances()
		{
		return new IterorImpl(this.instances.iterator());
		}
	
	
	@Override
	public InstanceOfClass getInstanceById(String id)
		{
		CloseableIterator<InstanceOfClass> iter=null;
		try
			{
			iter=listInstances();
			while(iter.hasNext())
				{
				InstanceOfClass i=iter.next();
				if(i.getId().equals(id)) return i;
				}
			}
		catch (Exception err)
			{
			throw new RuntimeException(err);
			}
		finally
			{
			if(iter!=null) iter.close();
			}
		return null;
		}
	
	@Override
	public OntModel getOntModel()
		{
		return model;
		}
	public void write(File file)
		throws XMLStreamException,IOException
		{
		FileWriter out=new FileWriter(file);
		write(out);
		out.close();
		}
	
	public void write(Writer out)
		throws XMLStreamException
		{
		XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
		
		XMLStreamWriter w= xmlfactory.createXMLStreamWriter(out);
		w.writeStartDocument("UTF-8","1.0");
		w.writeStartElement("rdf","RDF",RDF.NS);
		CloseableIterator<InstanceOfClass> iter=null;
		try
			{
			iter=listInstances();
			while(iter.hasNext())
				{
				InstanceOfClass i=iter.next();
				writeInstance(i,w);
				}
			}
		catch (Exception err)
			{
			throw new RuntimeException(err);
			}
		finally
			{
			if(iter!=null) iter.close();
			}
		w.writeEndElement();
		w.writeEndDocument();
		w.flush();
		}
	
	private void writeInstance(InstanceOfClass ioc,XMLStreamWriter w)
	throws XMLStreamException
		{
		OntClass ontClass=ioc.getOntClass();
		w.writeStartElement(ontClass.getPrefix(),ontClass.getLocalName(),ontClass.getNamespaceUri());
		w.writeAttribute("rdf", RDF.NS, "ID", ioc.getId());
		for(InstanceOfProperty prop: ioc.getProperties())
			{
			OntProperty ontProp=prop.getOntProperty();
			w.writeStartElement(ontProp.getPrefix(),ontProp.getLocalName(),ontProp.getNamespaceUri());
			w.writeCharacters(prop.getValue());
			w.writeEndElement();
			}
		w.writeEndElement();
		}
	
	
	private void skip(XMLEventReader reader) throws XMLStreamException
		{
		int depth=1;
		while(reader.hasNext())
			{
			XMLEvent evt=reader.nextEvent();
			if(evt.isStartElement())
				{
				depth++;
				}
			else if(evt.isEndElement())
				{
				--depth;
				if(depth==0) return;
				}
			}
		throw new IllegalStateException("should not happen");
		}

	private void insert(File xmlFile,InstanceOfClass node)
		throws Exception
		{
		InstanceOfClass matcher=null;
		/** TODO
		for(InstanceOfClass b:this.instances)
			{
			if(!b.match(node)) continue;
			if(matcher!=null)
				{
				System.err.println("Ambigous");
				System.exit(-1);
				}
			matcher=b;
			} */
		
		File xmlOut=null;
		XMLOutputFactory xmlOutputFactory=XMLOutputFactory.newFactory();
		XMLStreamWriter writer=xmlOutputFactory.createXMLStreamWriter(new FileWriter(xmlOut));
		
		boolean nodeInserted=false;
		boolean ignoreNextCloseElement=false;
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
		xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
		xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
		XMLEventReader reader= xmlInputFactory.createXMLEventReader(new FileReader(xmlFile));
		while(reader.hasNext())
			{
			XMLEvent evt=reader.nextEvent();
			if(evt.isStartDocument())
				{
				writer.writeStartDocument("UTF-8", "1.0");
				}
			else if(evt.isStartElement())
				{
				XMLEvent evt2=reader.peek();
				boolean isClosed= evt2.isEndElement();
				StartElement element=evt.asStartElement();
				Attribute att=element.getAttributeByName(new QName(RDF.NS, "ID", "rdf"));
				if(att==null) att=element.getAttributeByName(new QName(RDF.NS, "about", "rdf"));
				if(att!=null
					//TODO && att.getValue().equals(matcher.getId(node))
					)
					{
					skip(reader);
					//TODO matcher.write(writer,node);
					nodeInserted=true;
					}
				else
					{
					QName qName=element.getName();
					if(isClosed)
						{
						writer.writeEmptyElement(qName.getPrefix(),qName.getLocalPart(),qName.getNamespaceURI());
						ignoreNextCloseElement=true;
						}
					else
						{
						writer.writeStartElement(qName.getPrefix(),qName.getLocalPart(),qName.getNamespaceURI());
						ignoreNextCloseElement=false;
						}
					Iterator<?> iter=element.getAttributes();
					while(iter.hasNext())
						{
						att=(Attribute)iter.next();
						QName attName=att.getName();
						writer.writeAttribute(attName.getPrefix(),attName.getNamespaceURI(),attName.getLocalPart(),att.getValue());
						}
					}
				
				}
			else if(evt.isEndElement())
				{
				if(!ignoreNextCloseElement)
					{
					writer.writeEndElement();
					}
				ignoreNextCloseElement=false;
				}
			else if(evt.isEndDocument())
				{
				if(!nodeInserted)
					{
					//TODO matcher.write(writer,node);
					}
				writer.writeEndDocument();
				}
			else if(evt.isCharacters())
				{
				Characters chars=evt.asCharacters();
				writer.writeCharacters(chars.getData());
				}
			}
		reader.close();
		writer.flush();
		writer.close();
		}
	
	}

/** InstanceOfPropertyImpl */
class InstanceOfPropertyImpl
	implements InstanceOfProperty
	{
	private OntProperty ontProperty;
	private InstanceOfClass instance;
	private String value="";
	@Override
	public InstanceOfClass getInstance()
		{
		return instance;
		}
	@Override
	public OntProperty getOntProperty()
		{
		return ontProperty;
		}
	
	@Override
	public String getValue()
		{
		return value;
		}
	
	@Override
	public boolean match(Pattern regex)
		{
		return false;
		}
	}

/** InstanceOfClassImpl */
class InstanceOfClassImpl
	implements InstanceOfClass
	{
	private String id;
	private List<InstanceOfProperty> properties=new Vector<InstanceOfProperty>();
	private OntClass ontClass;
	private Store dataStore;
	
	public InstanceOfClassImpl(
		Store dataStore,
		OntClass ontClass
		)
		{
		this.dataStore=dataStore;
		this.ontClass=ontClass;
		}
	
	@Override
	public boolean match(Pattern regex)
		{
		return false;
		}

	@Override
	public String getId()
		{
		return id;
		}

	@Override
	public String getName()
		{
		return null;
		}

	@Override
	public String getDescription()
		{
		return null;
		}

	@Override
	public List<InstanceOfProperty> getProperties()
		{
		return properties;
		}
	
	@Override
	public List<InstanceOfProperty> getProperties(OntProperty ontProp)
		{
		List<InstanceOfProperty> array=new ArrayList<InstanceOfProperty>();
		for(InstanceOfProperty iop:this.getProperties())
			{
			if(iop.getOntProperty().equals(ontProp))
				{
				array.add(iop);
				}
			}
		return array;
		}

	@Override
	public Store getDataStore()
		{
		return this.dataStore;
		}

	@Override
	public OntClass getOntClass()
		{
		return ontClass;
		}
	}


class InstanceTable
	extends AbstractTableModel
	{
	private Vector<InstanceOfClass> instances=new Vector<InstanceOfClass>();
	
	public InstanceOfClass getInstanceAt(int rowIndex)
		{
		return this.instances.get(rowIndex);
		}
	
	@Override
	public int getColumnCount()
		{
		return 2;
		}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
		{
		InstanceOfClass ioc= getInstanceAt(rowIndex);
		switch(columnIndex)
			{
			case 0:return ioc.getName();
			case 1:return ioc.getDescription();
			}
		return null;
		}
	
	@Override
	public int getRowCount()
		{
		return instances.size();
		}
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
		{
		return false;
		}
	}

/**
 * OnePropertyEditor
 * @author pierre
 *
 */
class OnePropertyEditor
	extends JPanel
	{
	abstract private class Extractor
		{
		protected Object component;
		public Extractor(Object component)
			{
			this.component=component;
			}
		public abstract String getValue();
		public abstract void setValue(String s);
		};
	private OntProperty ontProperty;
	private Extractor extractor;
	protected OnePropertyEditor(OntProperty ontProperty)
		{
		super(new BorderLayout());
		this.ontProperty=ontProperty;
		switch(ontProperty.getPropertyType())
			{
			case STRING:
				{
				OntStringProperty pp=OntStringProperty.class.cast(ontProperty);
				if(!pp.getEnum().isEmpty())
					{
					JComboBox comboBox=new JComboBox(new Vector<String>(pp.getEnum()));
					this.add(comboBox,BorderLayout.CENTER);
					this.extractor=new Extractor(comboBox)
						{
						@Override
						public String getValue()
							{
							return (String)JComboBox.class.cast(this.component).getSelectedItem();
							}
						public void setValue(String s)
							{
							JComboBox.class.cast(this.component).setSelectedItem(s);
							}
						};
					}
				else if(pp.isMultiline())
					{
					JTextArea area=new JTextArea(5,10);
					JScrollPane scroll=new JScrollPane(area);
					this.add(scroll,BorderLayout.CENTER);
					this.extractor=new Extractor(area)
						{
						@Override
						public String getValue()
							{
							return (String)JTextComponent.class.cast(this.component).getText();
							}
						public void setValue(String s)
							{
							JTextComponent.class.cast(this.component).setText(s);
							JTextComponent.class.cast(this.component).setCaretPosition(0);
							}
						};
					}
				else
					{
					JTextField tf=new JTextField();
					this.add(tf,BorderLayout.CENTER);
					this.extractor=new Extractor(tf)
						{
						@Override
						public String getValue()
							{
							return (String)JTextComponent.class.cast(this.component).getText();
							}
						public void setValue(String s)
							{
							JTextComponent.class.cast(this.component).setText(s);
							JTextComponent.class.cast(this.component).setCaretPosition(0);
							}
						};
					}
				break;
				}
			case LONG:
				{
				JTextField tf=new JTextField();
				this.add(tf,BorderLayout.CENTER);
				this.extractor=new Extractor(tf)
					{
					@Override
					public String getValue()
						{
						return (String)JTextComponent.class.cast(this.component).getText();
						}
					public void setValue(String s)
						{
						JTextComponent.class.cast(this.component).setText(s);
						JTextComponent.class.cast(this.component).setCaretPosition(0);
						}
					};
				break;
				}
			case DOUBLE:
				{
				JTextField tf=new JTextField();
				this.add(tf,BorderLayout.CENTER);
				this.extractor=new Extractor(tf)
					{
					@Override
					public String getValue()
						{
						return (String)JTextComponent.class.cast(this.component).getText();
						}
					public void setValue(String s)
						{
						JTextComponent.class.cast(this.component).setText(s);
						JTextComponent.class.cast(this.component).setCaretPosition(0);
						}
					};
				break;
				}
			case OBJECT:
				{
				break;
				}
			default:
				{
				throw new IllegalArgumentException("property not handled:"+ontProperty);
				}
			}
		}
	
	
	public String getValue()
		{
		return this.extractor.getValue();
		}
	
	public OntProperty getOntProperty()
		{
		return ontProperty;
		}
	}

@SuppressWarnings("serial")
class InstanceEd
	extends JDialog
	{
	private static final int EDITOR_WIDTH=600;
	private Store store;
	private InstanceOfClass instance;
	private OntClass ontClass;
	private int exitStatus;
	private JPanel mainPane;
	private JScrollPane scrollPane;
	
	/**
	 * 
	 * PropertyPart
	 *
	 */
	private class PropertyPart
		extends JPanel
		{
		private OntProperty ontProperty;
		private Box verticalBox;
		//private List<OneProperty> propertyValues=new Vector<OneProperty>();
		
		/**
		 * OneProperty
		 *
		 */
		private abstract class OneProperty
			extends JPanel
			{
			
			protected OneProperty()
				{
				super(new BorderLayout());
				Box right=Box.createVerticalBox();
				this.add(right,BorderLayout.EAST);
				AbstractAction action=new AbstractAction("\u292B")
					{
					@Override
					public void actionPerformed(ActionEvent e)
						{
						getPropertyPart().remove(OneProperty.this);
						getPropertyPart().validate();
						}
					};
				
				JButton button=new JButton(action);
				button.setPreferredSize(new Dimension(16,16));
				right.add(button);
				button.setForeground(Color.RED);
				}
			
			public PropertyPart getPropertyPart()
				{
				return PropertyPart.this;
				}
			
			public OntProperty getOntProperty()
				{
				return getPropertyPart().getOntProperty();
				}
			public abstract void setValue(String text);
			public abstract String getValue();
			}
		
		private abstract class OneDataProperty
		extends OneProperty
			{
			protected JTextComponent textComponent;
			
			public void setValue(String text)
				{
				this.textComponent.setText(text);
				this.textComponent.setCaretPosition(0);
				}
			public String getValue()
				{
				return this.textComponent.getText();
				}
			
			}
		
		private class OneLongProperty
			extends OneDataProperty
			{
			OneLongProperty()
				{
				super.textComponent=new JTextField(20);
				this.add(super.textComponent,BorderLayout.CENTER);
				}
			}
		
		private class OneDoubleProperty
			extends OneDataProperty
				{
				OneDoubleProperty()
					{
					super.textComponent=new JTextField(20);
					this.add(super.textComponent,BorderLayout.CENTER);
					}
				}
		
		private class OneStringProperty
			extends OneDataProperty
				{
				private JComboBox comboBox=null;
				private JScrollPane scrollPane=null;
				OneStringProperty()
					{
					super();
					System.err.println("OK1");
					
					OntStringProperty pp=OntStringProperty.class.cast(getOntProperty());
					if(!pp.getEnum().isEmpty())
						{System.err.println("OK2");
						
						super.textComponent=null;
						this.comboBox=new JComboBox(
							new Vector<String>(pp.getEnum())	
							);
						this.add(this.comboBox,BorderLayout.CENTER);
						}
					else if(pp.isMultiline())
						{
						System.err.println("OK2");
						
						super.textComponent=new JTextArea(5,20);
						this.scrollPane=new JScrollPane(this.textComponent);
						this.add(this.scrollPane,BorderLayout.CENTER);
						}
					else
						{
						System.err.println("OK3");
						super.textComponent=new JTextField(20);
						this.add(this.textComponent,BorderLayout.CENTER);
						}
					}
				
				@Override
				public String getValue()
					{
					String s=null;
					if(this.comboBox!=null)
						{
						s=(String)this.comboBox.getSelectedItem();
						if(s==null) s="";
						}
					else
						{
						s=this.textComponent.getText();
						}
					return s;
					}
				
				}
		
		private class OneObjectProperty
		extends OneProperty
			{
			private JTextField textField;
			OneObjectProperty()
				{
				this.textField=new JTextField(20);
				this.textField.setEditable(false);
				this.textField.setText("XXXXXXXXXX");
				this.add(this.textField,BorderLayout.CENTER);
				}
			
			@Override
			public String getValue()
				{
				return textField.getText();
				}
			@Override
			public void setValue(String text)
				{
				textField.setText(text);
				textField.setCaretPosition(0);
				}
			
			}
		
		
		PropertyPart(OntProperty ontProperty)
			{
			super(new BorderLayout(2,2));
			setBorder(new EmptyBorder(2,2,2,2));
			JPanel top=new JPanel(new FlowLayout(FlowLayout.LEADING));
			this.add(top,BorderLayout.NORTH);
			this.ontProperty=ontProperty;
			JLabel label=new JLabel(ontProperty.getLabel());
			label.setToolTipText(ontProperty.getDescription());
			top.add(label);
			
			AbstractAction action=new AbstractAction("\u2295")
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					addNewValue();
					}
				};
			action.putValue(AbstractAction.SHORT_DESCRIPTION,"Add a new "+ontProperty.getLabel());
			JButton button=new JButton(action);
			button.setForeground(Color.GREEN);
			button.setPreferredSize(new Dimension(16,16));
			top.add(button);
			
			this.verticalBox = Box.createVerticalBox();
			this.add(this.verticalBox,BorderLayout.CENTER);
			}
		
		public OntProperty getOntProperty()
			{
			return ontProperty;
			}
		
		public void addNewValue()
			{
			OneProperty one=createOneProperty();
			this.verticalBox.add(one);
			this.verticalBox.validate();
			}
		
		private OneProperty createOneProperty()
			{
			OneProperty oneProp=null;
			switch(this.getOntProperty().getPropertyType())
				{
				case DOUBLE: oneProp= new OneDoubleProperty();break;
				case LONG: oneProp= new OneLongProperty();break;
				case STRING: oneProp=new OneStringProperty();break;
				case OBJECT: oneProp=new OneObjectProperty();break;
				default: throw new IllegalArgumentException("?");
				}
			System.err.println(oneProp.getClass());
			return oneProp;
			}
		
		void fromInstance(InstanceOfClass instance)
			{
			for(InstanceOfProperty iop:instance.getProperties(this.getOntProperty()))
				{
				String s=iop.getValue();
				if(s==null || s.trim().length()==0) continue;
				OneProperty oneProp=createOneProperty();
				oneProp.setValue(s);
				this.verticalBox.add(oneProp);
				}
			}
		
	
		}
	
	public InstanceEd(Window w,Store store,InstanceOfClass instance)
		{
		this(w,store,instance.getOntClass(),instance);
		}
	
	
	public InstanceEd(Window w,Store store,OntClass ontClass)
		{
		this(w,store,ontClass,null);
		}
	
	private InstanceEd(Window w,Store store,OntClass ontClass,InstanceOfClass instance)
		{
		super(w,ontClass.getLabel(),ModalityType.APPLICATION_MODAL);
		this.store=store;
		boolean instanceIsNew=false;
		this.ontClass=ontClass;
		this.instance=instance;
		if(this.instance==null)
			{
			instanceIsNew=true;
			this.instance=new InstanceOfClassImpl(store,ontClass);
			}
		
		this.addWindowListener(
			new WindowAdapter()
				{
				public void windowOpened(WindowEvent e)
					{
					Dimension s=Toolkit.getDefaultToolkit().getScreenSize();
					Dimension w=e.getWindow().getSize();
					e.getWindow().setBounds(
						(s.width-w.width)/2,
						(s.height-w.height)/2,
						w.width,
						w.height
						);
					}
				}
			);
		JPanel pane=new JPanel(new BorderLayout(5,5));
		pane.setBorder(new EmptyBorder(5,5,5,5));
		
		this.mainPane=new JPanel();
		this.mainPane.setLayout(new BoxLayout(this.mainPane, BoxLayout.Y_AXIS));
		pane.add(this.scrollPane=new JScrollPane(this.mainPane));
		
		
		for(OntProperty ontProperty:ontClass.getProperties())
			{
			PropertyPart part=new PropertyPart(ontProperty);
			this.mainPane.add(part);
			if(instanceIsNew)
				{
				
				}
			else
				{
				
				}
			}
		
		JPanel bot=new JPanel(new FlowLayout(FlowLayout.TRAILING));
		pane.add(bot,BorderLayout.SOUTH);
		
		AbstractAction action=new AbstractAction("OK")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				int n=mainPane.getComponentCount();
				for(int i=0;i<n;++i)
					{
					Component c=mainPane.getComponent(i);
					if(!(c instanceof PropertyPart)) continue;
					}
				closeWithStatus(JOptionPane.OK_OPTION);
				}
			};
		bot.add(new JButton(action));
		
		action=new AbstractAction("Cancel")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				closeWithStatus(JOptionPane.NO_OPTION);
				}
			};
		bot.add(new JButton(action));
		
		
		
		this.setContentPane(pane);
		}
	
	public void setInstance(InstanceOfClass instance)
		{
		this.instance = instance;
		setOntClass(instance.getOntClass());
		}
	
	public void setOntClass(OntClass ontClass)
		{
		this.ontClass = ontClass;
		}
	
	private void closeWithStatus(int status)
		{
		this.exitStatus=status;
		this.setVisible(false);
		this.dispose();
		}
	public int getExitStatus()
		{
		return exitStatus;
		}
	
	public InstanceOfClass getInstance()
		{
		return instance;
		}
	
	}


class PropertyPane
	extends JPanel
	{
	private OntProperty ontProperty;
	private Box mainPane;
	
	abstract class ValuePane
		extends JPanel
		{
		protected ValuePane()
			{
			super(new BorderLayout());
			JButton but=new JButton("[-]");
			but.setPreferredSize(new Dimension(12,12));
			this.add(but,BorderLayout.EAST);
			}
		}
	
	class AbstractTextPane
		extends ValuePane
			{
			protected AbstractTextPane()
				{
				
				}
			}
	
	class TextPane
	extends AbstractTextPane
		{
		private JTextField textField;
		public TextPane()
			{
			this.textField=new JTextField();
			super.add(this.textField,BorderLayout.CENTER)
;			}
		}
	
	class TextAreaPane
	extends AbstractTextPane
		{
		private JTextArea textArea;
		public TextAreaPane()
			{
			this.textArea=new JTextArea(4,40);
			super.add(new JScrollPane(this.textArea),BorderLayout.CENTER)
;			}
		}
	
	
	PropertyPane(OntProperty ontProperty)
		{
		this(null,ontProperty);
		
		}
	PropertyPane(InstanceOfProperty property)
		{
		this(property,property.getOntProperty());
		}
	PropertyPane(InstanceOfProperty property,OntProperty ontProperty)
		{
		super(new BorderLayout(5,5));
		JPanel top=new JPanel(new BorderLayout(1,1));
		this.add(top,BorderLayout.NORTH);
		JLabel label=new JLabel(ontProperty.getLabel());
		top.add(label,BorderLayout.CENTER);
		label.setToolTipText(ontProperty.getDescription());
		top.add(new JButton("[+]"),BorderLayout.EAST);
		
		this.mainPane=Box.createVerticalBox();
		this.add(mainPane,BorderLayout.CENTER);
		for(int i=0;i< 50;i++)
			{
			this.mainPane.add(i%2==0?new TextAreaPane():new TextPane());
			}
		}
	}

class InstancePane
	extends JPanel
	{
	private static final long serialVersionUID = 1L;
	private OntClass ontClass;
	private InstanceOfClass instance;
	private boolean instanceIsNew;
	private JPanel mainPane;
	private Store store;
	InstancePane(Store store,OntClass ontClass)
		{
		this(store,ontClass,null);
		}
	
	InstancePane(InstanceOfClass instance)
		{
		this(instance.getDataStore(),null,instance);
		}
	
	private InstancePane( Store store,OntClass ontClass,InstanceOfClass instance)
		{
		super(new BorderLayout(5, 5));
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.store=store;
		this.ontClass=ontClass;
		this.instanceIsNew=(instance==null);
		this.instance=instance;
		if(this.instance==null)
			{
			//this.instance=new InstanceOfClassImpl();
			}

		}
	}

@SuppressWarnings("serial")
class Frame
	extends JFrame
	{
	private File saveAs;
	private Store dataStore;
	private boolean documentModified=false;
	private JPanel cardPane;
	private Map<OntClass, Boolean> showOntClasses;
	private JList  instanceList;
	class LabelPane
		{
		OntProperty property;
		
		LabelPane(OntProperty property)
			{
			this.property=property;
			}
		
		}
	
	
	Frame(Store dataStore)
		{
		super("Structured");
		this.dataStore=dataStore;
		this.showOntClasses=new HashMap<OntClass, Boolean>();
		for(OntClass ontClass:getSchema().getOntClasses())
			{
			this.showOntClasses.put(ontClass,Boolean.TRUE);
			}
		
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter()
			{
			@Override
			public void windowOpened(WindowEvent e)
				{
				Dimension d=Toolkit.getDefaultToolkit().getScreenSize();
				e.getWindow().setBounds(50, 50, d.width-100, d.height-100);
				reloadInstanceList();
				}
			
			@Override
			public void windowClosing(WindowEvent e)
				{
				doMenuClose();
				}
			});
		
		
		this.cardPane=new JPanel(new CardLayout(5,5));
		this.cardPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(this.cardPane);
		JMenuBar bar=new JMenuBar();
		setJMenuBar(bar);
		JMenu menu=new JMenu("File");
		bar.add(menu);
		AbstractAction action;
		
		action=new AbstractAction("Quit")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				doMenuClose();
				}
			};
		menu.add(action);
		JPanel contentPane=new JPanel(new BorderLayout());
		this.cardPane.add(contentPane,"MAIN");
		
		contentPane.add(new JScrollPane(
			this.instanceList=new JList(new DefaultListModel()))
			);
		
		Box top=Box.createVerticalBox();
		contentPane.add(top,BorderLayout.NORTH);
		JPanel hbox=new JPanel(new FlowLayout(FlowLayout.LEADING));
		top.add(hbox);
		
		JLabel label=new JLabel("Filter:",JLabel.RIGHT);
		hbox.add(label);
		JTextField tfRegex=new JTextField(20);
		hbox.add(tfRegex);
		action=new AbstractAction("OK")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				reloadInstanceList();
				}
			};
		tfRegex.addActionListener(action);	
		hbox.add(new JButton(action));
		hbox.add(new JSeparator(JSeparator.VERTICAL));
		
		action=new AbstractAction("Edit")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				int i=instanceList.getSelectedIndex();
				if(i==-1) return;
				InstanceOfClass instance=(InstanceOfClass)instanceList.getSelectedValue();
				editInstance(instance);
				}
			};
		hbox.add(new JButton(action));
		
		
		
		hbox=new JPanel(new FlowLayout(FlowLayout.LEADING));
		top.add(hbox);
		hbox.setBorder(new TitledBorder("Classes"));
		
		for(OntClass clazz: dataStore.getOntModel().getOntClasses())
			{
			JToggleButton but=new JToggleButton(clazz.getLabel());
			hbox.add(but);
			but.setSelected(this.showOntClasses.get(clazz));
			action=new AbstractAction(clazz.getLabel())
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					OntClass ontClass=OntClass.class.cast(this.getValue("ontClass"));
					showOntClasses.put(ontClass, !showOntClasses.get(ontClass));
					reloadInstanceList();
					}
				};
			action.putValue(AbstractAction.SHORT_DESCRIPTION, clazz.getDescription());
			action.putValue("ontClass",clazz);
			but.addActionListener(action);
			}
		
		JMenu subMenu=new JMenu("Create");
		menu.add(subMenu);
		for(OntClass ontClass: getSchema().getOntClasses())
			{
			action=new AbstractAction(ontClass.getQName())
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					OntClass ontclass=OntClass.class.cast(this.getValue("ontClass"));
					createOntClass(ontclass);
					}
				};
			action.putValue("ontClass", ontClass);
			action.putValue(AbstractAction.SHORT_DESCRIPTION, ontClass.getDescription());
			subMenu.add(action);
			}
		}
	
	
	
	private void createOntClass(OntClass clazz)
		{
		InstanceEd editor=new InstanceEd(
			this,this.dataStore,
			clazz);
		editor.pack();
		editor.setVisible(true);
		if(editor.getExitStatus()!=JOptionPane.OK_OPTION) return;
		InstanceOfClass instance=editor.getInstance();
		documentModified=true;
		}
	
	private void editInstance(InstanceOfClass instance)
		{
		
		}
	
	
	private void reloadInstanceList()
		{
		DefaultListModel lm=DefaultListModel.class.cast(this.instanceList.getModel());
		lm.setSize(0);
		CloseableIterator<InstanceOfClass> iter=null;
		try
			{
			iter=getDataStore().listInstances();
			while(iter.hasNext())
				{
				InstanceOfClass i=iter.next();
				OntClass clazz=i.getOntClass();
				if(!this.showOntClasses.get(clazz)) continue;
				lm.addElement(i);
				}
			}
		catch (Exception e)
			{
			throw new RuntimeException(e);
			}
		finally
			{
			if(iter!=null) iter.close();
			}
		}
	
	private void doMenuClose()
		{
		this.setVisible(false);
		this.dispose();
		}
	
	public Store getDataStore()
		{
		return dataStore;
		}
	
	public OntModel getSchema()
		{
		return getDataStore().getOntModel();
		}
	}

@SuppressWarnings("serial")
class OntClassChooser
	extends JDialog
	{
	private OntClass selected=null;
	public OntClassChooser(
		Window owner,
		List<OntClass> ontClasses	
		)
		{
		super(owner,"Class chooser",ModalityType.TOOLKIT_MODAL);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		JPanel pane=new JPanel(new BorderLayout(5,5));
		pane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(pane);
		Box vbox=Box.createVerticalBox();
		pane.add(vbox,BorderLayout.CENTER);
		this.setContentPane(pane);
		ButtonGroup group=new ButtonGroup();
		for(OntClass ontclass: ontClasses)
			{
			AbstractAction action=new AbstractAction(
					ontclass.getLabel()
					)
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					OntClass c=OntClass.class.cast(getValue("ontclass"));
					OntClassChooser.this.selected=c;
					OntClassChooser.this.setVisible(false);
					OntClassChooser.this.dispose();
					}
				};
			action.putValue(AbstractAction.SHORT_DESCRIPTION, ontclass.getDescription());
			action.putValue(AbstractAction.LONG_DESCRIPTION, ontclass.getDescription());
			action.putValue("ontclass",ontclass);
			JToggleButton button=new JToggleButton(action);
			button.setFont(button.getFont().deriveFont(32f).deriveFont(Font.PLAIN));
			group.add(button);
			vbox.add(button);
			}
		
		
		JPanel bot=new JPanel(new FlowLayout(FlowLayout.TRAILING));
		pane.add(bot,BorderLayout.SOUTH);
		bot.add(new JButton(new AbstractAction("Cancel")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				OntClassChooser.this.selected=null;
				OntClassChooser.this.setVisible(false);
				OntClassChooser.this.dispose();
				}
			}));
		}
	
	public OntClass getSelected()
		{
		return this.selected;
		}
	}



/** Structured */
public class Structured
	{
	public static void main(String[] args)
		{
		try
			{
			JFrame.setDefaultLookAndFeelDecorated(true);
			JDialog.setDefaultLookAndFeelDecorated(true);
			
			
			int optind=0;
			while(optind< args.length)
				{
				if(args[optind].equals("-h") ||
				   args[optind].equals("-help") ||
				   args[optind].equals("--help"))
					{
					System.err.println("Options:");
					System.err.println(" -h help; This screen.");
					return;
					}
				else if(args[optind].equals("-p"))
					{
					//program= args[++optind];
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
			OntModelImpl model=new OntModelImpl();
			model.read(new File("/home/pierre/jeter.rdf"));
			//model.write(System.out);
			
			if("1".equals("2"))
				{
				OntClassChooser f=new OntClassChooser(null, model.getOntClasses());
				f.pack();
				f.setVisible(true);
				return;
				}
			
			if("1".equals("2"))
				{
				InstancePane x=null;//new InstancePane(model.getOntClasses().get(0));
				JDialog d=new JDialog();
				d.setContentPane(x);
				d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				d.pack();
				d.setVisible(true);
				return;
				}
			
			DataStoreImpl dataStore=new DataStoreImpl(model);
			final Frame f=new Frame(dataStore);
			SwingUtilities.invokeAndWait(new Runnable()
				{
				@Override
				public void run()
					{
					f.setVisible(true);
					
					}
				});
			} 
		catch(Throwable err)
			{
			err.printStackTrace();
			}
		}
	}
