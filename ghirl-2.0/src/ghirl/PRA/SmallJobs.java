package ghirl.PRA;

import ghirl.PRA.schema.ETGraph;
import ghirl.PRA.schema.ETGraphPathRank;
import ghirl.PRA.schema.Query;
import ghirl.PRA.util.FFile;
import ghirl.PRA.util.FSystem;
import ghirl.PRA.util.StopWatch;
import ghirl.graph.ICompact;
import ghirl.graph.PersistantCompactTokyoGraph;
import ghirl.graph.SparseCompactGraph;
import ghirl.util.Config;

public class SmallJobs {
	
/*
 * 		net.loadTaskFile(fnSchema);
		net.schema.onLoadSchema(net);			
		net.createPathTrees(graph);
		net.initWeights();
		net.loadPathWeights(fnWeight);
 */
	/*		walker.setNumSteps(1000); 
	walker.setNumLevels(3);
	//walker.setSamplingPolicy(false);
  walker.setGraph(g);
  walker.setUniformEdgeWeights(); 
  walker.reset();*/
	
	public static String testFile="scenarios.Woolford_JL.predict";
	public static String TESTROOT="tests";
	
	public static String DB=
		"Yeast2";
		//"testCompactGraph";
	

	
	public static void testPCRW()throws Exception{		

		FSystem.printMemoryTime();
		ICompact graph=getTCGraph();	//getCGraph();
		FSystem.printMemoryTime();
		
		ETGraph net=new ETGraphPathRank("conf",graph);//,"YA-Py.WJ");
		net.loadPathWeights("model");//fnWeight
		
		FFile.mkdirs("result"+net.p.code);
		
		//Walker walker= new PathWalker(graph, net);
    StopWatch sw= new StopWatch();
		for (Query q : net.loadQuery("scenarios.WJL")){
			net.predict(q);
			//System.out.println("nPart="+q.mResult.size());
		}
		sw.printElapsedTime();

		
		return;
	}
	
	public static ICompact getTCGraph()throws Exception{		
		PersistantCompactTokyoGraph graph;
		ghirl.util.Config.setProperty(Config.DBDIR, "..");
		if (FFile.exist("../"+DB+"_compactTokyo") ){//&& 1==2){
			graph = new PersistantCompactTokyoGraph(DB,'r');
		}
		else{
			graph = new PersistantCompactTokyoGraph(DB,'w');
			graph.load("../"+DB);//tests
		}
		return graph;
	}
	public static ICompact getCGraph()throws Exception{		
		SparseCompactGraph graph = new SparseCompactGraph();
		graph.load("../"+DB);
		graph.loadMMGraphIdx();
		return graph;
		

	}

	public static void main(String args[]) {
		try{		
			testPCRW();

		} catch (Exception ex) {
			System.err.print(ex);
			ex.printStackTrace();
		}
	}
}