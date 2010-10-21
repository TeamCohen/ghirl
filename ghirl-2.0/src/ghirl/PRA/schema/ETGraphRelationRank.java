package ghirl.PRA.schema;

import ghirl.PRA.schema.PathTree.PathNode;
import ghirl.PRA.schema.Schema.EntType;
import ghirl.PRA.schema.Schema.Relation;
import ghirl.PRA.util.TMap.MapID;
import ghirl.PRA.util.TVector.VectorD;
import ghirl.PRA.util.TVector.VectorMapID;
import ghirl.graph.PersistantCompactTokyoGraph;

import java.util.Map;

/**
 * same as PathRank except that the model is parameterized by Relation 
 * @author nlao
 *
 */
public class ETGraphRelationRank  extends ETGraph{
	private static final long serialVersionUID = 2008042701L; // YYYYMMDD
	public ETGraphRelationRank(String fnConf, PersistantCompactTokyoGraph g){//,String fnSchema){
		super(fnConf,g);//, fnSchema);
	}
	//public double[] wPath=null; //weights

	/*public MapID runQuery(Query q, ERGraph graph){//, int nStep){
		VectorMapID A=forest.getPathFeatures(q);
		MapID mSys=A.weightedSum(vwPath);
		return mSys;
	}*/
	public VectorD vwRel=null; //weights
	@Override public String getWeightCount(){
		return  vwRel.countNonZero()+"\t"+vwRel.size()+"\t"+vwRel.normL1();		
	}
	
	private double[] _wRel2wPath(double[] wRel){
		double[] wPath= new double[vsPath.size()];
		//for (PathNode n: forest.vPathNode){
		for (int i=0; i< vPathNode.size(); ++i){
			PathNode n=vPathNode.get(i);
			double d=1;
			for (Map.Entry<Integer,Integer>e: n.miRelCount.entrySet()){
				int iRel = e.getKey();
				int c=e.getValue();
				for (int j=0; j< c; ++j)
					d*= wRel[iRel];
			}				
			wPath[i]=d;
		}
		return wPath;
	}
	protected double[] normalizedWRel(double[] wRel){
		double[] wRel_= new double[wRel.length];//normalized
		for (EntType et: schema.vEntType){
			et.Z= 0;
			for (Relation r: et.vRelationTo)
				et.Z+= wRel[r.id];
		}			
		for (Relation r: schema.vRel) 
			wRel_[r.id]= wRel[r.id]/r.etFr.Z;
		return wRel_;
	}
	protected VectorD normalizedWRel(VectorD wRel){
		VectorD wRel_= new VectorD(wRel.size());//normalized
		for (EntType et: schema.vEntType){
			et.Z= 0;
			for (Relation r: et.vRelationTo)
				et.Z+= wRel.get(r.id);
		}			
		for (Relation r: schema.vRel) 
			wRel_.set(r.id, wRel.get(r.id)/r.etFr.Z);
		return wRel_;
	}
	private double[] wRel2wPath(double[] wRel){
		double[] wPath= new double[vsPath.size()];
		double[] wRel_=null;
		//if (super.p.bNormalizeWeights)
		//wRel_=normalizedWRel(wRel);		else 
			wRel_=wRel;		
		
		for (int i=0; i< vPathNode.size(); ++i){
			PathNode n=vPathNode.get(i);
			double d=1;
			for (Map.Entry<Integer,Integer>e: n.miRelCount.entrySet()){
				int iRel = e.getKey();
				int c=e.getValue();
				for (int j=0; j< c; ++j)
					d*= wRel_[iRel];
			}				
			wPath[i]=d;
		}
		return wPath;
	}
	private VectorD wRel2wPath(VectorD wRel){
		VectorD wPath= new VectorD(vsPath.size());
		VectorD wRel_=null;
		//if (p.bNormalizeWeights)	wRel_=normalizedWRel(wRel);	else 
			wRel_=wRel;		
		
		for (int i=0; i< vPathNode.size(); ++i){
			PathNode n=vPathNode.get(i);
			double d=1;
			for (Map.Entry<Integer,Integer>e: n.miRelCount.entrySet()){
				int iRel = e.getKey();
				int c=e.getValue();
				for (int j=0; j< c; ++j)
					d*= wRel_.get(iRel);
			}				
			wPath.set(i,d);
		}
		return wPath;
	}
	
	private double[] gPath2gRel(double[] gPath,double[] wRel,double[] wPath){
		double[] gRel= new double[vwRel.size()];
		MapID m=B.weightedSum(gPath);
		//MapID m=B.weightedSum( FArrayD.multiply(gPath,wPath));
		for (Map.Entry<Integer,Double>e: m.entrySet()){
			int iRel = e.getKey();
			Double c=e.getValue();
			if (wRel[iRel]!=0.0)
				gRel[iRel]= c/wRel[iRel];
			else
				gRel[iRel]= c; // a terrible approximation, hope it works
			
			if (Double.isNaN(gRel[iRel]))
				System.err.println("NaN");
		}
		return gRel;
	}
	
	private VectorD gPath2gRel(VectorD gPath,VectorD wRel,VectorD wPath){
		VectorD gRel= new VectorD(vwRel.size());
		MapID m=B.weightedSum(gPath);
		//MapID m=B.weightedSum( FArrayD.multiply(gPath,wPath));
		for (Map.Entry<Integer,Double>e: m.entrySet()){
			int iRel = e.getKey();
			Double c=e.getValue();
			if (wRel.get(iRel)!=0.0)
				gRel.set(iRel,c/wRel.get(iRel));
			else
				gRel.set(iRel,c); // a terrible approximation, hope it works
			
			//if (Double.isNaN(gRel.get(iRel)))			System.err.println("NaN");
		}
		return gRel;
	}
	

	VectorMapID B=null;// new VectorMapID();

	@Override public void initWeights(){
		
		vsFeature.clear();
		if (p.bBias)
			vsFeature.add("bias");

		vwRel = new VectorD(schema.vRel.size(),1.0);//p.dampening);
		vwPath= wRel2wPath(vwRel);
		
		vsFeature.addAll( schema.vRel.getVS());		

		
		B= getPowerMatrix();
		getParameters();
		return;
	}
	
	public void setParameters(double[] x){
		vwFeature.setAll(x);
		int k=0;
		if (p.bBias){
			bias = x[k]; ++k;
		}
		
		for (int i=0; i< vwRel.size(); ++i){
			vwRel.set(i,x[k]);++k;
		}
		vwPath=wRel2wPath(vwRel);
		
		if (p.bEntBias)
		for (int i=0; i< vwPath.size(); ++i){
			vwIR.set(i,x[k]);++k;
		}

		
	}
	@Override public double[] getParameters(){//VectorD 
		
		int k=0;
		if (p.bBias){
			vwFeature.setE(0,bias); ++k;
		}
		
		for (int i=0; i< vwRel.size(); ++i){
			vwFeature.setE(k, vwRel.get(i));++k;
		}
		
		if (p.bEntBias)
		for (int i=0; i< vwPath.size(); ++i){
			vwFeature.setE(k, vwIR.get(i));++k;
		}
		//return vwF;
		return vwFeature.toDoubleArray();
	}
	/*@Override public VectorD getG0(){
		VectorD vG0=gPath2gRel(eva.g0Path, vwRel, vwPath);
		if (p.bBias)
			vG0.insertElementAt(eva.g0Bias,0);
		return vG0;
	}*/


}


