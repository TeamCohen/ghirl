package ghirl.PRA.util;

import ghirl.PRA.util.Interfaces.IMultiplyOn;
import ghirl.PRA.util.Interfaces.IPlusObjOn;
import ghirl.PRA.util.TMap.MapID;
import ghirl.PRA.util.TSet.SetI;
import ghirl.PRA.util.TVector.VectorI;

import java.io.Serializable;

public class MAP  implements   Serializable,IPlusObjOn, IMultiplyOn{
	private static final long serialVersionUID = 2008042701L; // YYYYMMDD

	public double map=0;
	public double pK=0;	//precision at K=
	public double prec=0;
	public double recall=0;
	public double nRet=0;
	public double nRel=0;
	public double mrr=0;
	public double n=0;
	public void clear(){
		map=0;
		pK=0;
		prec=0;
		recall=0;
		nRet=0;
		nRel=0;		
		mrr=0;
		n=0;
	}
	public void evaluate(MapID mSys, SetI mGold){
		VectorI vi=(VectorI) mSys.toVectorKeySortedByValueDesc();
		evaluate(vi, mGold);
	}
	public void evaluate(VectorI vSys, SetI mGold){
		clear();
		n=1;
		for (int id: vSys){
			++nRet;
			if (mGold.contains(id)){
				++nRel;
				prec= nRel/nRet;
				map += prec;
				if (mrr==0.0)
					mrr= 1.0/nRet;
			}
			else
				prec= nRel/nRet;
			
			if (nRet== mGold.size())
				pK= prec;
			
		}
		map/=mGold.size();

		recall = nRel/mGold.size();
		
		if (vSys.size()<mGold.size())
			pK= nRel/mGold.size();
		return;
	}
	public static String title(){
		return "mrr\tMAP\tp@K";
	}
	public String print(){
		return String.format("%.3f\t%.3f\t%.3f",mrr,map, pK);
	}
	
	public MAP plusObjOn(Object x){
		if (x == null) return this;
		MAP e = (MAP) x;
		map += e.map;
		pK += e.pK;
		prec += e.prec;
		recall += e.recall;
		nRet += e.nRet;
		nRel += e.nRel;
		mrr+= e.mrr;
		n+= e.n;
		return this;
	}
	public MAP multiplyOn(Double x){
		map*=x;
		pK*=x;
		prec*=x;
		recall*=x;
		nRet*=x;
		nRel*=x;
		mrr*=x;
		n*=x;
		return this;			
	}
	public MAP meanOn(){
		if (n!=0.0)
			multiplyOn(1.0/n);
		return this;
	}
}
