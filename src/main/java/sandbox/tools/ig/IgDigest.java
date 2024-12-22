package sandbox.tools.ig;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.date.DateParser;
import sandbox.io.IOUtils;
import sandbox.jcommander.DurationConverter;
import sandbox.jcommander.NoSplitter;
import sandbox.lang.StringUtils;

public class IgDigest extends Launcher {
	private static final sandbox.Logger LOG = sandbox.Logger.builder(IgDigest.class).build();
	enum What {depicted,photo};
	
	@Parameter(names= {"-o","--out"},description=OUTPUT_OR_STANDOUT)
	private Path output = null;
	@Parameter(names={"-since","--since"},description="Since. "+DurationConverter.OPT_DESC,converter = DurationConverter.class,splitter = NoSplitter.class)
	private Duration since = Duration.ofDays(365);
	@Parameter(names={"-what","--what"},description="What to do")
	private What what = What.depicted;

	
	private final DateParser dateParser = new DateParser();
	
    private class Entry {
    	Date date;
    	String url;
    	String depicted;
    	String photo;
    	String img;
    	String ig;
    }
    
    private String cleanName(String s) {
    	if(s==null) return null;
    	while(s.startsWith("@")) s= s.substring(1);
    	return s;
    }
    
    private Entry mkEntry(final Map<String,String> record) {
    	if(record.getOrDefault("deleted","").equalsIgnoreCase("true")) return null;
    	
    	final Entry entry = new Entry();
    	
    	
    	if(record.containsKey("date") ) {
    		entry.date = dateParser.apply(record.get("date")).orElse(null);
    		if(entry.date==null) {
    			System.err.println("Cannot parse date: "+record.get("date"));
    			}
    		}
    	if(entry.date!=null && since!=null) {
    		final Date today = new Date();
	    		final long diff = today.getTime() - entry.date.getTime();
				if( diff > since.toMillis() ) {
					return null;
					}
				}
    	entry.url = record.get("url");
    	entry.depicted = cleanName(record.get("depicted"));
    	entry.photo = cleanName(record.get("photo"));
    	entry.img = record.get("img");
    	entry.ig = record.get("ig");
    	return entry;
    	}
    @Override
    public int doWork(List<String> args) {
    	try {
    		final List<Entry> entries = new ArrayList<>();
    		for(Path path : IOUtils.unrollPaths(args)) {
    			BufferedReader r = IOUtils.openBufferedReaderFromPath(path);
    			int nLine=0;
    			Map<String,String> record = new HashMap<>();
    			for(;;) {
    				String line = r.readLine();
    				if(line!=null)    nLine++;
    				if(StringUtils.isBlank(line)) {
    					if(!record.isEmpty()) {
    						final Entry entry = mkEntry(record);
    						if(entry!=null) {
    							entries.add(entry);
    							}
    						}
    					if(line==null) break;
    					record.clear();
    					continue;
    					}
    				int sep = line.indexOf(':');
    				if(sep==-1 || sep==0) {
    					System.err.println("Missing sepatator line "+nLine+" in "+path+" : "+line);
    					continue;
    					}
    				String key = line.substring(0,sep).trim().toLowerCase();
    				String value = line.substring(sep+1).trim();
    				if(StringUtils.isBlank(key)) {
    					System.err.println("Missing key line "+nLine+" in "+path+" : "+line);
    					continue;
    					}
    				if(StringUtils.isBlank(value)) continue;
    				if(record.containsKey(key)) {
    					System.err.println("duplicate key '"+key+"' line "+nLine+" in "+path+" : "+line);
    					continue;
    					}
    				record.put(key, value);
    				}
    			}
    		
    		if(what.equals(What.depicted) || what.equals(What.photo) ) {
	    		final Function<Entry, String> extractor;
    			if(what.equals(What.depicted)) {
	    			extractor = E->E.depicted;
	    			}
	    		else
	    			{
	    			extractor = E->E.photo;
	    			}
	    		final Map<String,Integer> count=new HashMap<>();
	    		entries.removeIf(P->StringUtils.isBlank(extractor.apply(P)) || StringUtils.isBlank(P.img));
	    		
	    		for(Entry e:entries) {
	    			final String k = extractor.apply(e);
	    			count.put(k, count.getOrDefault(k, 0)+1);
	    			}
	    		
	    		try(PrintWriter pw = IOUtils.openPathAsPrintWriter(output)) {
		    		final Set<String> seen = new HashSet<>();
	    			for(int i=0;i< entries.size();i++) {
		    			if(i>0) pw.println();
		    			final Entry entry = entries.get(i);
		    			final String k = extractor.apply(entry);
		    			pw.println("id: id"+(i+1));
		    			pw.println("score: "+count.get(k));
		    			pw.println("label: "+ k );
		    			pw.println("img: "+ entry.img);
		    			if(!seen.add(entry.img)) {
		    				System.err.println("Duplicate img: "+entry.img);
		    				}
		    			if(!StringUtils.isBlank(entry.ig)) pw.println("url: "+ entry.ig);
		    			}
		    		pw.flush();
		    		}
	    		}
    		
    		return 0;
    		}
    	catch(final Throwable err) {
    		LOG.error(err);
    		return -1;
    		}
    	}
    
public static void main(String[] args) {
	new IgDigest().instanceMainWithExit(args);
}
}
