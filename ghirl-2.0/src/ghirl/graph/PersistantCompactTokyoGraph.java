package ghirl.graph;

import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.StringUtil;
import ghirl.PRA.ICompact;
import ghirl.PRA.util.TSet.SetI;
import ghirl.PRA.util.TSet.SetS;
import ghirl.PRA.util.TVector.VectorS;
import ghirl.persistance.TokyoCabinetPersistance;
import ghirl.persistance.TokyoValueIterator;
import ghirl.util.CompactTCDistribution;
import ghirl.util.Config;
import ghirl.util.Distribution;
import ghirl.util.SerializationUtil;
import ghirl.util.TreeDistribution;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import tokyocabinet.BDB;
import tokyocabinet.BDBCUR;
import tokyocabinet.Util;

public class PersistantCompactTokyoGraph 
implements Graph, Closable, ICompact {
	private static final Logger logger = Logger.getLogger(PersistantCompactTokyoGraph.class);
	/** This string is appended to the fileStem to create the named location where PersistantCompactTokyoGraph stores its data. **/
	public static final String FILECODE="_compactTokyo";
	protected static final String
	NAME_GRAPHIDS="_nodes",
	NAME_IDGRAPHS="_nodeids",
	NAME_LINKLABELS="_links",
	NAME_LABELLINKS="_linkids",
	NAME_WALKLINKS="_walklinks",
	NAME_WALKINFO="_walkinfo";
	protected static final int BYTES_PER_INT=4;

	/** Maps nodeid:int -> flavor$shortName:String */
	public BDB node_id2string;
	/** Maps flavor$shortName:String -> nodeid:int */
	protected BDB node_string2id;

	/** Maps linkid:int -> String */
	protected BDB link_id2string;
	/** Maps link:String -> linkid:int */
	protected BDB link_string2id;

	/** Maps node:String -> links:Set<String> ?? */
	protected BDB walkLinks;
	/** Maps node+link:String -> CompactTCDistribution */
	protected BDB walkDistributions;

	protected TokyoCabinetPersistance tc;
	protected List<BDB> dbs = new ArrayList<BDB>(); 


	/**
	 * @param fileStem
	 * @param mode
	 *MODE_READ = 'r',
		MODE_WRITE= 'w',
		MODE_APPEND='a';
	 * @throws IOException
	 */
	public PersistantCompactTokyoGraph(String fileStem, char mode) 
	throws IOException {
		this.tc = new TokyoCabinetPersistance();
		tc.mode = mode;
		String fqpath = Config.getProperty(Config.DBDIR)
		+ File.separator	+ fileStem	+ FILECODE	+ File.separator;

		File dbdir = new File(fqpath);
		if (!dbdir.exists()) dbdir.mkdir();
		int imode = tc.MODES.get(mode);
		node_id2string = tc.initDB(fqpath+NAME_GRAPHIDS, imode); dbs.add(node_id2string);
		node_string2id = tc.initDB(fqpath+NAME_IDGRAPHS, imode); dbs.add(node_string2id);
		link_id2string = tc.initDB(fqpath+NAME_LINKLABELS, imode); dbs.add(link_id2string);
		link_string2id = tc.initDB(fqpath+NAME_LABELLINKS, imode); dbs.add(link_string2id);
		walkLinks = tc.initDB(fqpath+NAME_WALKLINKS, imode); dbs.add(walkLinks);
		walkDistributions = tc.initDB(fqpath+NAME_WALKINFO, imode); dbs.add(walkDistributions);
		if (mode!=tc.MODE_WRITE){
			cacheLabelMap();
			report();
			//vEdgeLabel=getOrderedEdgeLabels();
		}
	}

	//these information are need but not present in Tokyocab
	//protected String[] vEdgeLabel=null; 
	protected Set<String> mEdgeLabel=null; 
	protected Set<String> mNodeLabel=null; 
	protected void cacheLabelMap(){
		//String vLabel[]= getOrderedEdgeLabels();
		//System.out.println("Edge Labels are:\n"+FString.join(vLabel,"\n"));

		VectorS vLabel= getEdgeLabels();
		System.out.println("Edge Labels are:\n"+vLabel.join(", "));

		mEdgeLabel=new SetS(vLabel);		

		String vEntType[]= getOrderedNodeLabels();
		//mNodeLabel=new SetS(vEntType);
		//System.out.println("Node Labels are: "+FString.join(vEntType,", "));
		return;
	}

	private int getSize(BDB idx){
		BDBCUR cur = new BDBCUR(idx);
		cur.last();
		int nlinks = Util.unpackint(cur.val());
		return nlinks;
	}
	/**
	 * Summarize the loaded graph
	 */
	public void report(){
		//System.out.println(getSize(node_string2id) +" nodes " 
		//+ getSize(this.walkLinks) + " links");

		//this.dump();
	}
	public void dump() {
		System.out.println("iterating over graph nodes");
		//this.getOrderedEdgeLabels();
		int nN=0; int top=10;
		int nL=0;
		for (Iterator i=getNodeIterator(); i.hasNext(); ) {
			GraphId id = (GraphId)i.next();
			++nN;
			int idx = getNodeIdx(id);
			if (idx < 0) continue;
			Set<Integer> linkIds = getStoredWalkLinks(idx);
			for (int k: linkIds){
				Distribution d= walk1(idx, k);
				//System.out.println(id+"-->\n"+d);
				nL+=d.size();
			}
		}
		System.out.println(nN +" nodes " + nL + " links in total");
		//212,167 nodes 5,561,850 links in total

		//212,167 graphNode.pct
		//718,143 graphRow.pct
		//graphRow.pct: 701,241 lines(s) in 11.01 sec

		//18 nodes 34 links in total
		return;
	}

	public void load(String folder) 
	throws IOException, FileNotFoundException {
		if (!folder.endsWith(File.separator))
			folder= folder+ File.separator;

		File linkFile = new File(folder+"graphLink.pct");
		File nodeFile = new File(folder+"graphNode.pct");
		File walkFile = new File(folder+"graphRow.pct");
		File sizeFile = new File(folder+"graphSize.pct");

		load(sizeFile, linkFile, nodeFile, walkFile);
	}

	public void load(File sizeFile, File linkFile, File nodeFile, File walkFile) 
	throws IOException, FileNotFoundException {

		String line;
		String parts[];

		LineNumberReader sizeIn = new LineNumberReader(new FileReader(sizeFile));
		line = sizeIn.readLine();
		parts = line.split(" ");
		int numLinks = StringUtil.atoi(parts[0]);
		int numNodes = StringUtil.atoi(parts[1]);
		sizeIn.close();

		logger.info("Creating compact graph on disk with "+numLinks+" links and "+numNodes+" nodes");

		LineNumberReader linkIn = new LineNumberReader(new FileReader(linkFile));
		int id=0;	putLink("",id); // null link
		//		int id=-1;
		while((line = linkIn.readLine()) != null) {
			putLink(line,++id);
		}
		linkIn.close();


		ProgressCounter npc = new ProgressCounter("loading "+nodeFile,"lines");
		LineNumberReader nodeIn = new LineNumberReader(new FileReader(nodeFile));
		id = 0;
		putNode("",id); // null ID
		while((line = nodeIn.readLine())!= null) {
			putNode(line,++id);
			npc.progress();
		}
		npc.finished();
		nodeIn.close();


		ProgressCounter wpc = new ProgressCounter("loading "+walkFile,"lines");
		LineNumberReader walkIn = new LineNumberReader(new FileReader(walkFile));
		Set<Integer> links = null; int lastSrc=-1; int srcId = -2;
		while((line = walkIn.readLine()) != null) {
			parts = line.split(" ");
			srcId = Util.atoi(parts[0]);

			if (srcId != lastSrc) {
				if (links != null) 
					putWalkLinks(lastSrc, links);
				links = new TreeSet<Integer>();
				lastSrc = srcId;
			}

			int linkId = Util.atoi(parts[1]); links.add(linkId);
			int numDest = Util.atoi(parts[2]);
			int[] destId = new int[numDest];
			float[] totalWeightSoFar = new float[numDest];
			float tw = 0;
			int k=0;
			for (int i=3; i<parts.length; i++) {
				String[] destWeightParts = parts[i].split(":");
				destId[k] = Util.atoi(destWeightParts[0]);
				tw += StringUtil.atof(destWeightParts[1]);
				totalWeightSoFar[k] = tw;
				k++;
			}
			putStoredDistribution(srcId, linkId, destId, totalWeightSoFar);

			wpc.progress();
		}
		putWalkLinks(srcId, links);
		wpc.finished();
		walkIn.close();


		tc.freeze(dbs);
		logger.info("Finished loading compact graph.");

		cacheLabelMap();
		report();
	}

	protected void putNode(String nodekey, int id) {
		putIndex(nodekey, id, this.node_id2string, this.node_string2id);
	}

	protected void putLink(String link, int id) {
		putIndex(link, id, this.link_id2string, this.link_string2id);
	}

	protected void putIndex(String key, int id, BDB db2string, BDB db2id) {
		logger.debug("Adding index "+id+" for "+key);
		byte[] bid = Util.packint(id), bkey = key.getBytes();
		db2string.put(bid, bkey);
		db2id.put(bkey, bid);
	}

	protected void putWalkLinks(int nodeid, Set<Integer> linkIds) {
		if (logger.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("Adding links to "+nodeid+":");
			for (int i : linkIds) sb.append("\n\t"+i);
			logger.debug(sb.toString());
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int length = linkIds.size();
		SerializationUtil.serializeInt(length, baos, BYTES_PER_INT);
		for(int i : linkIds) {
			SerializationUtil.serializeInt(i, baos, BYTES_PER_INT);
		}
		try { baos.close(); } catch (IOException e) {
			logger.error("Trouble closing byte array stream for writing walk links:",e);
		}
		this.walkLinks.put(Util.packint(nodeid), baos.toByteArray());
	}

	protected Set<Integer> getStoredWalkLinks(int nodeid) {
		byte[] result = this.walkLinks.get(Util.packint(nodeid));
		if (result == null) return Collections.EMPTY_SET;
		ByteArrayInputStream bais = new ByteArrayInputStream(result);
		int length = SerializationUtil.deserializeInt(bais, BYTES_PER_INT);
		Set<Integer> s = new TreeSet<Integer>();
		for(int i=0; i<length; i++) 
			s.add(SerializationUtil.deserializeInt(bais, BYTES_PER_INT));
		if (logger.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("Got links for "+nodeid+":");
			for (int i : s) sb.append("\n\t"+i);
			logger.debug(sb.toString());
		}
		return s;
	}

	@Override
	public boolean contains(GraphId node) {
		return node_string2id.get(makeKey(node)) != null;
	}



	@Override
	public Iterator getNodeIterator() {
		BDBCUR cur = new BDBCUR(this.node_id2string);
		Iterator it = new TokyoValueIterator(cur);
		cur.next(); // skip the null elt: must do this after constructor
		return it;
	}

	@Override
	public GraphId[] getOrderedIds() {
		throw new UnsupportedOperationException("Can't store GraphID array in memory; use getNodeIterator() instead.");
	}

	@Override
	public String getProperty(GraphId from, String prop) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Distribution asQueryDistribution(String queryString) {
		return CommandLineUtil.parseNodeOrNodeSet(queryString,this);
	}

	@Override
	public Set followLink(GraphId from, String linkLabel) {
		Set s = new TreeSet();
		Distribution result = walk1(from, linkLabel);
		for (Iterator it = result.iterator(); it.hasNext();) {
			s.add(it.next());
		}
		return s;
	}

	@Override
	public Set getEdgeLabels(GraphId from) {
		int fromIndex =getNodeIdx(from);
		//getStoredIndex(this.node_string2id, makeKey(from));
		if (fromIndex < 0) return Collections.EMPTY_SET;
		Set<String> result = new TreeSet<String>();
		for (int li : getStoredWalkLinks(fromIndex)) {
			byte[] r = this.link_id2string.get(Util.packint(li));
			if (r==null) {
				logger.debug("Unknown link in walkLinks set (node "+fromIndex+", link "+li+")");
				return Collections.EMPTY_SET;
			}
			String s = new String(r);
			result.add(s);
		}
		return result;
	}


	/**this implementation so how does not work with Yeast2 data
	 * re-implemented in getEdgeLabels()*/
	@Override
	public String[] getOrderedEdgeLabels() {
		BDBCUR cur = new BDBCUR(link_string2id);
		cur.last();
		int nlinks = Util.unpackint(cur.val());
		cur.first(); cur.next(); // skip the null link
		String[] result = new String[nlinks];
		for (int i=0; i<nlinks; i++) { 
			result[i] = cur.key2();
			cur.next();
		}
		return result;
	}

	public VectorS getEdgeLabels() {
		BDBCUR cur = new BDBCUR(link_string2id);
		cur.first();

		VectorS result = new VectorS(); 
		String key;String value;
		while((key = cur.key2()) != null){
			value = cur.val2();
			result.add(key);
			//if(value != null)       System.out.println(key + ":" + value);
			cur.next();
		}


		/*		cur.last();
		int nlinks = Util.unpackint(cur.val());
		cur.first(); cur.next(); // skip the null link
		String[] result = new String[nlinks];
		for (int i=0; i<nlinks; i++) { 
			result[i] = cur.key2();
			cur.next();
		}*/
		return result;
	}

	//@Override
	public String[] getOrderedNodeLabels() {
		BDBCUR cur = new BDBCUR(node_string2id);
		cur.last();
		int nlinks = Util.unpackint(cur.val());
		cur.first(); cur.next(); // skip the null link
		String[] result = new String[nlinks];
		for (int i=0; i<nlinks; i++) { 
			result[i] = cur.key2();
			cur.next();
		}
		return result;
	}

	public int[] getOrderedEdgeIDs() {
		BDBCUR cur = new BDBCUR(link_string2id);
		cur.last();
		int nlinks = Util.unpackint(cur.val());
		cur.first(); cur.next(); // skip the null link
		int[] result = new int[nlinks];
		for (int i=0; i<nlinks; i++) { 
			result[i] =Util.unpackint(cur.key());
			cur.next();
		}
		return result;
	}


	@Override
	public String getTextContent(GraphId id) {
		return id.getShortName();
	}

	@Override
	public Distribution walk1(GraphId from) {
		int fromNodeIndex = getNodeIdx(from);
		//getStoredIndex(this.node_string2id, makeKey(from));
		if (fromNodeIndex < 0) return TreeDistribution.EMPTY_DISTRIBUTION;
		Set<Integer> linkIds = getStoredWalkLinks(fromNodeIndex);
		Distribution accum = new TreeDistribution();
		for (int li : linkIds) {
			Distribution di = getStoredDistribution(fromNodeIndex, li);
			accum.addAll(di.getTotalWeight(), di);
		}
		return accum;
	}


	protected int getStoredIndex(BDB db, byte[] key) {
		byte[] result = db.get(key);
		if (result == null) return -1;
		int i = Util.unpackint(result);
		return i;
	}

	@Override public int getNodeIdx(GraphId from){
		return getStoredIndex(this.node_string2id, makeKey(from));
	}
	@Override
	public GraphId getNodeId(String flavor, String shortNodeName) {
		/*	if (!mNodeLabel.contains(flavor)){
			System.err.println("flavor not found. name="+flavor);
			return null;
		}*/

		GraphId node = new GraphId(flavor,shortNodeName);
		if (contains(node)) return node;
		return null;
	}
	public Integer getNodeIdx( String flavor, String name) {
		// mNodeLabel to be created
		/*	if (!this.schema.miEntType.containsKey(flavor)){
				System.err.println("flavor not found. name="+flavor);
				continue;
			}*/
		return getNodeIdx(new GraphId(flavor,name));
	}
	public SetI getNodeIdx( String flavor, String[] vs) {//int iSec
		SetI m= new SetI();
		for (String name: vs){
			m.add(getNodeIdx(flavor, name));
		}
		return m;
	}
	public String getNodeName(int idx){
		return getStoredString(node_id2string, Util.packint(idx));
	}
	protected String getStoredString(BDB db, byte[] key) {
		byte[] result = db.get(key);
		if (result == null) return null;
		return new String(result);
	}
	public String[] getNodeName(Collection<Integer> vi){
		String vs[]= new String[vi.size()];
		int i=0;
		for (Integer idx: vi){
			vs[i]=this.getNodeName(idx);
			++i;
		}
		return vs;
	}

	@Override
	public Distribution walk1(GraphId from, String linkLabel) {
		int fromNodeIndex = getNodeIdx(from);
		if (fromNodeIndex < 0) return TreeDistribution.EMPTY_DISTRIBUTION;
		int linkIndex = getStoredIndex(this.link_string2id, linkLabel.getBytes());
		if (linkIndex < 0) return TreeDistribution.EMPTY_DISTRIBUTION;

		return getStoredDistribution(fromNodeIndex, linkIndex);
	}

	@Override public Distribution walk1(int from,int linkLabel){
		return getStoredDistribution(from, linkLabel);
	}



	@Override public SetI walk2(int from,int linkLabel){
		Distribution d= getStoredDistribution(from, linkLabel);
		SetI m= new SetI();

		for (Iterator i=d.iterator(); i.hasNext(); ) {
			GraphId id = (GraphId)i.next();
			//double w = d.getLastWeight();
			m.add(getNodeIdx(id));
		}
		return m;
	}

	/*	@Override public MapID walk2(int from,int linkLabel){

		Distribution d=walk1(from, linkLabel);
		MapID mTrg= new MapID();
		// randomly walk away from the subsample
		for (Iterator i=d.iterator(); i.hasNext(); ) {
			GraphId id = (GraphId)i.next();
			double w = d.getLastWeight();
			mTrg.plusOn(getIdxID(id), w);
		}
		return mTrg;
	}
	public VectorI walk3(int from,int linkLabel){

		Distribution d=walk1(from, linkLabel);
		VectorI mTrg= new VectorI();mTrg.ensureCapacity(d.size());
		// randomly walk away from the subsample
		for (Iterator i=d.iterator(); i.hasNext(); ) {
			GraphId id = (GraphId)i.next();
			double w = d.getLastWeight();
			mTrg.plusOn(getIdxID(id), w);
		}
		return mTrg;
	}*/
	// TokyoCabinet is totally happy on byte arrays.  No fun.

	protected byte[] makeKey(GraphId node) { return node.toString().getBytes(); }

	// this is probably not so good.
	// I could just write out both ints in bytes.  That makes me cry inside, but I'd get over it.
	protected byte[] makeKey(int fromNodeIndex, int linkIndex) {
		return (fromNodeIndex + "#" + linkIndex).getBytes();
	}




	protected Distribution getStoredDistribution(int fromNodeIndex, int linkIndex) {
		return getStoredDistribution(makeKey(fromNodeIndex,linkIndex));
	}
	protected Distribution getStoredDistribution(byte[] key) {
		byte[] result = this.walkDistributions.get(key);
		if (result == null) return TreeDistribution.EMPTY_DISTRIBUTION;
		try {
			CompactTCDistribution d = new CompactTCDistribution(result, node_id2string);
			return d;
		} catch (IOException e) {
			logger.error("Problem extracting distribution from disk for key "+key,e);
		}
		return TreeDistribution.EMPTY_DISTRIBUTION;
	}
	protected void putStoredDistribution(int fromNodeIndex, int linkIndex, 
			int[] objectIndex, float[] totalWeightSoFar) {
		if (logger.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("Adding distribution for node "+fromNodeIndex+" link "+linkIndex+":");
			for (int i : objectIndex) sb.append("\n\t"+i);
			logger.debug(sb.toString());
		}
		CompactTCDistribution ctd = new CompactTCDistribution(objectIndex, totalWeightSoFar, node_id2string, true);
		try {
			this.walkDistributions.put(
					makeKey(fromNodeIndex,linkIndex),
					ctd.serialize());
		} catch (IOException e) {
			logger.error("Problem serializing distribution for node number "+fromNodeIndex+", link number "+linkIndex,e);
		}
	}

	@Override
	public void close() {
		tc.close(dbs);
	}
}
