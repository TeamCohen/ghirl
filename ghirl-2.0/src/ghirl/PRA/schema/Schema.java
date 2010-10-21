package ghirl.PRA.schema;

import ghirl.PRA.util.CTag;
import ghirl.PRA.util.TVector;
import ghirl.PRA.util.Interfaces.IGetStringByString;
import ghirl.PRA.util.TMap.MapSI;
import ghirl.PRA.util.TMap.TMapSX;
import ghirl.PRA.util.TVector.VectorI;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Schema {
	public int lenShortName=4;
	
	private static final long serialVersionUID = 2008042701L; // YYYYMMDD
	//public PathForest forest;//= new PathForest();
	//public ERGraph graph=null;

	public EntType etTime=null;	
	public Relation rTime=null;	

	public TVector<EntType> vEntType = new TVector<EntType>(EntType.class);
	public TVector<Relation> vRel = new TVector<Relation>(EntType.class);

	private static final Pattern pattern = Pattern.compile("(.*)\\((.*)\\).*");
	public void parseSchemaLine(String line) {
		TVector<EntType> vET = new TVector<EntType>(EntType.class);
		boolean bArgRelation = false;
		// treating one argument as	relation (dynamic relation)
		String[] vsLine = line.split("\\|");
		
		Matcher matcher = pattern.matcher(vsLine[0]);
		if (!matcher.matches()) {// advisedBy(person, person)
			System.out.print("bad formated schema: " + line);
			return;
		}
		String Rel = matcher.group(1);// attribute
		String EntTypes = matcher.group(2);// entity types
		// VectorI viFormat= new VectorI();
		String[] vs = EntTypes.split(", *");
		vET.clear();
		for (String ET : vs) {
			if (ET.equals("*R")) {// R(animal,*A)
				vET.add(null);
				bArgRelation = true;
			} else {
				vET.add(tryAddEntityType(ET));
			}
		}
		EntType et1 = vET.get(0);
		EntType et2 = vET.get(1);
		
		Format form = new Format(null, et1, et2, line);
		
		for (int i=1; i<vsLine.length;++i){
			String tag=vsLine[i];
			//if (tag.equals("NoBack"))		form.bNoBack=true;
			
			if (vsLine[i].equals("NoBF"))			form.bNoBF=true;
			else if (vsLine[i].equals("NoFB"))form.bNoFB=true;
			//else if (vsLine[i].equals("NoRepeat"))form.bNoRepeat=true;
			else if (vsLine[i].equals("NoBB"))			form.bNoBB=true;
			else if (vsLine[i].equals("+1"))			form.timeShift=1;
			else if (vsLine[i].equals("NoFF"))form.bNoFF=true;
			else if (tag.equals("NoDir"))			form.bNoDir=true;			
			else if (tag.equals("NoTwin"))			form.bNoTwin=true;			
			else{
				System.err.println("unknown tag="+tag);
				System.exit(-1);
			}				
			/**/
		}		
		if (!bArgRelation)
			form.relation=tryAddRelation(Rel, form);	
		mFormat.put(Rel, form);
		return;
	}
	public String toString() {
		return report();
	}

	public String report() {// TRMRF net){
		return String.format("|EntType|=%d |Relation|=%d "
				, this.vEntType.size(),	this.vRel.size());
	}

	public static class Format implements Serializable {
		private static final long serialVersionUID = 2008042701L;
		public String txt = null;
		public Relation relation = null;
		// public VectorI vi=new VectorI();
		// public TVector<EntityType> vEntType= new
		// TVector<EntityType>(EntityType.class);
		public EntType et1, et2;
		//public boolean bNoBack=false;
		public boolean bNoDir=false;
		public boolean bNoFB=false;
		public boolean bNoBF=false;
		public boolean bNoFF=false;
		public boolean bNoBB=false;
		public boolean bNoTwin=false;
		public int timeShift=0;
//		public boolean bNoRepeat=false;
		// TVector<EntityType> vEntType
		public Format(Relation relation, EntType et1, EntType et2, String txt) {//
			this.relation = relation;
			this.et1 = et1;
			this.et2 = et2;
			// this.vEntType.addAll(vEntType);
			// this.vi.addAll(vi);
			this.txt = txt;
		}
	}
	public TMapSX<Format> mFormat = new TMapSX<Format>(Format.class);
	// public MapVectorSI mviFormat= new MapVectorSI();
	
	
	public MapSI miRelation = new MapSI();//relation name--> id
	public MapSI miRelation_S = new MapSI();//relation short name--> id

	
	//lenShortName=getInt("lenShortName",	4);

	//String nameS,
	public Relation addRelation(
			String name,  EntType et, EntType etTo) {
		String nameS=null;
		//if (name.startsWith("_"))
			//nameS= name.substring(0,Math.min(3, name.length()));
		//else
		nameS= name.substring(0,Math.min(lenShortName, name.length()));

		Relation r = new Relation(vRel.size(),name,nameS, et, etTo);
		vRel.add(r);
		et.vRelationTo.add(r);
		etTo.vRelationFrom.add(r);
		return r;
	}
	public Relation tryAddRelation(	String name, Format form){
		//EntType et, EntType etTo, boolean bNoBack) {
	
		Integer id = miRelation.get(name);
		if (id != null) return vRel.get(id);
		
		Relation r = addRelation( name,  form.et1, form.et2);
		//r.bNoBack=form.bNoBack;
		r.bNoBack=form.bNoFB;
		r.bNoRepeat=form.bNoFF;// .bNoRepeat;
		r.timeShift=form.timeShift;
		miRelation.put(r.name, r.id);
		
		Integer id1= miRelation_S.get(r.nameS);
		if (id1!=null){
			System.err.println("Relation short name clash "+
					r.name+" v.s. " + vRel.get(id1));
			//System.exit(-1);
		}		
		miRelation_S.put(r.nameS, r.id);
		
		if (!form.bNoTwin){		
			Relation _r = addRelation("_" + r.name, r.etTo, r.etFr);		
			_r.bIsAMirror=true;
			r.twin = _r;
			_r.twin=r;
			
			_r.bNoBack=form.bNoBF;
			_r.bNoRepeat=form.bNoBB;
			_r.timeShift=form.timeShift;
			miRelation.put(_r.name, _r.id);
		}

		return r;
	}
	
	public MapSI miEntType = new MapSI();

	protected EntType tryAddEntityType(String name) {
		Integer ID = miEntType.get(name);
		if (ID != null) return this.vEntType.get(ID);
		EntType et = new EntType(vEntType.size(), name);
		vEntType.add(et);
		miEntType.put(name, et.id);
		return et;
	}


	public VectorI getEntTypeIDs(String[] vs) {
		return (VectorI) miEntType.subV(vs);
	}
	
	public TVector<EntType> getEntType(String[] vs) {
		return vEntType.sub(getEntTypeIDs(vs));
	}
	
	public EntType getEntType(String name) {
		EntType et= vEntType.getN(miEntType.get(name));
		if (et==null){
			System.err.print("unknown entity type="+name);
			System.exit(-1);
		}
		return et;
	}
	
	public Relation getRelation(String name) {
		return vRel.getN(miRelation.get(name));
	}
	// how to make sure this function is called only once?
	// make it private and call it in constructor
	// however, we may have dynamic entity types in db file?
	// which is read after the construction of ETGraph
	
	
	public void onLoadSchema(ETGraph net){
		for (EntType et: net.vSeedET)
			addRelation("R"+et.name, etRoot, et);
		
		if (net.p.bEntityRank){
			addRelation("RE", etRoot, T0);			
			T0.vRelationTo.clear();
			for (EntType et: vEntType)
				addRelation( et.name, T0, et);
		}
		
		if (net.p.bHiddenFactor){
			addRelation("HF", etRoot, T_HF);			
			T_HF.vRelationTo.clear();
			for (EntType et: vEntType)
				addRelation( et.name, T0, et);
		}
		
		//System.out.println(this);
		return ;
	}
	// special type and relation for entity rank
	public EntType etRoot= new EntType(-1,"Root");
	public EntType T0= new EntType(-2,"T0");	//root of entity rank
	public EntType T1= new EntType(-3,"T1");	//root of special relations
	
	public EntType T_HF= new EntType(-4,"HF");	//root of hidden factors
	
	//public void onLoadSchema(LearnerGhirl learner){};


	public static class EntType implements Serializable , IGetStringByString{
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD
		public String name;
		public String nameS;
		public int id=-1;
		public double Z=0;

		public TVector<Relation> vRelationTo 
			= new TVector<Relation>(Relation.class);
		public TVector<Relation> vRelationFrom 
		= new TVector<Relation>(Relation.class);
		
		public String getString(String key) {
			if (key.equals(CTag.name))return name;		
			if (key.equals(CTag.nameS))return nameS;		
			return null;
		}
		
		public EntType(int id, String name){//, String nameS
			this.id = id;
			this.name=name;	
			this.nameS=name.substring(0, 1);//nameS;
			
		}

		public String toString() {
			return String.format("ET%d)%s",id,name);//, et.name, etTo.name);
		}
	}

	
	public static class Relation implements Serializable, IGetStringByString {
		

		
		private static final long serialVersionUID = 2008042701L; 
		public int id;
		public String name;
		public String nameS;
		public EntType etFr;
		public EntType etTo;
		public boolean bNoBack=false;//block the path walking forward backward
		public boolean bNoRepeat=false;
		public int timeShift=0;
		//public boolean bBlockBackward=false;
		public int count=0;
		public Relation twin=null;// reversed relation
		public boolean bIsAMirror=false;
		public Relation(int id, String name, String nameS
				, EntType et, EntType etTo){
			this.id=id;
			this.name=name;
			this.etFr=et;
			this.etTo=etTo;
			this.nameS=nameS;
			//name.substring(0,1);//Math.min(2, name.length()));
		}
		public Relation(Relation r){
			this(r.id+1, "_" + r.name, "_"+r.nameS, r.etTo, r.etFr);
			bIsAMirror=true;
			r.twin = this;
			twin=r;
			//this.nameS= name.substring(0,2);
		}
		public String getString(String key) {
			if (key.equals(CTag.name))return name;		
			if (key.equals(CTag.nameS))return nameS;		
			return null;
		}
		public String toString() {
			return String.format("R%d)%s=%s->%s"
				,id,name, etFr.name, etTo.name);
		}
		public boolean canFollow(Relation r0){
			if (r0==null ) return true;
			if (r0.bNoRepeat)
				if (r0.equals(this))
					return false;	// no repetition allowed
			if (r0.bNoBack)
				if (r0.twin!=null)
					if (r0.twin.equals(this))
						return false;	// no backward walking
			return true;
		}		
	}
	
}
