/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	Dec-2010
 * Contact:
 * 	plindenbaum@yahoo.fr
 * Reference:
 *   http://plindenbaum.blogspot.com/2011/01/my-tool-to-annotate-vcf-files.html
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Wiki
 *  https://github.com/lindenb/jsandbox/wiki/JSandbox-Wiki
 * Motivation:
 * 	Annotate a VCF file with the UCSC data. No SQL Driver required or a local database.
 * Compilation:
 *        cd jsandbox; ant vcfannotator
 */
package sandbox;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import sandbox.io.IOUtils;

class IOUtils
	{
	static private final int BUFSIZ=1000000;
	static private final Logger LOG=Logger.getLogger("vcf.annotator");
	static int TIMEOUT_SECONDS=60;
	static int TRY_CONNECT=10;
	private static InputStream _openStream(String uri)
		{
		try
			{
			InputStream in=null;
			if(	uri.startsWith("http://") || 
				uri.startsWith("https://") ||
				uri.startsWith("ftp://")
				)
				{
				for(int nTry=1; nTry<=TRY_CONNECT; ++nTry)
					{
					URL url=new URL(uri);
					try
						{
						URLConnection con=url.openConnection();
						con.setConnectTimeout(TIMEOUT_SECONDS*1000);
						in=con.getInputStream();
						}
					catch(Exception err)
						{
						LOG.severe(err.getMessage());
						in=null;
						}
					if(in!=null) break;
					System.err.println("Trying to connect... ("+(nTry+1)+"/"+TRY_CONNECT+") "+uri);
					}
				}
			else
				{
				in=new FileInputStream(uri);
				}
			if(in==null) return null;
			
			if(in!=null)
				{
				in=new BufferedInputStream(in,BUFSIZ);
				}
			
			if(uri.toLowerCase().endsWith(".gz"))
				{
				in=new GZIPInputStream(in);
				}
			
			return in;
			}
		catch (IOException e)
			{
			LOG.info("error "+e.getMessage());
			return null;
			}
		
		}
	
	private static BufferedReader _open(String uri)
		{
		InputStream in=_openStream(uri);
		if(in!=null)
			{
			return new BufferedReader(new InputStreamReader(in),BUFSIZ);
			}
		return null;
		}
	
	
	public static BufferedReader tryOpen(String uri)
		{
		LOG.info("try open "+uri);
		BufferedReader r=_open(uri);
		if(r==null)
			{
			System.err.println("Cannot open \""+uri+"\"");
			return null;
			}
		return r;
		}
	
	public static InputStream mustOpenStream(String url)
	throws IOException
		{
		LOG.info("must open stream "+url);
		InputStream r= _openStream(url);
		if(r==null) throw new IOException("Cannot open \""+url+"\"");
		return r;
		}
	
	public static BufferedReader mustOpen(String url)
		throws IOException
		{
		LOG.info("must open "+url);
		BufferedReader r= _open(url);
		if(r==null) throw new IOException("Cannot open \""+url+"\"");
		return r;
		}
	}

/**
 * 
 * Genetic Code
 *
 */
abstract class GeneticCode
	{
	/** the standard genetic code */
	private static final GeneticCode STANDARD=new GeneticCode()
		{
		@Override
		protected String getNCBITable()
			{
			return "FFLLSSSSYY**CC*WLLLLPPPPHHQQRRRRIIIMTTTTNNKKSSRRVVVVAAAADDEEGGGG";
			}
		};
	/** mitochondrial genetic code */
	private static final GeneticCode MITOCHONDRIAL=new GeneticCode()
		{
		@Override
		protected String getNCBITable()
			{
			return "FFLLSSSSYY**CCWWLLLLPPPPHHQQRRRRIIMMTTTTNNKKSS**VVVVAAAADDEEGGGG";
			}
		};
	/** get the genetic-code table (NCBI data) */ 
	protected abstract String getNCBITable();
	
	/** convert a base to index */
	private static int base2index(char c)
		{
		switch(c)
			{
			case 'T': return 0;
			case 'C': return 1;
			case 'A': return 2;
			case 'G': return 3;
			default: return -1;
			}
		}
	/** translate cDNA to aminoacid */
	public char translate(char b1,char b2,char b3)
		{
		int base1= base2index(b1);
		int base2= base2index(b2);
		int base3= base2index(b3);
		if(base1==-1 || base2==-1 || base3==-1)
			{
			return '?';
			}
		else
			{
			return getNCBITable().charAt(base1*16+base2*4+base3);
			}
		}
	
	/** get the standard genetic code */
	public static GeneticCode getStandard()
		{
		return STANDARD;
		}
	
	/** get the mitochondrial genetic code */
	public static GeneticCode getMitochondrial()
		{
		return MITOCHONDRIAL;
		}
	
	/** get a genetic code from a chromosome name (either std or mitochondrial */
	public static GeneticCode getByChromosome(String chr)
		{
		if(chr.equalsIgnoreCase("chrM")) return getMitochondrial();
		return getStandard();
		}
	}

/** a pair chromosome / position */
class ChromPosition
	implements Comparable<ChromPosition>
	{
	private String chromosome;
	private int position;
	public ChromPosition(String chromosome,int position)
		{
		this.chromosome=chromosome;
		this.position=position;
		}
	
	public String getChromosome()
		{
		return chromosome;
		}
	
	public int getPosition()
		{
		return position;
		}
	
	@Override
	public boolean equals(Object obj)
		{
		if (this == obj) { return true; }
		if (obj == null) { return false; }
		if (getClass() != obj.getClass()) { return false; }
		ChromPosition other = (ChromPosition) obj;
		if (position != other.position) { return false; }
		if (!chromosome.equalsIgnoreCase(other.chromosome)) { return false; }
		return true;
		}
	
	@Override
	public int hashCode()
		{
		final int prime = 31;
		int result = 1;
		result = prime * result + chromosome.hashCode();
		result = prime * result + position;
		return result;
		}
	
	@Override
	public int compareTo(ChromPosition o)
		{
		int i= getChromosome().compareToIgnoreCase(o.getChromosome());
		if(i!=0) return i;
		return getPosition()-o.getPosition();
		}
	@Override
	public String toString()
		{
		return getChromosome()+":"+getPosition();
		}
	}

/**
 * A record in a VCF file
 */
class VCFCall
	implements Comparable<VCFCall> //order by chromPosition
	{
	@SuppressWarnings("unused")
	private static Logger LOG=Logger.getLogger("vcf.annotator");
	/** the position */
	private ChromPosition chromPosition;
	/** columns in the VCF */
	private String columns[];
	
	/** cstor */
	VCFCall(String columns[])
		{
		this.columns=columns;
		if(!columns[0].toLowerCase().startsWith("chr"))
			{
			columns[0]="chr"+columns[0];
			}
		this.chromPosition=new ChromPosition(columns[0], Integer.parseInt(columns[1]));
		}
	
	/** get the columns from the VCF line */
	public String[] getColumns()
		{
		return columns;
		}
	
	/** get the position */
	public ChromPosition getChromPosition()
		{
		return this.chromPosition;
		}
	/** compare by position */
	@Override
	public int compareTo(VCFCall o)
		{
		return getChromPosition().compareTo(o.getChromPosition());
		}
	/** returns the VCF line */
	public String getLine()
		{
		StringBuilder line=new StringBuilder(100);
		for(int i=0;i< columns.length;++i)
			{
			if(i!=0) line.append("\t");
			line.append(columns[i]);
			}
		return line.toString();
		}
	
	
	@Override
	public String toString()
		{
		return getLine();
		}
	
	public void addProperties(String opcode,Map<String, String> map)
		{
		StringBuilder b=new StringBuilder();
		boolean first=true;
		for(String key:map.keySet())
			{
			if(!first) b.append("|");
			first=false;
			b.append(key);
			b.append(":");
			b.append(map.get(key));
			}
		addProperty(opcode, b.toString());
		}
	
	public void addProperty(String key,String value)
		{
		if(columns[7].equals(".")) columns[7]="";	
		if(!columns[7].isEmpty()) this.columns[7]+=";";
		columns[7]+=(key+"="+value);
		}
	
	public void addId(String newId)
		{
		String rsId= this.getColumns()[2];
		if(rsId.equals(".")) rsId="";
		Set<String> set=new HashSet<String>(Arrays.asList(rsId.split("[;]")));
		
		set.remove("");
		set.add(newId);
		rsId="";
		for(String s:set)
			{
			if(!rsId.isEmpty()) rsId+=";";
			rsId+=s;
			}
		this.getColumns()[2]=rsId;
		}
	}

/**
 * A VCF file
 * @author pierre
 *
 */
