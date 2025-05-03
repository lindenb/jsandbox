package sandbox.nashorn;

import java.util.Optional;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class NashornUtils {
public static final String NASHORN="nashorn";
public static Optional<ScriptEngine> makeEngine() {
	final ScriptEngineManager mgr=new  ScriptEngineManager();
	final ScriptEngine scripEngine= mgr.getEngineByName(NASHORN);
	return Optional.ofNullable(scripEngine);
	}

public static ScriptEngine makeRequiredEngine() {
	return makeEngine().orElseThrow(()->new RuntimeException("Cannot get a javascript engine \""+NASHORN+"\""));
	}
}
