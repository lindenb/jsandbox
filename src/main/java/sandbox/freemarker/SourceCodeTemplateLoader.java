package sandbox.freemarker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import freemarker.cache.TemplateLoader;

public class SourceCodeTemplateLoader implements TemplateLoader {
	private final Class<?> clazz;
	public SourceCodeTemplateLoader(Class<?> clazz) {
		this.clazz = clazz;
		}
	@Override
	public void closeTemplateSource(Object arg0) throws IOException {
		}

	@Override
	public Object findTemplateSource(String templateName) throws IOException {
		try {
			final String filename =this.clazz.getSimpleName()+".java";
			final InputStream in =this.clazz.getResourceAsStream(filename);
			if(in==null) throw new IllegalArgumentException("cannot find "+filename);
			try(BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
				String line;
				final String startTemplate = "BEGIN_TEMPLATE "+templateName;
				StringBuilder sb = null;
				while((line=br.readLine())!=null) {
					if(sb==null && line.trim().equalsIgnoreCase(startTemplate)) {
						sb= new StringBuilder();
						}
					else if(sb!=null && line.trim().equals("END_TEMPLATE")) {
						return sb.toString();
						}
					else if(sb!=null)
						{
						sb.append(line).append("\n");
						}
					}
				}
			throw new IllegalArgumentException("cannot find freemarker template '"+templateName+"' in "+filename+". did you set 'config.setLocalizedLookup(false)' ?");
			}
		catch(Throwable err) {
			throw new RuntimeException(err);
			}
		}

	
	@Override
	public long getLastModified(Object templateSource) {
		return -1L;
	}

	@Override
	public Reader getReader(Object templateSource, String encoding) throws IOException {
			return new StringReader((String)templateSource);
	}

}
