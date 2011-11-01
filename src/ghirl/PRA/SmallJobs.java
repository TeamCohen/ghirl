package ghirl.PRA;

import edu.cmu.lti.algorithm.container.MapID;
import edu.cmu.lti.util.file.FFile;
import edu.cmu.lti.util.run.Param;
import edu.cmu.lti.util.system.FSystem;
import edu.cmu.pra.model.ModelPathRank;
import edu.cmu.pra.model.PRAModel;
import edu.cmu.pra.model.Query;
import ghirl.graph.CompactGraph;
import ghirl.graph.ICompact;
import ghirl.graph.PersistantCompactTokyoGraph;
import ghirl.graph.SparseCompactGraph;
import ghirl.util.Config;

public class SmallJobs {

	public static String TESTROOT = "tests";

	public static ICompact getTCGraph() throws Exception {
		PersistantCompactTokyoGraph graph;
		FSystem.printMemoryTime();

		ghirl.util.Config.setProperty(Config.DBDIR, "..");
		if (FFile.exist("../" + graph_folder_ + "_compactTokyo")) {//&& 1==2){
			graph = new PersistantCompactTokyoGraph(graph_folder_, 'r');
		} else {
			graph = new PersistantCompactTokyoGraph(graph_folder_, 'w');
			graph.load("../" + graph_folder_);//tests
		}
		FSystem.printMemoryTime();

		return graph;
	}

	public static String graph_folder_ = "../Yeast2.txtLink";//"Yeast2.cite";

	public static SparseCompactGraph getSCGraph() throws Exception {
		if (Param.ms.containsKey("graph_folder"))
			graph_folder_ = Param.ms.get("graph_folder");
		
		FSystem.printMemoryTime();

		SparseCompactGraph graph = new SparseCompactGraph();
		graph.load( graph_folder_);
		graph.loadMMGraphIdx();

		FSystem.printMemoryTime();

		return graph;

	}

	public static void augamentGraph(CompactGraph graph) {
		// 
		/*int idxA=graph.getNodeIdx("author", "Woolford_JL");
		SetI m=graph.getNodeIdx("paper"
			, "3282992 2673535 3058476 2179050".split(" "));
		graph.AddExtraLinks("Read", idxA, m);*/
		graph.AddExtraLinks("CRead", "author", "Woolford_JL", "paper",
				"3282992 2673535 3058476 2179050".split(" "));
	}

	public static String model_ = "model";
	public static void testPrediction() throws Exception {

		SparseCompactGraph graph = getSCGraph();
		//augamentGraph(graph);
		
		if (Param.ms.containsKey("model"))
			model_ = Param.ms.get("model");
		
		PRAModel model = new ModelPathRank(graph);
		model.loadModel(model_);

//		Query query = model.parseQuery("year$2010	year$2009	author$Woolford_JL	");
		Query query = model.parseQuery("YEAR$2010	YEAR$2009	AUTHOR$Woolford_JL	");

		MapID result = model.predict(query);

		System.out.println(model.walker_.printDistribution(result, 10, "=", "\n"));

		return;
	}

	public static void main(String args[]) {
		Param.overwriteFrom("./conf");
		String task = args[0];
		try {
			if (task.equals("testPrediction")) testPrediction();

		} catch (Exception ex) {
			System.err.print(ex);
			ex.printStackTrace();
		}
	}
}
