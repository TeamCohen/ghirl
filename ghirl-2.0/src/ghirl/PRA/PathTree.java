package ghirl.PRA;

import ghirl.PRA.PRAModel.ESparsityMode;
import ghirl.PRA.Schema.EntType;
import ghirl.PRA.Schema.Relation;
import ghirl.PRA.util.CTag;
import ghirl.PRA.util.FString;
import ghirl.PRA.util.FSystem;
import ghirl.PRA.util.TVector;
import ghirl.PRA.util.Interfaces.IGetStringByString;
import ghirl.PRA.util.TMap.MapID;
import ghirl.PRA.util.TMap.MapII;
import ghirl.PRA.util.TMap.MapSI;
import ghirl.PRA.util.TMap.TMapIX;
import ghirl.PRA.util.TMap.TMapMapIIX;
import ghirl.PRA.util.TSet.SetI;
import ghirl.PRA.util.TVector.VectorI;
import ghirl.PRA.util.TVector.VectorMapID;
import ghirl.PRA.util.TVector.VectorS;
import ghirl.graph.GraphId;
import ghirl.graph.ICompact;
import ghirl.util.Distribution;

import java.io.BufferedWriter;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import sun.swing.SwingUtilities2.Section;


/**
 * a PathTree is a set of type paths starting from some entity type 
 * @author nlao
 *
 */
public class PathTree implements Serializable {
	private static final long serialVersionUID = 2008042701L; // YYYYMMDD

	//public Graph graph;

	private PRAModel net;
	// should we separate RW parameters from net?
	// it should not be inside PathTree for sure
	
	/*private Schema schema;
	public int maxStep=0;
	public int minStep=0;
	public ESparsityMode sMode;
	//following reversed links to produce pack propagation
*/
	public boolean bReversedPath=false;
	
	public PathTree(	PRAModel net,EntType etSrc){
		this.etSrc= etSrc;
		this.etTgt= net.etTrg;
		this.net= net;
		//this(net.schema, etSrc, net.etTrg	, net.p.maxStep, net.p.sMode);
	}
	/*
//PersistantCompactTokyoGraph Graph graph,
	public PathTree(	Schema schema	,EntType etSrc, EntType etTgt
			, int maxStep,ESparsityMode sMode){
		//,boolean bBlockBackward
		//this.bBlockBackward=bBlockBackward;
		this.schema = schema;
		//this.graph= graph;
		this.etSrc= etSrc;
		this.etTgt= etTgt;
		this.maxStep= maxStep;
		this.sMode=sMode;
	}*/
	public EntType etSrc;
	public EntType etTgt;

	public VectorMapID walk(MapID secM){//, int time){ ICompact graph,
		clear();		
		
		root.secM.plusOn(secM);
		walkRecur( root);//, time);graph,
		
		return vSecM;
	}


	
	public PathNode root=null;
	
	public TVector<PathNode> vNode
		=new TVector<PathNode>(PathNode.class);	//nodes of target type
	public VectorS vsPath= new VectorS();
	public MapSI miPathID=null;// new MapSI();
	
	
	public TVector<PathNode> getPathNodes(VectorS vs){
		TVector<PathNode> vN	=new TVector<PathNode>(PathNode.class);
		for (String name:vs)
			vN.add(vNode.get(miPathID.get(name)));
		return vN;
	}
	public TVector<PathNode> getAllPathNodes(){
		TVector<PathNode> vN	=new TVector<PathNode>(PathNode.class);
		getAllPathNodesRecur(vN,root);
		return vN;
	}
	private void getAllPathNodesRecur(TVector<PathNode> vN, PathNode n){
		vN.add(n);
		for (PathNode c: n.mRelChild.values())
			getAllPathNodesRecur(vN, c);
	}
	
	public void createPathTree(){//int maxStep, int minStep){
		//this.maxStep=maxStep;
		//this.minStep=minStep;
		vNode.clear();
		mmvTgtStepPathNames.clear();
		root =createPathTreeRecur(null,  null);// , etSrc.nameS);//new MapII());
		vsPath=vNode.getVS(CTag.nameS);
		miPathID= (MapSI) vsPath.toMapValueId();
	}

	public SetI miForbiddenET= new SetI();
	public TMapMapIIX<VectorS> mmvTgtStepPathNames//=null;
		=new TMapMapIIX<VectorS>(VectorS.class);
	
