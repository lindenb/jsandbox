package sandbox.tools.rdf2graph;

import java.awt.Color;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.gexf.Gexf;
import sandbox.io.IOUtils;
import sandbox.lang.StringUtils;
import sandbox.tools.central.ProgramDescriptor;
import sandbox.xml.stream.XmlStreamWriter;

public class RdfToGraph extends Launcher
{
private static final Logger LOG=Logger.builder(RdfToGraph.class).build();
private enum Format {graphviz,gexf};
@Parameter(names={"-o"},description=OUTPUT_OR_STANDOUT)
private Path output;
@Parameter(names={"--schema"},description="don't print instances of classes but show relationships between the class/properties")
private boolean show_schema = false;
@Parameter(names={"--literals"},description="hide literals")
private boolean hideLiterals = false;
@Parameter(names={"--format"},description="output format")
private Format outputFormat = Format.graphviz;

private String nodeToTitle(final Model model,final Resource sub, final Set<Property> propertiesForName) {
	String title= sub.toString();
	StmtIterator iter2 =model.listStatements(sub, null, RDFNode.class.cast(null));
	while(iter2.hasNext()) {
		final Statement stmt = iter2.next();
		if(!propertiesForName.contains(stmt.getPredicate())) continue;
		if(!stmt.getObject().isLiteral()) continue;
		title = stmt.getObject().asLiteral().getString();
		break;
		}
	iter2.close();
	return title;
	}

private Model extractSchema(final Model  model,final Set<Property> propertiesForName) {
	final Model modelOut = ModelFactory.createDefaultModel();
	
	/* add classes names */
	final Consumer<Resource> appendLabels = (RSRC->{
		StmtIterator iter = model.listStatements(RSRC, null,(RDFNode)null);
		while(iter.hasNext()) {
			final Statement stmt = iter.next();
			if(!propertiesForName.contains(stmt.getPredicate())) continue;
			if(!stmt.getObject().isLiteral()) continue;
			modelOut.add(stmt);
			}
		iter.close();
		});
	
	StmtIterator iter2 =model.listStatements();
	while(iter2.hasNext()) {
		final Statement stmt = iter2.next();
		
		
		final ExtendedIterator<Resource> iter_s = model.
				listObjectsOfProperty(stmt.getSubject(), RDF.type).
				filterKeep(N->N.isResource()).
				mapWith(N->N.asResource());
		while(iter_s.hasNext()) {
			final Resource subject_type = iter_s.next();
			appendLabels.accept(subject_type);
			
			appendLabels.accept(stmt.getPredicate());

			
			/* it's a literal, just add one example of value */
			if(stmt.getObject().isLiteral()) {
				final ExtendedIterator<Statement> iter_o = modelOut.
					listStatements(subject_type,stmt.getPredicate(),(RDFNode)null).
					filterKeep(S->S.getObject().isLiteral());
				/* no example provided before ? */
				if(!iter_o.hasNext()) {
					modelOut.add(subject_type,stmt.getPredicate(),stmt.getObject().asLiteral());
					}
				iter_o.close();
				}
			else if(stmt.getObject().isResource() && !stmt.getObject().isAnon())
				{
				final ExtendedIterator<Resource> iter_o = model.
						listObjectsOfProperty(stmt.getObject().asResource(), RDF.type).
						filterKeep(N->N.isResource()).
						mapWith(N->N.asResource());
				while(iter_o.hasNext()) {
					final Resource object_type = iter_o.next();
					appendLabels.accept(object_type);
					modelOut.add(subject_type,stmt.getPredicate(),object_type);
					}
				iter_o.close();
				}
			}
		iter_s.close();
		
		
		}
	return modelOut;
	}

@Override
public int doWork(final List<String> args) {
	try {
		final Set<Property> propertiesForName = new HashSet<>();
		propertiesForName.add(RDFS.label);
		propertiesForName.add(DC.title);
		propertiesForName.add(ResourceFactory.createProperty("http://xmlns.com/foaf/0.1/", "name"));
		
		
		final Model model0 = ModelFactory.createDefaultModel();
		for(String arg: args) {
			if(!IOUtils.isURL(arg)) {
				arg = Paths.get(arg).toUri().toString();
				}
			model0.read(arg);
			}
		final Model model;
		if(show_schema) {
			model = extractSchema(model0,propertiesForName);
			}
		else	
			{
			model = model0;
			}
			
		if(model.isEmpty()) {
			LOG.error("empty rdfstore");
			return -1;
			}
		final Set<Resource> rdfTypesSet = new HashSet<>();
		// collect rdf types
		{
		NodeIterator iter1 =model.listObjectsOfProperty(RDF.type);
		while(iter1.hasNext()) {
			final RDFNode obj = iter1.next();
			if(!obj.isResource()) continue;
			rdfTypesSet.add(obj.asResource());
			}
		iter1.close();
		}
		
			
		switch(this.outputFormat) {
			case gexf:
				{
				try(PrintWriter pw = super.openPathAsPrintWriter(this.output)) {
		    		final  XmlStreamWriter w= XmlStreamWriter.of(pw);		    		
		    		w.writeStartDocument("UTF-8","1.0");
		    		w.writeStartElement("gexf");
		    		w.writeAttribute("xmlns", Gexf.XMLNS);
		    		w.writeAttribute("xmlns:viz", Gexf.XMLNS_VIZ);
		    		w.writeAttribute("version",Gexf.VERSION);
	
					/* meta */
					w.writeStartElement("meta");
						w.writeStartElement("creator");
						  w.writeCharacters(RdfToGraph.class.getCanonicalName());
						w.writeEndElement();
						w.writeStartElement("description");
						  w.writeCharacters("RdfToGraph");
						w.writeEndElement();
					w.writeEndElement();

		    		/* graph */
		    		w.writeStartElement("graph");
		    		w.writeAttribute("mode", "static");
		    		w.writeAttribute("defaultedgetype", "directed");
		    		
		    		
		    		
		    		/* attributes */
		    		w.writeStartElement("attributes");
		    		w.writeAttribute("class","node");
		    		w.writeAttribute("mode","static");
		    		    		
		    		w.writeEndElement();//attributes
		    		
		    		final Map<RDFNode,String> node2id = new HashMap<>();
		    		/* nodes */
		    		w.writeStartElement("nodes");
		    		StmtIterator iter2 =model.listStatements();
	    			while(iter2.hasNext()) {
	    				final Statement stmt = iter2.next();
	    				if(stmt.getPredicate().equals(RDF.type)) continue;
		    			for(int side=0;side<2;++side) {
		    				final RDFNode node= (side==0?stmt.getSubject():stmt.getObject());
		    				if(node2id.containsKey(node)) continue;
		    				if(node.isLiteral() && this.hideLiterals) continue;
		    				final String id="n"+node2id.size();
		    				node2id.put(node,id);
			    			w.writeStartElement("node");
			    			w.writeAttribute("id", id);
			    			if(node.isLiteral()) {
			    				w.writeAttribute("label",node.asLiteral().toString());    			
			    				}
			    			else
			    				{
			    				final String title= nodeToTitle(model,node.asResource(),propertiesForName);
			    				w.writeAttribute("label",title);    	
			    				}
			    			if(node.isResource() ) {
			    				/** search rdfType */
								Resource rdfType=null;
								NodeIterator iter1 =model.listObjectsOfProperty(node.asResource(),RDF.type);
								while(iter1.hasNext()) {
									final RDFNode obj = iter1.next();
									if(!obj.isResource()) continue;
									rdfType = obj.asResource();
									}
								iter1.close();
								if(rdfType!=null && rdfTypesSet.contains(rdfType)) {
									int idx=0;
									for(Resource r2:rdfTypesSet) {
										if(r2==rdfType) {
											break;
											}
										idx++;
										}
					    			final Color col = Color.getHSBColor(idx/(float)rdfTypesSet.size(), 0.9f, 0.9f);
									w.writeEmptyElement("viz","color", Gexf.XMLNS_VIZ);
									w.writeAttribute("r",""+col.getRed());
									w.writeAttribute("g",""+col.getGreen());
									w.writeAttribute("b",""+col.getBlue());
									w.writeAttribute("a","0.6");
									}
				    			}

			    			w.writeEndElement();
		    				}
		    			}
		    		iter2.close();
		    		w.writeEndElement();//nodes

		    		w.writeStartElement("edges");
		    		int relid=0;
		    		iter2 =model.listStatements();
		    		while(iter2.hasNext()) {
		    			final Statement stmt = iter2.next();
		    			if(!node2id.containsKey(stmt.getSubject())) continue;
		    			if(!node2id.containsKey(stmt.getObject())) continue;
		    			w.writeEmptyElement("edge");
		    			w.writeAttribute("id", "E"+(++relid));
		    			w.writeAttribute("type","directed");
		    			w.writeAttribute("source",node2id.get(stmt.getSubject()));
		    			w.writeAttribute("target",node2id.get(stmt.getObject()));
		    			w.writeAttribute("label",model.shortForm(stmt.getPredicate().getURI()));
		    			}
		    		iter2.close();
		    		w.writeEndElement();//edge
		    		
		    		
		    		w.writeEndElement();
		    		w.writeEndDocument();
		    		w.close();
		    		pw.flush();
					}
				break;
				}
			default:
			case graphviz:
				{
				final Map<Resource,String> subjects2id = new HashMap<>();
					{
					ResIterator iter1 =model.listSubjects();
					while(iter1.hasNext()) {
						final Resource obj = iter1.next();
						subjects2id.put(obj,"n"+subjects2id.size()+1);
						}
					iter1.close();
					}
	
					
				try(PrintWriter pw = super.openPathAsPrintWriter(this.output)) {
					final int colorscheme= Math.max(11,Math.min(3, rdfTypesSet.size()));
					pw.println("digraph G {");
					pw.println("graph [fontsize=2];");
					pw.println("edge [fontsize=24];");
					pw.println("node [fontsize=24,style=filled,colorscheme=rdylgn"+colorscheme+"];");
					
					
					for(Resource sub: subjects2id.keySet()) {
						pw.print(subjects2id.get(sub));
						pw.print("[shape=rectangle");
						
						/** search rdfType */
						Resource rdfType=null;
						NodeIterator iter1 =model.listObjectsOfProperty(sub,RDF.type);
						while(iter1.hasNext()) {
							final RDFNode obj = iter1.next();
							if(!obj.isResource()) continue;
							rdfType = obj.asResource();
							}
						iter1.close();
						if(rdfType!=null) {
							int idx=0;
							for(Resource r2:rdfTypesSet) {
								if(r2==rdfType) {
									break;
									}
								idx++;
								}
							idx = idx%colorscheme;
							pw.print(";fillcolor="+(idx+1));
							}
						final String title= nodeToTitle(model,sub,propertiesForName);
						pw.print(";label=\""+StringUtils.escapeC(title)+"\"");
						pw.println("]");
						}
					int subjectid=1;
					StmtIterator iter2 =model.listStatements();
					while(iter2.hasNext()) {
						final Statement stmt = iter2.next();
						if(stmt.getPredicate().equals(RDF.type)) continue;
						if(!subjects2id.containsKey(stmt.getSubject())) continue;
						if(stmt.getSubject().isLiteral() && this.hideLiterals) continue;
						if(propertiesForName.contains(stmt.getPredicate())) continue;
						
						final String objectid;
						if(stmt.getObject().isResource() && subjects2id.containsKey(stmt.getObject().asResource())) {
							objectid = subjects2id.get(stmt.getObject().asResource());
							} else {
							objectid = "o"+(subjectid++);
							if(stmt.getObject().isLiteral()) {
								pw.print(objectid);
								pw.print("[shape=oval;label=\""+StringUtils.escapeC(stmt.getObject().asLiteral().toString())+"\"");
								pw.println("]");
								}
							else
								{
								pw.print(objectid);
								pw.print("[shape=oval;style=filled;fillcolor=pink;label=\"<"+StringUtils.escapeC(stmt.getObject().toString())+">\"");
								pw.println("]");
								}
							}
						pw.println(subjects2id.get(stmt.getSubject())+" -> "+objectid+"[label=\""+model.shortForm(stmt.getPredicate().getURI())+"\"]");
						}
					iter2.close();
					
					pw.println("}");
					pw.flush();
					}
				break;
				}
			}
		return 0;
		}
	catch(Throwable err) {
		LOG.error(err);
		return -1;
		}
	}


public static ProgramDescriptor getProgramDescriptor() {
	return new ProgramDescriptor() {
		@Override
		public String getName() {
			return "rdf2graph";
			}
		};
	}

public static void main(String[] args) {
	new RdfToGraph().instanceMain(args);
}
}
