package sandbox.tools.lisp;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import sandbox.Launcher;
import sandbox.lisp.LispEngine;
import sandbox.lisp.LispNode;
import sandbox.tools.central.ProgramDescriptor;

public class LispApp extends Launcher {
@Override
public int doWork(List<String> args) {
	try {
		final LispEngine engine = new LispEngine();
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