	private PathNode createPathTreeRecur(PathNode p, Relation r0){	
		//Relation r0=p.r0;
		PathNode n;
		EntType et;
		if (p==null){//(nStep==0){//if (r0!=null)
			et= etSrc;
			n=new PathNode(this, p, etSrc, etSrc.nameS , etSrc.nameS, 0);
		}
		else{
			if (this.bReversedPath){
				et= r0.etFr;
				n=new PathNode(this, p, et
						, et.nameS+"("+r0.nameS+")"+p.nameS 
						, et.name+"("+r0.name+")"+p.nameS
						,p.nStep+1);
			}
			else{
				et= r0.etTo;
				n=p.extend(r0);
				//new PathNode(et, p.nameS+"("+r0.nameS+")"+et.nameS	,p.nStep+1);
			}
			n.r0=r0;
			n.miRelCount.putAll(p.miRelCount);
			n.miRelCount.plusOn(r0.id,1);
		}
		if (et.id>=0)
			n.sec= null;//graph.vSect.get(et.id);
		
		mmvTgtStepPathNames.getC(et.id).getC(n.nStep).add(n.nameS);

		if (miForbiddenET.contains(et.id))
			return n;		

		//if (etTgt!=null){
		if (et.equals(etTgt) || etTgt==null){
			n.bReachTarget=true;
			if (n.nStep>=net.p.minStep)
				vNode.add(n);			
		}		
		
		if (n.nStep==net.p.maxStep)
			return n;		
		
		//--nStep;
		if (this.bReversedPath){
			for (Relation r: n.et.vRelationFrom){
				if (miForbiddenET.contains(r.etFr.id))
					continue;
				if (!r.canFollow(n.r0))
					continue;
				PathNode c =createPathTreeRecur(n, r);
				if (c.bReachTarget || etTgt==null)
					n.mRelChild.put(r.id,c);
			}			
		}
		else{
			for (Relation r: n.et.vRelationTo){
				if (miForbiddenET.contains(r.etTo.id))
					continue;
				if (!r.canFollow(n.r0))
					continue;
				
				PathNode c =createPathTreeRecur(n, r);
				if (c.bReachTarget || etTgt==null)
					n.mRelChild.put(r.id,c);
			}
		}
		
		if (n.mRelChild.size()>0)
			n.bReachTarget=true;
		return n;
	}
	
	
	public void clear(){
		//root.clearPathTreeRecur();
		initPathTreeRecur(root);
		vSecM.clear();
	}
	private void initPathTreeRecur(PathNode n){
		//n.secM.clear();
		//n.secM= new SectionM(graph.vSect.get(n.et.id));
		n.secM= new MapID();
		for ( PathNode c: n.mRelChild.values())
			initPathTreeRecur(c);
	}
	
	public VectorMapID vSecM=new VectorMapID();
	//public TVector<SectionM> vSecM
		//=new TVector<SectionM>(SectionM.class);
	
	private void walkRecur(PathNode n){//, int time){ICompact graph,
		if (n.nStep>=net.p.minStep)
		if (n.et.equals(etTgt) || etTgt==null)
			vSecM.add(n.secM);		
		
		for (Map.Entry<Integer, PathNode>e: n.mRelChild.entrySet()){
			Relation r= net.schema.vRel.get(e.getKey());			
			PathNode c =e.getValue();
			step( n.secM,c.secM, 1.0, r.id);//, time);graph,
			
			walkRecur(c);//, time);graph,
		}
		return;
	}
	
	
	private Random rand=new Random();
	/**
	 * produce one distribution from another  
	 * by taking one step random walk	 *///ICompact graph,
	public void step(MapID mSrc, MapID mTrg, double p0, int iRel){
		//p0 *=LearnerPRA.p1.RWDampening;
		for (Map.Entry<Integer,Double> e: mSrc.entrySet()){
			Distribution d=net.g.walk1(e.getKey(), iRel+1);
			//MapID d=((ICompact)graph).walk2(e.getKey(), iRel+1);
			double mass=e.getValue()*p0;
			double p=mass/d.size();		
			
			if (net.p.sMode.equals(ESparsityMode.Q)){
						
				if (p<=net.p.minParticle){
					int nPart=(int)(mass/net.p.minParticle);
					for (int i=0; i<nPart; ++i){
						int idx =net.g.getNodeIdx((GraphId)d.sample(rand));
						mTrg.plusOn(idx, net.p.minParticle);
					}
					continue;
				}			
			}
			for (Iterator i=d.iterator(); i.hasNext(); ) {
				GraphId id = (GraphId)i.next();
				double w = d.getLastWeight();
				mTrg.plusOn(net.g.getNodeIdx(id), w*p);
			}

		}
		return ;
	}
	
	public VectorI viEnt= new VectorI();

	
	
	public void trimPathTree(double vW[]){
		vSecM.clear();
		trimPathTreeRecur(root,vW);
	}
	
