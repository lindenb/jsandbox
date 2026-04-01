package sandbox.nashorn;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import com.beust.jcommander.Parameter;

import sandbox.io.IOUtils;

public class NashornParameters {
 @Parameter(names={"-f","--script"},description="javascript file")
private Path scriptPath = null;
 @Parameter(names={"-e","--expression"},description="javascript expression")
private String scriptExpr = null;

public void check() {
	if(scriptPath==null && scriptExpr==null) {
		throw new IllegalArgumentException("both --script and --expression are undefined");
		}
	if(scriptPath!=null && scriptExpr!=null) {
		throw new IllegalArgumentException("both --script and --expression both defined");
		}
	}
public Reader getReader() throws IOException {
	check();
	if(scriptPath!=null) {
		return Files.newBufferedReader(this.scriptPath);
		}
	else
		{
		return new StringReader(this.scriptExpr);
		}
	}
public String slurpScript() throws IOException {
	try(Reader r=getReader()) {
		return IOUtils.slurp(r);
		}
	}


@Override
public String toString() {
	return "script-file:"+this.scriptExpr+" script-expr:"+this.scriptExpr;
	}
}
