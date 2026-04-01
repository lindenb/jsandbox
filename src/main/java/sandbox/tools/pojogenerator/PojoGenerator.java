package sandbox.tools.pojogenerator;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class PojoGenerator {

	public static class Type {
		
		}
	
	public static class SimpleType extends Type {
		final String type;
		SimpleType(final String type){
			this.type = type;
			}
		}
	
	public static class ListOf extends Type {
		final Type type;
		ListOf(final Type type){
			this.type = type;
			}
		}
	public static class MapOf extends Type {
		final Type key;
		final Type value;
		MapOf(Type key,Type value) {
			this.key = key;
			this.value = value;
		}
	}

	
	public abstract class TypeDef
	    {
	    protected final String name;
	    protected TypeDef(final String name) {
	    	this.name = name;
	    	}
	    
	    public String getJavaName()
	        {
	        return name.substring(0,1).toUpperCase()+name.substring(1);
	        }
	    }

	public class ColumnDef extends TypeDef
	    {
	    public Type type;
		ColumnDef(final String name,final Type type)  {
			super(name);
			this.type = type;
			}
	    public String getPropertyName()
	    	{
	    	return name.toUpperCase()+"_PROPERTY";
	    	}
	    }

	public class ClassDef extends TypeDef
	    {
	    public final List<ColumnDef> columns=new ArrayList<ColumnDef>();
	    ClassDef(final String name)  {
			super(name);
			}
	    public void writeXsd(XMLStreamWriter w) throws XMLStreamException {
	    	w.writeStartElement("xsd", "element", XMLConstants.W3C_XML_SCHEMA_NS_URI);
	    	
	    	w.writeEndElement();
	    	}
		}

	
	boolean createInterface=false;
	boolean createListeners=false;
	String packageName="generated";
	Path fileout=null;
	List<ClassDef> defs=new ArrayList<ClassDef>();
	
	/** print class */
    public void printClass(PrintWriter out,ClassDef clazz)
            {
            String className=clazz.getJavaName()+(createInterface?"Impl":"");
            out.println("package "+packageName+";");
            out.print("class "+className);
            if(createInterface) out.println("Impl");
            if(createInterface)
            	{
             	out.print(" implements ");
             	out.print(" "+clazz.getJavaName());
             	}
            out.println("\t{");
            if(createListeners)
            	{
            	for(ColumnDef col: clazz.columns)
                 {
                 out.println("public static final String "+col.getPropertyName()+"=\""+col.name+"\";");
                 }
            	out.println("\tprivate java.beans.PropertyChangeSupport listener=null;");
            	}
            	
            out.println("/** empty constructor */");
            out.println("public "+className+"()");
            out.println("	{");
            out.println("	}");
            
            out.println("/** constructor */");
            out.print("public "+className+"(");
            for(int i=0;i< clazz.columns.size();++i)
            	{
            	ColumnDef col=clazz.columns.get(i);
            	if(i>0) out.print(",");
            	out.print(col.type+" "+col.name);
            	}
            out.println(")");
            out.println("	{");
            for(ColumnDef col: clazz.columns)
                {
                out.println("	this."+col.name+" = "+col.name+";");
                }
            out.println("	}");
            
            for(ColumnDef col: clazz.columns)
                    {
                    out.println("\tprivate\t"+col.type+"\t"+col.name+";");
                    }
            for(ColumnDef col: clazz.columns)
                {
                /** GETTER */
                 if(createInterface) out.println("@Override");
                 out.println("public "+col.type+" get"+col.getJavaName()+"()");
                 out.println("\t{");
                 out.println("\treturn this."+col.name+";");
                 out.println("\t}");
                 
                 /** SETTER */
                 if(createInterface) out.println("@Override");
                 out.println("public void set"+col.getJavaName()+"("+col.type+" "+col.name+")");
                 out.println("\t{");
                 if(createListeners)
                 	{
                	out.println("\t"+col.type+" _old"+col.name+"=this."+col.name+";");
                	}
               	 out.println("\tthis."+col.name+"="+col.name+";");
                  if(createListeners)
                 	{
                	out.println("\tif(this.listener!=null) this.listener.firePropertyChange("+col.getPropertyName()+");");
                	}
                
                }
            out.println("\t}");
            }
    

    void printInterface(PrintWriter out,ClassDef clazz)
            {
            out.println("package "+packageName+";");
            out.println("class "+clazz.getJavaName());
            out.println("\t{");
            for(ColumnDef col: clazz.columns)
                    {
                    out.println("public void set"+col.getJavaName()+"("+col.type+" "+col.name+");");
                    out.println("public "+col.type+" get"+col.getJavaName()+"();");
                    }
           
            out.println("\t}");
            }
    
    public void printTupleBinding(PrintWriter out,ClassDef clazz)
        {
        out.println("package "+packageName+";");
        out.println("class "+clazz.name+"Binding extends TupleBinding<"+clazz.name+">");
        out.println("\t{");
        for(ColumnDef col: clazz.columns)
                {
                //col.printClass(out);
                }
       
        out.println("\t}");
        }
    
    public Type createMapOf(Type key,Type value) {
    	return new MapOf(key,value);
    	}
    
    public Type createListOf(Type t) {
    	return new ListOf(t);
    	}
    
    public Type createSimpleType(String s) {
    	return new SimpleType(s);
    	}
    
 	public ClassDef createClass(String s) {
    	return new ClassDef(s);
    	}
	
	public ColumnDef createColumn(final String name,final Type type)  {
		return new ColumnDef(name,type);
		}
		
		
	private void run() throws Exception
		{

		}
	
	public static void main(String[] args)
    {

    try {
 	   
       
        } 
    catch (Exception e)
        {
        e.printStackTrace();
        }
    }

}
