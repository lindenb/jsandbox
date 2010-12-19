package sandbox;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
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
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



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

	}

/**
 * A VCF file
 * @author pierre
 *
 */
class VCFFile
	{
	private static Logger LOG=Logger.getLogger("vcf.annotator");
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
    
    @SuppressWarnings("unchecked")
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
			final String COLS="#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT";
			String last=headers.get(headers.size()-1);
			if(!last.startsWith(COLS))
				{
				throw new IOException("Error in header got "+line+" but expected "+COLS);
				}
			}
		
		while(line!=null)
			{
			LOG.info(line);
			if(line.startsWith("#")) throw new IOException("line starting with # after header!"+line);
			String tokens[]=tab.split(line, 9);
			if(tokens.length<9) throw new IOException("illegal number of columns in "+line);
			getCalls().add(new VCFCall(tokens));
			line=in.readLine();
			}
		
		Collections.sort(getCalls());
		LOG.info("vcf:"+getCalls().size());
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
	private int chromStart;
	
	public GenomicSequence(byte array[],String chrom,int chromStart)
		{	
		this.chrom=chrom;
		this.array=array;
		this.chromStart=chromStart;
		}
	
	public String getChrom()
		{
		return chrom;
		}
	public int getChromStart()
		{
		return chromStart;
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
	public char charAt(int index)
		{
		if(index < getChromStart() || index >=getChromEnd())
			{
			throw new IndexOutOfBoundsException("index:"+index);
			}
		return (char)(array[index-chromStart]);
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
	private int reserve=100;
	private SAXParser parser;
	public DasSequenceProvider(String ucscBuild)
		{
		this.ucscBuild=ucscBuild;
		SAXParserFactory f=SAXParserFactory.newInstance();
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


	public GenomicSequence getSequence(String chrom, int chromStart, int chromEnd)
			throws IOException
		{
		if(chromStart<1 || chromStart>=chromEnd)
			{
			throw new IllegalArgumentException("Error in start/end");
			}
		this.reserve=(1+(chromEnd-chromStart));
		this.baos=null;
		try
			{
			String uri="http://genome.ucsc.edu/cgi-bin/das/"+
					this.ucscBuild+
					"/dna?segment="+
					URLEncoder.encode(chrom, "UTF-8")+
					":"+(chromStart+1)+","+(chromEnd+2)
					;
			LOG.info(uri);
			this.parser.parse(uri, this);
			GenomicSequence g= new GenomicSequence(this.baos.toByteArray(),chrom,chromStart);
			this.baos=null;
			return g;
			}
		catch (SAXException err)
			{
			throw new IOException(err);
			}
		
		}
	
	}

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
		

		public String getName()
			{
			return this.name;
			}
		

		public String getChromosome()
			{
			return this.chrom;
			}
		
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


class Mapability2SQL
	{
	private static final String base="http://hgdownload.cse.ucsc.edu/goldenPath/hg18/encodeDCC/wgEncodeMapability/";
	private String currChrom=null;
	private int currPosition=0;
	private int currStep=1;
	private int currSpan=1;
	private int variation_index=-1;
	VCFFile vcfFile;
	
	private void fixedStep(String line)
		{
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
		currPosition-=currStep;
		variation_index= vcfFile.getCalls().size();
		for(int i=0;i< vcfFile.getCalls().size();++i)
			{
			VCFCall c= this.vcfFile.getCalls().get(i);
			if(!c.getChromPosition().getChromosome().equalsIgnoreCase(currChrom)) continue;
			if(c.getChromPosition().getPosition()+1  >= currPosition)
				{
				variation_index=i;
				break;
				}
			}
		}
	
	
	
	/**
	 * The Broad alignability track displays whether a region is made up of mostly unique or mostly non-unique sequence. 
	 * @throws Exception
	 */
	public void run() throws Exception
		{
		scanWig("wgEncodeBroadMapabilityAlign36mer.wig.gz","wgEncodeBroadMapabilityAlign36mer");
		for(int i: new int[]{20,24,35})
			{
			scanWig("wgEncodeDukeUniqueness"+i+"bp.wig.gz","wgEncodeDukeUniqueness"+i);
			}
		}
	
	private void scanWig(String path,String table) throws Exception
		{
		URL url=new URL(base+path);
		BufferedReader r=new BufferedReader(
			new InputStreamReader(
			new GZIPInputStream(url.openStream())));
		String line;
		
		while((line=r.readLine())!=null)
			{
			if(line.startsWith("fixedStep"))
				{
				fixedStep(line);
				continue;
				}
			
			currPosition+=currStep;
			
			//advance variation_index
			while(
				variation_index<  vcfFile.getCalls().size() &&
				(vcfFile.getCalls().get(variation_index).getChromPosition().getPosition() -1 ) <currPosition &&
				(vcfFile.getCalls().get(variation_index).getChromPosition().getChromosome().equalsIgnoreCase(currChrom))
				)
				{
				variation_index++;
				}
			
			int n2=variation_index;
			while(n2 < vcfFile.getCalls().size() &&
					vcfFile.getCalls().get(n2).getChromPosition().getPosition() -1 >=currPosition &&
					vcfFile.getCalls().get(n2).getChromPosition().getPosition() -1 <(currPosition+currStep) &&
					vcfFile.getCalls().get(n2).getChromPosition().getChromosome().equalsIgnoreCase(currChrom))
					{
					//insert(table,variations.get(n2).getId(),line);
					++n2;
					}
			}
		r.close();
		}
		
	}


class GenomicSuperDupAnnotator
	{
	static Logger LOG=Logger.getLogger("vcf.annotator");
	
	VCFFile vcfFile;
	public void run()
		throws IOException
		{
		Pattern tab=Pattern.compile("\t");
		Set<String> chromosomes= vcfFile.getChromosomes();
		URL url=new URL("http://hgdownload.cse.ucsc.edu/goldenPath/hg18/database/genomicSuperDups.txt.gz");
		BufferedReader in=new BufferedReader(new InputStreamReader(new GZIPInputStream(url.openStream())));
		String line;
		while((line=in.readLine())!=null)
			{
			String tokens[]=tab.split(line);
			if(!chromosomes.contains(tokens[1])) continue;
			int chromStart=Integer.parseInt(tokens[2]);
			int chromEnd=Integer.parseInt(tokens[3]);
			int i=this.vcfFile.lowerBound(new ChromPosition(tokens[1], chromStart+1));
			while(i< this.vcfFile.getCalls().size())
				{
				VCFCall call= this.vcfFile.getCalls().get(i);
				int d=tokens[1].compareToIgnoreCase(call.getChromPosition().getChromosome());
				if(d<0) { ++i; continue;}
				if(d>0) break;
				if(chromStart+1> call.getChromPosition().getPosition())
					{
					++i;
					continue;
					}
				if(chromEnd+1 <= call.getChromPosition().getPosition())
					{
					break;
					}
				//TODP
				LOG.info(line);
				LOG.info(call.getLine());
				++i;
				}
			}
		}
	}


class SnpAnnotator
	{
	static Logger LOG=Logger.getLogger("vcf.annotator");
	
	VCFFile vcfFile;
	private String table;
	public SnpAnnotator(String table)
		{
		this.table=table;
		}
	
	public void run()
		throws IOException
		{
		Pattern tab=Pattern.compile("\t");
		Set<String> chromosomes= vcfFile.getChromosomes();
		URL url=new URL("http://hgdownload.cse.ucsc.edu/goldenPath/hg18/database/"+table+".txt.gz");
		BufferedReader in=new BufferedReader(new InputStreamReader(new GZIPInputStream(url.openStream())));
		String line;
		while((line=in.readLine())!=null)
			{
			String tokens[]=tab.split(line);
			if(!chromosomes.contains(tokens[1])) continue;
			int chromStart=Integer.parseInt(tokens[2]);
			int chromEnd=Integer.parseInt(tokens[3]);
			int i=this.vcfFile.lowerBound(new ChromPosition(tokens[1], chromStart+1));
			while(i< this.vcfFile.getCalls().size())
				{
				VCFCall call= this.vcfFile.getCalls().get(i);
				int d=tokens[1].compareToIgnoreCase(call.getChromPosition().getChromosome());
				if(d<0) { ++i; continue;}
				if(d>0) break;
				if(chromStart+1> call.getChromPosition().getPosition())
					{
					++i;
					continue;
					}
				if(chromEnd+1 <= call.getChromPosition().getPosition())
					{
					break;
					}
				//TODP
				LOG.info(line);
				LOG.info(call.getLine());
				++i;
				}
			}
		}
	}



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
	VCFAnnotator owner;
	VCFFile vcfFile;
	DasSequenceProvider dasServer;
	
	
	PredictionAnnotator(VCFAnnotator owner) throws Exception
		{
		this.owner=owner;
		this.dasServer=new DasSequenceProvider("hg18");
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
		URL url=new URL("http://hgdownload.cse.ucsc.edu/goldenPath/hg18/database/knownGene.txt.gz");
		BufferedReader in=new BufferedReader(new InputStreamReader(new GZIPInputStream(url.openStream())));
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
		
		url=new URL("http://hgdownload.cse.ucsc.edu/goldenPath/hg18/database/kgXref.txt.gz");
		in=new BufferedReader(new InputStreamReader(new GZIPInputStream(url.openStream())));
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
    	            	genomicSeq=this.dasServer.getSequence(
    	            		gene.getChromosome(),
    	            		Math.max(gene.getTxStart()-extra,0),
    	            		gene.getTxEnd()+extra
    	            		);
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
	        						annotations.put("exon", exon.getName());
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
		    				System.err.println(annotations);
		    				System.err.println(call.getLine());
		    				System.exit(-1);//TODO
		    				}
		    			}
	        		}//end of simpe ATCG
            	else
            		{
            		LOG.info("not a substiturion");
            		}
            	annotations.put("strand", ""+gene.getStrand());
            	annotations.put("kgId", gene.getName());
            	annotations.put("geneSymbol", gene.getGeneSymbol());
            	System.err.println(" "+annotations+" "+call);
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
	private String genomeVersion="hg18";
	private PredictionAnnotator prediction;
	private VCFAnnotator() throws Exception
		{
		this.prediction=new PredictionAnnotator(this);
		}
	
	public String getUcscGenomeVersion() {
		return genomeVersion;
		}
	
	
	
	public static void main(String[] args)
		{
		try {
			LOG.setLevel(Level.ALL);
			VCFAnnotator app=new VCFAnnotator();
			int optind=0;
			while(optind<args.length)
				{
				if(args[optind].equals("-h"))
					{
					System.out.println("VCF annotator");
					System.out.println(" -b ucsc.build default:"+app.genomeVersion);
					return;
					}
				else if(args[optind].equals("-b"))
					{
					app.genomeVersion=args[++optind];
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
			
			VCFFile vcf= VCFFile.parse(new BufferedReader(new FileReader(new File("/home/pierre/sample1.vcf"))));
			GenomicSuperDupAnnotator an1=new GenomicSuperDupAnnotator();
			an1.vcfFile=vcf;
			an1.run();
			
			SnpAnnotator an2=new SnpAnnotator("snp129");
			an2.vcfFile=vcf;
			an2.run();
			
			SnpAnnotator an3=new SnpAnnotator("snp130");
			an3.vcfFile=vcf;
			an3.run();
			
			
			app.prediction.vcfFile=vcf;
			app.prediction.preLoadUcsc();
			app.prediction.run();
			}
		catch (Exception e) {
			e.printStackTrace();
			}
		}
	}