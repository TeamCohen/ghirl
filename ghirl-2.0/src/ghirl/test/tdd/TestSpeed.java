package ghirl.test.tdd;

import edu.cmu.lti.util.run.StopWatch;
import ghirl.graph.BasicWalker;
import ghirl.graph.Closable;
import ghirl.graph.Graph;
import ghirl.graph.GraphId;
import ghirl.graph.GraphLoader;
import ghirl.graph.MutableTextGraph;
import ghirl.graph.PersistantCompactTokyoGraph;
import ghirl.graph.Walker;
import ghirl.test.verify.TestTextGraph;
import ghirl.util.Distribution;

import java.io.File;
import java.util.Iterator;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class TestSpeed {
	protected static final Logger logger= Logger.getLogger(TestTextGraph.class);
	protected static String DBNAME = "Yeast2";
	protected static String TESTROOT = "tests";//graph/17-mar-2010
	protected static String COMPACTSRC = 
		//"tests/TestCompactTextGraph";
		"/usr0/nlao/code_java/ni/run/yeast2/run/Yeast2_compact";

	public static void setUpLogger() {
		Logger.getRootLogger().removeAllAppenders();
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG); 
		Logger.getLogger("ghirl.graph.GraphLoader").setLevel(Level.INFO);
		logger.debug("Set up logger.");
	}
	
	public static Graph makeDB()  {
		ghirl.util.Config.setProperty("ghirl.dbDir", TESTROOT);

		Logger.getRootLogger().setLevel(Level.ERROR);
		try{
			GraphLoader loader = new GraphLoader(new MutableTextGraph("yeast-mar-17-2010",'w'));
			loader.load(new File("nodes_geneLiterature.ghirl"));
			loader.load(new File("nodes_pmAbstract.ghirl"));
			loader.load(new File("edges_geneAssoc.ghirl"));
			loader.load(new File("isa.ghirl"));
			loader.load(new File("go_info.ghirl"));
			loader.getGraph().freeze();
			return loader.getGraph();
		}
		catch(Exception e){
			return null;
		}
	//	TextGraph graph = (TextGraph) CommandLineUtil.makeGraph("loader.bsh");
		//return graph;
		/*
Task loading graph/17-mar-2010/nodes_geneLiterature.ghirl: 304990 lines(s) in 44.19 sec
Task loading graph/17-mar-2010/nodes_pmAbstract.ghirl: 593447 lines(s) in 55.62 sec
Task loading graph/17-mar-2010/isa.ghirl: 113561 lines(s) in 6.12 sec
Task loading graph/17-mar-2010/go_info.ghirl: 126272 lines(s) in 18.73 sec
		 */
	}
	public static Graph loadTC() throws Exception {
		//Logger.getRootLogger().setLevel(Level.INFO);

		
		PersistantCompactTokyoGraph cgraph 
			= new PersistantCompactTokyoGraph(DBNAME,'w');		
		cgraph.load(COMPACTSRC);
		
		
		/* why we need two graphs?
		 * TextGraph graph = (TextGraph) GraphFactory.makeGraph(
				"-bshgraph","tests/compact-persistant-sparse-loader.bsh");		
		((MutableGraph)graph).freeze();
		((TextGraph)graph).close();
		graph = new TextGraph(DBNAME, cgraph);
		*/
		
		return cgraph;
		
	}	
	public static Graph loadSleepyCat() throws Exception {	
		//Logger.getRootLogger().setLevel(Level.ERROR);
		GraphLoader loader = new GraphLoader(new MutableTextGraph("yeast-mar-17-2010",'r'));
		return loader.getGraph();
	}
	
	public static Graph getGraph()  {
		ghirl.util.Config.setProperty("ghirl.dbDir", TESTROOT);
		Logger.getRootLogger().setLevel(Level.ERROR);
		try{		
			return loadTC();
		} catch (Exception ex) {
			System.err.print(ex);
			ex.printStackTrace();
		}
		return null;
	}
	public static int NUMSTEPS = 1000;
	public static int depth = 3;
	
	public static Walker getWalker(Graph g) {
		Walker walker= new BasicWalker();
		walker.setNumSteps(NUMSTEPS); 
		walker.setNumLevels(depth);
		//walker.setSamplingPolicy(false);
		
    walker.setGraph(g);
//    walker.setInitialDistribution( initDist );
    walker.setUniformEdgeWeights(); // TODO: ?? should edge weights really start at uniform?
    walker.reset();
    return walker;
	}
	
	public static int nTest = 0;
	public static int nDownSample=10000;
	//cat graphSize.pct	212167 35
	public static void testSpeed() {
		
		Graph g = getGraph();
		Walker w= getWalker(g);
		StopWatch sw= new StopWatch();
		
		
		System.out.println("*************************** testSpeed");
		int nNode=-1;
		for (Iterator it=g.getNodeIterator(); it.hasNext(); ){
			GraphId id=(GraphId)it.next();
			++nNode;
			if (nNode%nDownSample!=0) continue;
			++nTest;			
			//Distribution m =g.walk1(id);	//System.out.println(m);			
			Distribution rlt=w.search(id);			//System.out.println(rlt);
			
			System.out.println(id+" --> "+rlt.size() +" results");
			//System.out.println(g.getEdgeLabels(id));			
		}
		
		System.out.println("nTest="+nTest);
		sw.printElapsedTime();
		
		((Closable)g).close();
		return;
	}

	public static void main(String[] args) {
		testSpeed();
	}
}
