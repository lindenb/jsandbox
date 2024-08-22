package sandbox.tools.central;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import sandbox.tools.java2xml.JavaToXml;
import sandbox.tools.svgshow.SVGShow;
import sandbox.tools.swingfactory.SwingFactory;
import sandbox.tools.swingwatcher.SwingWatcher;
import sandbox.util.Pair;

public class SandboxCentral {
	private final List<Pair<Class<?>,ProgramDescriptor>> programs= new ArrayList<>();
	
	private void register(Class<?> clazz) {
		try {
			Method m=clazz.getMethod("getProgramDescriptor");
			if(!Modifier.isPublic(m.getModifiers())) throw new IllegalStateException("not public "+m);
			if(!Modifier.isStatic(m.getModifiers())) throw new IllegalStateException("not static "+m);
			if(!ProgramDescriptor.class.isAssignableFrom( m.getReturnType())) throw new IllegalStateException("not assignable from");
			ProgramDescriptor descriptor = (ProgramDescriptor)m.invoke(null);
			if(descriptor==null) throw new IllegalStateException("null for "+m);
			programs.add(Pair.of(clazz, descriptor));
		} catch(Throwable err) {
			throw new RuntimeException(err);
		}
	}
	
	private void usage(PrintStream out) {
		out.println("# JSANDBOX");
		out.println("## Program availables");
		for(Pair<Class<?>,ProgramDescriptor> p:programs) {
			if(p.getValue().isHidden()) continue;
			out.println(" * "+p.getValue().getName()+" : "+p.getValue().getDescription());
			}
		}
	
	private void instanceMain(List<String> args) {
		register(SwingFactory.class);
		register(SwingWatcher.class);
		register(JavaToXml.class);
		register(sandbox.tools.drawinggrid.DrawingGrid.class);
		register(SVGShow.class);
		Collections.sort(programs,(A,B)->A.getValue().getName().compareTo(B.getValue().getName()));
		if(args.isEmpty()) {
			usage(System.err);
			System.exit(-1);
			}
		if(args.size()==1 && (args.get(0).equals("--help") || args.get(0).equals("-h"))) {
			usage(System.out);
			System.exit(0);
			}
		final String prg = args.get(0);
		Class<?> clazz= programs.stream().filter(P->P.getValue().getName().equalsIgnoreCase(prg)).map(P->P.getKey()).findFirst().orElse(null);
		if(clazz==null) {
			System.err.println("Cannot find program "+prg+" available are "+ programs.stream().map(P->P.getValue().getName()).collect(Collectors.joining(",")));
			System.exit(-1);
			}
		try {
			final Method m= clazz.getMethod("main", String[].class);
			args=args.subList(1, args.size());
			m.invoke(null,(Object)args.toArray(new String[args.size()]));
			}
		catch(Throwable err) {
			err.printStackTrace();
			System.exit(-1);
			}
		}
	
	public static void main(String[] args) {
		new SandboxCentral().instanceMain(Arrays.asList(args));
	}

}