class VCFFile
	{
	private static Logger LOG=Logger.getLogger("vcf.annotator");
	//private static final String DEFAULT_HEADER="#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tSample";
	private List<String> headers=new ArrayList<String>();
	private List<VCFCall> calls=new ArrayList<VCFCall>(10000);
	
	public VCFFile()
		{
		
		}
	
	public List<String> getHeaders()
		{
		return headers;
		}
	
	public List<VCFCall> getCalls()
		{
		return calls;
		}
	
	public  int lowerBound( ChromPosition position)
		{
		return lowerBound(0, getCalls().size(), position);
		}
    /** C+ lower_bound */
    public  int lowerBound(
                int first, int last,
                ChromPosition position
                )
        {
        int len = last - first;
        while (len > 0)
                {
                int half = len / 2;
                int middle = first + half;
                VCFCall call= getCalls().get(middle);
                if ( call.getChromPosition().compareTo(position) < 0  )
                        {
                        first = middle + 1;
                        len -= half + 1;
                        }
                else
                        {
                        len = half;
                        }
                }
   
        return first;
        }
    /** get the calls at given position */
	public List<VCFCall> get(ChromPosition pos)
    	{
    	int i=lowerBound(0,getCalls().size(),pos);
    	List<VCFCall> array=new ArrayList<VCFCall>(5);
    	while(i< getCalls().size())
    		{
    		VCFCall call=getCalls().get(i);
    		if(!call.getChromPosition().equals(pos)) break;
    		array.add(call);
    		++i;
    		}
    	return array;
    	}
	
	/** prints the VCF line */
	public void print(PrintWriter out)
		{
		for(String header:getHeaders())
			{
			out.println(header);
			}
		for(VCFCall c:getCalls())
			{
			out.println(c.getLine());
			}
		out.flush();
		}
	
	/** read VCF file */
	private void read(BufferedReader in)
	throws IOException
		{
		Pattern tab=Pattern.compile("[\t]");
		String line;
		while((line=in.readLine())!=null)
			{
			LOG.info(line);
			if(!line.startsWith("#")) break;
			this.headers.add(line);
			}
		
		if(!headers.isEmpty())
			{
			final String fmt="##fileformat=VCFv";
			String first= headers.get(0);
			
			if(first.startsWith("##format"))
				{
				first="##file"+first.substring(2);
				}
			
			if(!(first.startsWith(fmt)))
				{
				throw new IOException("firt line should starts with "+fmt);
				}
			String last=headers.get(headers.size()-1);
			if(!last.startsWith("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO"))
				{
				throw new IOException("Error in header got "+line+" but expected "+
						"#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO");
				}
			}
		else
			{
			this.headers.add("##fileformat=VCFv4.0");
			this.headers.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tSample");
			}
		
		while(line!=null)
			{
			//LOG.info(line);
			if(line.startsWith("#")) throw new IOException("line starting with # after header!"+line);
			String tokens[]=tab.split(line);
			if(tokens.length<8) throw new IOException("illegal number of columns in "+line);
			getCalls().add(new VCFCall(tokens));
			line=in.readLine();
			}
		
		Collections.sort(getCalls());
		LOG.info("vcf:"+getCalls().size()+" calls");
		}
	
	public Set<String> getChromosomes()
		{
		Set<String> set=new HashSet<String>();
		for(VCFCall c: getCalls())
			{
			set.add(c.getChromPosition().getChromosome());
			}
		LOG.info(set.toString());
		return set;
		}
	
	public void addHeader(String key,String value)
		{
		while(!key.startsWith("##")) key="#"+key;
		String line= key+"="+value;
		if(this.headers.contains(line)) return;
		this.headers.add(this.headers.size()-1,line);
		}
	
	public void addInfo(
		String id,Integer number,String type,String desc)
		{
		String line;
		if(this.headers.get(0).startsWith("##fileformat=VCFv4"))
			{
			line="<ID="+id+",Number="+(number==null?".":number.toString())+
				",Type="+type+",Description=\""+desc+"\">";
			}
		else if(this.headers.get(0).startsWith("##fileformat=VCFv3"))
			{
			line=id+","+(number==null?".":number.toString())+
			","+type+",\""+desc+"\"";
			}
		else
			{
			throw new IllegalArgumentException("VCF format not handled. "+this.headers.get(0));
			}
		addHeader("##INFO",line);
		}
	
	
	
	public static VCFFile parse(BufferedReader in)
	throws IOException
		{
		VCFFile vcf=new VCFFile();
		vcf.read(in);
		return vcf;
		}
	}


/** CharSeq a simple string impl */
interface CharSeq
	{
	public int length();
	public char charAt(int i);
	}

/**
 * Abstract implementation of CharSeq
 */
abstract class AbstractCharSeq implements CharSeq
	{
	AbstractCharSeq()
		{
		}
	
	@Override
	public int hashCode()
		{
		return getString().hashCode();
		}
	
	public String getString()
		{
		StringBuilder b=new StringBuilder(length());
		for(int i=0;i< length();++i) b.append(charAt(i));
		return b.toString();
		}
	
	@Override
	public String toString()
		{
		return getString();
		}
	}




/**
 * 
 * A GenomicSequence
 *
 */
class GenomicSequence
	extends AbstractCharSeq
	{
	private String chrom;
	private byte array[];
	private int chromStart0;
	
	public GenomicSequence(byte array[],String chrom,int chromStart0)
		{	
		this.chrom=chrom;
		this.array=array;
		this.chromStart0=chromStart0;
		}
	
	public String getChrom()
		{
		return chrom;
		}
	public int getChromStart()
		{
		return chromStart0;
		}
	public int getChromEnd()
		{
		return getChromStart()+array.length;
		}
	
	@Override
	public int length()
		{
		return getChromEnd();
		}
	
	@Override
	public char charAt(int index0)
		{
		if(index0 < getChromStart() || index0 >=getChromEnd())
			{
			throw new IndexOutOfBoundsException("index:"+index0);
			}
		return (char)(array[index0-chromStart0]);
		}
	}


class DefaultCharSeq extends AbstractCharSeq
	{
	private CharSequence seq;
	DefaultCharSeq(CharSequence seq)
		{
		this.seq=seq;
		}
	@Override
	public char charAt(int i)
		{
		return seq.charAt(i);
		}
	@Override
	public int length() {
		return seq.length();
		}
	}

class MutedSequence extends AbstractCharSeq
	{
	private CharSequence wild;
	private Map<Integer, Character> pos2char=new TreeMap<Integer, Character>();
	MutedSequence(CharSequence wild)
		{
		this.wild=wild;
		}
	
	void put(int pos,char c)
		{
		this.pos2char.put(pos, c);
		}
	
	@Override
	public char charAt(int i)
		{
		Character c= pos2char.get(i);
		return c==null?wild.charAt(i):c;
		}
	
	@Override
	public int length()
		{
		return this.wild.length();
		}
	}


class ProteinCharSequence extends AbstractCharSeq
	{
	private CharSeq cDNA;
	private GeneticCode geneticCode;
	ProteinCharSequence(GeneticCode geneticCode,CharSeq cDNA)
		{
		this.geneticCode=geneticCode;
		this.cDNA=cDNA;
		}
	
	@Override
	public char charAt(int i)
		{
		return geneticCode.translate(
			cDNA.charAt(i*3+0),
			cDNA.charAt(i*3+1),
			cDNA.charAt(i*3+2));
		}	
	
	@Override
	public int length()
		{
		return this.cDNA.length()/3;
		}
}


/**
 * Calls Ucsc DAS to fetch a DNA sequence using a SAX parser
 */
class DasSequenceProvider
	extends DefaultHandler
	{
	private static Logger LOG=Logger.getLogger("vcf.annotator");
	private String ucscBuild;
	private ByteArrayOutputStream baos=null;
	int reserve=100000;
	private SAXParser parser;
	public DasSequenceProvider(String ucscBuild)
		{
		this.ucscBuild=ucscBuild;
		SAXParserFactory f=SAXParserFactory.newInstance();
		f.setSchema(null);
		f.setNamespaceAware(false);
		f.setValidating(false);
		try
			{
			this.parser=f.newSAXParser();
			}
		catch(Exception err)
			{
			throw new RuntimeException(err);
			}
		}
	
	 @Override
	public void startDocument() throws SAXException
		{
		baos=null;
		}
	
	 public InputSource resolveEntity (String publicId, String systemId)
         {
         return new InputSource(new StringReader(""));
         }
	
	@Override
    public void startElement(String uri, String localName, String name,
            Attributes attributes) throws SAXException
        {
        if(name.equals("DNA"))
            {
            this.baos=new ByteArrayOutputStream(this.reserve);
            }
        }
	
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException
        {
        if(this.baos==null) return;
        for(int i=0;i< length;++i)
            {
            char c= Character.toUpperCase(ch[start+i]);
            if(Character.isWhitespace(c)) continue;
            this.baos.write((byte)c);
            }
        }


	public GenomicSequence getSequence(String chrom, int chromStart0, int chromEnd0)
			throws IOException
		{
		if(chromStart0 <0 || chromStart0 >=chromEnd0)
			{
			throw new IllegalArgumentException("Error in start/end");
			}
		this.reserve=(1+(chromEnd0-chromStart0));
		this.baos=null;
		try
			{
			String uri="http://genome.ucsc.edu/cgi-bin/das/"+
					this.ucscBuild+
					"/dna?segment="+
					URLEncoder.encode(chrom+":"+(chromStart0+1)+","+(chromEnd0+2), "UTF-8")
					;
			LOG.info(uri);
			InputStream in=IOUtils.mustOpenStream(uri);
			this.parser.parse(in, this);
			in.close();
			GenomicSequence g= new GenomicSequence(
				this.baos.toByteArray(),
				chrom,
				chromStart0
				);
			this.baos=null;
			return g;
			}
		catch (SAXException err)
			{
			throw new IOException(err);
			}
		
		}
	
	public GenomicSequence getSequence(String chrom)
	throws IOException
		{
		this.baos=null;
		this.reserve=200000000;
		try
			{
			String uri="http://genome.ucsc.edu/cgi-bin/das/"+
					this.ucscBuild+
					"/dna?segment="+
					URLEncoder.encode(chrom, "UTF-8")
					;
			LOG.info(uri);
			InputStream in=IOUtils.mustOpenStream(uri);
			this.parser.parse(in, this);
			in.close();
			GenomicSequence g= new GenomicSequence(this.baos.toByteArray(),chrom,0);
			this.baos=null;
			return g;
			}
		catch (SAXException err)
			{
			throw new IOException(err);
			}
		
		}
	
	}

