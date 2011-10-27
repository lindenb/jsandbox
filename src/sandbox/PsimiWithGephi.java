/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	Feb-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * Reference:
 *
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Wiki
 *
 * Motivation:
 * 	Paint a PSI-MI graph using the GEPHI library
 * Compilation:
 *       TODO
 */
package sandbox;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.Attributes;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.EdgeData;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeData;
import org.gephi.graph.api.UndirectedGraph;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.preview.api.ColorizerFactory;
import org.gephi.preview.api.EdgeColorizer;
import org.gephi.preview.api.NodeChildColorizer;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.ranking.api.ColorTransformer;
import org.gephi.ranking.api.NodeRanking;
import org.gephi.ranking.api.RankingController;
import org.gephi.ranking.api.SizeTransformer;
import org.gephi.statistics.plugin.GraphDistance;
import org.openide.util.Lookup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * PsimiWithGephi
 */
public class PsimiWithGephi
	{
	@SuppressWarnings("unused")
	static private final Logger LOG=Logger.getLogger("gephi.psi-mi");
	private final static String PSI_NS="net:sf:psidev:mi";
	private File outFile=null;

	private void run(InputStream xmlIn)
		throws Exception
		{
		DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
		f.setCoalescing(true);
		f.setNamespaceAware(true);
		f.setValidating(false);
		f.setExpandEntityReferences(true);
		f.setIgnoringComments(true);
		f.setIgnoringElementContentWhitespace(true);
		DocumentBuilder docBuilder= f.newDocumentBuilder();
		Document dom=docBuilder.parse(xmlIn);

		XPathFactory xpathFactory=XPathFactory.newInstance();
		XPath xpath=xpathFactory.newXPath();
		xpath.setNamespaceContext(new NamespaceContext()
			{
			@Override
			public Iterator<?> getPrefixes(String namespaceURI)
				{
				return Arrays.asList("p").iterator();
				}

			@Override
			public String getPrefix(String namespaceURI)
				{
				if(namespaceURI.equals(PSI_NS)) return "p";
				System.err.println("?"+namespaceURI);
				return null;
				}

			@Override
			public String getNamespaceURI(String prefix)
				{
				if(prefix.equals("p")) return PSI_NS;
				System.err.println("?"+prefix);
				return null;
				}
			});


		 //Init a project - and therefore a workspace
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        @SuppressWarnings("unused")
		Workspace workspace = pc.getCurrentWorkspace();
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
        AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
        RankingController rankingController = Lookup.getDefault().lookup(RankingController.class);

        UndirectedGraph graph = graphModel.getUndirectedGraph();


        AttributeColumn xtraNodeAtt = attributeModel.getNodeTable().addColumn("ACN", AttributeType.STRING);
        AttributeColumn xtraEdgeAtt = attributeModel.getEdgeTable().addColumn("STRING.SCORE", AttributeType.STRING);



        //get the XML nodes
        NodeList nodes=(NodeList)xpath.evaluate("/p:entrySet/p:entry/p:interactorList/p:interactor", dom,XPathConstants.NODESET);
		for(int i=0;i< nodes.getLength();++i)
			{
			Element e=Element.class.cast(nodes.item(i));
			Node gephiNode = graphModel.factory().newNode(e.getAttribute("id"));
			NodeData nodeData=gephiNode.getNodeData();
			String label=xpath.evaluate("p:names/p:shortLabel", e);
			if(!label.isEmpty()) nodeData.setLabel(label);
			Attributes atts=nodeData.getAttributes();
			atts.setValue(xtraNodeAtt.getIndex(), label);
			graph.addNode(gephiNode);


			}
		nodes=(NodeList)xpath.evaluate("/p:entrySet/p:entry/p:interactionList/p:interaction", dom,XPathConstants.NODESET);
		for(int i=0;i< nodes.getLength();++i)
			{
			Element e=Element.class.cast(nodes.item(i));
			String ref1= xpath.evaluate("p:participantList/p:participant[1]/p:interactorRef",e);
			String ref2= xpath.evaluate("p:participantList/p:participant[2]/p:interactorRef",e);
			if(ref1.equals(ref2) || ref1.isEmpty() || ref2.isEmpty()) continue;
			Node gephiNode1=graph.getNode(ref1);
			Node gephiNode2=graph.getNode(ref2);
			float score=1f;
			try
				{
				score=Float.parseFloat(xpath.evaluate("p:confidenceList/p:confidence[1]/p:value",e));
				}catch(Exception err){score=1f;}

			Edge edge = graphModel.factory().newEdge(gephiNode1,gephiNode2,score,false);
			EdgeData edgeData=edge.getEdgeData();
			edgeData.setColor(1f, 0.6f, 0.2f);
			Attributes atts=edgeData.getAttributes();
			atts.setValue(xtraEdgeAtt.getIndex(), "Hello");
			graph.addEdge(edge);
			}
	    dom=null;
	    nodes=null;

	    YifanHuLayout layout = new YifanHuLayout(null, new StepDisplacement(1f));
        layout.setGraphModel(graphModel);
        layout.resetPropertiesValues();
        layout.setOptimalDistance(200f);

        for (int i = 0; i < 150 && layout.canAlgo(); i++)
        		{
        		layout.goAlgo();
        		}
        //Get Centrality
        GraphDistance distance = new GraphDistance();
        distance.setDirected(true);
        distance.execute(graphModel, attributeModel);

        //Rank color by Degree
        NodeRanking<?> degreeRanking = rankingController.getRankingModel().getDegreeRanking();

        ColorTransformer<?> colorTransformer = rankingController.getObjectColorTransformer(degreeRanking);
        colorTransformer.setColors(new Color[]{Color.GRAY, Color.DARK_GRAY});
        rankingController.transform(colorTransformer);

        //Rank size by centrality
        AttributeColumn centralityColumn = attributeModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
        NodeRanking<?> centralityRanking = rankingController.getRankingModel().getNodeAttributeRanking(centralityColumn);
        SizeTransformer<?> sizeTransformer = rankingController.getObjectSizeTransformer(centralityRanking);
        sizeTransformer.setMinSize(20);//min node size
        sizeTransformer.setMaxSize(100);//max node size
        rankingController.transform(sizeTransformer);


        //Preview
        PreviewModel preview = Lookup.getDefault().lookup(PreviewController.class).getModel();
        ColorizerFactory colorizerFactory = Lookup.getDefault().lookup(ColorizerFactory.class);
        preview.getBackgroundColor();
        preview.getNodeSupervisor().setShowNodeLabels(Boolean.TRUE);
        preview.getNodeSupervisor().setShowNodes(Boolean.TRUE);
        preview.getNodeSupervisor().setShowNodeLabelBorders(Boolean.TRUE);
        preview.getNodeSupervisor().setNodeLabelColorizer((NodeChildColorizer) colorizerFactory.createCustomColorMode(Color.GREEN));
        preview.getUndirectedEdgeSupervisor().setColorizer((EdgeColorizer) colorizerFactory.createCustomColorMode(Color.PINK));

        preview.getNodeSupervisor().setBaseNodeLabelFont(preview.getNodeSupervisor().getBaseNodeLabelFont().deriveFont(8));



        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        ec.exportFile(outFile);

		}



	public static void main(String[] args)
		{
		PsimiWithGephi app=null;
		try
			{
			app=new PsimiWithGephi();
			int optind=0;
			while(optind< args.length)
				{
				if(args[optind].equals("-h") ||
				   args[optind].equals("-help") ||
				   args[optind].equals("--help"))
					{
					System.err.println("Options:");
					System.err.println(" -h help; This screen.");
					System.err.println(" -t <int> restrict to a ncbi-taxon-id ");
					System.err.println(" -o 'file.svg/file.pdf'.");
					return;
					}
				else if(args[optind].equals("-o"))
					{
					app.outFile=new File(args[++optind]);
					}
				else if(args[optind].equals("--"))
					{
					optind++;
					break;
					}
				else if(args[optind].startsWith("-"))
					{
					System.err.println("Unknown option "+args[optind]);
					return;
					}
				else
					{
					break;
					}
				++optind;
				}

			if(app.outFile==null)
				{
				System.err.println("-o fileout missing");
				return;
				}

			if(args.length==optind)
				{
				app.run(System.in);
				}
			else if(optind+1==args.length)
				{
				InputStream in;
				String uri=args[optind++];
				if(	uri.startsWith("http://") ||
					uri.startsWith("https://") ||
					uri.startsWith("ftp://"))
					{
					in=new URL(uri).openStream();
					}
				else
					{
					in=new FileInputStream(uri);
					}
				if(uri.endsWith(".gz")) in=new GZIPInputStream(in);
				app.run(in);
				in.close();
				}
			else
				{
				System.err.println("Illegal number of arguments.");
				}
			}
		catch(Throwable err)
			{
			err.printStackTrace();
			}
		}
	}
