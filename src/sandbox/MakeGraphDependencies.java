/**
 * 
 */
package sandbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author lindenb
 *
 */
public class MakeGraphDependencies
	{
	/** input stream */
    private BufferedReader in=null;
    private Map<String,Target> name2target=new HashMap<String,Target>();

    private static class Target
        {
        int id;
        String name;
        Set<Target> children=new HashSet<Target>();
        boolean must_remake=false;
        
        public int hashCode()
            {
            return name.hashCode();
            }
        public boolean equals(Object o)
            {
            return o==this;
            }
        String node()
            {
            return "n"+id;
            }
        }
    
    /** get target, create it it doesn't exist  */
    private Target getTarget(String name)
        {
        Target t=this.name2target.get(name);
        if(t==null)
            {
            t=new Target();
            t.id=this.name2target.size();
            t.name=name;
            name2target.put(name,t);
            }
        return t;
        }
    
    /** get next line from stdin  */
    private String nextLine() throws IOException
        {
        if(in==null) in=new BufferedReader(new InputStreamReader(System.in));
        return in.readLine();
        }


    /** extract target name */
    private static String targetName(String line)
        {
        int i=line.indexOf('`');
        int j=line.indexOf('\'');
        return line.substring(i+1,j);
        }
    
    /** recursive scannning for the current target  */
    private void recursive(Target target)  throws Exception
        {
        String line;
        while((line=nextLine())!=null)
            {
            line=line.trim();
            if(line.startsWith("Considering target file"))
                {
                Target child=getTarget(targetName(line));
                target.children.add(child);
                recursive(child);
                }
            else if(line.startsWith("Must remake target "))
            	{
            	getTarget(targetName(line)).must_remake=true;
            	}
            else if(line.startsWith("Pruning file "))
            	{
            	target.children.add(getTarget(targetName(line)));
            	}
            else if(line.startsWith("Finished prerequisites of target file "))
                {
                if(!targetName(line).equals(target.name))
                	{
                	throw new IllegalStateException("expected "+ target.name + " got "+ line);
                	}
                return;
                }
            }
        throw new IllegalStateException(target.name);
        }

    private void usage()
    	{
		System.err.println("Options:");
		System.err.println(" -h help; This screen.");
		System.err.println(" -G gexf output.");
    	}
    
    /**  method */
    private void run(String args[])  throws Exception
        {
        boolean gexf=false;
        int optind=0;
		while(optind< args.length)
			{
			if(args[optind].equals("-h") ||
			   args[optind].equals("-help") ||
			   args[optind].equals("--help"))
				{
				usage();
				return;
				}
			else if(args[optind].equals("-g"))
				{
				gexf=true;
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
		
        Target root=getTarget("[ROOT]");
        String line;
        while((line=nextLine())!=null)
            {
            line=line.trim();
            if(!line.startsWith("Considering target file")) continue;
            Target child=getTarget(targetName(line));
            root.children.add(child);
            recursive(child);
            }
        if(name2target.size()<=1)
        	{
        	System.err.println("No target found.\nUsage:\n make -d --dry-run | java -jar makegraphdependencies.jar\n");
        	System.exit(-1);
        	}
        if(gexf)
        	{
    		XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
    		XMLStreamWriter w= xmlfactory.createXMLStreamWriter(System.out,"UTF-8");
    		
    		w.writeStartDocument("UTF-8","1.0");
    		w.writeStartElement("gexf");
    		w.writeAttribute("xmlns", "http://www.gexf.net/1.2draft");
    		w.writeAttribute("version", "1.2");
    		
    		
    		/* meta */
    		w.writeStartElement("meta");
    			w.writeStartElement("creator");
    			  w.writeCharacters(MakeGraphDependencies.class.getCanonicalName());
    			w.writeEndElement();
    			w.writeStartElement("description");
    			  w.writeCharacters("Twitter Graph");
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
    		
    		/* nodes */
    		w.writeStartElement("nodes");
    		for(Target t:this.name2target.values())
    			{
    			w.writeStartElement("node");
    			w.writeAttribute("id", String.valueOf(t.node()));
    			w.writeAttribute("label", t.name);    			
    			w.writeEndElement();
    			}

    		w.writeEndElement();//nodes
    		
    		/* edges */
    		int relid=0;
    		w.writeStartElement("edges");
    	    for(Target t:this.name2target.values())
 	            {
 	           for(Target c:t.children)
	                {
	    			w.writeEmptyElement("edge");
	    			w.writeAttribute("id", "E"+(++relid));
	    			w.writeAttribute("type","directed");
	    			w.writeAttribute("source",c.node());
	    			w.writeAttribute("target",t.node());
	                }
    			}

    		w.writeEndElement();//edges

    		w.writeEndElement();//graph
    		
    		w.writeEndElement();//gexf
    		w.writeEndDocument();
    		w.flush();

        	}
        else //dot
        	{
	        System.out.println("digraph G {");
	        for(Target t:this.name2target.values())
	            {
	            System.out.println(t.node()+"[label=\""+t.name
	            		+"\" color=\""+
	            		(t.must_remake?"red":"green")+
	            		"\"];");
	            }
	        for(Target t:this.name2target.values())
	            {
	            for(Target c:t.children)
	                {
	                System.out.println(c.node()+" -> "+t.node()+";");
	                }
	            }
	        System.out.println("}");
        	}
        }
    
    public static void main(String args[]) throws Exception
        {
        new MakeGraphDependencies().run(args);
        }

	}
