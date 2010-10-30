package ghirl.PRA.schema;

import ghirl.graph.ICompact;


public class ETGraphPathRank extends ETGraph{
	private static final long serialVersionUID = 2008042701L; // YYYYMMDD
	public ETGraphPathRank(String fnConf, ICompact g){//,String fnSchema){
		super(fnConf,g);//, fnSchema);
	}
	@Override public void initWeights(){
		vsFeature.clear();

		if (p.bBias){
			this.bias=0;
			vsFeature.add("bias");
		}
		
		vsFeature.addAll(vsPath);
		
		//super.initWeights(0.0);
		
		vwPath.reset(vsPath.size(),0.0);
		getParameters();
		return;
	}
	
	@Override public void setParameters(double[] x){
		//vwFeature.setAll(x);
		int k=0;
		if (p.bBias){
			bias = x[k]*p.scBias; ++k;
		}
		
		for (int i=0; i< vwPath.size(); ++i){
			vwPath.set(i,x[k]);++k;
		}
		
		if (p.bEntBias)
		for (int i=0; i< vwPath.size(); ++i){
			vwIR.set(i,x[k]);++k;
		}
		getParameters();
	}
	@Override public double[] getParameters(){//VectorD 
		
		int k=0;
		if (p.bBias){
			vwFeature.setE(k,bias); ++k;
		}
		
		for (int i=0; i< vwPath.size(); ++i){
			vwFeature.setE(k, vwPath.get(i));++k;
		}
		
		if (p.bEntBias)
		for (int i=0; i< vwPath.size(); ++i){
			vwFeature.setE(k, vwIR.get(i));++k;
		}
		//return vwF;
		return vwFeature.toDoubleArray();
	}
	/*@Override public VectorD getG0(){
		VectorD vG0=(VectorD) eva.g0Path.clone();
		if (p.bBias)
			vG0.insertElementAt(eva.g0Bias*p.scBias,0);
		return vG0;
	}*/

}
