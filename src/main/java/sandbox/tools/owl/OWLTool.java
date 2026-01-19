package sandbox.tools.owl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.io.IOUtils;
import sandbox.lang.StringUtils;
import sandbox.tools.central.ProgramDescriptor;

public class OWLTool extends Launcher
	{
	private static final String OWL="http://www.w3.org/2002/07/owl#";
	private static final String RDF="http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private static final String RDFS="http://www.w3.org/2000/01/rdf-schema#";
	private static final Logger LOG=Logger.builder(OWLTool.class).build();
	private static final QName rdfAbout=new QName(RDF,"about","rdf");
	private static final QName rdfRsrc=new QName(RDF,"resource","rdf");

	private enum SearchMethod {children,parents};
	private enum OutputFormat {tabular,recutils};
	
    @Parameter(names={"-o","--output"},description=OUTPUT_OR_STANDOUT)
    private Path out = null; 
    @Parameter(names={"-uri","--uri"},description="term uri")
    private String term_uri= null;
    @Parameter(names={"--method"},description="What to search")
    private SearchMethod method= SearchMethod.children;
    @Parameter(names={"--format"},description="output format")
    private OutputFormat outputFormat= OutputFormat.tabular;

    
    private class TermImpl {
	   	final String accession;
	    String name;
	    final Set<TermImpl> children = new HashSet<>();
	    final Set<TermImpl> parents = new HashSet<>();
		TermImpl(final String uri) {
				this.accession = uri;
				this.name= this.accession;
				}
		
		boolean isRoot(){
			return this.parents.isEmpty();
			}
		
		@Override
		public int hashCode()
			{
			return this.accession.hashCode();
			}
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (!(obj instanceof TermImpl)) return false;
			final TermImpl other = (TermImpl) obj;
			return this.accession.equals(other.accession);
			}
		
		void collectChildren(final Set<TermImpl> set) {
			if(set.contains(this)) return;
			set.add(this);
			for(TermImpl c:this.children) c.collectChildren(set);
			}
		void collectParents(final Set<TermImpl> set) {
			if(set.contains(this)) return;
			set.add(this);
			for(TermImpl c:this.parents) c.collectParents(set);
			}
		@Override
		public String toString() {
			return "("+this.accession+" "+this.name+")";
			}
    	}
	
    
    private final Map<String, TermImpl> uri2term = new HashMap<>();
    
    
    private final Set<TermImpl> getRoots() {
    	return uri2term.values().stream().filter(T->T.isRoot()).collect(Collectors.toSet());
    	}
    
    private TermImpl findTermByUri(final String uri) {
    	TermImpl t= uri2term.get(uri);
    	if(t==null) {
    		t = new TermImpl(uri);
    		uri2term.put(uri,t);
    	}
    	return t;
    }
    
    private void consumme(final XMLEventReader r) throws IOException,XMLStreamException {
	while(r.hasNext())
		{
		final XMLEvent evt=r.nextEvent();
		if(evt.isStartElement())
			{
			consumme(r);
			}
		else if(evt.isEndElement()) {
			break;
			}
		}
    }
    
    private String parseRestriction(final XMLEventReader r) throws IOException,XMLStreamException {
    	boolean onPropertyOk=true;
    	String parentUri  = null;
    	while(r.hasNext())
    		{
    		final XMLEvent evt=r.nextEvent();
    		if(evt.isStartElement())
    			{
    			final StartElement E=evt.asStartElement();
    			final QName qN=E.getName();
    			if( OWL.equals(qN.getNamespaceURI()) && qN.getLocalPart().equals("someValuesFrom"))
    				{
    				final Attribute att=E.getAttributeByName(rdfRsrc);
    				if(att!=null) {
    					parentUri = att.getValue();
    					}
    				}
    			else if( OWL.equals(qN.getNamespaceURI()) && qN.getLocalPart().equals("onProperty"))
					{
					final Attribute att=E.getAttributeByName(rdfRsrc);
					if(att!=null) {
						//System.err.println( "RESTRICTION\t"+  att.getValue());
						
						/*
						String onProperty = att.getValue();
						if(!(onProperty.equals("http://purl.obolibrary.org/obo/RO_0002202"))) {
							onPropertyOk  = false;
							}*/
						}
					}
    			consumme(r);	
    			}
    		else if(evt.isEndElement()) {
    			break;
    			}
    		}
    	return onPropertyOk?parentUri:null;
        }
    
    private String subClassWithRestriction(final XMLEventReader r) throws IOException,XMLStreamException {
    	String parentUri  = null;
    	while(r.hasNext())
    		{
    		final XMLEvent evt=r.nextEvent();
    		if(evt.isStartElement())
    			{
    			final StartElement E=evt.asStartElement();
    			final QName qN=E.getName();
    			if( OWL.equals(qN.getNamespaceURI()) && qN.getLocalPart().equals("Restriction"))
    				{
    				parentUri = parseRestriction(r);
    				}
    			else
    				{
    				consumme(r);
    				}
    			}
    		else if(evt.isEndElement()) {
    			break;
    			}
    		}
    	return parentUri;
        }
    
	private void parseClass(
			final StartElement root,
			final XMLEventReader r) throws IOException,XMLStreamException
		{
		final Attribute aboutAtt=root.getAttributeByName(rdfAbout);
		if(aboutAtt==null)
			{
			LOG.info("no rdf:about in "+root.getLocation());
			consumme(r);
			return;
			}
		final String termUri=aboutAtt.getValue();
		boolean obsolete_flag=false;
		final TermImpl term;
		if(this.uri2term.containsKey(termUri)) {
			term = this.uri2term.get(termUri);
			}
		else
			{
			term = new TermImpl(termUri);
			}
		final Set<TermImpl> parents = new HashSet<>();	
		while(r.hasNext())
			{
			final XMLEvent evt=r.nextEvent();
			if(evt.isStartElement())
				{
				final StartElement E=evt.asStartElement();
				final QName qN=E.getName();
				final String localName = qN.getLocalPart();
				if( RDFS.equals(qN.getNamespaceURI()) && localName.equals("label"))
					{
					term.name = r.getElementText().trim();
					}
				else if( OWL.equals(qN.getNamespaceURI()) && localName.equals("deprecated"))
					{
					if(r.getElementText().equals("true")) obsolete_flag=true;
					}
				else if( RDFS.equals(qN.getNamespaceURI()) && localName.equals("subClassOf"))
					{
					String parentUri = null;
					final Attribute rsrc=E.getAttributeByName(rdfRsrc);
					if(rsrc!=null){
						parentUri = rsrc.getValue();
						consumme(r);
						}
					else {
						parentUri = subClassWithRestriction(r);
						}
					
					if(parentUri==null)
						{
						LOG.warning("no subClassOf/@rdf:resource for "+termUri);
						}
					else if(!parentUri.equals(termUri)) {
						parents.add(findTermByUri(parentUri));
						}
					}
				else
					{
					consumme(r);
					}
				}
			else if(evt.isEndElement())
				{
				final EndElement E=evt.asEndElement();
				final QName qN=E.getName();
				final String localName = qN.getLocalPart();
				if( OWL.equals(qN.getNamespaceURI()) && localName.equals("Class")) {
					break;
					}
				}
			}
		if(obsolete_flag) {
			return;
		}
		term.parents.addAll(parents);
		for(final TermImpl p:parents) {
			p.children.add(term);
			}
		
		if(!this.uri2term.containsKey(termUri)) {
			this.uri2term.put(termUri, term);
			}
		}	
    
    @Override
    public int doWork(final List<String> args) {
    	InputStream is = null;
		try
			{
			final String input = oneFileOrNull(args);
			is = (input==null?System.in:IOUtils.openStream(input));
			final XMLInputFactory xif = XMLInputFactory.newFactory();
			final XMLEventReader r=xif.createXMLEventReader(is);
			while(r.hasNext())
				{
				final XMLEvent evt=r.nextEvent();
				if(evt.isStartElement())
					{
					final StartElement E=evt.asStartElement();
					final QName qN=E.getName();
					if(qN.getLocalPart().equals("Class") && 
						OWL.equals(qN.getNamespaceURI()))
						{
						this.parseClass(E,r);
						}
					}
				}
			r.close();
			is.close();
			is=null;
			
			
			final Set<TermImpl> query;
			if(!StringUtils.isBlank(this.term_uri)) {
				final TermImpl t = this.uri2term.get(term_uri);
				if(t==null) {
					LOG.error("term not found "+term_uri);
					return -1;
					}
				query = new HashSet<>();
				query.add(t);
				}
			else
				{
				query = getRoots();
				}
			final Set<TermImpl> set = new HashSet<>();
			for(final TermImpl t:query) {
				if(method.equals(SearchMethod.children)) {
					t.collectChildren(set);
					} 
				else
					{
					t.collectParents(set);
					}
				}
			
				
			try(PrintWriter w=IOUtils.openPathAsPrintWriter(this.out)) {
				switch(this.outputFormat) {
					case recutils:
						w.println("%rec: Ontology");
						w.println("%key: id");
						w.println("%required: label");
						w.println();
						for(final TermImpl c:set) {
							w.println("id: "+c.accession);
							w.println("label: "+c.name);
							final Set<TermImpl> parents = new HashSet<>();
							c.collectParents(parents);
							parents.remove(c);
							for(final TermImpl p: parents) {
								w.println("is_a: "+p.accession);
								}
							w.println();
							}
						break;
					case tabular:
						for(final TermImpl c:set) {
							w.print(c.accession);
							w.print("\t");
							w.print(c.name);
							w.println();
							}
						break;
					default:break;
					}
				
				w.flush();
				}
				
			return 0;
			} 
		catch(final Throwable err)
			{
			LOG.error(err);
			return -1;
			}
		}
    
    public static ProgramDescriptor getProgramDescriptor() {
    	return new ProgramDescriptor() {
    		@Override
    		public String getName() {
    			return "owltool";
    			}
    		};
    	}	
    
	public static void main(final String[] args) {
		new OWLTool().instanceMainWithExit(args);
		}
		
	}
