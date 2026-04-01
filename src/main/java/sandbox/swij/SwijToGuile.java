package sandbox.swij;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import sandbox.Launcher;
/**
# Example
```
echo "ZOER bcf_destroy(bcf_t* b); long  bcf_init(long t,int i, bool b);" | java -jar dist/swij2guile.jar 
```

*/
public class SwijToGuile extends Launcher {
private	String prefix="scm_";

private class GuileFun {
	private final SwijParser.Function fun;
	GuileFun(final SwijParser.Function fun) {
		this.fun=fun;
	}
	
	String getGuileName() {
	return this.fun.name.replace('_','-')+(this.fun.type.isBoolean()?"?":"");
	}
	
	void print(PrintStream w) {
		final String margin="  ";
		w.println("/**");
		w.println(" * BEGIN_DOC");
		w.println(" * ## "+getGuileName());
		w.println(" * ");
		w.println(" * ```");
		  w.print(" * ("+getGuileName());
		for(int i=0;i< fun.parameters.size();i++) {
			w.print(" "+fun.parameters.get(i).name);
			}
		w.println(")");
		w.println(" * ```");
		w.println(" * END_DOC");
		w.println(" **/");
		w.println("#define FUN_NAME \""+getGuileName()+"\"");
		w.print("SCM_DEFINE(");
		w.print(prefix);
		w.print(fun.name);
		w.print(",FUN_NAME,");
		w.print(fun.parameters.size());
		w.print(",0,0,(");
		for(int i=0;i< fun.parameters.size();i++) {
			if(i>0) w.print(", ");
			w.print("SCM ");
			w.print("s_"+fun.parameters.get(i).name);
		}
		w.print("),\"(\" FUN_NAME \"");
		
		 for(int i=0;i< fun.parameters.size();i++) {
                        w.print(" "+fun.parameters.get(i).name);
                        }

		w.println(") a wrapper for the C function "+ fun.name + "\") {");
		if(!fun.type.isVoid()) {
			w.println(margin + fun.type.getCName()+" _ret = "+fun.type.getDefaultValue()+":");
			}
		for(int i=0;i< fun.parameters.size();i++) {
			final SwijParser.Param p = fun.parameters.get(i);
			w.println(margin + p.type.getCName()+" "+p.name+"="+p.type.getDefaultValue()+";");
			}
		for(int i=0;i< fun.parameters.size();i++) {
			final SwijParser.Param p = fun.parameters.get(i);
			if(p.type.isInt()) {
				w.println(margin + "SCM_ASSERT(scm_is_integer(s_"+p.name+"),s_"+p.name+","+i+",FUN_NAME);");
				}
			else if(p.type.isShort()) {
				w.println(margin + "SCM_ASSERT(scm_is_short(s_"+p.name+"),s_"+p.name+","+i+",FUN_NAME);");
				}
			else if(p.type.isLong()) {
				w.println(margin + "SCM_ASSERT(scm_is_long(s_"+p.name+"),s_"+p.name+","+i+",FUN_NAME);");
				}
			else if(p.type.isBoolean()) {
				w.println(margin + "SCM_ASSERT(scm_is_bool(s_"+p.name+"),s_"+p.name+","+i+",FUN_NAME);");
				}
			else if(p.type.isDouble() || p.type.isFloat()) {
				w.println(margin + "SCM_ASSERT(scm_is_double(s_"+p.name+"),s_"+p.name+","+i+",FUN_NAME);");
				}
			else if(p.type.isString()) {
				w.println(margin + "SCM_ASSERT(scm_is_string(s_"+p.name+"),s_"+p.name+","+i+",FUN_NAME);");
				}
			else
				{
				w.println(margin + "SCM_ASSERT(SCM_POINTER_P(s_"+p.name+"),s_"+p.name+","+i+",FUN_NAME);");
				}
			}
		for(int i=0;i< fun.parameters.size();i++) {
			final SwijParser.Param p = fun.parameters.get(i);
			if(p.type.isInt()) {
				w.println(margin + p.name+"= scm_to_int(s_"+p.name+");");
				}
			else if(p.type.isShort()) {
				w.println(margin + p.name+"= scm_to_int(s_"+p.name+");");
				}
			else if(p.type.isLong()) {
				w.println(margin + p.name+"= scm_to_long(s_"+p.name+");");
				}
			else if(p.type.isFloat()) {
				w.println(margin + p.name+"= (float)scm_to_double(s_"+p.name+");");
				}
			else if(p.type.isDouble()) {
				w.println(margin + p.name+"= scm_to_double(s_"+p.name+");");
				}
			else if(p.type.isBoolean()) {
				w.println(margin + p.name+"= scm_to_bool(s_"+p.name+");");
				}
			else if(p.type.isString()) {
				w.println(margin + p.name+"= scm_to_locale_string(s_"+p.name+");");
				}
			else
				{
				w.println(margin + p.name+"=("+p.type.getCName()+")scm_to_pointer(s_"+p.name+");");
				}
			}
		if(!fun.type.isVoid()) {
			w.print(margin + "_ret = ");
			}
		w.print(fun.name);
		w.print("(");
		for(int i=0;i< fun.parameters.size();i++) {
			if(i>0) w.print(", ");
			w.print(fun.parameters.get(i).name);
			}
		w.println(");");
		
		for(int i=0;i< fun.parameters.size();i++) {
			final SwijParser.Param p = fun.parameters.get(i);
			 if(p.type.isString()) {
					w.println(margin + "free("+p.name+");");
					}
			}
		if(fun.type.isVoid()) {
			w.println(margin + "return SCM_UNDEFINED;");
			}
		else if(fun.type.isString()) {
			w.println(margin + "{");
			w.println(margin + "SCM _ret2= scm_from_locale_string(_ret);");
			w.println(margin + "free(_ret);//TODO");
			w.println(margin + "return _ret2;");
			w.println(margin + "}");
			}
		else if(fun.type.isBoolean()) {
			w.println(margin + "return scm_from_bool(_ret);");
			}
		else if(fun.type.isInt()) {
			w.println(margin + "return scm_from_int(_ret);");
			}
		else if(fun.type.isShort()) {
			w.println(margin + "return scm_from_short(_ret);");
			}
		else if(fun.type.isLong()) {
			w.println(margin + "return scm_from_long(_ret);");
			}
		else if(fun.type.isFloat()) {
			w.println(margin + "return scm_from_double((double)_ret);");
			}
		else if(fun.type.isDouble()) {
			w.println(margin + "return scm_from_double(_ret);");
			}
		else
			{
			w.println(margin + "if(_ret==NULL) return  SCM_UNDEFINED;");
			w.println(margin + "return scm_from_pointer(_ret,_dispose_"+fun.type.getCName().replace("*","_ptr")+");");
			}
		w.println("}");
		w.println("#undef FUN_NAME");
		w.println();
	}
}
	
@Override
public int doWork(List<String> args) {
	InputStream in=null;
	
	try {
		final String input=oneFileOrNull(args);
		in= (input==null?System.in:Files.newInputStream(Paths.get(input)));
		SwijParser parser = new SwijParser(in);
		parser.input();
		in.close();
		PrintStream out=System.out;
		
		for(final String fun :parser.functions.stream().map(F->F.type).filter(T->!(T.isPrimitive() || T.isVoid())).
			map(T->"_dispose_"+T.getCName().replace("*","_ptr")).
			collect(Collectors.toSet())) {
		out.println("static void "+fun+"(void* ptr) {");
		out.println("  }");
		}


		for(SwijParser.Function fun:parser.functions) {
			GuileFun gf=new GuileFun(fun);
			gf.print(System.out);
		}
		return 0;
		}
	catch(Throwable err) {
		err.printStackTrace();
		return -1;
		}
	finally
		{
		
		}
	}	
	
public static void main(final String args[]) {
	new SwijToGuile().instanceMainWithExit(args);
	}

}