/**
 * 
 * KnownGene
 *
 */
class KnownGene
	{
	private static final long serialVersionUID = 1L;
	private String name;
	private String chrom;
	private char strand;
	private int txStart;
	private int txEnd;
	private int cdsStart;
	private int cdsEnd;
	private int exonStarts[];
	private int exonEnds[];
	private String geneSymbol;
	
	abstract class Segment
		{
		private int index;
		protected Segment(int index)
			{
			this.index=index;
			}
		
		public int getIndex()
			{
			return index;
			}
		
		public KnownGene getGene()
			{
			return KnownGene.this;
			}
		
		public boolean contains(int position)
			{
			return getStart()<=position && position< getEnd();
			}
		public abstract boolean isSplicingAcceptor(int position);
		public abstract boolean isSplicingDonor(int position);
		public boolean isSplicing(int position)
			{
			return isSplicingAcceptor(position) || isSplicingDonor(position);
			}
		
		public abstract String getName();
		public abstract int getStart();
		public abstract int getEnd();
		}
	
	class Exon extends Segment
		{
		private Exon(int index)
			{
			super(index);
			}
		
		@Override
		public String getName()
			{
			if(getGene().getStrand()=='+')
				{
				return "Exon "+(getIndex()+1);
				}
			else
				{
				return "Exon "+(getGene().getExonCount()-getIndex());
				}
			}
		
		@Override
		public int getStart()
			{
			return getGene().getExonStart(getIndex());
			}
		
		@Override
		public int getEnd()
			{
			return getGene().getExonEnd(getIndex());
			}
		
		@Override
		public String toString()
			{
			return getName();
			}
		
		
		public Intron getNextIntron()
			{
			if(getIndex()+1>=getGene().getExonCount()) return null;
			return getGene().getIntron(getIndex());
			}
		public Intron getPrevIntron()
			{
			if(getIndex()<=0) return null;
			return getGene().getIntron(getIndex()-1);
			}
		
		@Override
		public boolean isSplicingAcceptor(int position)
			{
			if(!contains(position)) return false;
			if(isForward())
				{
				if(getIndex()== 0) return false;
				return position==getStart();
				}
			else
				{
				if(getIndex()+1== getGene().getExonCount()) return false;
				return position==getEnd()-1;
				}
			}
		
		@Override
		public boolean isSplicingDonor(int position)
			{
			if(!contains(position)) return false;
			if(isForward())
				{
				if(getIndex()+1== getGene().getExonCount()) return false;
				return  (position==getEnd()-1) ||
						(position==getEnd()-2) ||
						(position==getEnd()-3) ;
				}
			else
				{
				if(getIndex()== 0) return false;
				return  (position==getStart()+0) ||
						(position==getStart()+1) ||
						(position==getStart()+2) ;
				}
			}
		
		}
		
	class Intron extends Segment
			{
			Intron(int index)
				{
				super(index);
				}
			
			@Override
			public int getStart()
				{
				return getGene().getExonEnd(getIndex());
				}
			
			@Override
			public int getEnd()
				{
				return getGene().getExonStart(getIndex()+1);
				}
			
			@Override
			public String getName() {
				if(getGene().isForward())
					{
					return "Intron "+(getIndex()+1);
					}
				else
					{
					return "Intron "+(getGene().getExonCount()-getIndex());
					}
				}

			public boolean isSplicingAcceptor(int position)
				{
				if(!contains(position)) return false;
				if(isForward())
					{
					return  (position==getEnd()-1) ||
							(position==getEnd()-2);
					}
				else
					{
					return	position==getStart() ||
							position==getStart()+1;
					}
				}
			

			public boolean isSplicingDonor(int position)
				{
				if(!contains(position)) return false;
				if(isForward())
					{
					return	position==getStart() ||
							position==getStart()+1;
							
					}
				else
					{
					return  (position==getEnd()-1) ||
							(position==getEnd()-2);
					}
				}
			
			}
	
		/**
		 * 
		 * KnownGene 
		 * 
		 */
		public KnownGene(String tokens[])
			throws IOException
			{
			this.name = tokens[0];
			this.geneSymbol=tokens[0];
			this.chrom= tokens[1];
	        this.strand = tokens[2].charAt(0);
	        this.txStart = Integer.parseInt(tokens[3]);
	        this.txEnd = Integer.parseInt(tokens[4]);
	        this.cdsStart= Integer.parseInt(tokens[5]);
	        this.cdsEnd= Integer.parseInt(tokens[6]);
	        int exonCount=Integer.parseInt(tokens[7]);
	        this.exonStarts = new int[exonCount];
	        this.exonEnds = new int[exonCount];
	            
            
            int index=0;
            for(String s: tokens[8].split("[,]"))
            	{
            	this.exonStarts[index++]=Integer.parseInt(s);
            	}
            index=0;
            for(String s: tokens[9].split("[,]"))
            	{
            	this.exonEnds[index++]=Integer.parseInt(s);
            	}
			}
		
		/** returns knownGene ID */
		public String getName()
			{
			return this.name;
			}
		
		/** returns chromosome name */
		public String getChromosome()
			{
			return this.chrom;
			}
		
		/** returns the strand */
		public char getStrand()
			{
			return strand;
			}
		boolean isForward()
        	{
        	return getStrand()=='+';
        	}

		public int getTxStart()
			{
			return this.txStart;
			}

		public int getTxEnd()
			{
			return this.txEnd;
			}
		

		public int getCdsStart()
			{
			return this.cdsStart;
			}
		

		public int getCdsEnd()
			{
			return this.cdsEnd;
			}
		

		public int getExonStart(int index)
			{
			return this.exonStarts[index];
			}
		

		public int getExonEnd(int index)
			{
			return this.exonEnds[index];
			}
		

		public Exon getExon(int index)
			{
			return new Exon(index);
			}
		public Intron getIntron(int i)
			{
			return new Intron(i);
			}
		public int getExonCount()
			{
			return this.exonStarts.length;
			}
		public String getGeneSymbol()
			{
			return geneSymbol;
			}
		
		public void setGeneSymbol(String geneSymbol)
			{
			this.geneSymbol = geneSymbol;
			}
		
		}


abstract class AbstractWigAnalysis
	{
	static Logger LOG=Logger.getLogger("vcf.annotator");
	protected String currChrom=null;
	protected int currPosition=0;
	protected int currStep=1;
	protected int currSpan=1;
	protected int variation_index=-1;
	private VCFFile vcfFile=null;
	
	protected AbstractWigAnalysis()
		{
		
		}
	
	public void setVcfFile(VCFFile vcfFile)
		{
		this.vcfFile = vcfFile;
		}
	public VCFFile getVcfFile()
		{
		return vcfFile;
		}
	
	protected void fixedStep(String line)
		{
		LOG.info(line);
		currChrom=null;
		currPosition=0;
		currStep=1;
		currSpan=1;
		for(String s:line.split("[ ]+"))
			{
			if(s.startsWith("chrom="))
				{
				int i=s.indexOf('=');
				currChrom=s.substring(i+1);
				}
			else if(s.startsWith("start="))
				{
				int i=s.indexOf('=');
				currPosition=Integer.parseInt(s.substring(i+1));
				currPosition--;//suspect that WIG files starts with 1 not 0
				}
			else if(s.startsWith("step="))
				{
				int i=s.indexOf('=');
				currStep=Integer.parseInt(s.substring(i+1));
				}
			else if(s.startsWith("span="))
				{
				int i=s.indexOf('=');
				currSpan=Integer.parseInt(s.substring(i+1));
				}
			}
		
		variation_index= vcfFile.lowerBound(
			new ChromPosition(currChrom, 0)
			);
		if(variation_index < vcfFile.getCalls().size())
			{
			ChromPosition pos = vcfFile.getCalls().get(variation_index).getChromPosition();
			if(!pos.getChromosome().equalsIgnoreCase(currChrom))
				{
				variation_index=  vcfFile.getCalls().size();
				}
			}
		}
	
	protected void scanWig(BufferedReader r)
		throws IOException
		{
		String line;
		
		while((line=r.readLine())!=null)
			{
			if(line.startsWith("fixedStep"))
				{
				fixedStep(line);
				continue;
				}
			
			
			//advance variation_index
			while(variation_index<  vcfFile.getCalls().size())
				{
				ChromPosition pos=vcfFile.getCalls().get(variation_index).getChromPosition();
				
				if(!( (pos.getPosition() -1 ) < currPosition &&
					  (pos.getChromosome().equalsIgnoreCase(currChrom))
					))
					{
					break;
					}
				
				variation_index++;
				}
			
			int n2=variation_index;
			while(n2 < vcfFile.getCalls().size())
					{
					ChromPosition pos=vcfFile.getCalls().get(n2).getChromPosition();
					if(!( pos.getPosition() -1 >=currPosition &&
						  pos.getPosition() -1 <(currPosition+currStep) &&
					      pos.getChromosome().equalsIgnoreCase(currChrom)
					     ))
						{
						break;
						}

					found(vcfFile.getCalls().get(n2),line);
					++n2;
					}

			currPosition+=currStep;
			}
		r.close();
		}
	
	protected abstract void found(VCFCall call,String line);
	
	}

