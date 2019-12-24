package sandbox.swij;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import sandbox.Launcher;

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
		w.print("static SCM ");
		w.print(prefix);
		w.print(fun.name);
		w.print("(");
		for(int i=0;i< fun.parameters.size();i++) {
			if(i>0) w.print(", ");
			w.print("SCM ");
			w.print("s_"+fun.parameters.get(i).name);
		}
		w.println(") {");
		if(!fun.type.isVoid()) {
			w.println(fun.type.getCName()+" _ret = "+fun.type.getDefaultValue()+":");
			}
		for(int i=0;i< fun.parameters.size();i++) {
			final SwijParser.Param p = fun.parameters.get(i);
			w.println(p.type.getCName()+" "+p.name+"="+p.type.getDefaultValue()+";");
			}
		for(int i=0;i< fun.parameters.size();i++) {
			final SwijParser.Param p = fun.parameters.get(i);
			if(p.type.isInt()) {
				w.println("SCM_ASSERT (scm_is_integer(s_"+p.name+"),s_"+p.name+","+i+",\""+getGuileName()+"\");");
				}
			else if(p.type.isBoolean()) {
				w.println("SCM_ASSERT (scm_is_bool(s_"+p.name+"),s_"+p.name+","+i+",\""+getGuileName()+"\");");
				}
			else if(p.type.isString()) {
				w.println("SCM_ASSERT (scm_is_string(s_"+p.name+"),s_"+p.name+","+i+",\""+getGuileName()+"\");");
				}
			else
				{
				w.println("SCM_ASSERT (SCM_POINTER_P(s_"+p.name+"),s_"+p.name+","+i+",\""+getGuileName()+"\");");
				}
			}
		for(int i=0;i< fun.parameters.size();i++) {
			final SwijParser.Param p = fun.parameters.get(i);
			if(p.type.isInt()) {
				w.println(p.name+"= scm_to_int(s_"+p.name+");");
				}
			else if(p.type.isBoolean()) {
				w.println(p.name+"= scm_to_bool(s_"+p.name+");");
				}
			else if(p.type.isString()) {
				w.println(p.name+"= scm_to_locale_string(s_"+p.name+");");
				}
			else
				{
				w.println(p.name+"=("+p.type.getCName()+")scm_to_pointer(s_"+p.name+");");
				}
			}
		if(!fun.type.isVoid()) {
			w.print("_ret = ");
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
					w.println("free("+p.name+");");
					}
			}
		if(fun.type.isVoid()) {
			w.println("return SCM_UNDEFINED;");
			}
		else if(fun.type.isString()) {
			w.println("{");
			w.println("SCM _ret2= scm_from_locale_string(_ret);");
			w.println("free(_ret);//TODO");
			w.println("return _ret2;");
			w.println("}");
			}
		else if(fun.type.isBoolean()) {
			w.println("return scm_from_bool(_ret);");
			}
		else if(fun.type.isInt()) {
			w.println("return scm_from_int(_ret);");
			}
		else
			{
			w.println("if(_ret==NULL) return  SCM_UNDEFINED;");
			w.println("return scm_from_pointer(_ret,_dispose_"+fun.type.getCName().replace("*","_ptr")+");");
			}
		w.println("}");
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
		for(SwijParser.Function fun:parser.functions) {
			GuileFun gf=new GuileFun(fun);
			gf.print(System.out);
		}
		out.println("void init() {");
		for(SwijParser.Function fun:parser.functions) {
				GuileFun gf=new GuileFun(fun);
		  		out.println("scm_c_define_gsubr (\""+ gf.getGuileName() +"\", "+fun.parameters.size()+", 0, 0,"+ prefix+fun.name +");");
			 }
		out.println("}");
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
