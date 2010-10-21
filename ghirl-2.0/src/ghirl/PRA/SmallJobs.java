package ghirl.PRA;

import ghirl.PRA.schema.ETGraph;
import ghirl.PRA.schema.ETGraphPathRank;
import ghirl.PRA.schema.Query;
import ghirl.PRA.util.FFile;
import ghirl.PRA.util.StopWatch;
import ghirl.PRA.util.TVector.VectorMapID;
import ghirl.graph.PersistantCompactTokyoGraph;
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
	
	
	public static void testPCRW()throws Exception{		


		ghirl.util.Config.setProperty(Config.DBDIR, "..");
		PersistantCompactTokyoGraph graph=null;
		if (FFile.exist("../"+DB+"_compactTokyo") ){//&& 1==2){
			graph = new PersistantCompactTokyoGraph(DB,'r');
		}
		else{
			graph = new PersistantCompactTokyoGraph(DB,'w');
			graph.load("../"+DB);//tests
		}
		
		ETGraph net=new ETGraphPathRank("conf",graph);//,"YA-Py.WJ");
		net.loadPathWeights("model");//fnWeight

		
		//Walker walker= new PathWalker(graph, net);
    StopWatch sw= new StopWatch();
		for (Query q : net.loadQuery("scenarios.WJL")){
			net.predict(q);
		}
		sw.printElapsedTime();

		
		//   |entity|=212167 |link|=5561850 
		
		//wrong PF
		//Time Elapsed= 96.5s		nParticl=0.0001	
		//Time Elapsed= 94.1s		nParticl=0.001		#elements=1,076,830
		//Time Elapsed= 31.8s	nParticl=0.003	#elements=812,793
		//Time Elapsed= 9.1s	nParticl=0.01 	elements=342,093
		
		/*  ni code		
		//nPar=0.001	#elements=3399	Time Elapsed= 0.4s
									#elements=1609	Time Elapsed= 0.1s		
		
		//exact				#elements=69495	Time Elapsed= 7.4s
									#elements=9560	Time Elapsed= 2.5s
		*/
		
		//exact
		//#elements=1,418,062	Time Elapsed= 97.6s	
		//#elements=1418062		Time Elapsed= 102.4s
		/*  ghirl code
		#nPar=0.001		#elements=37640	Time Elapsed= 2.0s
		#nPar=0.003		#elements=13837	Time Elapsed= 1.2s
		#nPar=0.01		#elements=4844	Time Elapsed= 0.7s
		*/
		return;
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