/**
 * 
 * Annotation for Mapability
 * 
 */
class MapabilityAnnotator
	extends AbstractWigAnalysis
	{
	private String table;
	private String genomeVersion;
	
	public MapabilityAnnotator(String genomeVersion)
		{
		this.genomeVersion=genomeVersion;
		}
	/**
	 * The Broad alignability track displays whether a region is made up of mostly unique or mostly non-unique sequence. 
	 * @throws Exception
	 */
	public void run() throws Exception
		{
		scanWig("wgEncodeBroadMapabilityAlign36mer.wig.gz",
				"wgEncodeBroadMapabilityAlign36mer");
		for(int i: new int[]{20,24,35})
			{
			scanWig("wgEncodeDukeUniqueness"+i+"bp.wig.gz",
					"wgEncodeDukeUniqueness"+i);
			}
		}
	
	@Override
	protected void found(VCFCall call,String line)
		{
		call.addProperty("MAPABILITY_"+table.toUpperCase(), line);
		}
	
	private void scanWig(String path,String table) throws Exception
		{
		this.table=table;
		
		BufferedReader r=IOUtils.tryOpen(
			"http://hgdownload.cse.ucsc.edu/goldenPath/"+ this.genomeVersion+
			"/encodeDCC/wgEncodeMapability/"+path);
		if(r==null) return;
		getVcfFile().addInfo(
				"MAPABILITY_"+table.toUpperCase(),1, "String",
				"level of sequence uniqueness for "+genomeVersion+". See ftp://encodeftp.cse.ucsc.edu/pipeline/"+genomeVersion+"/wgEncodeMapability/index.html");
		scanWig(r);
		r.close();
		}
		
	}

/**
 * 
 * Annotation for PhastCons
 * 
 */
class PhastConsAnnotator
	extends AbstractWigAnalysis
	{
	private String genomeVersion;
	public PhastConsAnnotator(String genomeVersion)
		{
		this.genomeVersion=genomeVersion;
		}
	
	public void run() throws Exception
		{
        for(String c:getVcfFile().getChromosomes())
                {        	
        		BufferedReader r=IOUtils.tryOpen("http://hgdownload.cse.ucsc.edu/goldenPath/"+genomeVersion+"/phastCons44way/vertebrate/"+c+".phastCons44way.wigFix.gz");
        		if(r==null) return ;
        		getVcfFile().addInfo("phastCons44way", 1, "Float", "phastCons scores for multiple alignments of 44 vertebrate genomes to the human genome");
                scanWig(r);
                r.close();
                }
		}
	
	@Override
	protected void found(VCFCall call,String line)
		{
		call.addProperty("phastCons44way", line);
		//LOG.info(table+" "+call+" "+line+" "+this.currChrom+" "+this.currPosition);
		}	
		
	}

/**
 * http://genome.ucsc.edu/cgi-bin/hgTrackUi?db=hg18&g=pgSnp&hgt_tSearch=Search
 * @author pierre
 *
 */
class PersonalGenomeAnnotator
	extends AbstractRangeAnnotator
	{
	private String name;
	private String genomeVersion;
	PersonalGenomeAnnotator()
		{
		}
	
	PersonalGenomeAnnotator(String name)
		{
		this.name=name;
		}
	
	public void setName(String name)
		{
		this.name = name;
		}
	
	public String getName()
		{
		return name;
		}
	public String getTable()
		{
		return "pg"+getName();
		}
	
	public void setGenomeVersion(String genomeVersion)
		{
		this.genomeVersion = genomeVersion;
		}
	
	public String getGenomeVersion()
		{
		return genomeVersion;
		}
	
	public void run()
		throws IOException
		{
		BufferedReader in=IOUtils.tryOpen(
				"http://hgdownload.cse.ucsc.edu/goldenPath/"+
				getGenomeVersion()+"/database/"+getTable()+".txt.gz");
		if(in==null) return ;
		getVcfFile().addInfo(getName().toUpperCase(), 1, "String", getName()+"'s Personal genome");
		run(in);
		in.close();
		}
	
	@Override
	protected int getSplitMax()
		{
		return 6;
		}
	
	@Override
	protected int getChromosomeColumn()
		{
		return 1;
		}
	
	@Override
	protected int getChromStartColumn()
		{
		return 2;
		}
	
	@Override
	protected int getChromEndColumn()
		{
		return 3;
		}
	
	@Override
	public void annotate(VCFCall call, String[] tokens)
		{
		call.addProperty(getName().toUpperCase(), tokens[4]);
		call.addId(getName().toUpperCase()+":"+tokens[1]+":"+(1+Integer.parseInt(tokens[2]))+":"+tokens[4]);
		}
	}



/**
 * GenomicSuperDupAnnotator
 */
class GenomicSuperDupAnnotator
	extends AbstractRangeAnnotator
	{
	private String genomeVersion;
	
	GenomicSuperDupAnnotator(String genomeVersion)
		{
		this.genomeVersion=genomeVersion;
		}
	
	
	
	public void run()
		throws IOException
		{
		BufferedReader in=IOUtils.tryOpen("http://hgdownload.cse.ucsc.edu/goldenPath/"+genomeVersion+"/database/genomicSuperDups.txt.gz");
		if(in==null) return ;
		getVcfFile().addInfo("SEGDUP", null,"String","large genomic duplications");
		run(in);
		in.close();
		}
	
	@Override
	protected int getSplitMax()
		{
		return 6;
		}
	
	@Override
	protected int getChromosomeColumn()
		{
		return 1;
		}
	
	@Override
	protected int getChromStartColumn()
		{
		return 2;
		}
	
	@Override
	protected int getChromEndColumn()
		{
		return 3;
		}
	
	@Override
	public void annotate(VCFCall call, String[] tokens)
		{
		call.addProperty("SEGDUP",tokens[4]);
		}
	}



/**
 * AbstractRangeAnnotator
 */
abstract class AbstractRangeAnnotator
	{
	static protected Logger LOG=Logger.getLogger("vcf.annotator");
	private VCFFile vcfFile;
	
	
	protected AbstractRangeAnnotator()
		{
		}
	
	public void setVcfFile(VCFFile vcfFile)
		{
		this.vcfFile = vcfFile;
		}
	
	public VCFFile getVcfFile()
		{
		return vcfFile;
		}
	
	abstract protected int getSplitMax();
	abstract protected int getChromStartColumn();
	abstract protected int getChromEndColumn();
	abstract protected int getChromosomeColumn();
	
	public void run(BufferedReader in)
		throws IOException
		{
		if(in==null) return;
		final int chrom_col_index= getChromosomeColumn();
		final int chromstart_col_index=getChromStartColumn();
		final int chromend_col_index=getChromEndColumn();
		int nLines=0;
		String currentChromosome=null;
		int currentIndex=0;
		int prev_chromStart=0;
		Pattern tab=Pattern.compile("\t");
		String line;
		while((line=in.readLine())!=null)
			{
			++nLines;
			if(nLines%150000==0)
				{
				LOG.info(line);
				}
			String tokens[]=tab.split(line,getSplitMax());
			String chrom=tokens[chrom_col_index];
			if( currentChromosome==null ||
			   !currentChromosome.equalsIgnoreCase(chrom))
				{
				currentChromosome = chrom;
				prev_chromStart=-1;
				currentIndex = getVcfFile().lowerBound(new ChromPosition(chrom, 0));
				if(currentIndex< vcfFile.getCalls().size())
					{
					ChromPosition pos = vcfFile.getCalls().get(currentIndex).getChromPosition();
					if(!pos.getChromosome().equalsIgnoreCase(currentChromosome))
						{
						currentIndex=  vcfFile.getCalls().size();
						}
					}
				}
			
			int chromStart=Integer.parseInt(tokens[chromstart_col_index]);
			int chromEnd=Integer.parseInt(tokens[chromend_col_index]);
			
			if(chromStart< prev_chromStart)
				{
				throw new IOException("exected sorted data chrom/chromStart");
				}
			prev_chromStart=chromStart;
			
			//advance variation_index
			while(currentIndex<  vcfFile.getCalls().size())
				{
				ChromPosition pos = vcfFile.getCalls().get(currentIndex).getChromPosition();
				if(!(pos.getPosition()-1 < chromStart &&
					 pos.getChromosome().equalsIgnoreCase(currentChromosome))
					 )
					{
					break;
					}
				currentIndex++;	
				}
			int n2=currentIndex;
			while(n2 < vcfFile.getCalls().size())
				{
				ChromPosition pos = vcfFile.getCalls().get(n2).getChromPosition();
				if(!(pos.getPosition() -1 >=chromStart &&
					((chromStart== chromEnd && pos.getPosition() -1 == chromStart) || (chromStart< chromEnd && pos.getPosition() -1 < chromEnd)) &&
					pos.getChromosome().equalsIgnoreCase(currentChromosome))
					)
					{
					break;
					}
				VCFCall call=vcfFile.getCalls().get(n2);
				LOG.info(call.getLine());
				LOG.info(line);
				annotate(call,tokens);
				++n2;
				}
			}
		}
	
	public abstract void annotate(VCFCall c,final String tokens[]);
	}


