package sandbox;

import java.awt.Insets;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.cli.CommandLine;

import com.sun.javafx.geom.Rectangle;

public class GitRepoHistory extends AbstractApplication {
	private File gitWorkDir=null;
	private List<GitFile> files= new ArrayList<>();
	private List<GitEvent> events = new ArrayList<>();
	private List<GitCommit> commits = new ArrayList<>();
	
	private class GitFile
		{
		private Optional<Long> myMaxSize=Optional.empty();
		final String path;
		final List<GitEvent> events = new ArrayList<>();
		private GitFile(final String path) {
			this.path = path;
		}
		@Override
		public int hashCode() {
			return path.hashCode();
			}
		
		@Override
		public boolean equals(final Object obj) {
			return obj==this || this.path.equals(GitFile.class.cast(obj).path);
			}
		@Override
		public String toString() {
			return path;
			}
		
		public boolean exists(final GitCommit commit) {
			boolean found=false;
			for(final GitCommit c :GitRepoHistory.this.commits) {
				if(c.index>commit.index) break;
				for(final GitEvent e: this.events) {
					if(!e.commit.equals(c)) continue;
					if(!e.file.equals(this))throw new IllegalStateException();
					found=true;
					if(e.is_delete_file) found=false;
				}
				
			}
			return found;
		}
		
		public long maxSize() {
			if(myMaxSize.isPresent()) return myMaxSize.get();
			long fileSize=0L;
			for(final GitCommit c :GitRepoHistory.this.commits) {
				Optional<Long> fs = this.size(c);
				if(!fs.isPresent()) continue;
				fileSize=Math.max(fileSize, fs.get());
			}
			myMaxSize = Optional.of(fileSize);
			return fileSize;
		}
		
		public Optional<Long> size(final GitCommit commit) {
			Long fileSize=null;
				for(final GitEvent e: this.events) {
					if(e.commit.index> commit.index) continue;//event are sorted on commit index
					if(!e.file.equals(this))throw new IllegalStateException();
					if(e.is_delete_file) {
						fileSize=null;
						continue;
					}
					if(fileSize==null) fileSize=0L;
					fileSize+= e.diffLines();
				
				
			}
			return fileSize==null?Optional.empty():Optional.of(fileSize);
		}
		
		
		}
	
	private static class Diff 
		{
		final int mul;
		final long start;
		final long count;
		Diff(final String s) {
			mul=(s.charAt(0)=='+'?1:-1);
			int comma=s.indexOf(',');
			if(comma==-1)
				{
				start=0;
				count=Long.parseLong(s.substring(1));
				}
			else
				{
				start=Long.parseLong(s.substring(1,comma));
				count=Long.parseLong(s.substring(comma+1));
				}
			}
		
		long diff() { return count*mul;}
		
		@Override
		public String toString() {
			return ""+(mul==1?"+":"-")+start+","+count;
			}
		}
	
	private class GitEvent
		{
		final GitFile file;
		final GitCommit commit;
		final Diff components[];
		boolean is_delete_file;
		GitEvent(final GitFile file,final GitCommit commit,final String diff,boolean is_delete_file) {
			this.file = file;
			this.commit = commit;
			String tokens[] =diff.split("[ ]");
			this.components=new Diff[tokens.length];
			for(int i=0;i< tokens.length;++i) {
				try {
					this.components[i] = new Diff(tokens[i]);
				} catch(IllegalArgumentException err) {
					throw new IllegalArgumentException(diff,err);
				}
			}
			this.is_delete_file=is_delete_file;
			}
		
		long diffLines() {
			long n=0;
			for(Diff c:this.components) n+=c.diff();
			return n;
		}
		
		
		@Override
		public String toString() {
			return "@@ "+Arrays.toString(this.components)+" @@ "+this.file+" "+this.commit+" "+is_delete_file;
			}
		}
	
	private class GitCommit 
		{
		int index=-1;
		private final GitCommit next;
		private GitCommit prev = null;
		private final String hash;
		private  String author;
		private  String date;
		
		
		GitCommit(final String hash,final GitCommit next) {
			this.hash=hash;
			this.next=next;
			if(next!=null) next.prev=this;
			}
		@Override
		public int hashCode() {
			return hash.hashCode();
			}
		
		@Override
		public boolean equals(Object obj) {
			return obj==this || this.hash.equals(GitCommit.class.cast(obj).hash);
			}
		@Override
		public String toString() {
			return hash;
			}
		
		
		}
	
	private GitFile getGitFileByName(final String s) {
		for(GitFile gf:this.files ){
			if(gf.path.equals(s)) return gf;
		}
		
		GitFile gf=new GitFile(s);
		this.files.add(gf);
		
		return gf;
	}
	
