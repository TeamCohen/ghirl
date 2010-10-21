package ghirl.PRA.schema;

import ghirl.PRA.ICompact;
import ghirl.PRA.schema.PathTree.PathNode;
import ghirl.PRA.schema.Schema.EntType;
import ghirl.PRA.util.CTag;
import ghirl.PRA.util.Counter;
import ghirl.PRA.util.FFile;
import ghirl.PRA.util.FString;
import ghirl.PRA.util.FSystem;
import ghirl.PRA.util.FTable;
import ghirl.PRA.util.TVector;
import ghirl.PRA.util.TMap.MapID;
import ghirl.PRA.util.TMap.MapIIb;
import ghirl.PRA.util.TMap.MapSI;
import ghirl.PRA.util.TMap.MapSS;
import ghirl.PRA.util.TSet.SetI;
import ghirl.PRA.util.TVector.VectorD;
import ghirl.PRA.util.TVector.VectorI;
import ghirl.PRA.util.TVector.VectorMapID;
import ghirl.PRA.util.TVector.VectorS;
import ghirl.graph.Graph;
import ghirl.graph.PersistantCompactTokyoGraph;

import java.io.BufferedReader;
import java.io.BufferedWriter;


/** This is a PRA model
 *  It does not know about ERGraph 
 * */
public abstract class ETGraph {


	//public ERGraph graph=null;
	//ETGraph schema=null;
	public Schema schema=null;
	
	public PersistantCompactTokyoGraph g;
	MapSS mFeatureComments;
	public Param p;
	public ETGraph(String fnConf,PersistantCompactTokyoGraph g){
		this.g=g;
		//,String fnSchema){//ERGraph graph){
		//this.graph= graph;
		//this.schema= graph.schema;
		//../
		mFeatureComments= MapSS.fromFile("mFeatureComments.txt");
		
		Param.overwriteFrom(fnConf);
		p=new Param();		
		
		schema=new Schema();
		schema.lenShortName=p.lenShortName;
		
		
		loadTaskFile(p.taskFile);
		schema.onLoadSchema(this);			/** now schema is ready*/
		//System.out.println(this);		
		
		createPathTrees();
		
		loadIgnoredPath();
		initWeights();
		//loadWeights(fnModel);
	}
	
	//public Section secTrg=null;
	public EntType etTrg=null;	
	//which entity types are used as query
	public TVector<EntType> vSeedET= new TVector<EntType>(EntType.class);
	
	
	public VectorI vSeedCol=new VectorI();
	public int iColTrg=-1;
	//public VectorI viSeedET=null;


	protected void parseFormular(String formular){
		//0,year|1,author-->2,paper|0,_Year

		String vs[]=formular.split("-->");

		/*load source*/
		String vsQ[]=vs[0].split("\\||,");
		vSeedET.clear(); vSeedCol.clear();
		for (int i=0;i<vsQ.length; i+=2){
			int iCol = Integer.parseInt(vsQ[i]);
			EntType ET = schema.getEntType(vsQ[i+1]);
			vSeedCol.add(iCol);
			vSeedET.add(ET);
			
			if (!ET.equals(vET.get(iCol))){
				System.err.print( ET +" not matched with "+vET.get(iCol));
				System.exit(-1);
			}
		}

		/*load target*/
		//String vsT[];
		vsTarget=vs[1].split("\\||,");
		iColTrg=Integer.parseInt(vsTarget[0]);
		etTrg= schema.getEntType(vsTarget[1]);
		if (!vET.get(iColTrg).equals(etTrg))			
			System.exit(-1);
		
		//for (int i=2;i<vsT.length;i+=2)
			//addConstraintPath(Integer.parseInt(vsT[i]), vsT[i+1]);
				
	}
	public String vsTarget[]=null;	
	public String vTitle[]=null;
	public TVector<EntType> vET=null;
	

	
	public  boolean loadTaskFile(String taskFile) {		
		System.out.print("loading task="+taskFile);//p.taskFile);

		BufferedReader brTask = FFile.bufferedReaderOrDie(taskFile);
		
		/* line #1 */	vTitle=FFile.readLine(brTask).split(",");		
		/* line #2 */	String formular = FFile.readLine(brTask);
		/* line #3 */	p.dbName = FFile.readLine(brTask);
		/* line #4 */	p.dbFiles = FFile.readLine(brTask);
	
		/*load type graph*/
		String line = null;	
		while ((line = FFile.readLine(brTask)) != null) {
			if (line.startsWith("#"))	break;
      schema.parseSchemaLine(line);
		}		
		FFile.close(brTask);
		vET	=schema.getEntType(vTitle);
		
		parseFormular(formular);


		//System.out.println(schema.toString());
		System.out.println("... done");

		return true;
	}
	public TVector<PathTree> vPTree= new TVector<PathTree>(PathTree.class);