/**
 * Transcription Binding Factors
 */
class TranscriptionBindingSitesAnnotator
extends AbstractRangeAnnotator
	{
	private String genomeVersion;
	TranscriptionBindingSitesAnnotator(String genomeVersion)
		{
		this.genomeVersion=genomeVersion;
		}
	
	public void run()
	throws IOException
		{
		BufferedReader in=IOUtils.tryOpen(
			"http://hgdownload.cse.ucsc.edu/goldenPath/"+genomeVersion+"/database/tfbsConsSites.txt.gz");
		if(in==null) return;
		getVcfFile().addInfo("TFBS", null, "String","tfbsConsSites. Transcription Factor Binding sites conserved in the human/mouse/rat alignment ");
		run(in);
		in.close();
		}
	
	@Override
	protected int getSplitMax()
		{
		return 6;
		}
	@Override
	protected int getChromosomeColumn()
		{
		return 1;
		}
	@Override
	protected int getChromStartColumn()
		{
		return 2;
		}
	@Override
	protected int getChromEndColumn()
		{
		return 3;
		}
	@Override
	public void annotate(VCFCall c, String[] tokens)
		{
		c.addProperty("TFBS", tokens[4]);
		}
	}


/**
 * RepeatMaskerAnnotator
 */
class RepeatMaskerAnnotator
 extends AbstractRangeAnnotator
	{
	private String genomeVersion;
	RepeatMaskerAnnotator(String genomeVersion)
		{
		this.genomeVersion=genomeVersion;
		}
	
	public void run()
	throws IOException
		{
		for(String chr:getVcfFile().getChromosomes())
			{
			BufferedReader in=IOUtils.tryOpen(
				"http://hgdownload.cse.ucsc.edu/goldenPath/"+
				this.genomeVersion+"/database/"+
				chr+
				"_rmskRM327.txt.gz");
			if(in==null) continue;
			run(in);
			in.close();
			}
		getVcfFile().addInfo("RMSK", null, "String","Repeating Elements by RepeatMasker version 3.2.7");
		}
	
	@Override
	protected int getSplitMax()
		{
		return 12;
		}
	@Override
	protected int getChromosomeColumn()
		{
		return 5;
		}
	@Override
	protected int getChromStartColumn()
		{
		return 6;
		}
	@Override
	protected int getChromEndColumn()
		{
		return 7;
		}
	@Override
	public void annotate(VCFCall c, String[] tokens)
		{
		c.addProperty("RMSK", tokens[10]);
		}
	}

/**
 * SNP annotator
 * @author pierre
 */
class SnpAnnotator extends AbstractRangeAnnotator
	{
	private String genomeVersion;
	private String table;
	public SnpAnnotator(String genomeVersion,String table)
		{
		this.genomeVersion=genomeVersion;
		this.table=table;
		}
	
	public void run()
		throws IOException
		{
		BufferedReader in=IOUtils.tryOpen(
			"http://hgdownload.cse.ucsc.edu/goldenPath/"+genomeVersion+"/database/"+table+".txt.gz");
		if(in==null) return;
		getVcfFile().addHeader("##"+table.toUpperCase(),"table "+table+" from UCSC");
		
		run(in);
		in.close();
		}
	
	@Override
	protected int getSplitMax()
		{
		return 6;
		}
	
	@Override
	protected int getChromosomeColumn()
		{
		return 1;
		}

	@Override
	protected int getChromStartColumn()
		{
		return 2;
		}
	
	@Override
	protected int getChromEndColumn()
		{
		return 3;
		}
	
	
	@Override
	public void annotate(VCFCall call, String[] tokens)
		{
		call.addId(tokens[4]);
		}	
	}

/*************************************************************************************/
class PolyXAnnotator
	{
	static final int EXTEND=100;
	static final int EXTRA=EXTEND+5;
	static Logger LOG=Logger.getLogger("vcf.annotator");
	private VCFFile vcfFile;
	boolean loadWholeSegment=false;
	
	//private String genomeVersion;
	private DasSequenceProvider dasServer;
	PolyXAnnotator(String genomeVersion) throws Exception
		{
		//this.genomeVersion=genomeVersion;
		this.dasServer=new DasSequenceProvider(genomeVersion);
		}
	
	
	public void setVcfFile(VCFFile vcfFile)
		{
		this.vcfFile = vcfFile;
		}
	
	public void run() throws IOException
		{
		GenomicSequence genomicSeq=null;
	
		this.vcfFile.addInfo("POLYX",
				null,
				"Integer",
				"Number of repeated bases"
				);
		
		
	    for(VCFCall call:this.vcfFile.getCalls())
		        {
		        LOG.info(call.toString());
		        String ref=call.getColumns()[3].toUpperCase();
		    	if(ref.length()!=1) continue;
		    	
		    	
		    	ChromPosition pos=call.getChromPosition();
		    	final int position0=pos.getPosition()-1;
		    	
        		if(genomicSeq==null ||
        	               !pos.getChromosome().equals(genomicSeq.getChrom()) ||
        	               !(genomicSeq.getChromStart()<=(position0-EXTEND) && (position0+EXTEND) <= genomicSeq.getChromEnd())
        	               )
	            	{
	            	final int maxTry=20;
	            	for(int tryCount=1;tryCount<=maxTry;++tryCount)
	            		{
	            		genomicSeq=null;
	            		try
	            			{
	            			if(this.loadWholeSegment)
	            				{
	            				genomicSeq=this.dasServer.getSequence(
			    	            		pos.getChromosome()
			    	            		);
	            				}
	            			else
		            			{
		    	            	genomicSeq=this.dasServer.getSequence(
		    	            		pos.getChromosome(),
		    	            		Math.max(position0-EXTRA,0),
		    	            		position0+EXTRA
		    	            		);
		            			}
	            			}
	            		catch (Exception e)
	            			{
							LOG.info("Cannot get DAS-DNA sequence "+e.getMessage());
							genomicSeq=null;
							}
	            		if(genomicSeq!=null)
	            			{
	            			break;
	            			}
	            		LOG.info("try to get DAS-DNA "+(tryCount)+"/"+maxTry);
	            		}
	            	if(genomicSeq==null)
	            		{
	            		throw new IOException("Cannot get DAS-DNA");
	            		}
	            	}
        		
        		char theBase=genomicSeq.charAt(position0);
        		if(!String.valueOf(theBase).equalsIgnoreCase(ref))
        			{
        			System.err.println("Warning REF!=GENOMIC SEQ!!! at "+theBase+"/"+ref+" pos0="+position0);
        			return;
        			}
        		int count=1;
        		
        		int index= position0 -1;
        		while(index> position0-EXTEND && index>=0)
        			{
        			if(Character.toUpperCase(genomicSeq.charAt(index))!=Character.toUpperCase(theBase))
        				{
        				break;
        				}
        			++count;
        			--index;
        			}
        		
        		index= position0 +1;
        		while(index< position0+EXTEND && index < genomicSeq.length())
        			{
        			if(Character.toUpperCase(genomicSeq.charAt(index))!=Character.toUpperCase(theBase))
        				{
        				break;
        				}
        			++count;
        			++index;
        			}
		        call.addProperty("POLYX", String.valueOf(count));
		      	}
			}	
	
	}

/*************************************************************************************/

/**
 * PredictionAnnotator
 *
 */
