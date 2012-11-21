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

    /**  method */
    private void run()  throws Exception
        {
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
        if(name2target.isEmpty())
        	{
        	System.err.println("No target found.\nUsage:\n make -dq | java -jar makegraphdependencies.jar\n");
        	System.exit(-1);
        	}
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
    
    public static void main(String args[]) throws Exception
        {
        new MakeGraphDependencies().run();
        }

	}