	public VectorD vwPath=new VectorD();// weights of selected paths

	public VectorS vsPath= new VectorS();// selected paths
	
	public MapSI msiPath= new MapSI();// path --> id

	public TVector<PathNode> vPathNode
		=new TVector<PathNode>(PathNode.class);// selected nodes	
	
	public static final MapID mEmplty= new MapID();

	public VectorMapID getPowerMatrix(){
		VectorMapID B= new VectorMapID();
		for (PathNode n: vPathNode)
			B.add(n.miRelCount.toDouble());
		return B;
	}

	
	public TVector<PathNode> vConstraint
		=new TVector<PathNode>(PathNode.class);	
	
	public VectorI vitConstraint = new VectorI();// tree id
	
	protected void addConstraintPaths(){
		//int id, String path
		vConstraint.clear();
		for (int i=2;i<vsTarget.length;i+=2){

			int id=Integer.parseInt(vsTarget[i]);
			vitConstraint.add(id);
			PathTree tree = vPTree.get(id);
			PathNode n=tree.getNodeByPath(vsTarget[i+1]);
			vConstraint.add(n);
		}
	}
	
	//TODO: do cache here
	protected void generateMask(Query q){
		q.mMask=new SetI();		
		for (int i=0; i< vConstraint.size();++i){
			int id= vitConstraint.get(i);
			PathNode n = vConstraint.get(i);
			SetI mSeed = new SetI(q.vmSeeds.get(i).keySet());
			SetI m= n.getCoverageRecur(mSeed);//, q.time);
			
			if (i==0)
				q.mMask.addAll(m);
			else 
				q.mMask=q.mMask.andSet(m);				
		}			
		
		if (q.mMask.size()==0) 
			System.err.println("mMask.size()==0");
		
		if (!q.mMask.containsAll(q.mRel))
			FSystem.die("hard constraint should not eliminate relevant entities");
		
		return;
	}
	

	protected SetI mIgPath=null;
	protected void loadIgnoredPath(){
		mIgPath=new SetI();
		if (p.sIgnoredPathes==null)
			return;// null;
		for (String path:p.sIgnoredPathes.split(",") )
			mIgPath.add(msiPath.get(path));
		//return m;
	}
	
	PathTree treeE0; 	
	private static Counter cnWalk=new Counter(100,'h');
	public VectorMapID getTreeFeatures(Query q){//ICompact g,
		cnWalk.step();
		VectorMapID A= new VectorMapID();
		
		for (int i=0; i< q.vmSeeds.size();++i){			
		//for (PathTree t : vPTree){
			PathTree t = this.vPTree.get(i);
			MapID m= q.vmSeeds.get(i);
			t.walk(m);//, q.time);
			A.addAll(t.vSecM);
			//if (bwDataTree!=null)			dumpTree(q, t);
		}		
		
		if (vConstraint.size()>0){
			generateMask(q);
			for (int i=0; i<A.size();++i)
				//A.get(i).subOn(q.mMask);
				A.set(i, A.get(i).sub(q.mMask));
		}		
		//if (p.bDumpData)			dumpData(q);
		q.A=A;			
		if (mIgPath!=null)
			for (int i: mIgPath)
				q.A.set(i, mEmplty);
		return A;
	}
	public void dumpData( Query q,String fn){
		System.out.println("dumping Q="+q.name);
		FFile.mkdirsTrimFile(fn);
		BufferedWriter bw=FFile.bufferedWriter(fn);		
		FFile.writeln(bw, q.print(g));
		
		for (PathTree tree: this.vPTree)
			tree.dumpData(bw);
		FFile.close(bw);
	}
	public void predict(Query q){//,SetI mIgPath){ICompact g,
		if (q.A==null){
			getTreeFeatures(q);
			/*if (mIgPath!=null)
				for (int i: mIgPath)
					q.A.set(i, net.mEmplty);
			System.out.println(q.A);*/
		}
		
		q.mResult=q.A.weightedSum(vwPath);
		
		if (true)//p.bDumpData)
			dumpData( q,"result/data/"+q.name);
		
		if (1==0){// basic version
			//MapSD rlt= q.mResult.subPositive().replaceKey(graph.vEntName);
			//rlt.saveSorted("result/"+q.name);
		}
		else{// printing important features
			//q.mResult=q.mResult.subLargerThan(1e-3);
			
			BufferedWriter bw = FFile.bufferedWriter("result/"+q.name);
			for (Integer id:	q.mResult.toVectorKeySortedByValueDesc()){
				double score= q.mResult.get(id);				
				//if (score<1e-3) break;
				
				MapID m=q.A.getRowM(id).multiply(vwFeature);
				
				FFile.write(bw, String.format(
					"%.1e\t%s\t%s\n", score, g.getNodeName(id)
					, m.joinE("="," ","%.1e")));
			}		
			FFile.close(bw);
		}
	}