class PredictionAnnotator
	{
	static final String KEY_TYPE="type";
	static final String KEY_SPLICING="splicing";
	static Logger LOG=Logger.getLogger("vcf.annotator");
	private Map<String, List<KnownGene>> chrom2genes=new HashMap<String, List<KnownGene>>();
	private VCFFile vcfFile;
	private DasSequenceProvider dasServer;
	private String genomeVersion;
	boolean loadWholeSegment=false;
	
	PredictionAnnotator(String genomeVersion) throws Exception
		{
		this.genomeVersion=genomeVersion;
		this.dasServer=new DasSequenceProvider(genomeVersion);
		}
	
	
	public void setVcfFile(VCFFile vcfFile)
		{
		this.vcfFile = vcfFile;
		}
	
	private static char complement(char c)
		{
		switch(c)
			{
			case 'A': return 'T';
			case 'T': return 'A';
			case 'G': return 'C';
			case 'C': return 'G';
			default:throw new IllegalArgumentException(""+c);
			}
		}
	
	public void preLoadUcsc() throws IOException
		{
		Map<String, KnownGene> kgId2gene=new HashMap<String, KnownGene>();
		Pattern tab=Pattern.compile("\t");
		Set<String> chromosomes= vcfFile.getChromosomes();
		BufferedReader in=IOUtils.mustOpen("http://hgdownload.cse.ucsc.edu/goldenPath/"+genomeVersion+"/database/knownGene.txt.gz");
		String line;
		while((line=in.readLine())!=null)
			{
			String tokens[]=tab.split(line);
			if(!chromosomes.contains(tokens[1])) continue;
			KnownGene g=new KnownGene(tokens);
			List<KnownGene> L=this.chrom2genes.get(g.getChromosome());
			if(L==null)
				{
				L=new ArrayList<KnownGene>();
				this.chrom2genes.put(g.getChromosome(),L);
				}
			L.add(g);
			kgId2gene.put(g.getName(),g);
			}
		in.close();
		
		for(String chr:this.chrom2genes.keySet())
			{
			List<KnownGene> L=this.chrom2genes.get(chr);
			LOG.info(chr+":"+L.size());
			Collections.sort(L,
				new Comparator<KnownGene>()
					{
					@Override
					public int compare(KnownGene o1, KnownGene o2)
						{
						return o1.getTxStart()-o2.getTxStart();
						}
					});
			}
		

		in=IOUtils.mustOpen("http://hgdownload.cse.ucsc.edu/goldenPath/"+genomeVersion+"/database/kgXref.txt.gz");
		while((line=in.readLine())!=null)
			{
			String tokens[]=tab.split(line);
			KnownGene g=kgId2gene.get(tokens[0]);
			if(g==null) continue;
			g.setGeneSymbol(tokens[4]);
			}
		in.close();
		}
	
	
	private List<KnownGene> getGenes(ChromPosition pos)
		{
		List<KnownGene> array = new ArrayList<KnownGene>();
		List<KnownGene> genes = this.chrom2genes.get(pos.getChromosome());
		if(genes==null)
			{
			return array;
			}
		
		int first=0;
	   
	    while(first< genes.size())
	    	{
	    	KnownGene gene= genes.get(first);
	    	if( gene.getTxStart()<= (pos.getPosition()-1 ) &&
	    		(pos.getPosition()-1)< gene.getTxEnd())
	    		{
	    		array.add(gene);
	    		}
	    	++first;
	    	}
		return array;
		}
	
	public void run() throws IOException
		{
		final int extra=10000;
		GenomicSequence genomicSeq=null;

		this.vcfFile.addInfo("PREDICTION",
				null,
				"String",
				"Basic predictions from UCSC knownGenes"
				);
		
		
	    for(VCFCall call:this.vcfFile.getCalls())
            {
            LOG.info(call.toString());
            int position= call.getChromPosition().getPosition()-1;
            String ref=call.getColumns()[3].toUpperCase();
        	String alt=call.getColumns()[4].toUpperCase();
        	
            if(ref.equals("A"))
    			{
    				 if(alt.equals("W")) { alt="T"; }
    			else if(alt.equals("M")) { alt="C"; }
    			else if(alt.equals("R")) { alt="G"; }
    			}
    		else if(ref.equals("C"))
    			{
    				 if(alt.equals("S")) { alt="G"; }
    			else if(alt.equals("M")) { alt="A"; }
    			else if(alt.equals("Y")) { alt="T"; }
    			}
    		else if(ref.equals("G"))
    			{
    				 if(alt.equals("S")) { alt="C"; }
    			else if(alt.equals("K")) { alt="T"; }
    			else if(alt.equals("R")) { alt="A"; }
    			}
    		else if(ref.equals("T"))
    			{
    				 if(alt.equals("W")) { alt="A"; }
    			else if(alt.equals("K")) { alt="G"; }
    			else if(alt.equals("Y")) { alt="C"; }
    			}
    		
    		LOG.info(ref+" "+alt);
            List<KnownGene> genes= getGenes(call.getChromPosition());
            
            if(genes.isEmpty())
            	{
            	LOG.info("GENOMIC");
            	continue;
            	}
            
            for(KnownGene gene:genes)
            	{
            	LOG.info(gene.getName());
            	
            	//switch to 0 based coordinate
        		
            	Map<String, String> annotations=new HashMap<String, String>();
        		
        		
            	if( (ref.equals("A") || ref.equals("T") || ref.equals("G") || ref.equals("C")) &&
            		(alt.equals("A") || alt.equals("T") || alt.equals("G") || alt.equals("C"))
            		)
	        		{
	        		LOG.info("fetch genome");
            		GeneticCode geneticCode=GeneticCode.getByChromosome(gene.getChromosome());
	        		StringBuilder wildRNA=null;
	        		ProteinCharSequence wildProt=null;
	        		ProteinCharSequence mutProt=null;
	        		MutedSequence mutRNA=null;
	        		int position_in_cdna=-1;
	        		
	        		
	        		if(genomicSeq==null ||
	        	               !gene.getChromosome().equals(genomicSeq.getChrom()) ||
	        	               !(genomicSeq.getChromStart()<=gene.getTxStart() && gene.getTxEnd()<= genomicSeq.getChromEnd())
	        	               )
    	            	{
    	            	final int maxTry=20;
    	            	for(int tryCount=1;tryCount<=maxTry;++tryCount)
    	            		{
    	            		genomicSeq=null;
    	            		try
    	            			{
    	            			if(loadWholeSegment)
    	            				{
    	            				genomicSeq=this.dasServer.getSequence(gene.getChromosome());
    	            				}
    	            			else
	    	            			{
			    	            	genomicSeq=this.dasServer.getSequence(
			    	            		gene.getChromosome(),
			    	            		Math.max(gene.getTxStart()-extra,0),
			    	            		gene.getTxEnd()+extra
			    	            		);
	    	            			}
    	            			}
    	            		catch (Exception e)
    	            			{
								LOG.info("Cannot get DAS-DNA sequence "+e.getMessage());
								genomicSeq=null;
								}
    	            		if(genomicSeq!=null)
    	            			{
    	            			break;
    	            			}
    	            		LOG.info("try to get DAS-DNA "+(tryCount)+"/"+maxTry);
    	            		}
    	            	if(genomicSeq==null)
    	            		{
    	            		throw new IOException("Cannot get DAS-DNA");
    	            		}
    	            	}
	        		
	        		if(!String.valueOf(genomicSeq.charAt(position)).equalsIgnoreCase(ref))
	        			{
	        			System.err.println("Warning REF!=GENOMIC SEQ!!! at "+genomicSeq.charAt(position)+"/"+ref);
	        			return;
	        			}
	        		
	        		if(gene.isForward())
	            		{
	            		if(position < gene.getCdsStart())
	            			{
	            			annotations.put("type", "UTR5");
	            			}
	            		else if( gene.getCdsEnd()<= position )
	            			{
	            			annotations.put("type", "UTR3");
	            			}
	            		
	            		int exon_index=0;
	            		while(exon_index< gene.getExonCount())
	            			{
	            			KnownGene.Exon exon= gene.getExon(exon_index);
	            			for(int i= exon.getStart();
	            					i< exon.getEnd();
	            					++i)
	            				{
	            				if(i==position)
	        						{
	        						annotations.put("exon.name", exon.getName());
	        						}
	            				if(i< gene.getCdsStart()) continue;
	            				if(i>=gene.getCdsEnd()) break;
	        					
	        					if(wildRNA==null)
	        						{
	        						wildRNA=new StringBuilder();
	        						mutRNA=new MutedSequence(wildRNA);
	        						}
	        					
	        					if(i==position)
	        						{
	        						annotations.put("type", "EXON");
	        						annotations.put("exon.name",exon.getName());
	        						position_in_cdna=wildRNA.length();
	        						annotations.put("pos.cdna", String.valueOf(position_in_cdna));
	        						//in splicing ?
	        						if(exon.isSplicing(position))
	        							{
	        							annotations.put(KEY_SPLICING, "SPLICING");
	        							
	        							if(exon.isSplicingAcceptor(position))
	        								{
	        								annotations.put(KEY_SPLICING, "SPLICING_ACCEPTOR");
	        								}
	        							else  if(exon.isSplicingDonor(position))
	        								{
	        								annotations.put(KEY_SPLICING, "SPLICING_DONOR");
	        								}
	        							}
	        						}
	        					
	            				wildRNA.append(genomicSeq.charAt(i));
	            				
	            				if(i==position)
	            					{
	            					mutRNA.put(
	            							position_in_cdna,
	            							alt.charAt(0)
	            							);
	            					}
	            				
	            				if(wildRNA.length()%3==0 && wildRNA.length()>0 && wildProt==null)
		            				{
		            				wildProt=new ProteinCharSequence(geneticCode,new DefaultCharSeq(wildRNA));
		            				mutProt=new ProteinCharSequence(geneticCode,mutRNA);
		            				}
	            				}
	            			KnownGene.Intron intron= exon.getNextIntron();
	            			if(intron!=null && intron.contains(position))
	            				{
	            				annotations.put("intron.name",intron.getName());
	            				annotations.put("type", "INTRON");
	            				
	            				if(intron.isSplicing(position))
	        						{
	            					annotations.put(KEY_SPLICING, "INTRON_SPLICING");
	        						if(intron.isSplicingAcceptor(position))
	        							{
	        							annotations.put(KEY_SPLICING, "INTRON_SPLICING_ACCEPTOR");
	        							}
	        						else if(intron.isSplicingDonor(position))
	        							{
	        							annotations.put(KEY_SPLICING, "INTRON_SPLICING_DONOR");
	        							}
	        						}
	            				}
	            			++exon_index;
	            			}
	            		
	            		
	            		
	            		}
	            	else // reverse orientation
	            		{
	            	
	            		if(position < gene.getCdsStart())
	            			{
	            			annotations.put(KEY_TYPE, "UTR3");
	            			}
	            		else if( gene.getCdsEnd()<=position )
	            			{
	            			annotations.put(KEY_TYPE, "UTR5");
	            			}
	            	
	            		
	            		int exon_index = gene.getExonCount()-1;
	            		while(exon_index >=0)
	            			{
	            			KnownGene.Exon exon= gene.getExon(exon_index);
	            			for(int i= exon.getEnd()-1;
	            				    i>= exon.getStart();
	            				--i)
	            				{
	            				if(i==position)
	        						{
	            					annotations.put("exon.name", exon.getName());
	        						}
	            				if(i>= gene.getCdsEnd()) continue;
	            				if(i<  gene.getCdsStart()) break;
	            				
	            				if(wildRNA==null)
	        						{
	        						wildRNA=new StringBuilder();
	        						mutRNA=new MutedSequence(wildRNA);
	        						}
	            				
	            				if(i==position)
	        						{
	            					annotations.put(KEY_TYPE, "EXON");
	            					position_in_cdna=wildRNA.length();
	        						annotations.put("pos.cdna",String.valueOf(position_in_cdna));
	        						//in splicing ?
	        						if(exon.isSplicing(position))
	        							{
	        							annotations.put(KEY_SPLICING, "INTRON_SPLICING");
	        							
	        							if(exon.isSplicingAcceptor(position))
	        								{
	        								annotations.put(KEY_SPLICING, "INTRON_SPLICING_ACCEPTOR");
	        								}
	        							else  if(exon.isSplicingDonor(position))
	        								{
	        								annotations.put(KEY_SPLICING, "INTRON_SPLICING_DONOR");
	        								}
	        							}
	        						
	        						
	        						mutRNA.put(
	        								position_in_cdna,
	        								complement(alt.charAt(0))
	        								);
	        						}
	            				
	            				wildRNA.append(complement(genomicSeq.charAt(i)));
	            				if( wildRNA.length()%3==0 &&
	            					wildRNA.length()>0 &&
	            					wildProt==null)
		            				{
		            				wildProt=new ProteinCharSequence(geneticCode,new DefaultCharSeq(wildRNA));
		            				mutProt=new ProteinCharSequence(geneticCode,mutRNA);
		            				}
	            				
	            				}
	            			
	            			KnownGene.Intron intron= exon.getPrevIntron();
	            			if(intron!=null &&
	            				intron.contains(position))
	            				{
	            				annotations.put("intron.name",intron.getName());
	            				annotations.put(KEY_TYPE, "INTRON");
	            				
	            				if(intron.isSplicing(position))
	        						{
	            					annotations.put(KEY_SPLICING, "INTRON_SPLICING");
	        						if(intron.isSplicingAcceptor(position))
	        							{
	        							annotations.put(KEY_SPLICING, "INTRON_SPLICING_ACCEPTOR");
	        							}
	        						else if(intron.isSplicingDonor(position))
	        							{
	        							annotations.put(KEY_SPLICING, "INTRON_SPLICING_DONOR");
	        							}
	        						}
	            				}
	            			--exon_index;
	            			}

	            		}//end of if reverse
	        		if( wildProt!=null &&
	        			mutProt!=null && 
	        			position_in_cdna>=0)
		    			{
	            		int pos_aa=position_in_cdna/3;
	            		int mod= position_in_cdna%3;
	            		annotations.put("wild.codon",""+
	            			wildRNA.charAt(position_in_cdna-mod+0)+
	            			wildRNA.charAt(position_in_cdna-mod+1)+
	            			wildRNA.charAt(position_in_cdna-mod+2)
	            			);
	            		annotations.put("mut.codon",""+
	            			mutRNA.charAt(position_in_cdna-mod+0)+
	            			mutRNA.charAt(position_in_cdna-mod+1)+
	            			mutRNA.charAt(position_in_cdna-mod+2)
	            			);
	            		annotations.put("position.protein",String.valueOf(pos_aa+1));
	            		annotations.put("wild.aa",String.valueOf(wildProt.charAt(pos_aa)));
	            		annotations.put("mut.aa",String.valueOf(mutProt.charAt(pos_aa)));
		    			if(isStop(wildProt.charAt(pos_aa)) &&
		    			   !isStop(mutProt.charAt(pos_aa)))
		    				{
		    				annotations.put("type", "EXON_STOP_LOST");
		    				}
		    			else if( !isStop(wildProt.charAt(pos_aa)) &&
		    				 isStop(mutProt.charAt(pos_aa)))
		    				{
		    				annotations.put("type", "EXON_STOP_GAINED");
		    				}
		    			else if(wildProt.charAt(pos_aa)==mutProt.charAt(pos_aa))
		    				{
		    				annotations.put("type", "EXON_CODING_SYNONYMOUS");
		    				}
		    			else
		    				{
		    				annotations.put("type", "EXON_CODING_NON_SYNONYMOUS");
		    				}
		    			}
	        		}//end of simpe ATCG
            	else
            		{
	        		Integer wildrna=null;
	        		int position_in_cdna=-1;
	        		
	        		
	        		
	        		if(gene.isForward())
	            		{
	            		if(position < gene.getCdsStart())
	            			{
	            			annotations.put("type", "UTR5");
	            			}
	            		else if( gene.getCdsEnd()<= position )
	            			{
	            			annotations.put("type", "UTR3");
	            			}
	            		
	            		int exon_index=0;
	            		while(exon_index< gene.getExonCount())
	            			{
	            			KnownGene.Exon exon= gene.getExon(exon_index);
	            			for(int i= exon.getStart();
	            					i< exon.getEnd();
	            					++i)
	            				{
	            				if(i==position)
	        						{
	        						annotations.put("exon.name", exon.getName());
	        						}
	            				if(i< gene.getCdsStart()) continue;
	            				if(i>=gene.getCdsEnd()) break;
	        					
	        					if(wildrna==null)
	        						{
	        						wildrna=0;
	        						}
	        					
	        					if(i==position)
	        						{
	        						annotations.put(KEY_TYPE, "EXON");
	        						annotations.put("exon.name",exon.getName());
	        						position_in_cdna=wildrna;
	        						annotations.put("pos.cdna", String.valueOf(wildrna));
	        						//in splicing ?
	        						if(exon.isSplicing(position))
	        							{
	        							annotations.put(KEY_SPLICING, "SPLICING");
	        							
	        							if(exon.isSplicingAcceptor(position))
	        								{
	        								annotations.put(KEY_SPLICING, "SPLICING_ACCEPTOR");
	        								}
	        							else  if(exon.isSplicingDonor(position))
	        								{
	        								annotations.put(KEY_SPLICING, "SPLICING_DONOR");
	        								}
	        							}
	        						}
	        					
	        					wildrna++;
	            				}
	            			KnownGene.Intron intron= exon.getNextIntron();
	            			if(intron!=null && intron.contains(position))
	            				{
	            				annotations.put("intron.name",intron.getName());
	            				annotations.put("type", "INTRON");
	            				
	            				if(intron.isSplicing(position))
	        						{
	            					annotations.put(KEY_SPLICING, "INTRON_SPLICING");
	        						if(intron.isSplicingAcceptor(position))
	        							{
	        							annotations.put(KEY_SPLICING, "INTRON_SPLICING_ACCEPTOR");
	        							}
	        						else if(intron.isSplicingDonor(position))
	        							{
	        							annotations.put(KEY_SPLICING, "INTRON_SPLICING_DONOR");
	        							}
	        						}
	            				}
	            			++exon_index;
	            			}
	            		}
	            	else // reverse orientation
	            		{
	            	
	            		if(position < gene.getCdsStart())
	            			{
	            			annotations.put(KEY_TYPE, "UTR3");
	            			}
	            		else if( gene.getCdsEnd()<=position )
	            			{
	            			annotations.put(KEY_TYPE, "UTR5");
	            			}
	            	
	            		
	            		int exon_index = gene.getExonCount()-1;
	            		while(exon_index >=0)
	            			{
	            			KnownGene.Exon exon= gene.getExon(exon_index);
	            			for(int i= exon.getEnd()-1;
	            				    i>= exon.getStart();
	            				--i)
	            				{
	            				if(i==position)
	        						{
	            					annotations.put("exon.name", exon.getName());
	        						}
	            				if(i>= gene.getCdsEnd()) continue;
	            				if(i<  gene.getCdsStart()) break;
	            				
	            				if(wildrna==null)
	        						{
	        						wildrna=0;
	        						}
	            				
	            				if(i==position)
	        						{
	            					annotations.put(KEY_TYPE, "EXON");
	            					position_in_cdna=wildrna;
	        						annotations.put("pos.cdna",String.valueOf(position_in_cdna));
	        						//in splicing ?
	        						if(exon.isSplicing(position))
	        							{
	        							annotations.put(KEY_SPLICING, "INTRON_SPLICING");
	        							
	        							if(exon.isSplicingAcceptor(position))
	        								{
	        								annotations.put(KEY_SPLICING, "INTRON_SPLICING_ACCEPTOR");
	        								}
	        							else  if(exon.isSplicingDonor(position))
	        								{
	        								annotations.put(KEY_SPLICING, "INTRON_SPLICING_DONOR");
	        								}
	        							}
	        						}
	            				
	            				wildrna++;
	            				}
	            			
	            			KnownGene.Intron intron= exon.getPrevIntron();
	            			if(intron!=null &&
	            				intron.contains(position))
	            				{
	            				annotations.put("intron.name",intron.getName());
	            				annotations.put(KEY_TYPE, "INTRON");
	            				
	            				if(intron.isSplicing(position))
	        						{
	            					annotations.put(KEY_SPLICING, "INTRON_SPLICING");
	        						if(intron.isSplicingAcceptor(position))
	        							{
	        							annotations.put(KEY_SPLICING, "INTRON_SPLICING_ACCEPTOR");
	        							}
	        						else if(intron.isSplicingDonor(position))
	        							{
	        							annotations.put(KEY_SPLICING, "INTRON_SPLICING_DONOR");
	        							}
	        						}
	            				}
	            			--exon_index;
	            			}

	            		}//end of if reverse
	        		if( wildrna!=null &&
	        			position_in_cdna>=0)
		    			{
	            		int pos_aa=position_in_cdna/3;
	            		annotations.put("position.protein",String.valueOf(pos_aa+1));
		    			}
            		}
            	annotations.put("strand", ""+gene.getStrand());
            	annotations.put("kgId", gene.getName());
            	annotations.put("geneSymbol", gene.getGeneSymbol());
            	call.addProperties("PREDICTION", annotations);
            	}
            }
	   
		}
	
	
	private boolean isStop(char aa)
		{
		return aa=='*';
		}
	
	
	}

