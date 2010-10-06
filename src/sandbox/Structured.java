import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;


import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
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


enum RangeType
	{
	STRING,
	LONG,
	DATE,
	OBJECT
	}

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

abstract class Range
	{
	public abstract  String validate(InstanceOfProperty prop,String input);
	}

class StringRange
	extends Range
	{
	private Pattern pattern;
	
	public Pattern getPattern()
		{
		return pattern;
		}
	
	public String validate(InstanceOfProperty prop,String input)
		{
		if(getPattern()!=null && getPattern().matcher(input).matches())
			{
			return "doesn't match "+getPattern().pattern();
			}
		return null;
		}
	}

class LongRange
extends Range
	{
	@Override
	public String validate(InstanceOfProperty prop, String input)
		{
		try
			{
			Long.parseLong(input);
			}
		catch (NumberFormatException e)
			{
			return "Not a long";
			}
		return null;
		}
	}

class DoubleRange
extends Range
	{
	@Override
	public String validate(InstanceOfProperty prop, String input)
		{
		try
			{
			Double.parseDouble(input);
			}
		catch (NumberFormatException e)
			{
			return "Not a double";
			}
		return null;
		}
	}

class ObjectRange
extends Range
	{
	private Set<OntClass> ontClassInRange=new HashSet<OntClass>();
	
	void add(OntClass c)
		{
		this.ontClassInRange.add(c);
		}
	
	public Set<OntClass> getOntClassesInRange()
		{
		return this.ontClassInRange;
		}
	
	@Override
	public String validate(InstanceOfProperty prop, String input)
		{
		InstanceOfClass i=prop.getInstance().getDataStore().getInstanceById(input);
		if(i==null) return null;
		if(getOntClassesInRange().contains(i.getOntClass())) return null;
		return "Bad Class";
		}
	
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

interface OntProperty extends OntNode
	{
	public OntClass getOntClass();
	public Range getRange();
	public Integer getMinCardinality();
	public Integer getMaxCardinality();
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
	}

interface InstanceEditor
	{
	public void setDataStore(Store ds);
	public void setInstance(InstanceOfClass instance);
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
class OntPropertyImpl
	extends AbstractOntNode
	implements OntProperty,Cloneable
	{
	private OntModel ontModel;
	private OntClass ontClass;
	private Range range;
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
	
	void setRange(Range range)
		{
		this.range = range;
		}
	
	@Override
	public Range getRange()
		{
		return range;
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

	
	@Override
	protected Object clone()
		{
		OntPropertyImpl prop=new OntPropertyImpl();
		return prop;
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
				Attr  att=e1.getAttributeNodeNS(RDF.NS, "about");
				Set<OntClassImpl> ontClasses=new HashSet<OntClassImpl>();
				
				NodeList list2=(NodeList)xpath.evaluate("rdfs:domain/@rdf:resource",root,XPathConstants.NODESET);
				for(int j=0;j< list2.getLength();++j)
					{
					att=Attr.class.cast(list2.item(j));
					OntClassImpl c=OntClassImpl.class.cast(getOntClassByUri(att.getValue()));
					if(c==null) throw new IOException("cannot get OntClass "+att.getValue());
					ontClasses.add(c);	
					}
				
				for(OntClassImpl ontClass:ontClasses)
					{
					OntPropertyImpl ontProperty=new OntPropertyImpl();
					ontProperty.setUri(att.getValue());
					ontProperty.setOntClass(ontClass);
					ontClass.add(ontProperty);
					
					for(Node c2=e1.getFirstChild();c2!=null;c2=c2.getNextSibling())
						{
						if(c2.getNodeType()!=Node.ELEMENT_NODE) continue;
						Element e2=Element.class.cast(c2);
						if(	e2.getNamespaceURI().equals(RDFS.NS) &&
							e2.getLocalName().equals("label"))
							{
							ontProperty.setLabel(e2.getTextContent());
							}
						else if(e2.getNamespaceURI().equals(RDFS.NS) &&
								e2.getLocalName().equals("comment"))
							{
							ontProperty.setDescription(e2.getTextContent());
							}
						else if(e2.getNamespaceURI().equals(OWL.NS) &&
								e2.getLocalName().equals("minCardinality"))
							{
							ontProperty.setMinCardinality(Integer.parseInt(e2.getTextContent()));
							}
						else if(e2.getNamespaceURI().equals(OWL.NS) &&
								e2.getLocalName().equals("maxCardinality"))
							{
							ontProperty.setMaxCardinality(Integer.parseInt(e2.getTextContent()));
							}
						else if(e2.getNamespaceURI().equals(RDFS.NS) &&
								e2.getLocalName().equals("range"))
							{
							att=e2.getAttributeNodeNS(RDF.NS, "resource");
							if(att==null) continue;
							String range=att.getValue();
							if(range.equals(XSD.NS+"string"))
								{
								StringRange r=new StringRange();
								ontProperty.setRange(r);
								for(Node c3=c2.getFirstChild();
									c3!=null;c3=c3.getNextSibling())
									{
									if(c3.getNodeType()!=Node.ELEMENT_NODE) continue;
									Element e3=Element.class.cast(c3);
									if(	e3.getNamespaceURI().equals(RDFS.NS) &&
										e3.getLocalName().equals("pattern"))
										{
										ontProperty.setLabel(e3.getTextContent());
										}
									}
								}
							else if(range.equals(XSD.NS+"int"))
								{
								LongRange r=new LongRange();
								ontProperty.setRange(r);
								}
							else if(range.equals(XSD.NS+"double"))
								{
								DoubleRange r=new DoubleRange();
								ontProperty.setRange(r);
								}
							else
								{
								if(range.startsWith("#")) range=range.substring(1);
								OntClassImpl ont =OntClassImpl.class.cast(getOntClassByUri(range));
								if(ont==null) throw new IOException("cant find class in domain "+range);
								
								}
							}
						}
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
	public DataStoreImpl(OntModel model)
		{
		this.model=model;
		}
	
	public List<InstanceOfClass> getInstances()
		{
		return this.instances;
		}
	
	@Override
	public InstanceOfClass getInstanceById(String id)
		{
		for(InstanceOfClass i: getInstances())
			{
			if(i.getId().equals(id)) return i;
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
		for(InstanceOfClass ioc:getInstances())
			{
			writeInstance(ioc,w);
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

class InstanceEd
	extends JDialog
	{
	private Store store;
	private InstanceOfClass instance;
	private OntClass ontClass;
	//private JPanel contentPane;
	public InstanceEd()
		{
		this.addWindowListener(
			new WindowAdapter()
				{
				
				}
			);
		JPanel pane=new JPanel(new BorderLayout(5,5));
		pane.setBorder(new EmptyBorder(5,5,5,5));
		
		JPanel bot=new JPanel(new FlowLayout(FlowLayout.TRAILING));
		pane.add(bot,BorderLayout.SOUTH);
		
		AbstractAction action=new AbstractAction("OK")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				
				}
			};
		bot.add(new JButton(action));
		
		action=new AbstractAction("Cancel")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				
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
	}

@SuppressWarnings("serial")
class Frame
	extends JFrame
	{
	private File saveAs;
	private Store dataStore;
	private boolean documentModified=false;
	private JPanel cardPane;
	
	Frame(Store dataStore)
		{
		super("Structured");
		this.dataStore=dataStore;
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter()
			{
			@Override
			public void windowOpened(WindowEvent e)
				{
				Dimension d=Toolkit.getDefaultToolkit().getScreenSize();
				e.getWindow().setBounds(50, 50, d.width-100, d.height-100);
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
		
		contentPane.add(new JScrollPane(new JTable(10, 10)));
		
		JPanel top=new JPanel(new FlowLayout(FlowLayout.LEADING));
		contentPane.add(top,BorderLayout.NORTH);
		
		JMenu subMenu=new JMenu("Create");
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
		
		documentModified=true;
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

/** Structured */
public class Structured
	{
	public static void main(String[] args)
		{
		try
			{
			JFrame.setDefaultLookAndFeelDecorated(true);
			JDialog.setDefaultLookAndFeelDecorated(true);
			Structured app=new Structured();
			
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
			model.write(System.out);
			DataStoreImpl dataStore=new DataStoreImpl(model);
			final Frame f=new Frame(dataStore);
			if(model.equals(null)) SwingUtilities.invokeAndWait(new Runnable()
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
