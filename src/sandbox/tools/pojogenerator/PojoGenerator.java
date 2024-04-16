package sandbox.tools.pojogenerator;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
	    public List<ColumnDef> columns=new ArrayList<ColumnDef>();
	    ClassDef(final String name)  {
			super(name);
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

	
	private void run() throws Exception
		{
		if(fileout==null)
			{
			
			}
		else if(fileout.getName().toLowerCase().endsWith(".zip"))
			{
			ZipEntry entry;
			String path=packageName.replaceAll("[\\.]+","/");
			path="pojo/"+path;
			if(path.startsWith("/")) path=path.substring(1);
			FileOutputStream fout=new FileOutputStream(fileout);
			ZipOutputStream zout = new ZipOutputStream(fout);
			PrintWriter out;
			for(ClassDef c:defs)
				{
				if(this.createInterface)
					{
					entry= new ZipEntry(path+c.getJavaName()+".java");
					zout.putNextEntry(entry);
					out=new PrintWriter(zout);
					printInterface(out,c);
					out.flush();
					zout.closeEntry();
					zout.flush();
					}
				entry= new ZipEntry(path+c.getJavaName()+"Impl.java");
				zout.putNextEntry(entry);
				out=new PrintWriter(zout);
				printClass(out,c);
				out.flush();
				zout.closeEntry();
				zout.flush();
				}
			zout.finish();
			fout.flush();
			fout.close();
			}
		else if(fileout.isDirectory())
			{
			
			}
		else
			{
			
			}
		}
	
	public static void main(String[] args)
    {
    try {
 	    CodeGenerator app=new CodeGenerator();
        int optind=0;
        while(optind<args.length)
            {
            if(args[optind].equals("-h"))
                {
                System.out.println("-o <directory|*.zip> (default:stdout)");
                System.out.println("-p <package> default:"+app.packageName);
                return;
                }
            else if(args[optind].equals("-o"))
                {
                app.fileout=new File(args[++optind]);
                }
            else if(args[optind].equals("-p"))
                {
                app.packageName= args[++optind];
                }
            else if(args[optind].equals("--"))
                {
                optind++;
                break;
                }
            else if(args[optind].startsWith("-"))
                {
                System.err.println("Unnown option: "+args[optind]);
                return;
                }
            else
                {
                break;
                }
            ++optind;
            }
        
        if(optind==args.length)
            {
            app.defs.addAll(new PojoGenerator(new InputStreamReader(System.in)).input());
            }
        else
            {
            while(optind< args.length)
                {
                String inputName=args[optind++];
                FileReader in=new FileReader(inputName);
                app.defs.addAll(new PojoGenerator(in).input());
                in.close();
                }
            }
        app.run();
        } 
    catch (Exception e)
        {
        e.printStackTrace();
        }
    }

}