	@Override
	protected int execute(final CommandLine cmd) {
		final List<String> args = cmd.getArgList();
		
		if(args.size()!=1) {
			LOG.severe("Illegal number of args");
			return -1;
		}
		try
			{
			this.gitWorkDir = new File(args.get(0));
				
			 if(!gitWorkDir.isDirectory())
			 	{
				 LOG.severe("Not a directory "+gitWorkDir);
				return -1;
			 	}
			GitCommit next=null;
			Process proc = 
					new ProcessBuilder().
					directory(this.gitWorkDir).
					command("git","log","--pretty=%H").
					start();
			BufferedReader r=new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line;
			while((line=r.readLine())!=null) {
				final GitCommit commit = new GitCommit(line, next);
				this.commits.add(0, commit);
				next=commit;
			}
		
			for(int i=0;i< this.commits.size();++i) {
				this.commits.get(i).index=i;
			}
			
			
			r.close();
			if(proc.waitFor()!=0) {
				LOG.severe("git log failed");
				return -1;
			}
			
			for(GitCommit c:this.commits)
			{
			//	LOG.info("git log for "+c);
				proc =  new ProcessBuilder().
						directory(this.gitWorkDir).
						command("git","show",c.hash).
						start();
				r=new BufferedReader(new InputStreamReader(proc.getInputStream()));
				line=null;
				GitFile currFile =null;
				String prev=null;
				line=null;
				boolean is_delete_file = false;
				for(;;)
					{
					prev=line;
					line= r.readLine();
					if(line==null) break;
					//System.err.println(line);
					if(prev!=null &&   prev.startsWith("diff "))
						{
						is_delete_file=line.startsWith("delete file mode");
						}
					
					if(currFile==null && c.author==null && line.startsWith("Author: ")) {
						c.author = line.substring(8).trim();
						
						continue;
					}
					else if(currFile==null && c.date==null && line.startsWith("Date: ")) {
						c.date = line.substring(6).trim();
						continue;
					}
					else  if(line.startsWith("+++ ") && prev!=null && prev.startsWith("--- "))
						{
						currFile=null;
						if(prev.equals("--- /dev/null") && line.startsWith("+++ b/")) //new file
							{
							currFile = getGitFileByName(line.substring(line.indexOf('/')+1));
							}
						else if(prev.startsWith("--- a/") && line.equals("+++ /dev/null")) //new file
							{
							currFile = getGitFileByName(prev.substring(prev.indexOf('/')+1));
							}
						else if(line.startsWith("+++ b/") && prev.startsWith("--- a/")) //new file
							{
							final String s1=prev.substring(prev.indexOf('/')+1);
							final String s2=line.substring(line.indexOf('/')+1);
							if(!s1.equals(s2)) throw new IllegalStateException("boum");
							currFile = getGitFileByName(s1);
							}
						}
					else if( currFile!=null  && line.startsWith("@") && line.contains(" @")) {
						String arobase="";
						for(int i=0;i<line.length() && line.charAt(i)=='@';++i) {
							arobase+="@";
						}
						if(line.startsWith(arobase+" ") && line.contains(" "+arobase)) {
							String line0 = line.substring(arobase.length(),line.indexOf(" "+arobase)).trim();
							
							final GitEvent evt;
							try {
								evt = new GitEvent(currFile, c, line0,is_delete_file);
							} catch (IllegalArgumentException e) {
								throw new IllegalArgumentException(line+" "+c,e);
							}
							
							currFile.events.add(evt);
							this.events.add(evt);
							}
						}
					
				}
				r.close();
				if(proc.waitFor()!=0) {
					LOG.severe("git log failed");
					return -1;
				}
			}
			
			final Insets pageInsets = new Insets(500, 500, 100, 100);
			final int COMMIT_WIDTH=50;
			final int FILE_HEIGHT=80;
			XMLOutputFactory  xof = XMLOutputFactory.newFactory();
			XMLStreamWriter w= xof.createXMLStreamWriter(System.out);
			w.writeStartDocument("UTF-8", "1.0");
			w.setDefaultNamespace("http://www.w3.org/2000/svg");
			w.writeStartElement("svg");
			w.writeDefaultNamespace("http://www.w3.org/2000/svg");
			w.writeAttribute("width", String.valueOf(pageInsets.left + pageInsets.right + 1 +COMMIT_WIDTH*this.commits.size()));
			w.writeAttribute("height",String.valueOf(pageInsets.top + pageInsets.bottom +1+FILE_HEIGHT*this.files.size()));
			
			w.writeStartElement("style");
			w.writeCharacters(".evt {fill:green;stroke:white;}");
			w.writeEndElement();
			
			w.writeStartElement("g");
			w.writeAttribute("transform", "translate("+pageInsets.left+","+pageInsets.top+")");
			
			
			for(int x=0;x<this.commits.size();++x) {
				final GitCommit commit = this.commits.get(x);
				Rectangle commitRect=new Rectangle();
				commitRect.x =  COMMIT_WIDTH*x;
				commitRect.width =  COMMIT_WIDTH;
				commitRect.y = 0;
				commitRect.height = FILE_HEIGHT*this.files.size();
				
				w.writeStartElement("text");
				w.writeAttribute("x","0");
				w.writeAttribute("y","0");
				w.writeAttribute("title", commit.hash);
				w.writeAttribute("transform","translate("+String.valueOf(COMMIT_WIDTH/2.0+x*COMMIT_WIDTH)+",-12),rotate(-33)");
				w.writeCharacters(""+(commit.author==null?"":commit.author)+" "+(commit.date==null?"":commit.date));
				w.writeEndElement();
				
				w.writeEmptyElement("rect");
				w.writeAttribute("title", "commit "+commit.hash+" Author"+commit.author);
				w.writeAttribute("x",String.valueOf(commitRect.x));
				w.writeAttribute("y", "0");
				w.writeAttribute("width", String.valueOf(commitRect.width));
				w.writeAttribute("height", String.valueOf(commitRect.height));
				w.writeAttribute("style", "fill:gray;stroke:white;");
				w.writeCharacters("\n");
				}
			for(int y=0;y< this.files.size();++y) {
				final GitFile file =this.files.get(y);
				
				

				
				w.writeStartElement("g");
				w.writeAttribute("transform", "translate(0,"+(y*FILE_HEIGHT)+")");
				
				w.writeStartElement("text");
				w.writeAttribute("title", file.path);
				w.writeAttribute("x","-10");
				w.writeAttribute("y",String.valueOf(FILE_HEIGHT/2));
				w.writeAttribute("text-anchor","end");
				w.writeCharacters(file.path);
				w.writeEndElement();

				
				w.writeEmptyElement("rect");
				w.writeAttribute("x", "0");
				w.writeAttribute("y", "0");
				w.writeAttribute("width", String.valueOf(COMMIT_WIDTH*this.commits.size()));
				w.writeAttribute("height", String.valueOf(FILE_HEIGHT));
				w.writeAttribute("style", "fill:none;stroke:white;");
				w.writeEndElement();//g
				w.writeCharacters("\n");
				}
			
			for(int y=0;y< this.files.size();++y) {
				final GitFile file =this.files.get(y);
				int x=0;
				while(x<this.commits.size()) {
					
					final GitCommit commit0 = this.commits.get(x);
					Optional<Long> filesize0 = file.size(commit0);
					if(!filesize0.isPresent()) {x++; continue;}
					
					Rectangle rect=new Rectangle();
					double fs= filesize0.get()/(double)file.maxSize();
					rect.x =  COMMIT_WIDTH*x;
					rect.height = (int)((FILE_HEIGHT-1)*fs);
					rect.width =  COMMIT_WIDTH -2;
					rect.y = (int)(FILE_HEIGHT*y+FILE_HEIGHT-(FILE_HEIGHT-2)*fs);
					
					
					int next_x=x+1;
					while(next_x < this.commits.size())
						{
						final GitCommit commit1 = this.commits.get(next_x);
						Optional<Long> filesize1 = file.size(commit1);
						if(!filesize1.isPresent()) break;
						if(!filesize1.get().equals(filesize0.get())) break;
						rect.width = (COMMIT_WIDTH*(next_x-x));
						next_x++;
						}
					
					w.writeEmptyElement("rect");
					w.writeAttribute("x", String.valueOf(rect.x));
					w.writeAttribute("y", String.valueOf(rect.y));
					w.writeAttribute("width",String.valueOf(rect.width));
					w.writeAttribute("height",String.valueOf(rect.height));
					w.writeAttribute("class", "evt");
					
					/* if(x+1<this.commits.size()) {
						final GitCommit nextCommit = this.commits.get(x+1);
						for(GitEvent evt:file.events) {
							if(!evt.commit.equals(nextCommit)) continue;
							for(Diff diff:evt.components) {
							Rectangle rectEvt=new Rectangle();
							w.writeAttribute("x", String.valueOf(rectEvt.x));
							w.writeAttribute("y", String.valueOf(rectEvt.y));
							w.writeAttribute("width",String.valueOf(rectEvt.width));
							w.writeAttribute("height",String.valueOf(rectEvt.height));
							w.writeAttribute("class", "diff");
							}

						} 
					
					}*/
					w.writeCharacters("\n");
					x=next_x;
				}
				
				
				
			}
			
			w.writeEndElement();//g insets
			w.writeEndElement();
			w.writeEndDocument();
			w.flush();
			w.close();
			
			
			
			//LOG.info("DONE "+this.path2file.values()+" "+this.events);
			}
		catch(Throwable err) {
			err.printStackTrace();
			return -1;
		} finally
		{
			
		}
		return 0;
		}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
			new GitRepoHistory().instanceMainWithExit(args);
	}

}