/**
 * VCFAnnotator
 * Annotator for VCF
 *
 */
public class VCFAnnotator
	{
	static Logger LOG=Logger.getLogger("vcf.annotator");
	private VCFAnnotator() throws Exception
		{
		}
	public static void main(String[] args)
		{
		try {
			String proxyHost=null;
			String proxyPort=null;
			boolean basicPrediction=false;
			boolean mapability=false;
			boolean genomicSuperDups=false;
			boolean phastcons=false;
			boolean transfac=false;
			boolean rmsk=false;
			boolean polyX=false;
			boolean loadWholeSegment=false;
			List<PersonalGenomeAnnotator> personalGenomes=new ArrayList<PersonalGenomeAnnotator>();
			Set<String> dbsnpID=new HashSet<String>();
			String genomeVersion="hg18";
			LOG.setLevel(Level.OFF);
			int optind=0;
			while(optind<args.length)
				{
				if(args[optind].equals("-h"))
					{
					System.out.println("VCF annotator");
					System.out.println("Pierre Lindenbaum PhD. 2010. http://plindenbaum.blogspot.com");
					System.out.println("Options:");
					System.out.println(" -b ucsc.build default:"+ genomeVersion);
					System.out.println(" -m mapability.");
					System.out.println(" -g genomicSuperDups");
					System.out.println(" -p basic prediction");
					System.out.println(" -c phastcons prediction (phastCons44way)");
					System.out.println(" -t transcription factors sites prediction");
					System.out.println(" -pg personal genomes");
					System.out.println(" -rmsk repeat masker");
					System.out.println(" -polyX get the number of repeated bases in the genomic context");
					System.out.println(" -snp <id> add ucsc <id> must be present in \"http://hgdownload.cse.ucsc.edu/goldenPath/<ucscdb>/database/<id>.txt.gz\" e.g. snp129");
					System.out.println(" -whole  load whole chromosome in memory");
					System.out.println(" -log  <level> one value from "+Level.class.getName()+" default:"+LOG.getLevel());
					System.out.println(" -proxyHost <host>");
					System.out.println(" -proxyPort <port>");
					System.out.println(" -timeout <seconds> connection timout default:"+IOUtils.TIMEOUT_SECONDS);
					System.out.println(" -try <times> retry n-times of connection fails default:"+IOUtils.TRY_CONNECT);
					return;
					}
				else if(args[optind].equals("-whole"))
					{
					loadWholeSegment=true;
					}
				else if(args[optind].equals("-timeout"))
					{
					IOUtils.TIMEOUT_SECONDS=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-try"))
					{
					IOUtils.TRY_CONNECT=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-b"))
					{
					genomeVersion=args[++optind];
					if(!genomeVersion.equals("hg18"))
						{
						System.err.println("** WARNING. This tool was mostly developed for hg18. I didn't check the URL for the other assemblies.");
						}
					}
				else if(args[optind].equals("-m"))
					{
					mapability=true;
					}
				else if(args[optind].equals("-t"))
					{
					transfac=true;
					}
				else if(args[optind].equals("-polyX"))
					{
					polyX=true;
					}
				else if(args[optind].equals("-snp"))
					{
					dbsnpID.add(args[++optind]);
					}
				else if(args[optind].equals("-g"))
					{
					genomicSuperDups=true;
					}
				else if(args[optind].equals("-c"))
					{
					phastcons=true;
					}
				else if(args[optind].equals("-p"))
					{
					basicPrediction=true;
					}
				else if(args[optind].equals("-rmsk"))
					{
					rmsk=true;
					}
				else if(args[optind].equalsIgnoreCase("-pg"))
					{
					personalGenomes.add(new PersonalGenomeAnnotator("NA12878"));
					personalGenomes.add(new PersonalGenomeAnnotator("NA12891"));
					personalGenomes.add(new PersonalGenomeAnnotator("NA12892"));
					personalGenomes.add(new PersonalGenomeAnnotator("NA19240"));
					personalGenomes.add(new PersonalGenomeAnnotator("Sjk"));
					personalGenomes.add(new PersonalGenomeAnnotator("Venter"));
					personalGenomes.add(new PersonalGenomeAnnotator("Watson"));
					personalGenomes.add(new PersonalGenomeAnnotator("Yh1"));
					personalGenomes.add(new PersonalGenomeAnnotator("Yoruban3"));
					}
				else if(args[optind].equals("-log") ||
						args[optind].equals("--log") || 
						args[optind].equals("-debug"))
					{
					LOG.setLevel(Level.parse(args[++optind]));
					}
				else if(args[optind].equals("-proxyHost"))
					{
					proxyHost=args[++optind];
					}
				else if(args[optind].equals("-proxyPort"))
					{
					proxyPort=args[++optind];
					}
				else if(args[optind].equals("--"))
					{
					optind++;
					break;
					}
				else if(args[optind].startsWith("-"))
					{
					System.err.println("Unnown option: "+args[optind]);
					return;
					}
				else
					{
					break;
					}
				++optind;
				}
			
			if(proxyHost!=null)
				{
				LOG.info("setting proxy host");
				System.setProperty("http.proxyHost", proxyHost);
				}
			if(proxyPort!=null)
				{
				LOG.info("setting proxy port");
				System.setProperty("http.proxyPort", proxyPort);
				}
			
			VCFFile vcf=null;
			if(optind==args.length)
				{
				LOG.info("reading from stdin");
				vcf=VCFFile.parse(new BufferedReader(new InputStreamReader(System.in)));
				}
			else if(optind+1==args.length)
				{
				String filename=args[optind++];
				LOG.info("reading from "+filename);
				BufferedReader in=IOUtils.mustOpen(filename);
				vcf=VCFFile.parse(in);
				in.close();
				}
			else
				{
				System.err.println("Illegal Number of arguments");
				return;
				}
			
			for(String table: dbsnpID)
				{
				SnpAnnotator an2=new SnpAnnotator(genomeVersion,table);
				an2.setVcfFile(vcf);
				an2.run();
				}
			
			for(PersonalGenomeAnnotator pg:personalGenomes)
				{
				pg.setGenomeVersion(genomeVersion);
				pg.setVcfFile(vcf);
				pg.run();
				}
			
			if(mapability)
				{
				MapabilityAnnotator an4=new MapabilityAnnotator(genomeVersion);
				an4.setVcfFile(vcf);
				an4.run();
				}
			
			if(phastcons)
				{
				PhastConsAnnotator an4=new PhastConsAnnotator(genomeVersion);
				an4.setVcfFile(vcf);
				an4.run();
				}
			
			if(transfac)
				{
				TranscriptionBindingSitesAnnotator an=new TranscriptionBindingSitesAnnotator(genomeVersion);
				an.setVcfFile(vcf);
				an.run();
				}
			
			if(genomicSuperDups)
				{
				GenomicSuperDupAnnotator an1=new GenomicSuperDupAnnotator(genomeVersion);
				an1.setVcfFile(vcf);
				an1.run();
				}
			
			if(basicPrediction)
				{
				PredictionAnnotator predictor=new PredictionAnnotator(genomeVersion);
				predictor.loadWholeSegment=loadWholeSegment;
				predictor.setVcfFile(vcf);
				predictor.preLoadUcsc();
				predictor.run();
				}
			if(rmsk)
				{
				RepeatMaskerAnnotator predictor=new RepeatMaskerAnnotator(genomeVersion);
				predictor.setVcfFile(vcf);
				predictor.run();
				}
			if(polyX)
				{
				PolyXAnnotator predictor=new PolyXAnnotator(genomeVersion);
				predictor.loadWholeSegment=loadWholeSegment;
				predictor.setVcfFile(vcf);
				predictor.run();
				}
			
			PrintWriter out=new PrintWriter(System.out);
			vcf.print(out);
			out.flush();
			
			}
		catch (Exception e) {
			e.printStackTrace();
			}
		}
	}