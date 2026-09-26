package sandbox.tools.srtshifter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.lang.StringUtils;
import sandbox.tools.central.ProgramDescriptor;

public class SrtShifter extends Launcher {
	private static final Logger LOG = Logger.builder(SrtShifter.class).build();
    @Parameter(names= {"--start"},description="idx:seconds or idx:hour:minute:seconds where idx is ID in srt file, seconds is number of seconds in media",required = true)
    private String start_point;
    @Parameter(names= {"--end"},description="idx:seconds or idx:hour:minute:seconds where idx is ID in srt file, seconds is number of seconds in media",required = true)
    private String end_point;

	
    @Parameter(names= {"-o","--out"},description=OUTPUT_OR_STANDOUT)
    private Path outFile = null;

    private static class PointInTime {
		int id;
		long destination_millisec;
		}
    
	private static class Entry {
		int id;
		 long millisec_start;
		 long millisec_end;
		 List<String> text = new ArrayList<String>();
		 
		long duration() {
			return millisec_end-millisec_start;
		}
		 @Override
		public String toString() {
			return  id +" "+millisec_start+"-->"+millisec_end+" "+text;
		 	}
		}
    
	// eg. "00:02:15,304"
	private static long parseDuration(final String duration) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss,SSS");
        final LocalTime time = LocalTime.parse(duration, formatter);
        long milliseconds = Duration.ofNanos(time.toNanoOfDay()).toMillis();
		//System.err.println(duration+" = > "+milliseconds);

        return milliseconds;
        
        /*
        String tokens[]= s.split(":");
		if(tokens.length!=3) throw new IllegalArgumentException("expected 2 colons in "+s);
		long n= Integer.parseInt(tokens[0])*60*60*1000;//hours -> millisec
		n+= Integer.parseInt(tokens[1])*60*1000;//minutes -> millisec
		String[] comma = tokens[2].split("[,]");
		
		n+= Integer.parseInt(comma[0])*1000;//sec
		if(comma.length>=1) {
			n+=Integer.parseInt(comma[1]);
			}
		return n;*/
		}
	
	private static PointInTime parsePoint(String s) {
		String tokens[]= s.split(":");
		if(tokens.length==2) {
			final PointInTime entry = new PointInTime();
			entry.id= Integer.parseInt(tokens[0]);
			entry.destination_millisec = Integer.parseInt(tokens[1])*1000L;
			return entry;
			}
		else if(tokens.length==4) { //idx:hour:minute:seconds
			final PointInTime entry = new PointInTime();
			entry.id= Integer.parseInt(tokens[0]);
			int hours = Integer.parseInt(tokens[1]);
			int min = Integer.parseInt(tokens[2])+hours*60;
			int sec = Integer.parseInt(tokens[3])+min*60;
			entry.destination_millisec = sec*1000L;
			return entry;
			}
		throw new IllegalArgumentException("expected 1 or 4 colons in "+s);
		}
	
	private static String toDuration(long milliseconds) {
		Duration duration = Duration.ofMillis(milliseconds);

		long hours = duration.toHours();
		long minutes = duration.toMinutesPart();
		long seconds = duration.toSecondsPart();
		int millis = duration.toMillisPart();

		String result = String.format(
		    "%02d:%02d:%02d,%03d",
		    hours, minutes, seconds, millis
		);
		return result;
		}
	
    @Override
    public int doWork(final List<String> args) {
    	try {
    		final String input = oneFileOrNull(args);
    		final PointInTime my_start=parsePoint(this.start_point);
    		final PointInTime my_end=parsePoint(this.end_point);
    		try(BufferedReader br= super.openBufferedReader(input)) {
    			final List<Entry> entries= new ArrayList<>();
    			for(;;) {
    				String line= br.readLine();
    				if(line==null) break;
    				if(StringUtils.isBlank(line)) continue;
    				final Entry entry=new Entry();
    				entry.id=Integer.parseInt(line.trim());
    				line = br.readLine();
    				if(StringUtils.isBlank(line)) throw new IOException("duration missing");
    				int delim = line.indexOf("-->");
    				if(delim==-1) throw new IOException("delim --> missing in "+line);
    				entry.millisec_start = parseDuration(line.substring(0, delim).trim());
    				entry.millisec_end = parseDuration(line.substring(delim+3).trim());
    				if(entry.millisec_end< entry.millisec_start) throw new IllegalArgumentException(entry.toString());
    				for(;;) {
    					line= br.readLine();
        				if(StringUtils.isBlank(line)) break;
        				entry.text.add(line);
    					}
    				if(entry.text.isEmpty())  throw new IllegalArgumentException(entry.toString());
    				entries.add(entry);
    				}
    			
    			
    			final Entry evtBegin = entries.stream().filter(E->E.id==my_start.id).findFirst().orElseThrow();
    			final Entry evtEnd = entries.stream().filter(E->E.id==my_end.id).findFirst().orElseThrow();
    			// origina duraction between two point
    			final double original_duration = evtEnd.millisec_start - evtBegin.millisec_start;
    			final double dest_duration = my_end.destination_millisec - my_start.destination_millisec; 
    			try(PrintWriter pw=super.openPathAsPrintWriter(this.outFile)) {
    				final String CRLF="\r\n";
    				for(Entry entry: entries) {
    					pw.print(entry.id);
    					pw.print(CRLF);
    					double factor  = (entry.millisec_start - evtBegin.millisec_start)/original_duration;//YES start-start
    					long time = my_start.destination_millisec + (long)((dest_duration)*factor);
    								
    							// (long)(my_start.millisec+((entry.millisec_start - evtA.millisec_start)/(media_duration))*irl_duration);
    					
    					pw.print(toDuration(time));
    					pw.print(" --> ");
    					
    					time += entry.duration();
    					//time =  (long)(my_start.millisec+((entry.millisec_end - evtB.millisec_end)/(media_duration))*irl_duration);
    					pw.print(toDuration(time));
    					pw.print(CRLF);

    					
    					for(String s: entry.text) {
    						pw.print(s);
    						pw.print(CRLF);
    						}
    					pw.print(CRLF);
    					}
    				pw.flush();
    				}
    			}
    		return 0;
    		}
    	catch(Throwable err) {
    		LOG.error(err);
    		return -1;
    		}
    	}
    
	 public static ProgramDescriptor getProgramDescriptor() {
	    	return new ProgramDescriptor() {
	    		@Override
	    		public String getName() {
	    			return "srtshifter";
	    			}
	    	@Override
	    	public String getDescription() {
	    		return "shift / exand subtitle srt files";
	    	}
	    	};
	    }
	
public static void main(String[] args) {
	new SrtShifter().instanceMainWithExit(args);
	}

}
