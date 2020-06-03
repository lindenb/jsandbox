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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.beust.jcommander.Parameter;

import sandbox.IOUtils;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;

public class OWLTool extends Launcher
	{
	private static final String OWL="http://www.w3.org/2002/07/owl#";
	private static final String RDF="http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private static final String RDFS="http://www.w3.org/2000/01/rdf-schema#";
	private static final Logger LOG=Logger.builder(OWLTool.class).build();
	private static final QName rdfAbout=new QName(RDF,"about","rdf");
	private static final QName rdfRsrc=new QName(RDF,"resource","rdf");

	private enum SearchMethod {children,parents};
	
    @Parameter(names={"-o","--output"},description=OUTPUT_OR_STANDOUT)
    private Path out = null; 
    @Parameter(names={"-uri","--uri"},description="term uri")
    private String term_uri= null;
    @Parameter(names={"--method"},description="What to search")
    private SearchMethod method= SearchMethod.children;

    
    private class TermImpl {
	   	final String accession;
	    String name;
	    final Set<TermImpl> children = new HashSet<>();
	    final Set<TermImpl> parents = new HashSet<>();
		TermImpl(final String uri) {
				this.accession = uri;
				this.name= this.accession;
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
    	}
	
    
    private final Map<String, TermImpl> uri2term = new HashMap<>();
    
    
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
    
	private void parseClass(
			final StartElement root,
			final XMLEventReader r) throws IOException,XMLStreamException
		{
		final Attribute aboutAtt=root.getAttributeByName(rdfAbout);
		if(aboutAtt==null)
			{
			return;
			}
		final String termUri=aboutAtt.getValue();
		
		boolean obsolete_flag=false;
		final TermImpl term = new TermImpl(termUri);
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
					final Attribute rsrc=E.getAttributeByName(rdfRsrc);
					if(rsrc!=null){
						final String parentUri=rsrc.getValue();
						if(!parentUri.equals(termUri)) {
							parents.add(findTermByUri(parentUri));
							}
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
		if(obsolete_flag)  return;
		term.parents.addAll(parents);
		for(final TermImpl p:parents) p.children.add(term);
		this.uri2term.put(termUri, term);
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
			if(!StringUtils.isBlank(this.term_uri)) {
				TermImpl t = this.uri2term.get(term_uri);
				if(t==null) {
					LOG.error("term not found "+term_uri);
					return -1;
					}
				try(PrintWriter w=IOUtils.openPathAsPrintWriter(this.out)) {
					final Set<TermImpl> set = new HashSet<>();
					if(method.equals(SearchMethod.children)) {
						t.collectChildren(set);
					} else
						{
						t.collectParents(set);
						}
					
					for(TermImpl c:set) {
						w.print(c.accession);
						w.print("\t");
						w.print(c.name);
						w.println();
						}
					w.flush();
					}
				}
			return 0;
			} 
		catch(final Throwable err)
			{
			LOG.error(err);
			return -1;
			}
		}
	public static void main(final String[] args) {
		new OWLTool().instanceMainWithExit(args);
		}
		
	}
