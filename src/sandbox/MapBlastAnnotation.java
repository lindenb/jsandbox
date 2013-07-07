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
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
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
import sandbox.ncbi.gb.GBSeq;
import sandbox.ncbi.gb.GBSet;

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
	private BlastOutput blastOutput=null;
	private Bed currBed;

	
	

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
		JAXBContext jc = JAXBContext.newInstance("sandbox.ncbi.blast:sandbox.ncbi.gb");
		this.unmarshaller=jc.createUnmarshaller();
		}
	
	
	private static class BlastPos
		{
		int query;
		int hit;
		}
	
	private static class BedSegment
		{
		String chrom;
		int start;
		int end;
		String name;
		}
	
	private static class Interval
		{
		GBInterval gbInterval;
		
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
		
		
		public String getAcn()
			{
			return gbInterval.getGBIntervalAccession();
			}
		
		public boolean isForward()
			{
			return getGBStart1()<=getGBEnd1();
			}
		
		public int getStart0()
			{
			return Math.min(getGBStart1(), getGBEnd1())-1;
			}
		public int getEnd0()
			{
			return Math.max(getGBStart1(), getGBEnd1());
			}
		}

	private class Bed
		{
		GBFeature gbFeature;
		Hit hit;
		Hsp hsp;
		List<Interval> intervals=new ArrayList<Interval>();
		
		public String getChrom()
			{
			return hit.getHitDef();
			}
		
		private BlastPos convertQuery(int qPos)
			{
			BlastPos left=getBlastLeft();
			
			String qS=this.hsp.getHspQseq();
			String hS=this.hsp.getHspHseq();
			for(int i=0;i< qS.length() && i< hS.length();++i)
				{
				if(left.query>=qPos) break;
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
		
		private int queryShift()
			{
			return 1;
			}
		
		private int hitShift()
			{
			return (blastOutput.getBlastOutputProgram().equals("tblastn")?3:1)*(isBlastForward()?1:-1);
			}
		
		public boolean isBlastForward()
			{
			return Integer.parseInt(this.hsp.getHspHitFrom())< Integer.parseInt(this.hsp.getHspHitTo());
			}

		private boolean isSeqLetter(char c)
			{
			if(c=='-' || Character.isWhitespace(c)) return false;
			if(Character.isLetter(c)) return true;
			throw new IllegalArgumentException("letter: "+c);
			}
		
		private BlastPos getBlastLeft()
			{
			BlastPos p=new BlastPos();
			p.query=Integer.parseInt(this.hsp.getHspQueryFrom());
			p.hit=Integer.parseInt(this.hsp.getHspHitFrom());
			return p;
			}

		}
	
	
	
	
	
	private void print()
		{
		if(this.gbSet.getGBSeq().isEmpty()) return;
		final List<GBFeature> hFeatures=  this.gbSet.getGBSeq().get(0).getGBSeqFeatureTable().getGBFeature();

		
		BlastOutputIterations iterations=this.blastOutput.getBlastOutputIterations();
		for(Iteration iteration:iterations.getIteration())
			{
			for(GBFeature feature:hFeatures)
				{
				this.currBed=new Bed();
				this.currBed.gbFeature=feature;
				for(GBInterval interval:feature.getGBFeatureIntervals().getGBInterval())
					{
					Interval bi=new Interval();
					bi.gbInterval=interval;
					
					this.currBed.intervals.add(bi);
					}
				if(currBed.intervals.isEmpty()) continue;
				for(Hit hit:iteration.getIterationHits().getHit())
					{
					this.currBed.hit=hit;				
					for(Hsp hsp :hit.getHitHsps().getHsp())
						{
						this.currBed.hsp=hsp;
						
						
						
						}
					
					}
				}
			break;
			}
		
		
		//System.err.println("OK");
		
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
				System.out.println(" [XML GBSet Result] [XML NCBI BLAST results]");
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
		LOG.info("reading gbset"+args[optind]);
		this.gbSet=this.unmarshaller.unmarshal(this.docBuilder.parse(new File(args[optind])),GBSet.class).getValue();	
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