	public boolean _loadSchema(String fnSchema) {
		BufferedReader br = FFile.bufferedReader(fnSchema);
		String line = null;
		while ((line = FFile.readLine(br)) != null) {
			if (line.startsWith("#"))
				break;
			schema.parseSchemaLine(line);
		}
		// initSectEx();
		System.out.println(schema.report());
		return true;
	}


	public VectorD vwIR=new VectorD();
	public VectorD vwHF=new VectorD();

	public double bias=0;
	public VectorD vwFeature=new VectorD();
	public VectorS vsFeature=new VectorS(); //=null;// feature names

	public String getWeightCount(){
		return vwFeature.countNonZero()+"\t"+vwFeature.size()+"\t"+vwFeature.normL1();		
	}
	/*public void initWeights(double d){
		int dim = vsF.size();
		vwPath.reset(dim,d);
	}*/
	

	public void loadPathWeights(String fn){	
		
	/*	SetS mIgPath=null;
		if (p.sIgnoredPathes!=null)
			mIgPath=new SetS(p.sIgnoredPathes.split(","));
		*/
		VectorS vLine=FTable.loadLines(fn,true);
		
		if (vLine.size()!=vwFeature.size()){
			System.err.print("unmatched path tree size: "
					+vLine.size() +"(model file) !="
					+vwFeature.size()+" (current)");
			System.exit(-1);				
		}
		
		//String line;
		for (int i=0; i<vLine.size(); ++i){
			String vs[]= vLine.get(i).split("\t");
			String feature=vsPath.get(i);
			double weight=Double.parseDouble(vs[0]);
			
			if (!vs[1].equals(feature)){
				System.err.print("unmatched path name: "
						+vs[1] +"!="+feature);
				System.exit(-1);				
			}
			//if (!mIgPath.contains(feature))
				vwFeature.set(i,weight);
		}
		setParameters(vwFeature.toDoubleArray());	//.toArray()
	}

	//public EvaOpt eva= new EvaOpt();
	//public abstract VectorD getG0();
	public abstract void initWeights();
	public abstract void setParameters(double[] x);
	public abstract double[] getParameters();

	


	
	public void dumpWeights(String id){
		BufferedWriter bw=FFile.bufferedWriter(
				p.code+"/weights"+id);
		FFile.write(bw,"weight\tfeature\tcomments\n" );//\tAvg.#Signal
		for (int i=0; i<vsFeature.size();++i){
			String name=vsFeature.get(i);
			FFile.write(bw,"%.5f\t%s\t%s\n"	
				, vwFeature.get(i),name
				,this.mFeatureComments.getD(name, ""));
		}
		FFile.close(bw);


	}

	//PersistantCompactTokyoGraph
	public void createPathTrees(){//Graph g){//ERGraph g){
		//vsPath.clear();
		vPathNode.clear();

		for (EntType et: vSeedET){
			//PathTree tree = new PathTree( schema, et, etTrg,p.maxStep);
			PathTree tree = new PathTree( this, et);
			
			if (p.bDataDrivenPath)
				;//dataDrivenTree(null, tree);		
			else
				tree.createPathTree();
			vPTree.add(tree);
			vPathNode.addAll(tree.vNode);
		}
		addConstraintPaths();

		if (p.bEntityRank){
		/*	//vsPath.addAll(graph.entityRank.vsE0PathNames);
			treeE0 = new PathTree(g,schema, schema.T0, etTrg,p.maxStep);
			treeE0.createPathTree();
			vPathNode.addAll(
					treeE0.getPathNodes(g.entityRank.vsAllPath));*/
		}
		
		if (p.bHiddenFactor){
			//trees already created in onLoadDB();
		}
		
		vsPath= new VectorS();
		vsPath.addAll( vPathNode.getVS(CTag.nameS));
		report();
		msiPath=vsPath.toMapValueId();
		return;
	}
	
