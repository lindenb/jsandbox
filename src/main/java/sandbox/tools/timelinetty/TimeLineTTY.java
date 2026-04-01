package sandbox.tools.timelinetty;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.ansi.ANSI;
import sandbox.io.IOUtils;
import sandbox.lang.StringUtils;
import sandbox.tools.central.ProgramDescriptor;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.*;

import com.beust.jcommander.Parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TimeLineTTY extends Launcher {
	protected static final Logger LOG=Logger.builder(TimeLineTTY.class).build();
    @Parameter(names={"-o","--output"},description="output name")
    private Path out = null; 
    @Parameter(names={"-w","--width"},description="width")
    private int width=80;
    @Parameter(names={"--title"},description="main title")
    private String main_title="";

    
	private int[] dateToPixel(Date min,Date max,Date start,Date end) {
		long dDays = Math.max(1,max.toDays() - min.toDays());
		int x1 = (int)(width*((start.toDays()-min.toDays())/(double)dDays));
		return null;
		}
    
    private class MainTimeLine extends AbstractTimeLine {
    	public String getLabel() {
    		return TimeLineTTY.this.main_title;
    		}
    	public final int getX() {
    		return 0;
    		}
    	public final int getY() {
    		return 0;
    		}
    	@Override
    	public final  int getWidth() {
    		return Math.max(50, TimeLineTTY.this.width);
    		}
    	
    	void layout() {
    		List<Item> array = new ArrayList<>(this.items);
    		List<Item> done = new ArrayList<>(array.size());
    		Collections.sort(array);
    		while(!array.isEmpty()) {
    			Item first = array.remove(0);
    			int y=0;
    			for(;;) {
    				boolean f=false;
    				for(Item d:done) {
    					if(d.overlap(d.getX(),y,d.getWidth(),)) {
    						f=true;
    						break;
    						}
    					}
    				if(!f) break;
    				y++;
    				}
    			first.setY(y);
    			this.height = Math.max(this.height, first.getMaxY());
    			done.add(first);
	    		}
	    	}
    	
    	void print(PrintWriter pw) {
    		int y=0;
    		while(y< getHeight()) {
    			int x = 0;
    			while(x< getWidth()) {
    				Item item=null;
    				for(int i=0;i<this.items.size();++i) {
    					if(this.items.get(i).overlap(x,y)) {
    						item=this.items.get(i);
    						break;
    						}
    					}
    				if(item==null) {
    					++x;
    					}
    				else
    					{
    					x+= item.getWidth();
    					}
    				}
    			pw.println();
    			++y;
    			}
    		
    		}
    	}
    
    
	public static ProgramDescriptor getProgramDescriptor() {
    	return new ProgramDescriptor() {
    		@Override
    		public String getName() {
    			return "timelinetty";
    			}
    		};
    	}
	private class Renderer {
		int max_width=50;
		List<String> fold(String s) {
			if(s.length()<=max_width) return Arrays.asList(s);
			List<String> lines = new ArrayList<>();
			int i=0;
			while(i< s.length()) {
				int j= Math.min(i+max_width,s.length());
				lines.add(s.substring(i, j));
				i+=max_width;
				}
			return lines;
			}
		}
	
	private class TTYRenderer extends Renderer {
		void print(TimeLine tt) {
    		tt.layout(this);
    		
		}
	}
	
	private class Date implements Comparable<Date>{
		@Override
		public int compareTo(Date o) {
			return 0;
			}
		long toDays() {
			return 0L;
			}
		
		}

	private interface Item extends Comparable<Item> {
		public Date getStart();
		public Date getEnd();
		public default long getDurationInDays() {
			return 1L + getEnd().toDays() - getStart().toDays();
			}
		public String getLabel();
		public int getX();
		public int getY();
		public int getWidth();
		public int getHeight();
		public default int getMaxY() { return getY()+this.getHeight()-1;}
		public void layout(Renderer r);
		public TimeLine getParent();
		@Override
		default int compareTo(final Item o) {
			final int i=getStart().compareTo(o.getStart());
			if(i!=0) return 0;
			return getEnd().compareTo(o.getEnd());
			}
		}
	

	private static abstract class AbstractItem implements Item {
		String label="";
		int width=80;
		int height;
		int x=0;
		int y=0;
		@Override
		public String getLabel() {
			return label;
			}
		@Override
		public int getWidth() { return width; }
		@Override
		public int getHeight() { return height; }	
		@Override
		public int getX() { return x; }
		@Override
		public int getY() { return y; }
		}
	
	
	private static class DefaultEvent extends AbstractItem {
		
		Date startDate=null;
		Date endDate=null;
		@Override
		public void layout(Renderer r) {
			List<String> lines=r.fold(getLabel());
			height = lines.size();
			width  = lines.stream().mapToInt(L->L.length()).max().orElse(1);
			}
		
		public Date getStart()
			{
			return startDate;
			}
		public Date getEnd()
			{
			return endDate;
			}
		
		}
	
	private class AbstractTimeLine extends DefaultEvent {
	
		final List<Item> items= new ArrayList<>();
		public void layout(final Renderer r) {
			Collections.sort(this.items);
			for(Item i:this.items) {
				i.layout(r);
				}
			}
		@Override
		public int getWidth() { return width; }
		@Override
		public int getHeight() { return height; }	
		@Override
		public int getX( ) { return x; }
		@Override
		public int getY() { return y; }
		@Override
		public Date getStart()
			{
			return this.items.get(0).getStart();
			}
		@Override
		public Date getEnd()
			{
			return this.items.get(this.items.size()-1).getEnd();
			}
		}
	
	


    public int doWork(List<String> args) {
    	
    	
    	try {
    		final MainTimeLine timeline = new MainTimeLine();
	        final List<Path> inputs = IOUtils.unrollPaths(args);
	        XMLInputFactory xif= XMLInputFactory.newFactory();
	        if(inputs.isEmpty()) {
	        	XMLEventReader r= xif.createXMLEventReader(System.in);
	        	readEvents(timeline,r);
	        	r.close();
	        	}
	        else
	        	{
	        	for(Path p:inputs) {
	        		XMLEventReader r= xif.createXMLEventReader(new StreamSource(p.toFile()));
	        		readEvents(timeline,r);
	        		r.close();
	        		}
	        	}
	        if(timeline.items.isEmpty()) {
	        	LOG.error("No Event");
	        	return -1;
	        	}
	        timeline.layout();
    		try(PrintWriter pw=super.openPathAsPrintWriter(out)) {
    			timeline.print(pw);
    			}
	        
	        return 0;
    		}
    	catch(Throwable err) {
    		LOG.error(err);
    		return -1;
    		}
    	}

    
    private void readEvents(final MainTimeLine root,XMLEventReader r) throws Exception {
    	
    }

   private void renderTimeline(List<Event> events) {
        final int LINE_LENGTH = 80;
        final int EVENT_MARKER_WIDTH = 1;
        final int LABEL_PADDING = 2;
        final int BOX_HEIGHT = 4; // height for each event box
        events.sort(Comparator.comparing(e -> e.date));
        LocalDate minDate = events.get(0).date;
        LocalDate maxDate = events.get(events.size() - 1).date;
        long totalDays = minDate.until(maxDate).getDays();
        if (totalDays == 0) totalDays = 1; // Avoid division by zero

        // Calculate event positions (column index)
        int[] positions = new int[events.size()];
        Set<Integer> usedCols = new HashSet<>();
        for (int i = 0; i < events.size(); i++) {
            long daysFromStart = minDate.until(events.get(i).date).getDays();
            int col = (int)((double)daysFromStart / totalDays * (LINE_LENGTH - 1));
            // Avoid overlap: if already used, shift right
            while (usedCols.contains(col)) {
                col++;
                if (col >= LINE_LENGTH) col = LINE_LENGTH - 1; // Clamp
            }
            positions[i] = col;
            usedCols.add(col);
        }

        // Print timeline
        char[] timeline = new char[LINE_LENGTH];
        Arrays.fill(timeline, '-');
        for (int pos : positions) timeline[pos] = '|';
        System.out.println(new String(timeline));

        // Print event boxes below timeline
        String[] colorCodes = {ANSI.BLUE, ANSI.RED, ANSI.GREEN, ANSI.YELLOW};
        String[] emptyLines = new String[BOX_HEIGHT];
        Arrays.fill(emptyLines, "");

        // Each event box will be printed on its own line, horizontally, at correct position
        // Prepare lines for the boxes
        String[][] boxLines = new String[events.size()][BOX_HEIGHT];
        for (int i = 0; i < events.size(); i++) {
            Event ev = events.get(i);
            String label = colorCodes[i % colorCodes.length] + ev.label + ANSI.RESET;
            String dateStr = ev.date.toString();
            int boxWidth = Math.max(ev.label.length(), dateStr.length()) + 4; // 2 padding each side

            // Top border
            boxLines[i][0] = StringUtils.repeat(positions[i],' ') + "+" + StringUtils.repeat(boxWidth - 2,'-') + "+";
            // Label line
            int labelPadding = (boxWidth - 2 - ev.label.length()) / 2;
            boxLines[i][1] = repeat(" ", positions[i]) + "|" +
                    repeat(" ", labelPadding) + label + repeat(" ", boxWidth - 2 - labelPadding - ev.label.length()) + "|";
            // Date line
            int datePadding = (boxWidth - 2 - dateStr.length()) / 2;
            boxLines[i][2] = repeat(" ", positions[i]) + "|" +
                    repeat(" ", datePadding) + dateStr + repeat(" ", boxWidth - 2 - datePadding - dateStr.length()) + "|";
            // Bottom border
            boxLines[i][3] = repeat(" ", positions[i]) + "+" + repeat("-", boxWidth - 2) + "+";
        }

        // Now print each box on its own line, vertically spaced to avoid overlap
        for (int h = 0; h < BOX_HEIGHT; h++) {
            for (int i = 0; i < events.size(); i++) {
                System.out.print(boxLines[i][h]);
                System.out.print(" "); // space between boxes
            }
            System.out.println();
        }
    }

  
	
	public static void main(String[] args) {
		new TimeLineTTY().instanceMainWithExit(args);
	}
}
