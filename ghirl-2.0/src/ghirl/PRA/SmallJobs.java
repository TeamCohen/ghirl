package ghirl.PRA;

import ghirl.PRA.util.FFile;
import ghirl.PRA.util.FSystem;
import ghirl.PRA.util.StopWatch;
import ghirl.graph.ICompact;
import ghirl.graph.PersistantCompactTokyoGraph;
import ghirl.graph.SparseCompactGraph;
import ghirl.util.CompactImmutableArrayDistribution;
import ghirl.util.Config;
import ghirl.util.Distribution;

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

	
	public static void testPCRW()throws Exception{		

		FSystem.printMemoryTime();
		ICompact graph=getCGraph();	//getCGraph();
		FSystem.printMemoryTime();
		
		PRAModel net=new ModelPathRank("conf",graph);//,"YA-Py.WJ"./ );
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
	public static void testPrediction()throws Exception{
		ICompact graph=getCGraph();	//getCGraph();
		
		PRAModel net=new ModelPathRank("conf",graph);//,"YA-Py.WJ"./ );
		net.loadPathWeights("model");//fnWeight
		
		
		//Walker walker= new PathWalker(graph, net);
		
		Query q=net.parseQuery("2010,2009,Woolford_JL,");
		net.predict(q);
    
		Distribution d=new 
			CompactImmutableArrayDistribution(q.mResult, graph);
		
		System.out.println("result="+d);

	}

	public static void main(String args[]) {
		try{		
			//System.out.print( FFile.getFilePath("/usb2/nlao/software/nies/nies/PRA/"));
			testPrediction();
			//testPCRW();

		} catch (Exception ex) {
			System.err.print(ex);
			ex.printStackTrace();
		}
	}
}