	public void report(){

		//System.out.println("[paths are] \n"+vsPath.join("\n"));
		
		System.out.println(vsPath.size()+" paths in total");
		
		VectorI vCount=new VectorI();
		for (PathNode n: vPathNode)
			vCount.plusOnE(n.nStep, 1);
		System.out.println("[length distr]= "+vCount.joinIndexed());
		
	}
	
	//TODO: this should be moved to Graph (which is currently an interface)

	
	public TVector<Query> loadQuery(String fn){
		//TVector<Query>,Graph graph
		
		System.out.println("loadQuery");
		TVector<Query> vQuery= new TVector<Query>(Query.class);
		//vQuery.clear();
		
		BufferedReader br = FFile.bufferedReader(fn);
		
		String line = null;	int n=0;
	
		VectorI viET=schema.getEntTypeIDs(vTitle);
		MapIIb miET_Col =(MapIIb)viET.toMapValueId();
		
		int incomplete=0;
		int tot=0;
		while ((line = FFile.readLine(br)) != null) {
			++tot;
			String vs[]= FString.split(line,",");

			VectorMapID vmSeed= new VectorMapID(); 
			for (int iCol: vSeedCol){
				String seeds=vs[iCol];
			
				EntType ET=vET.get(iCol);
				//if (ET.equals(net.etTime))	
					//seeds= (Integer.parseInt(seeds)-1)+"";
				//shift the time entity back
				//TODO: (special treatment for time, might be urgly)
				
				SetI miEnt= g.getNodeIdx(ET.name, seeds.split(" "));
				//dist0.get(ET.id).plusOn(miEnt, 1.0/miEnt.size());
				MapID m= new MapID();
				for (Integer i: miEnt)
					m.put(i,1.0/miEnt.size());
				vmSeed.add(m);
			}
			
			String rels= vs[iColTrg];
			SetI miRelEnt= g.getNodeIdx(etTrg.name,FString.split(rels," "));
			if (miRelEnt.size()==0){
				++incomplete;		
				System.out.println("incomplete scenario=" +line);
				continue;
			}			
			
			int time=-1;
			//if (graph.p.bTimeStamped)
				//time=Integer.parseInt(vs[p.icQueryTime]);
			
			String name=vs[p.icQueryName];
			
			vQuery.add(new Query(	time, name, vmSeed, etTrg, miRelEnt));
		}		
		FFile.close(br);
		System.out.println("done loading |vQuery|="+vQuery.size()+"\n");
		System.out.println("#incomplete scenario=" +incomplete +"/"+tot);
		return vQuery;	
	}

	public static enum ESparsityMode{
		T//Truncation
		,S// Sampling
		,Q//particle filtering
		,B//Beam
		,N//none
		,R//Relative Truncation
	}
	public static class Param 	extends ghirl.PRA.util.Param{
		public String code;	
		
		public Param() {
			super (ETGraph.class);
			parse();
		}
		
		public String taskFile;


		
		public int batchSize;
		public boolean bEntBias;
		public boolean bMultiStage;
		public int maxStage;
		public double scBias;
		//public String code;
		private String parseEntBias(){
			bEntBias=getBoolean("bEntBias",false);
			maxStage= getInt("maxStage",20);
			bMultiStage=getBoolean("bMultiStage",false);
			
			batchSize=getInt("batchSize",	50);

			
			
			scBias=getDouble("scBias",0.01);			

			String cod="";
			
			if (bEntBias){
				cod+="_EB"+this.batchSize;
				if (bMultiStage)
					cod+=maxStage;
			}
			return cod;
		}
		
		/**entity rank*/
		//public String e0folder;
		public int maxE0Step;
		public int minE0Step;
		public boolean bEntityRank;
		public int e0Gap;//e0TimeDownSample
		public boolean bMergeE0;
		
		private String parseEntityRank(){
			bEntityRank=getBoolean("bEntityRank",false);
			maxE0Step=getInt("maxE0Step",10);
			minE0Step=getInt("minE0Step",2);
			e0Gap=getInt("e0Gap",5);
			bMergeE0=getBoolean("bMergeE0",false);
			if (bEntityRank)	return "_ER"+e0Gap;
			return "";
		}
		
		/**entity rank*/
		public boolean bHiddenFactor;
		public int hfGap;//e0TimeDownSample
		public int maxHFStep;
		public int minHFStep;
		public boolean bMergeHF;
		public boolean bOfflineHF;
		public boolean bUniqueG;	//
		public boolean bSamplingG;	//
		
