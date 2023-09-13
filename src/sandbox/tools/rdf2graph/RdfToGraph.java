package sandbox.tools.rdf2graph;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.io.IOUtils;

public class RdfToGraph extends Launcher
{
private static final Logger LOG=Logger.builder(RdfToGraph.class).build();

@Parameter(names={"-o"},description=OUTPUT_OR_STANDOUT)
private Path output;
@Parameter(names={"--literals"},description="hide literals")
private boolean hideLiterals = false;


@Override
public int doWork(final List<String> args) {
	try {
		final Set<Property> propertiesForName = new HashSet<>();
		propertiesForName.add(RDFS.label);
		propertiesForName.add(DC.title);
		propertiesForName.add(ResourceFactory.createProperty("http://xmlns.com/foaf/0.1/", "name"));
		
		
		final Model model = ModelFactory.createDefaultModel();
		for(String arg: args) {
			if(!IOUtils.isURL(arg)) {
				arg = Paths.get(arg).toUri().toString();
				}
			model.read(arg);
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
		return 0;
		}
	catch(Throwable err) {
		LOG.error(err);
		return -1;
		}
	}

public static void main(String[] args) {
	new RdfToGraph().instanceMain(args);
}
}
