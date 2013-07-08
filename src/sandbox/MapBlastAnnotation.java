/**
 * Author:
 * 		Pierre LIndenbaum PhD
 * WWW:
 * 		http://plindenbaum.blogspot.com
 * Mail:
 * 		plindenbaum@yahoo.fr
 * Motivation:
 * 		displays the annotation for a blast alignement.
 * 		annotations are fetched from the NCBI if sequence def starts with 'gi|...'
 * 
 */
package sandbox;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import sandbox.ncbi.blast.BlastOutput;
import sandbox.ncbi.blast.BlastOutputIterations;
import sandbox.ncbi.blast.Hit;
import sandbox.ncbi.blast.Hsp;
import sandbox.ncbi.blast.Iteration;
import sandbox.ncbi.gb.GBFeature;
import sandbox.ncbi.gb.GBInterval;
import sandbox.ncbi.gb.GBQualifier;
import sandbox.ncbi.gb.GBSeq;
import sandbox.ncbi.gb.GBSet;
import sandbox.uniprot.Entry;
import sandbox.uniprot.FeatureType;
import sandbox.uniprot.LocationType;
import sandbox.uniprot.Uniprot;

/**
 * MapBlastAnnotation
 */
public class MapBlastAnnotation
	{
	private static final Logger LOG=Logger.getLogger("MapBlastAnnotation");
	/** xml parser */
	private DocumentBuilder docBuilder;
	/** transforms XML/DOM to GBC entry */
	private Unmarshaller unmarshaller;
	
	private GBSet gbSet=null;
	private Uniprot uniprotSet=null;
	private BlastOutput blastOutput=null;

	
	

	/** constructor */
	private MapBlastAnnotation() throws Exception
		{
		//create a DOM parser
		DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
		f.setCoalescing(true);
		f.setNamespaceAware(true);
		f.setValidating(false);
		f.setExpandEntityReferences(true);
		f.setIgnoringComments(false);
		f.setIgnoringElementContentWhitespace(true);
		this.docBuilder= f.newDocumentBuilder();
		this.docBuilder.setEntityResolver(new EntityResolver()
			{
			@Override
			public InputSource resolveEntity(String publicId, String systemId)
					throws SAXException, IOException
				{
				return new InputSource(new StringReader(""));
				}
			});
		//create a Unmarshaller for NCBI
		JAXBContext jc = JAXBContext.newInstance(
				"sandbox.ncbi.blast:sandbox.ncbi.gb:sandbox.uniprot");
		this.unmarshaller=jc.createUnmarshaller();
		}
	
	
	private static class BlastPos
		{
		int query;
		int hit;
		@Override
		public String toString() {
			return "{hit:"+hit+",query:"+query+"}";
			}
		}
	private abstract class Interval
		{
		
		Hit hit;
		Hsp hsp;
		
		protected final BlastPos convertQuery(int qPos1)
			{
			BlastPos left=getHspStart1();
			
			String qS=this.hsp.getHspQseq();
			String hS=this.hsp.getHspHseq();
			for(int i=0;i< qS.length() && i< hS.length();++i)
				{
				if(left.query>=qPos1) break;
				if(isSeqLetter(qS.charAt(i)))
					{
					left.query+=this.queryShift();
					}
				if(isSeqLetter(hS.charAt(i)))
					{
					left.hit+=this.hitShift();
					}
				}
			return left;
			}
		
		protected final int queryShift()
			{
			return 1;
			}
		
		protected final int hitShift()
			{
			int shift=1;
			if(blastOutput.getBlastOutputProgram().equals("tblastn"))
				{
				shift=3;
				}
			else if(blastOutput.getBlastOutputProgram().equals("blastn"))
				{
				shift=1;
				}
			else
				{
				throw new RuntimeException("Sorry program not handled: "+blastOutput.getBlastOutputProgram());
				}
			return shift*(isHspForward()?1:-1);
			}
		
	
		private final boolean isSeqLetter(char c)
			{
			if(c=='-' || Character.isWhitespace(c)) return false;
			if(Character.isLetter(c)) return true;
			throw new IllegalArgumentException("letter: "+c);
			}

		
		
		protected final BlastPos getHspStart1()
			{
			BlastPos p=new BlastPos();
			p.query=Integer.parseInt(this.hsp.getHspQueryFrom());
			p.hit=Integer.parseInt(this.hsp.getHspHitFrom());
			return p;
			}
		protected final BlastPos getHspEnd1()
			{
			BlastPos p=new BlastPos();
			p.query=Integer.parseInt(this.hsp.getHspQueryTo());
			p.hit=Integer.parseInt(this.hsp.getHspHitTo());
			return p;
			}
		
		protected final boolean isHspForward()
			{
			if(getHspStart1().query>getHspEnd1().query) throw new IllegalStateException();
			return getHspStart1().hit<=getHspEnd1().hit;
			}
		
		public final int getHspQueryStart0()
			{
			return Math.min(getHspStart1().query,getHspEnd1().query)-1;
			}
		public final int getHspQueryEnd0()
			{
			return Math.max(getHspStart1().query,getHspEnd1().query);
			}

		public final int getBedScore()
			{
			int len=featureEnd0()-featureStart0();
			BlastPos left=getHspStart1();
			float match=0f;
			String qS=this.hsp.getHspQseq();
			String hS=this.hsp.getHspHseq();
			for(int i=0;i< qS.length() && i< hS.length();++i)
				{
				if(left.query-1 >= featureStart0() && left.query-1 < featureEnd0())
					{
					if(isSeqLetter(qS.charAt(i)) )
						{
						left.query+=this.queryShift();
						match+=0.5;
						}
					if(isSeqLetter(hS.charAt(i)))
						{
						left.hit+=this.hitShift();
						match+=0.5;
						}
					}
				}
			return (int)((match/len)*1000f);
			}

		public final boolean isFeatureOverlapHsp()
			{
			if(featureEnd0()<=getHspQueryStart0()) return false;
			if(featureStart0()>=getHspQueryEnd0()) return false;
			return true;
			}
		
		public abstract Color getColor();
		
		public final String getBedColor()
			{
			 Color c= getColor();
			 if(c==null) c=Color.WHITE;
			 return ""+c.getRed()+","+c.getGreen()+","+c.getBlue();
			}

		
		
		public final String getChrom()
			{
			return hit.getHitDef();
			}
		protected abstract int featureStart0();
		protected abstract int featureEnd0();

		public abstract String getBedName();
		public abstract int getBedStart();
		public abstract int getBedEnd();
		public abstract char getBedStrand();
		
		public final String toBedString()
			{
			StringBuilder b=new StringBuilder();
			b.append(getChrom()).append('\t');
			b.append(getBedStart()).append('\t');
			b.append(getBedEnd()).append('\t');
			b.append(getBedName()).append('\t');
			b.append(getBedScore()).append('\t');
			b.append(getBedStrand()).append('\t');
			b.append(getBedStart()).append('\t');
			b.append(getBedEnd()).append('\t');
			b.append(getBedColor()).append('\t');
			b.append(1).append('\t');
			b.append(getBedEnd()-getBedStart()).append('\t');
			b.append(getBedStart());
			return b.toString();
			}

		
		}
	
	/**===================================================================================
	 * Interval for Uniprot
	 */

	private class UniprotInterval extends Interval
		{
		FeatureType featureType;
		
		
		public int getEntryStart1()
			{
			LocationType lt=featureType.getLocation();
			if(lt==null) throw new IllegalStateException();
			if(lt.getPosition()!=null)
				{
				return lt.getPosition().getPosition().intValue();
				}
			else if(lt.getBegin()!=null)
				{
				return lt.getBegin().getPosition().intValue();
				}
			else
				{
				throw new IllegalStateException();
				}
			}
		
		public int getEntryEnd1()
			{
			LocationType lt=featureType.getLocation();
			if(lt==null) throw new IllegalStateException();
			if(lt.getPosition()!=null)
				{
				return lt.getPosition().getPosition().intValue();
				}
			else if(lt.getEnd()!=null)
				{
				return lt.getEnd().getPosition().intValue();
				}
			else
				{
				throw new IllegalStateException();
				}
			}
		
		@Override
		public String toString() {
			return "start1:"+getEntryStart1()+" end1:"+getEntryEnd1()+" acn:"+getBedName()+" start0:"+getEntryStart0()+" end0:"+getEntryEnd0()+"\n"+
					" hsp.start:"+getHspStart1()+" hsp.end:"+getHspEnd1()+" hsp.foward:"+isHspForward()+"\nHSP:overlap-gb:"+isFeatureOverlapHsp();
			}
		
		@Override
		public String getBedName()
			{
			return this.featureType.getType();
			}
		
		private int getEntryStart0()
			{
			return getEntryStart1()-1;
			}
		private int getEntryEnd0()
			{
			return getEntryEnd1();
			}
		
		@Override
		protected int featureEnd0()
			{
			return getEntryEnd1();
			}
		@Override
		protected int featureStart0()
			{
			return getEntryStart0();
			}
		
		
		@Override
		public int getBedStart()
			{
			return Math.min(convertQuery(getEntryStart1()).hit,convertQuery(getEntryEnd1()).hit)-1;
			}
		
		@Override
		public int getBedEnd()
			{
			return Math.max(convertQuery(getEntryStart1()).hit,convertQuery(getEntryEnd1()).hit)-1;//yes -1 because +1 of convertQuery
			}

		
		@Override
		public char getBedStrand()
			{
			int str=1;
			if(!isHspForward()) str*=-1;
			return str==1?'+':'-';
			}
		
		@Override
		public Color getColor()
			{
			String fkey="";//TODO
			if(fkey.equals("CDS"))
				{
				return Color.YELLOW;
				}
			if(fkey.equals("gene"))
				{
				return Color.ORANGE;
				}
			if(fkey.equals("gene"))
				{
				return Color.GREEN;
				}
			return Color.WHITE;
			}
		
		}
	
	/**===================================================================================
	 * Interval for Genbank
	 */
	private class GenbankInterval extends Interval
		{
		GBInterval gbInterval;
		GBFeature gbFeature;

		

		
		public int getGBStart1()
			{
			if(gbInterval.getGBIntervalPoint()!=null)
				{
				return Integer.parseInt(gbInterval.getGBIntervalPoint());
				}
			else if(gbInterval.getGBIntervalFrom()!=null)
				{
				return Integer.parseInt(gbInterval.getGBIntervalFrom());
				}
			else
				{
				throw new IllegalStateException();
				}
			}
		
		public int getGBEnd1()
			{
			if(gbInterval.getGBIntervalPoint()!=null)
				{
				return Integer.parseInt(gbInterval.getGBIntervalPoint());
				}
			else if(gbInterval.getGBIntervalTo()!=null)
				{
				return Integer.parseInt(gbInterval.getGBIntervalTo());
				}
			else
				{
				throw new IllegalStateException();
				}
			}
		
		@Override
		public String toString() {
			return "start1:"+getGBStart1()+" end1:"+getGBEnd1()+" forward:"+isGbForward()+" acn:"+getBedName()+" start0:"+getGBStart0()+" end0:"+getGBEnd0()+"\n"+
					" hsp.start:"+getHspStart1()+" hsp.end:"+getHspEnd1()+" hsp.foward:"+isHspForward()+"\nHSP:overlap-gb:"+isFeatureOverlapHsp();
			}
		
		@Override
		public String getBedName()
			{
			String fkey=this.gbFeature.getGBFeatureKey();
			for(GBQualifier q:this.gbFeature.getGBFeatureQuals().getGBQualifier())
				{
				String key=q.getGBQualifierName();
				String v=q.getGBQualifierValue();
				if(v==null || v.isEmpty()) continue;
				if(key.equals("product")) return v;
				else if(key.equals("region_name") && fkey.equals("Region"))
					{
					return  v;
					}
				
				else if(fkey.equals("CDS") &&  key.equals("locus_tag"))
					{
					return  v;
					}
				else if(fkey.equals("CDS") &&  key.equals("gene"))
					{
					return  v;
					}
				else if(fkey.equals("Protein") && key.equals("product"))
					{
					return  v;
					}
				else if(fkey.equals("Site") && key.equals("site_type"))
					{
					return  v;
					}
				else if(fkey.equals("Site") && key.equals("note"))
					{
					return  v;
					}
				}
			
			return fkey+":"+gbInterval.getGBIntervalAccession();
			}
		
		public boolean isGbForward()
			{
			return getGBStart1()<=getGBEnd1();
			}
		
		private int getGBStart0()
			{
			return Math.min(getGBStart1(), getGBEnd1())-1;
			}
		private int getGBEnd0()
			{
			return Math.max(getGBStart1(), getGBEnd1());
			}
		
		
		@Override
		protected int featureEnd0()
			{
			return getGBEnd0();
			}
		@Override
		protected int featureStart0()
			{
			return getGBStart0();
			}
		
	
		
		
		

		@Override
		public int getBedStart()
			{
			return Math.min(convertQuery(getGBStart1()).hit,convertQuery(getGBEnd1()).hit)-1;
			}
		@Override
		public int getBedEnd()
			{
			return Math.max(convertQuery(getGBStart1()).hit,convertQuery(getGBEnd1()).hit)-1;//yes -1 because +1 of convertQuery
			}

		
		
		@Override
		public char getBedStrand()
			{
			int str=1;
			if(!isGbForward()) str*=-1;
			if(!isHspForward()) str*=-1;
			return str==1?'+':'-';
			}
		@Override
		public Color getColor()
			{
			String fkey=this.gbFeature.getGBFeatureKey();
			if(fkey.equals("CDS"))
				{
				return Color.YELLOW;
				}
			if(fkey.equals("gene"))
				{
				return Color.ORANGE;
				}
			if(fkey.equals("gene"))
				{
				return Color.GREEN;
				}
			return Color.WHITE;
			}
		}

	private void printUniprot()
		{
		for(Entry entry:this.uniprotSet.getEntry())
			{	
			BlastOutputIterations iterations=this.blastOutput.getBlastOutputIterations();
			for(Iteration iteration:iterations.getIteration())
				{
				for(FeatureType feature:entry.getFeature())
					{
					for(Hit hit:iteration.getIterationHits().getHit())
						{
						for(Hsp hsp :hit.getHitHsps().getHsp())
							{
							UniprotInterval bi=new UniprotInterval();
							bi.featureType=feature;
							bi.hit=hit;
							bi.hsp=hsp;
							LOG.info("interval "+bi);
							if(!bi.isFeatureOverlapHsp()) continue;
							System.out.println(bi.toBedString());
							}
						}
					
					}
				break;
				}
			}
		
		//System.err.println("OK");
		
		}	
	
	
	
	private void printGB()
		{
		for(GBSeq gbSeq:this.gbSet.getGBSeq())
			{	
			BlastOutputIterations iterations=this.blastOutput.getBlastOutputIterations();
			for(Iteration iteration:iterations.getIteration())
				{
				for(GBFeature feature:gbSeq.getGBSeqFeatureTable().getGBFeature())
					{
					if(feature.getGBFeatureIntervals()==null) continue;
					if(feature.getGBFeatureKey().equals("source")) continue;
					LOG.info("feature key is "+feature.getGBFeatureKey());
					for(GBInterval interval:feature.getGBFeatureIntervals().getGBInterval())
						{
		
						for(Hit hit:iteration.getIterationHits().getHit())
							{
							for(Hsp hsp :hit.getHitHsps().getHsp())
								{
								GenbankInterval bi=new GenbankInterval();
								bi.gbFeature=feature;
								bi.gbInterval=interval;
								bi.hit=hit;
								bi.hsp=hsp;
								LOG.info("interval "+bi);
								if(!bi.isGbForward()) LOG.info("CHECK INTERVAL REVERSE");
								if(!bi.isFeatureOverlapHsp()) continue;
								System.out.println(bi.toBedString());
								}
							
							}
						
						}
					
					}
				break;
				}
			}
		
		//System.err.println("OK");
		
		}
	
	private void print()
		{
		if(this.gbSet!=null) printGB();
		else if(this.uniprotSet!=null) printUniprot();
		}
	
	public void run(String[] args) throws Exception
		{
				int optind=0;
		while(optind<args.length)
			{
			if(args[optind].equals("-h"))
				{
				System.out.println("BlastMapAnnot Pierre Lindenbaum PhD 2013.");
				System.out.println("Options:");
				System.out.println(" -h this screen");
				System.out.println(" [XML GBSet Result| uniprot XML] [XML NCBI BLAST results]");
				return;
				}
			else if(args[optind].equals("-L") && optind+1< args.length)
				{
				LOG.setLevel(Level.parse(args[++optind]));
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
		//read from stdin
		if(optind+1!=args.length && optind+2!=args.length)
			{
			System.err.println("Expected input is genbank.xml blast.xml");
			return;
			}
		LOG.info("reading entry "+args[optind]);
		Document domEntry=this.docBuilder.parse(new File(args[optind]));
		if(domEntry.getDocumentElement().getNodeName().equals("GBSet"))
			{
			LOG.info("parsing as GBSet");
			this.gbSet=this.unmarshaller.unmarshal(domEntry,GBSet.class).getValue();	
			}
		else
			{
			LOG.info("parsing as Uniprot");
			this.uniprotSet=this.unmarshaller.unmarshal(domEntry,Uniprot.class).getValue();	
			}
		Document blastDom;
		if(optind+2==args.length)
			{
			LOG.info("reading "+args[optind+1]);
			blastDom=this.docBuilder.parse(new File(args[optind+1]));
			}
		else
			{
			LOG.info("reading from stdin");
			blastDom=this.docBuilder.parse(System.in);
			}
		this.blastOutput=this.unmarshaller.unmarshal(blastDom,BlastOutput.class).getValue();	
		
		print();
		}
	public static void main(String[] args) throws Exception
		{
		new MapBlastAnnotation().run(args);
		}
	}
