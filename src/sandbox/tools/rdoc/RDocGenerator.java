package sandbox.tools.rdoc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.io.IOUtils;

public class RDocGenerator extends Launcher{

private static final Logger LOG = Logger.builder(RDocGenerator.class).build();

@Parameter(names= {"-o","--out"},description="The MAN directory output",required=true)
private Path manDir = null;
@Parameter(names= {"-f","--force"},description="Overwrite existing")
private boolean overwrite = false;
 

private String findOpcode(final String L) {
	if(L.isEmpty()) return null;
	final String tokens[] = L.split("[ \t]+");
	if(tokens.length==0) return null;
	if(tokens[0].startsWith("@")) return tokens[0].substring(1);
	return null;
	}
private boolean isOpcode(final String L) {
	return findOpcode(L)!=null;
}

private boolean isOpcode(final String L,final String key) {
	String s= findOpcode(L);
	if(s==null) return false;
	return s.equals(key);
}

@Override
public int doWork(final List<String> args) {
	try {
		IOUtils.assertDirectoryExist(this.manDir);
		
 			
		final List<String> buffer = new ArrayList<>();
		try(BufferedReader br = IOUtils.openBufferedReader(oneAndOnlyOneFile(args))) {
			for(;;) {
				String line= br.readLine();
				if(line==null) {
					break;
					}
				if(StringUtils.isBlank(line)) continue;
				if(!buffer.isEmpty() && !line.startsWith("#") && line.contains("function") && line.contains("<-")) {
					int i=0;
					final StringBuilder description = new StringBuilder();
					while(i<  buffer.size() ){
						final String s = buffer.get(i);
						if(isOpcode(s)) break;
						if(description.length()>0) description.append("\n");
						description.append(s);
						buffer.remove(i);
						}
					
					
					String returnStr = null;
					i=0;
					while(i<  buffer.size() ){
						final String s = buffer.get(i);
						if(isOpcode(s,"return")) {
							returnStr = s;
							buffer.remove(i);
							break;
							}
						else
							{
							i++;
							}
						}

					
					final StringBuilder example = new StringBuilder();
					
					boolean in_example = false;
					i=0;
					while(i<  buffer.size() ){
						final String s = buffer.get(i);
						if(!in_example) {
							if(isOpcode(s,"examples")) in_example=true;
							i++;
							}
						else
							{
							if(isOpcode(s)) {
								in_example=false;
								i++;
								}
							else
								{
								example.append(s).append("\n");
								buffer.remove(i);
								}
							}					
						}

					int arrow= line.indexOf("<-");
					final String funName = line.substring(0,arrow).trim();
					int openPar = line.indexOf("(");
					int endPar = line.indexOf(")",openPar);
					
					final Path rdFile = this.manDir.resolve(funName+".Rd");
					LOG.info("writing "+rdFile);
					if(Files.exists(rdFile) && !this.overwrite) {
						throw new IOException("file "+ rdFile+" already exists.");
						}
			
					PrintWriter out= IOUtils.openPathAsPrintWriter(rdFile);

					out.println("\\name{"+funName+"}");
					out.println("\\alias{"+funName+"}");
					out.println("\\title{"+funName+"}");
					out.println("\\usage{"+funName+line.substring(openPar,endPar+1)+"}");
					if(description.length()>0) {
						out.println("\\description{"+(description.toString().contains("\n")?description.toString():description.toString().trim())+"}");
						}
					else
						{
						out.println("\\description{No description available}");
						}
					if(returnStr!=null) {
						out.println("\\value{"+returnStr+"}");
						}
					out.println("\\arguments{");
					i=0;
					while(i<buffer.size()) {
						final String s = buffer.get(i);
						if(!isOpcode(s,"param")) {
							i++;
							continue;
							}
						else
							{
							buffer.remove(i);
							}
						final String tokens[] = s.split("[ \t]+",3);
						out.println("\t\\item{"+tokens[1]+"}{"+tokens[2]+"}");
						}
					out.println("}");
					if(example.length()>0) {
						out.println("\\examples{\n"+example+"\n}");
						}
					
					out.flush();
					out.close();
					buffer.clear();
					}
				else if(line.startsWith("#'"))
					{
					line = StringUtils.ltrim(line.substring(2));
					buffer.add(line);
					}
				}
			
			}
		return 0;
		}
	catch(Throwable err) {
		LOG.error(err);
		return -1;
		}
	}
public static void main(String[] args) {
	new RDocGenerator().instanceMain(args);
	}
}