	private boolean trimPathTreeRecur(PathNode n,double vW[]){
		n.bReachTarget=false;
		if (n.et.equals(etTgt)){
			n.bReachTarget= vW[vSecM.size()]!=0;
			vSecM.add(null);	
		}
		for (Map.Entry<Integer, PathNode>e: n.mRelChild.entrySet())
			if (trimPathTreeRecur(e.getValue(),vW))
				n.bReachTarget=true;
		return n.bReachTarget;
	}
	public void removeNoneTargetNodes(){
		//for (PathNode n: this.vNode)
		//System.out.println(this);
		vNode.clear();
		removeNoneTargetNodesRecur(root);
	}
	private boolean removeNoneTargetNodesRecur(PathNode n){
		
		n.bReachTarget=false;
		if (n.et.equals(etTgt) && n.nStep >= net.p.minStep){
			vNode.add(n);	
			n.bReachTarget=true;
		}
		SetI mGood=new SetI();
		for (Map.Entry<Integer, PathNode>e: n.mRelChild.entrySet()){
			if (!removeNoneTargetNodesRecur(e.getValue()))
				continue;
			n.bReachTarget=true;
			mGood.add(e.getKey());
		}
		n.mRelChild= (TMapIX<PathNode> ) n.mRelChild.sub(mGood);
		return n.bReachTarget;
	}
	public String toString(){
		return root.toStringRecur(0);
	}
	/**
	 * 
	 * @param path   relation>relation> ....
	 * 	e.g. 0,_Year>Author
	 * @return
	 */
	public PathNode getNodeByPath(String path){
		PathNode p=root;
		for (String rel:path.split(">")){
			Relation r= net.schema.getRelation(rel);
			if (r==null)
				FSystem.die("cannot find relation="+rel);
			p=p.mRelChild.get(r.id);				
			if (p==null)
				FSystem.die("error matching relation="+rel+" in path="+path);
		}
		return p;
	}
	
	public void dumpData( BufferedWriter bw){
		
		/*for (PathNode n: getAllPathNodes()){
			FFile.writeln(bw, n.nameS);//n.name);
			//if (graph.p.dbgTree==1)		
			FFile.writeln(bw,graph.vEntName.sub(n.secM.keySet()).toString());
			//FFile.writeln(bw, n.secM.replaceKey(graph.vEntName).toStringE("%.1e"));
		}*/
	}
	

	public static class PathNode implements  IGetStringByString{
		public EntType et;
		//public TVector<PathNode> vChildren
			//= new TVector<PathNode>(PathNode.class);
		public TMapIX<PathNode> mRelChild= new TMapIX<PathNode>(PathNode.class);
		public Section sec;
		public MapID secM= new MapID();
		public boolean bReachTarget=false;
		
		public String nameS;
		public String name;
		public int nStep=-1;
		public MapII miRelCount = new MapII();
		public Relation r0= null;	//which relation generates this node?
		public PathNode parent=null;
		public PathTree t;
		public PathNode(PathTree t, PathNode parent, EntType et, String nameS, String name, int nStep){//Section sec,
			this.t=t;
			this.parent= parent;
			this.et = et;
			this.nStep= nStep;
			//secM= new SectionM(sec);
			this.nameS=nameS;
			this.name=name;
			//if (name.indexOf("(Ca)")>=0)			System.out.print("error");
		}		
		public PathNode extend(Relation r){
			if (!r.etFr.equals(this.et)){
				System.err.println("error extending node: ");
				return null;
			}
			//if (r.name.equals("Ca"))			System.out.print("error");
			
			return new PathNode(t, this, r.etTo
				, nameS+"("+r.nameS+")"+r.etTo.nameS 
				, name+"("+r.name+")"+r.etTo.nameS
				, nStep+1);
		}
		
		public String getString(String key) {
			if (key.equals(CTag.nameS))return nameS;		
			if (key.equals(CTag.name))return name;//getName();		
			return null;
		}
		//public String getName(){		}
		
		public String toString(){
			return nameS;
		}
		public String toStringRecur(int nTab){
			StringBuffer sb= new StringBuffer();
			Integer size= secM!=null?secM.size():null;
			sb.append(String.format("%s%s(%d)\n"
					,FString.repeat("\t",nTab), et.nameS, size));
			
			for (Map.Entry<Integer, PathNode>e: mRelChild.entrySet()){
				//Relation r= graph.net.vRelation.get(e.getKey());			
				PathNode c =e.getValue();
				sb.append(c.toStringRecur(nTab+1));
			}
			return sb.toString();
		}

		public void _clearPathTreeRecur(){
			//secM.clear();
			/*secM= new SectionM(secM.b);
			for (Map.Entry<Integer, PathNode>e: mRelChild.entrySet())
				e.getValue()._clearPathTreeRecur();
				*/
		}
		
		
		/**determinstic rules should be expressed as constraints	
			need a seperate random walk channel, 
			exact RW is too slow, inexact RW will miss relevant entities
		 */
		public SetI getCoverageRecur(SetI mSeeds){//,int time){
			if (this.parent==null){ //this is root 
				return mSeeds;
			}
			else{
				SetI mS=this.parent.getCoverageRecur(mSeeds);//,time);
				SetI mCov=new SetI();
				for (Integer iE: mS){
					SetI m=t.net.g.walk2(iE, r0.id+1);
					mCov.addAll(m);
				}
				return mCov;
			}
		}

	}
}