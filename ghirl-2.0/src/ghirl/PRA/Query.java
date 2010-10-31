package ghirl.PRA;

import ghirl.PRA.Schema.EntType;
import ghirl.PRA.util.MAP;
import ghirl.PRA.util.TMap.MapID;
import ghirl.PRA.util.TSet.SetI;
import ghirl.PRA.util.TVector.VectorD;
import ghirl.PRA.util.TVector.VectorMapID;
import ghirl.graph.ICompact;

import java.io.Serializable;

public class Query implements Serializable {
	private static final long serialVersionUID = 2008042701L; 
	//public int id=-1;
	public String name;
	public int time=-1;
	
	//public ERGraphM dist0 ;		// seed entities
	public VectorMapID vmSeeds= new VectorMapID();
	
	//public EntType etTarget;	//the type of entity to be retrieved
	
	public SetI mRel;	//the set of relevant entities
	//public ERGraph g;
	
	public SetI mMask=null;
	public MapID mResult=null;
	public MapID mPosi=null;
	public MapID mNega=null;
	public MAP map=null;
	public SetI mN=null;
	public String toString(){
		return vmSeeds.toString();
	}
	//public VectorI viNega=null;
	
	public String print(ICompact g){

		StringBuffer sb= new StringBuffer();
		sb.append("Eq=");
		for (MapID m:vmSeeds)
			if (m.size()>0)
				sb.append("["+g.getNodeName(m.keySet())+"]");
		
		sb.append("\nmRel=["+g.getNodeName(mRel)+"]");
		
		if (mN!=null)
			sb.append("\nmN=["+g.getNodeName(mN)+"]");
		else
			sb.append("\nmN=null");

		
		if (mMask!=null)
			sb.append("\nmask.size()="+mMask.size());

		if (mResult!=null)
			sb.append("\nmResult=["+g.getNodeName(mResult.keySet())+"]");
			
		//if (viNega!=null)
			//sb.append("\nmNega=["+g.vEntName.sub(viNega).join(" ")+"]");

		return sb.toString();
	}
	//ERGraph g, 
	
	public VectorMapID A=null;	//random walk result (data)
	public Query(int time, String name
			, VectorMapID dist0,EntType etTarget
			, SetI mRel){
		//this.g=g;
		this.name= name;
		this.vmSeeds= dist0;//new ERGraphM(graph);
		//this.etTarget= etTarget;
		this.mRel= mRel;
		this.time=time;
	}
	public MAP eval(MapID mSys){
		MAP eva = new MAP();
		eva.evaluate(mSys, mRel);
		return eva;
	}
	public void eval(VectorD vdWeights){
		//TODO: we ignore bias here, but should not for EntityRank and EntBias
		//maybe we should extend vwPath to include their parameters
		mResult=A.weightedSum(vdWeights);
		map=eval(mResult);
		
		mPosi=mResult.sub(mRel);
		mNega=mResult.sub(mN);
	}
}


