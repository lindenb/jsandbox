/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	March-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * Reference:
 *   
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Wiki
 *  
 * Motivation:
 * 	map a mutation on a protein back to the genome
 * Compilation:
 *        cd jsandbox; ant backlocate
 * Usage:
 *         echo -e "NOTCH2\tP29A" | java -jar backlocate.jar
 */
package sandbox;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * BackLocate
 * Annotator for VCF
 *
 */
public class BackLocate
	{
	private static final Logger LOG=Logger.getLogger("back.locate");
	private String genomeVersion="hg19";
	private Connection connection;
	private GenomicSequence genomicSeq=null;
	private DasSequenceProvider dasServer=new DasSequenceProvider();
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
	/**
	 *  
	 * Genetic Code
	 *
	 */
	abstract static class GeneticCode
		{
		/** the standard genetic code */
		
		/** get the genetic-code table (NCBI data) */ 
		protected abstract String getNCBITable();
		
		/** convert a base to index */
		private int base2index(char c)
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
		
		
		}

	/** get a genetic code from a chromosome name (either std or mitochondrial */
	private static GeneticCode getGeneticCodeByChromosome(String chr)
		{
		if(chr.equalsIgnoreCase("chrM")) return MITOCHONDRIAL;
		return STANDARD;
		}

	
	/** CharSeq a simple string impl */
	static private interface CharSeq
		{
		public int length();
		public char charAt(int i);
		}

	/**
	 * Abstract implementation of CharSeq
	 */
	abstract static class AbstractCharSeq implements CharSeq
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
	static private class GenomicSequence
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

	
	static private class RNASequence extends AbstractCharSeq
		{
		List<Integer> genomicPositions=new ArrayList<Integer>();
		GenomicSequence genomic;
		char strand;
		RNASequence(GenomicSequence genomic,char strand)
			{
			this.genomic=genomic;
			this.strand=strand;
			}
		@Override
		public char charAt(int i)
			{
			char c=genomic.charAt(this.genomicPositions.get(i));
			return (strand=='+'?c:complement(c));
			}
		@Override
		public int length()
			{
			return genomicPositions.size();
			}
		}

static private class ProteinCharSequence extends AbstractCharSeq
	{
	private RNASequence cDNA;
	private GeneticCode geneticCode;
	ProteinCharSequence(GeneticCode geneticCode,RNASequence cDNA)
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
	private class DasSequenceProvider
		extends DefaultHandler
		{
		private ByteArrayOutputStream baos=null;
		int reserve=100000;
		private SAXParser parser;
		public DasSequenceProvider()
			{
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
						BackLocate.this.genomeVersion+
						"/dna?segment="+
						URLEncoder.encode(chrom+":"+(chromStart0+1)+","+(chromEnd0+2), "UTF-8")
						;
				LOG.info(uri);
				InputStream in=new URL(uri).openStream();
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
		
		}

/**
 * 
 * KnownGene
 *
 */
private class KnownGene
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
		
		
		
		
			/**
			 * 
			 * KnownGene 
			 * 
			 */
			public KnownGene(ResultSet row)
				throws SQLException
				{
				this.name = row.getString("name");
				this.chrom= row.getString("chrom");
		        this.strand = row.getString("strand").charAt(0);
		        this.txStart = row.getInt("txStart");
		        this.txEnd = row.getInt("txEnd");
		        this.cdsStart= row.getInt("cdsStart");
		        this.cdsEnd= row.getInt("cdsEnd");
		        int exonCount=row.getInt("exonCount");
		        this.exonStarts = new int[exonCount];
		        this.exonEnds = new int[exonCount];
		            
	            
	            int index=0;
	            for(String s: row.getString("exonStarts").split("[,]"))
	            	{
	            	this.exonStarts[index++]=Integer.parseInt(s);
	            	}
	            index=0;
	            for(String s: row.getString("exonEnds").split("[,]"))
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
			
	
			
			public int getExonCount()
				{
				return this.exonStarts.length;
				}
			
			public String getExonNameFromGenomicIndex(int genome)
				{
				for(int i=0;i< getExonCount();++i)
					{
					if(this.exonStarts[i]<=genome && genome< this.exonEnds[i])
						{
						if(this.strand=='+')
							{
							return "Exon "+(i+1);
							}
						else
							{
							return "Exon "+(getExonCount()-i);
							}
						}
					}
				throw new IndexOutOfBoundsException();
				}
			
			}


	
	

	

		
		
	

	
	private void backLocate(
		KnownGene gene,
		String geneName,
		char aa1,char aa2,
		int peptidePos1
		) throws IOException
		{
		final int extra=1000;
		
		GeneticCode geneticCode=getGeneticCodeByChromosome(gene.getChromosome());
		RNASequence wildRNA=null;
		ProteinCharSequence wildProt=null;
		
	        		
	        		
		if(genomicSeq==null ||
	               !gene.getChromosome().equals(genomicSeq.getChrom()) ||
	               !(genomicSeq.getChromStart()<=gene.getTxStart() && gene.getTxEnd()<= genomicSeq.getChromEnd())
	               )
        	{
        	LOG.info("fetch genome");
        	this.genomicSeq=this.dasServer.getSequence(
    	            		gene.getChromosome(),
    	            		Math.max(gene.getTxStart()-extra,0),
    	            		gene.getTxEnd()+extra
    	            		);
        	}
        	
	        		
	        		
	        		
	     if(gene.isForward())
    		{    		
    		int exon_index=0;
    		while(exon_index< gene.getExonCount())
    			{
    			for(int i= gene.getExonStart(exon_index);
    					i< gene.getExonEnd(exon_index);
    					++i)
    				{
    				if(i< gene.getCdsStart()) continue;
    				if(i>=gene.getCdsEnd()) break;
					
					if(wildRNA==null)
						{
						wildRNA=new RNASequence(genomicSeq,'+');
						}

    				wildRNA.genomicPositions.add(i);
    				
    				
    				
    				if(wildRNA.length()%3==0 && wildRNA.length()>0 && wildProt==null)
        				{
        				wildProt=new ProteinCharSequence(geneticCode,wildRNA);
        				}
    				}
    			++exon_index;
    			}
    		
    		
    		
    		}
	   else // reverse orientation
    		{
    		int exon_index = gene.getExonCount()-1;
    		while(exon_index >=0)
    			{
    			for(int i= gene.getExonEnd(exon_index)-1;
    				    i>= gene.getExonStart(exon_index);
    				--i)
    				{
    				if(i>= gene.getCdsEnd()) continue;
    				if(i<  gene.getCdsStart()) break;
    				
    				if(wildRNA==null)
						{
						wildRNA=new RNASequence(genomicSeq,'-');
						}
    				
    				
    				
    				wildRNA.genomicPositions.add(i);
    				if( wildRNA.length()%3==0 &&
    					wildRNA.length()>0 &&
    					wildProt==null)
        				{
        				wildProt=new ProteinCharSequence(geneticCode,wildRNA);
        				}
    				
    				}
    			--exon_index;
    			}

    		}//end of if reverse
	        		
	     if(wildProt==null)
	    	 {
	    	 System.err.println("#no protein found for transcript:"+gene.getName());
	    	 return;
	    	 }
	    int peptideIndex0= peptidePos1-1;
        if(peptideIndex0 >=wildProt.length())
        	{
        	System.err.println("#index out of range for :"+gene.getName()+" petide length="+wildProt.length());
	    	return;
        	}
        if(wildProt.charAt(peptideIndex0)!=aa1)
        	{
        	System.err.println("Warning ref aminod acid at "+peptidePos1+" is not the same ("+wildProt.charAt(peptideIndex0)+"/"+aa1+")");
        	}
        int indexesInRNA[]=new int[]{
        	0+ peptideIndex0/3,
        	1+ peptideIndex0/3,
        	2+ peptideIndex0/3
        	};
        String codon=""
        		+ wildRNA.charAt(indexesInRNA[0])
        		+ wildRNA.charAt(indexesInRNA[1])
        		+ wildRNA.charAt(indexesInRNA[2])
        		;
        		
        for(int indexInRna: indexesInRNA)
        	{
        	System.out.print(geneName);
        	System.out.print('\t');
        	System.out.print(aa1);
        	System.out.print('\t');
        	System.out.print(peptidePos1);
        	System.out.print('\t');
        	System.out.print(aa2);
        	System.out.print('\t');
        	System.out.print(gene.getName());
        	System.out.print('\t');
        	System.out.print(gene.getStrand());
        	System.out.print('\t');
        	System.out.print(wildProt.charAt(peptideIndex0));
        	System.out.print('\t');
        	System.out.print(indexInRna);
        	System.out.print('\t');
        	System.out.print(codon);
        	System.out.print('\t');
        	System.out.print(wildRNA.charAt(indexInRna));
        	System.out.print('\t');
        	System.out.print(gene.getChromosome());
        	System.out.print('\t');
        	System.out.print(wildRNA.genomicPositions.get(indexInRna));
        	System.out.print('\t');
        	System.out.print(gene.getExonNameFromGenomicIndex(wildRNA.genomicPositions.get(indexInRna)));
        	System.out.println();
        	}
		}

	private BackLocate() 
		{
		}
	
	private static char complement(char c)
		{
		switch(c)
			{
			case 'A':case 'a': return 'T';
			case 'T':case 't': return 'A';
			case 'G':case 'g': return 'C';
			case 'C':case 'c': return 'G';
			default:throw new IllegalArgumentException(""+c);
			}
		}
	
	private void run(BufferedReader in) throws IOException,SQLException
		{
		String line;
		while((line=in.readLine())!=null)
			{
			if(line.startsWith("#") || line.trim().isEmpty()) continue;
			int n=line.indexOf('\t');
			if(n==0 || n==-1) throw new IOException("Bad line. No tab found in "+line);
			String geneName=line.substring(0,n).trim();
			if(geneName.isEmpty()) throw new IOException("Bad line. No gene in "+geneName);
			String mut=line.substring(n+1).trim();
			if(!mut.matches("[A-Za-z\\*][0-9]+[A-Za-z\\*]")) throw new IOException("Bad mutation  in "+line);
			char aa1= mut.substring(0,1).toUpperCase().charAt(0);
			char aa2= mut.substring(mut.length()-1).toUpperCase().charAt(0);
			int position1=Integer.parseInt(mut.substring(1,mut.length()-1));
			if(position1==0) throw new IOException("Bad position  in "+line);
			Set<String> kgIds=new HashSet<String>();
			PreparedStatement pstmt=connection.prepareStatement("select kgID from kgXref where geneSymbol=?");
			pstmt.setString(1, geneName);
			ResultSet row=pstmt.executeQuery();
			while(row.next())
				{
				kgIds.add(row.getString(1));
				}
			row.close();
			pstmt.close();
			if(kgIds.isEmpty())
				{
				System.err.println("No kgXref found for "+geneName);
				continue;
				}
			pstmt=connection.prepareStatement("select * from knownGene where name=?");
			for(String kgId:kgIds)
				{
				pstmt.setString(1, kgId);
				row=pstmt.executeQuery();
				while(row.next())
					{
					KnownGene kg=new KnownGene(row);
					backLocate(kg, geneName, aa1, aa2, position1);
					}
				row.close();
				}
			pstmt.close();
			}
		}
	
	public static void main(String[] args)
		{
		BackLocate app=new BackLocate();
		try {
			String proxyHost=null;
			String proxyPort=null;
			
			LOG.setLevel(Level.OFF);
			int optind=0;
			while(optind<args.length)
				{
				if(args[optind].equals("-h"))
					{
					System.out.println("BackLocate");
					System.out.println("Pierre Lindenbaum PhD. 2011. http://plindenbaum.blogspot.com");
					System.out.println("Options:");
					System.out.println(" -b ucsc.build default:"+ app.genomeVersion);
					System.out.println(" -log  <level> one value from "+Level.class.getName()+" default:"+LOG.getLevel());
					System.out.println(" -proxyHost <host>");
					System.out.println(" -proxyPort <port>");
					System.out.println("e.g:  echo -e \"NOTCH2\\tM1I\" | java -jar dist/backlocate.jar");
					return;
					}
				
				else if(args[optind].equals("-b"))
					{
					app.genomeVersion=args[++optind];
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
			
			Class.forName("com.mysql.jdbc.Driver");
			app.connection=DriverManager.getConnection(
				"jdbc:mysql://genome-mysql.cse.ucsc.edu/"+
				app.genomeVersion+
				"?user=genome&password="
				);
			System.out.print("#User.Gene");
        	System.out.print('\t');
        	System.out.print("AA1");
        	System.out.print('\t');
        	System.out.print("petide.pos.1");
        	System.out.print('\t');
        	System.out.print("AA2");
        	System.out.print('\t');
        	System.out.print("knownGene.name");
        	System.out.print('\t');
        	System.out.print("knownGene.strand");
        	System.out.print('\t');
        	System.out.print("knownGene.AA");
        	System.out.print('\t');
        	System.out.print("index0.in.rna");
        	System.out.print('\t');
        	System.out.print("codon");
        	System.out.print('\t');
        	System.out.print("base.in.rna");
        	System.out.print('\t');
        	System.out.print("chromosome");
        	System.out.print('\t');
        	System.out.print("index0.in.genomic");
        	System.out.print('\t');
        	System.out.print("exon");
        	System.out.println();
			if(optind==args.length)
				{
				LOG.info("reading from stdin");
				app.run(new BufferedReader(new InputStreamReader(System.in)));
				}
			else
				{
				while(optind<args.length)
					{
					String filename=args[optind++];
					LOG.info("reading from "+filename);
					BufferedReader in=new BufferedReader(new FileReader(filename));
					app.run(in);
					in.close();
					}
				}
			}
		catch (Exception e) {
			e.printStackTrace();
			}
		finally
			{
			if(app.connection!=null)
				{
				try {app.connection.close();}
				catch(SQLException err){}
				}
			}	
		}
	}