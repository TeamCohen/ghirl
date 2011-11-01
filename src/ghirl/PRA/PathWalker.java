package ghirl.PRA;

import edu.cmu.pra.model.PRAModel;
import ghirl.graph.GraphId;
import ghirl.graph.ICompact;
import ghirl.graph.Walker;
import ghirl.util.Distribution;

import java.util.HashSet;
import java.util.Set;


/**
 * weighted combination of distribution from multiple paths
 * into a single distribution
 **/

public class PathWalker extends Walker {//implements CommandLineProcessor.Configurable{

	
	
	
/*
	public class MyCLP extends BasicCommandLineProcessor
	{
		public void steps(String s) { setNumSteps(StringUtil.atoi(s)); }
		public void levels(String s) { setNumLevels(StringUtil.atoi(s)); }
        public void probRemain(String s) { setProbRemainAtNode(StringUtil.atof(s)); }
		public void nodeStopList(String s) {
	    String[] ids = s.split("\\s*,\\s*");
	    for (int i=0; i<ids.length; i++) addToNodeStopList( GraphId.fromString(ids[i]) );
		}
	}
	public CommandLineProcessor getCLP() { return new MyCLP(); }
	*/
	
	protected double probRemainAtNode = 0;
	protected Set nodeStopList = new HashSet();
	protected int numLevels = 2;
	public void setProbRemainAtNode(double p) { probRemainAtNode=p; }
	public double getProbRemainAtNode() { return probRemainAtNode; }
	public void setNumLevels(int n) { this.numLevels = n; }
	public int getNumLevels() { return numLevels; }
	public void addToNodeStopList(GraphId id) { nodeStopList.add(id); }

	protected Set edgeStopList = new HashSet();
  public Set getEdgeStopList() {return edgeStopList; }
	@Override public void addToEdgeStopList(String edgeName) { 
		//edgeStopList.add(edgeName); 
	}
	protected int steps = 10000;	
	public int getNumSteps() { return steps; }
	public void setNumSteps(int n) { steps=n; }


	public void reset()	{
		//nodeSample = new TreeDistribution();
	}
	


	
	
	public Distribution getInitialDistribution() { 
		return null;//startDist; }
	}
	// node version
  public void setSamplingPolicy(boolean sample) {
    if (!sample) 
      System.out.println(
      	"An exhaustive walk is not supported by BasicWalker. Sampling...");
  }
  public void setStayWalkVersion(boolean stay) {
      if (stay) System.out.println("The stayWalkVersion is not supported by BasicWalker. Applying random walks with restart.");
  }

  public void setRandomEdgeWeights() { System.out.println("Random weights not supported by BasicWalker");}
  public void setUniformEdgeWeights() {;}

	public String toString(){
		return "[PathWalker]";
	}







	PRAModel net=null;
	//public PathWalker(){	}
	public PathWalker(ICompact g, PRAModel net){
		this.graph=g;
		this.net=net;	

		//seems useless
		setNumLevels(net.p.max_step);		
		setNumSteps(net.walker_.p.num_walkers); 
		//setSamplingPolicy();
	}
	

	//protected Distribution startDist;

	public void setInitialDistribution(Distribution dist){
		//this.startDist = dist;		reset();
		return;
		
	}
	public void walk(){

	}
	//private Distribution nodeSample;
	//private Random rand;
  public Distribution getNodeSample(){
 	 return null;
 	}

}