		private String parseHiddenFactor(){
			bHiddenFactor=getBoolean("bHiddenFactor",false);
			hfGap=getInt("hfGap",	1);
			maxHFStep=getInt("maxHFStep",	2);
			minHFStep=getInt("minHFStep",	0);
			bMergeHF=getBoolean("bMergeHF",false);
			bOfflineHF=getBoolean("bOfflineHF",false);
			bSamplingG=getBoolean("bSamplingG",false);
			bUniqueG=getBoolean("bUniqueG",false);
			
			String 	cod="";
			//if (this.bEntBias)		cod+="."+batchSize;

			if (bHiddenFactor){
				cod+="_HF"+minHFStep+"-"+maxHFStep;
				if (bUniqueG)cod+="u";
				if (bSamplingG)code+="s";
				if (bOfflineHF)	cod+="_gap"+hfGap;
			}
			return cod;
		}

		
		public double rDull;
		public boolean bDataDrivenPath;
		//public int nMinSupport;
		public double rMinQSupport;
		public int nDataDrivenSample;
		
		private String parseDataDriven(){
			rDull=getDouble("rDull",0.5);
			bDataDrivenPath=getBoolean("bDataDrivenPath",false);
			//nMinSupport=getInt("minSupport",	100);
			nDataDrivenSample=getInt("nDataDrivenSample",100);
			rMinQSupport=getDouble("rMinQSupport",	0.3);
			
			String cod="";
			if (bDataDrivenPath){
				cod +="_DD";
				cod += String.format("_mS%.1f",rMinQSupport );
				cod += String.format("_rD%.1f",rDull );
			}			
			return cod;
		}

		
		
		public double thSig;
		public double truncate=0.001;
		public int dbgTree;
		public boolean bRWRenormalize=false;
		public boolean bDisturb=false;
		public double RWDampening;
		public double alphaRT;
		public int maxWidth;
		public int nSampling;
		public ESparsityMode sMode;
		public double minParticle;
		public int maxStep;
		public int minStep;
		public String sIgnoredPathes;
		public String parseRandomWalk(){

			sIgnoredPathes=getString("sIgnoredPathes", null);

			dbgTree=getInt("dbgTree",	0);
			
			//bRWTruncate=getBoolean("bRWTruncate",false);
			//bRWSampling=getBoolean("bRWSampling",false);
			minParticle=getDouble("minParticle",0.001);
			truncate=getDouble("truncate",0.001);
			maxWidth= getInt("maxWidth",100);		
			nSampling= getInt("nSampling",1000);		

			bRWRenormalize=getBoolean("bRWRenormalize",false);
			
			RWDampening=getDouble("RWDampening",0.8);
			alphaRT=getDouble("alphaRT",0.5);
			thSig=getDouble("thSig",0.5);
			
			sMode=ESparsityMode.valueOf(getString("sMode"
					, ESparsityMode.N.name()));			

			maxStep= getInt("maxStep",3);		
			minStep= getInt("minStep",1);	
			
			String cod="L"+ this.maxStep+sMode;//"_"+
			
			switch (sMode){
			case T:cod += String.format("%.0e",truncate );break;
			case B:cod += String.format("%d",maxWidth ); break;
			case Q:cod += String.format("%.0e",minParticle ); break;
			case S:cod += String.format("%d",nSampling ); break;
			case R:cod += String.format("%.2f",alphaRT ); break;
			case N:break;
			}
			
			if (RWDampening!=1.0)	
				cod+=String.format("_dp%.1f",RWDampening);			
			if (bRWRenormalize)		cod+="N";			


			return cod;
		}
		public String dbFiles;
		public String dbName;
		
		public boolean bBias;	// a single bias as the last parameter
		

		
		public int icQueryTime=0;//column id in scenario file
		public int icQueryName=0;//column id in scenario file

		public int lenShortName=4;

		

		
		
		public void parse(){	
			taskFile=getString("taskFile", null);

			lenShortName=getInt("lenShortName",	2);

			icQueryTime=getInt("icQueryTime",	0);
			icQueryName=getInt("icQueryName",	0);
			
			bBias=getBoolean("bBias",false);
			

			
			code=taskFile+".L"+ this.maxStep;//"";
			
			if (bBias)	
				code+=String.format("b%.0e",scBias);	
			code+= parseDataDriven();
			code+=parseEntBias();
			code+=parseEntityRank();
			code+=parseHiddenFactor();
			code+=parseRandomWalk();
		}

	
	}
	
	

}
