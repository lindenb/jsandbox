package sandbox.tools.lisp;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.lisp.LispEngine;
import sandbox.lisp.LispNode;
import sandbox.lisp.LispSymbol;
import sandbox.tools.central.ProgramDescriptor;

public class LispApp extends Launcher {
	
	@Parameter(names= { "--debug"},description="debug")
	private boolean trace_flag=false;
	@DynamicParameter(names= { "-D","--define"},description="key/value as -Dkey=value")
	private Map<String,String> dynaParams=new HashMap<>();
	
	private LispEngine createLispEngine() {
		final LispEngine engine = new LispEngine();
		for(String key: dynaParams.keySet()) {
			engine.put(LispSymbol.of(key), LispEngine.expressionOf(dynaParams.get(key)));
			}
		engine.setEnableTracing(trace_flag);
		return engine;
		}
	
@Override
public int doWork(List<String> args) {
	try {
		final LispEngine engine = createLispEngine();
		String input=oneFileOrNull(args);
		LispNode rez=null;
		try(Reader r=(input==null?new InputStreamReader(System.in):Files.newBufferedReader(Paths.get(input)))) {
				rez=engine.execute(r);
				}
		System.err.println(rez);
		return 0;
	} catch (Exception e) {
		getLogger().error(e);
		return -1;
		}
	}

public static ProgramDescriptor getProgramDescriptor() {
	return new ProgramDescriptor() {
		@Override
		public String getName() {
			return "lisp";
			}
		};
	}

public static void main(String[] args) {
	new LispApp().instanceMainWithExit(args);
}
}
