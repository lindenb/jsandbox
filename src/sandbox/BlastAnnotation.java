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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import sandbox.ncbi.blast.BlastOutput;
import sandbox.ncbi.blast.BlastOutputIterations;
import sandbox.ncbi.blast.Hit;
import sandbox.ncbi.blast.Hsp;
import sandbox.ncbi.blast.Iteration;
import sandbox.ncbi.gbc.INSDFeature;
import sandbox.ncbi.gbc.INSDInterval;
import sandbox.ncbi.gbc.INSDQualifier;
import sandbox.ncbi.gbc.INSDSeq;
import sandbox.ncbi.gbc.INSDSeqFeatureTable;
import sandbox.ncbi.gbc.INSDSet;

public class BlastAnnotation
	{
	private int margin=9;
	private int fastaLineLength=50;
	private DocumentBuilder docBuilder;
	private Unmarshaller gbcUnmarshaller;
	
	private abstract class AbstractHspPrinter
		{
		int seqStart;
		int seqEnd;
		int sign;
		int stringStart;
		int stringEnd;
		
		protected Hsp hsp;
		private List<INSDFeature> features;
		
		public abstract String getSequence();
		public abstract int getSeqFrom();
		public abstract int getSeqTo();
		
		protected AbstractHspPrinter( Hsp hsp, List<INSDFeature> features)
			{
			this.hsp=hsp;
			this.features=features;
			
			this.sign= (getSeqFrom()< getSeqTo()?1:-1);
			this.seqStart=getSeqFrom();
			this.seqEnd=this.seqStart;
			this.stringStart=0;
			this.stringEnd=0;
			}
		
		boolean next()
			{
			if(this.stringEnd>=getSequence().length()) return false;
			this.seqStart=this.seqEnd;
			this.stringStart=this.stringEnd;
			for(int i=0;i< fastaLineLength &&
					this.stringStart+i< getSequence().length();
					++i)
				{
				if(Character.isLetter(getSequence().charAt(this.stringStart+i)))
					{
					this.seqEnd+=this.sign;
					}
				this.stringEnd++;
				}
			return true;
			}
		
		
		void print()
			{
			for(INSDFeature feature:this.features)
				{
				for(INSDInterval interval:feature.getINSDFeatureIntervals().getINSDInterval())
					{
					int intervalFrom=0;
					int intervalTo=0;
					if( interval.getINSDIntervalFrom()!=null &&
						interval.getINSDIntervalTo()!=null &&
						!(
						  (intervalFrom=Integer.parseInt(interval.getINSDIntervalFrom()))>=this.seqEnd ||
						  (intervalTo=Integer.parseInt(interval.getINSDIntervalTo()))<this.seqStart
						))
						{
						intervalTo++;
						}
					else if(interval.getINSDIntervalPoint()!=null &&
						(intervalFrom=Integer.parseInt(interval.getINSDIntervalPoint()))>=this.seqStart &&
						intervalFrom< this.seqEnd
						)
						{
						intervalTo=intervalFrom+1;
						}
					else
						{
						continue;
						}
					if(intervalFrom> intervalTo)
						{
						continue;
						}
					System.out.printf("      %"+margin+"s ","");
					intervalFrom=Math.max(this.seqStart,intervalFrom);
					intervalTo=Math.min(this.seqEnd,intervalTo);
					int genome=this.seqStart;
					for(	int i=0;i< fastaLineLength &&
							this.stringStart+i< this.stringEnd;
							++i)
						{
						boolean isSeq=Character.isLetter(getSequence().charAt(this.stringStart+i));
						boolean isGap=hsp.getHspMidline().charAt(this.stringStart+i)==' ';
						if(intervalFrom<=genome && genome< intervalTo)
							{
							System.out.print(isSeq?(isGap?":":"#"):"-");
							}
						else
							{
							System.out.print(" ");
							}
						if(Character.isLetter(getSequence().charAt(this.stringStart+i)))
							{
							genome+=this.sign;
							}
						}
					System.out.print(" ");
					System.out.print(feature.getINSDFeatureKey());
					System.out.print(" ");
					System.out.print(feature.getINSDFeatureLocation());
					
					for(INSDQualifier qual:feature.getINSDFeatureQuals().getINSDQualifier())
						{
						System.out.print(" ");
						System.out.print(qual.getINSDQualifierName());
						System.out.print(":");
						System.out.print(qual.getINSDQualifierValue());
						}
					
					System.out.println();
					}
				}
			}
		}
	
	private	class QPrinter
		extends AbstractHspPrinter
		{
		QPrinter( Hsp hsp, List<INSDFeature> features)
			{
			super(hsp,features);
			}
		@Override
		public int getSeqFrom()
			{
			return Integer.parseInt(this.hsp.getHspQueryFrom());
			}
		@Override
		public int getSeqTo()
			{
			return Integer.parseInt(this.hsp.getHspQueryTo());
			}
		public String getSequence()
			{
			return this.hsp.getHspQseq();
			}
		}
	
	private	class HPrinter
	extends AbstractHspPrinter
		{
		HPrinter( Hsp hsp, List<INSDFeature> features)
			{
			super(hsp,features);
			}
		
		@Override
		public int getSeqFrom()
			{
			return Integer.parseInt(this.hsp.getHspHitFrom());
			}
		
		@Override
		public int getSeqTo()
			{
			return Integer.parseInt(this.hsp.getHspHitTo());
			}
		
		public String getSequence()
			{
			return this.hsp.getHspHseq();
			}
		}
	
	
	private BlastAnnotation() throws Exception
		{
					
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
		JAXBContext jc = JAXBContext.newInstance("sandbox.ncbi.gbc");
		this.gbcUnmarshaller=jc.createUnmarshaller();
		}
	
	
	
	private List<INSDFeature> fetchAnnotations(String name)
		throws Exception
		{
		int pipe;
		if(name!=null &&
		name.startsWith("gi|") &&
		(pipe=name.indexOf('|',3))!=-1)
			{
			String gi=name.substring(3,pipe);
			String uri="http://www.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=protein&id="+gi+"&rettype=gbc&retmode=xml";
			INSDSet set=INSDSet.class.cast(this.gbcUnmarshaller.unmarshal(this.docBuilder.parse(uri)));
			if(!set.getINSDSeq().isEmpty())
				{
				INSDSeq seq= set.getINSDSeq().get(0);
				INSDSeqFeatureTable table=seq.getINSDSeqFeatureTable();
				return table.getINSDFeature();
				}
			}
		return new ArrayList<INSDFeature>();
		}
	
	private void parseBlast(BlastOutput blast)  throws Exception
		{
		System.out.println("QUERY: "+blast.getBlastOutputQueryDef());
		System.out.println("       ID:"+blast.getBlastOutputQueryID()+" Len:"+blast.getBlastOutputQueryLen());
		List<INSDFeature> qFeatures= fetchAnnotations(blast.getBlastOutputQueryID());
		BlastOutputIterations iterations=blast.getBlastOutputIterations();
		for(Iteration iteration:iterations.getIteration())
			{
			for(Hit hit:iteration.getIterationHits().getHit())
				{
				System.out.println(">"+hit.getHitDef());
				System.out.println(" "+hit.getHitAccession());
				System.out.println(" id:"+hit.getHitId()+" len:"+hit.getHitLen());
				List<INSDFeature> hFeatures= fetchAnnotations(hit.getHitId());
				for(Hsp hsp :hit.getHitHsps().getHsp())
					{
					System.out.println();
					System.out.println("   e-value:"+hsp.getHspEvalue()+" gap:"+hsp.getHspGaps()+" bitScore:"+hsp.getHspBitScore());
					System.out.println();
					QPrinter qPrinter=new QPrinter(hsp,qFeatures);
					HPrinter hPrinter=new HPrinter(hsp,hFeatures);
					
					
					while(qPrinter.next() && hPrinter.next())
						{
						qPrinter.print();
						System.out.printf("QUERY %0"+margin+"d ",qPrinter.seqStart);
						System.out.print(hsp.getHspQseq().substring(qPrinter.stringStart,qPrinter.stringEnd));
						System.out.printf(" %0"+margin+"d",qPrinter.seqEnd-(qPrinter.sign));
						System.out.println();
						System.out.printf("      %"+margin+"s ","");
						System.out.print(hsp.getHspMidline().substring(qPrinter.stringStart,qPrinter.stringEnd));
						System.out.println();
						System.out.printf("HIT   %0"+margin+"d ",hPrinter.seqStart);
						System.out.print(hsp.getHspHseq().substring(hPrinter.stringStart,hPrinter.stringEnd));
						System.out.printf(" %0"+margin+"d",hPrinter.seqEnd-(hPrinter.sign));
						System.out.println();
						hPrinter.print();
						System.out.println();
						}
					}
				
				}
			}
		
		
		//System.err.println("OK");
		}
	public static void main(String[] args)
		{
		try
			{
			BlastAnnotation app=new BlastAnnotation();
			int optind=0;
			while(optind<args.length)
				{
				if(args[optind].equals("-h"))
					{
					return;
					}
				else if(args[optind].equals("-L"))
					{
					app.fastaLineLength=Integer.parseInt(args[++optind]);
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
			JAXBContext jc = JAXBContext.newInstance("sandbox.ncbi.blast");
			Unmarshaller unmarshaller=jc.createUnmarshaller();
			if(optind==args.length)
				{
				app.parseBlast(BlastOutput.class.cast(unmarshaller.unmarshal(app.docBuilder.parse(System.in))));
				}
			else
				{
				while(optind< args.length)
					{
					String inputName=args[optind++];
					app.parseBlast(BlastOutput.class.cast(unmarshaller.unmarshal(app.docBuilder.parse(new File(inputName)))));
					}
				}
			}catch(Throwable err)
				{
				err.printStackTrace();
				}
		}

	}
