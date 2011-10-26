package ghirl.graph;

import edu.cmu.lti.algorithm.container.MapID;
import edu.cmu.lti.algorithm.container.SetI;
import edu.cmu.lti.algorithm.container.SetS;
import edu.cmu.lti.algorithm.container.VectorS;
import edu.cmu.lti.util.system.FSystem;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.StringUtil;
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

public class PersistantCompactTokyoGraph extends AbstractCompactGraph
implements Graph, Closable, ICompact {
	protected static final char SPACE_WORDCHAR = '\u0020';
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

	/** Maps linkid:int -> link:String */
	protected BDB link_id2string;
	/** Maps link:String -> linkid:int */
	protected BDB link_string2id;

	/** Maps nodeid:int -> links:set<linkid>: #links [linkid,linkid,...,linkid] */
	protected BDB walkLinks;
	/** Maps nodeid:int+linkid:int -> CompactTCDistribution */
	protected BDB walkDistributions;

	protected TokyoCabinetPersistance tc;
	protected List<BDB> dbs = new ArrayList<BDB>(); 

	/** For unit tests only **/
	protected PersistantCompactTokyoGraph() { logger.warn("Note: Empty constructor for unit tests only. If this is not a unit test, try again."); }
	
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
//			cacheLabelMap();
			report();
			//vEdgeLabel=getOrderedEdgeLabels();
		}
	}

	//these information are need but not present in Tokyocab
	//protected String[] vEdgeLabel=null; 
//	protected Set<String> mEdgeLabel=null; 
//	protected Set<String> mNodeLabel=null; 
//	protected void cacheLabelMap(){
//		//String vLabel[]= getOrderedEdgeLabels();
//		//System.out.println("Edge Labels are:\n"+FString.join(vLabel,"\n"));
//
//		VectorS vLabel= getEdgeLabels();
//		System.out.println("Edge Labels are:\n"+vLabel.join(", "));
//
//		mEdgeLabel=new SetS(vLabel);		
//
//		String vEntType[]= getOrderedNodeLabels();
//		//mNodeLabel=new SetS(vEntType);
//		//System.out.println("Node Labels are: "+FString.join(vEntType,", "));
//		return;
//	}

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
		logger.debug("iterating over graph nodes");
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
				//logger.debug(id+"-->\n"+d);
				nL+=d.size();
			}
		}
		logger.debug(nN +" nodes " + nL + " links in total");
		//212,167 nodes 5,561,850 links in total

		//212,167 graphNode.pct
		//718,143 graphRow.pct
		//graphRow.pct: 701,241 lines(s) in 11.01 sec

		//18 nodes 34 links in total
		return;
	}

//	public void load(String folder) 
//	throws IOException, FileNotFoundException {
//		if (!folder.endsWith(File.separator))
//			folder= folder+ File.separator;
//
//		File linkFile = new File(folder+"graphLink.pct");
//		File nodeFile = new File(folder+"graphNode.pct");
//		File walkFile = new File(folder+"graphRow.pct");
//		File sizeFile = new File(folder+"graphSize.pct");
//
//		load(sizeFile, linkFile, nodeFile, walkFile);
//	}
//
//	public void load(File sizeFile, File linkFile, File nodeFile, File walkFile) 
//	throws IOException, FileNotFoundException {
//
//		String line; int linenum;
//		String parts[];
//
//		LineNumberReader sizeIn = new LineNumberReader(new FileReader(sizeFile));
//		line = sizeIn.readLine();
//		parts = line.split(" ");
//		int numLinks = StringUtil.atoi(parts[0]);
//		int numNodes = StringUtil.atoi(parts[1]);
//		sizeIn.close();
//
//		logger.info("Creating compact graph on disk with "+numLinks+" links and "+numNodes+" nodes");
//
//		LineNumberReader linkIn = new LineNumberReader(new FileReader(linkFile));
//		int id=0;	putLink("",id); // null link
//		//		int id=-1;
//		for(linenum=0;(line = linkIn.readLine()) != null;linenum++) {
//			putLink(line,++id);
//		}
//		linkIn.close();
//
//
//		ProgressCounter npc = new ProgressCounter("loading "+nodeFile,"lines");
//		LineNumberReader nodeIn = new LineNumberReader(new FileReader(nodeFile));
//		id = 0;
//		putNode("",id); // null ID
//		for(linenum=0;(line = nodeIn.readLine())!= null; linenum++) {
//			putNode(line,++id);
//			npc.progress();
//		}
//		npc.finished();
//		nodeIn.close();
//
//
//		ProgressCounter wpc = new ProgressCounter("loading "+walkFile,"lines");
//		LineNumberReader walkIn = new LineNumberReader(new FileReader(walkFile));
//		Set<Integer> links = null; int lastSrc=-1; int srcId = -2;
//		for(linenum=0;(line = walkIn.readLine()) != null; linenum++) {
//			try {
//				//			parts = line.split("\\s+");
//				//			int cursor = 0;
//				TokenData token = nextToken(line,0); //cursor=token.nextIndex;
//				srcId = Util.atoi(token.token); 
//
//				if (srcId != lastSrc) {
//					if (links != null) 
//						putWalkLinks(lastSrc, links);
//					links = new TreeSet<Integer>();
//					lastSrc = srcId;
//				}
//
//				token = nextToken(line,token.nextIndex); //cursor = token.nextIndex;
//				int linkId = Util.atoi(token.token); links.add(linkId);
//				token = nextToken(line,token.nextIndex); //cursor = token.nextIndex;
//				int numDest = Util.atoi(token.token);
//				int[] destId = new int[numDest];
//				float[] totalWeightSoFar = new float[numDest];
//				float tw = 0;
//				int k=0;
//				//			for (int i=3; i<parts.length; i++) {
//				token = nextToken(line,token.nextIndex); //cursor = token.nextIndex;
//				while (token != null) {
//					TokenData weightToken = backToken(token.token,':',token.token.length());
//					//				String[] destWeightParts = token.token.split(":");
//					destId[k] = Util.atoi(token.token.substring(0,weightToken.nextIndex));
//					tw += StringUtil.atof(weightToken.token);
//					totalWeightSoFar[k] = tw;
//					k++;
//					token = nextToken(line,token.nextIndex);
//				}
//				putStoredDistribution(srcId, linkId, destId, totalWeightSoFar);
//
//				wpc.progress();
//			} catch (RuntimeException e) {
//				logger.error("Runtime error on line "+linenum+": "+line);
//				throw(e);
//			}
//		}
//		putWalkLinks(srcId, links);
//		wpc.finished();
//		walkIn.close();
//
//
//		tc.freeze(dbs);
//		logger.info("Finished loading compact graph.");
//
////		cacheLabelMap();
//		report();
//	}
//	protected class TokenData {
//		public String token;
//		public int nextIndex;
//		public TokenData(String t, int n) {token=t; nextIndex=n;}
//	}
//	protected TokenData nextToken(String line, int startAt) {
//		int len = line.length();
//		int i=startAt; if (i>=len) return null;
//		StringBuilder sb = new StringBuilder();
//		//1: skip delimiter chars at the head of the string
//		for (;line.charAt(i) <= SPACE_WORDCHAR; i++) if ((i+1)>=len) return null;
//		//2: accumulate chars until we see a delim char
//		for (;i<len;i++) {
//			char c = line.charAt(i);
//			if (c > SPACE_WORDCHAR) sb.append(c);
//			else break;
//		}
//		return new TokenData(sb.toString(),i);
//	}
//	protected TokenData backToken(String line, char delim, int startAt) {
//		int len = line.length();
//		int i=startAt; if (i<=0) return null;
//		if (i>=len) i=len-1;
//		StringBuilder sb = new StringBuilder();
//		//1: skip delimiter chars at the head of the string
//		for (;line.charAt(i) == delim; i--) if ((i-1)<0) return null;
//		//2: accumulate chars until we see a delim char
//		for (;i>=0;i--) {
//			char c = line.charAt(i);
//			if (c != delim) sb.append(c);
//			else break;
//		}
//		return new TokenData(sb.reverse().toString(),i);
//	}

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

	protected void putWalkInfoLinks(int nodeid, Set<Integer> linkIds) {
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
		throw new UnsupportedOperationException("No properties stored on nodes in "+PersistantCompactTokyoGraph.class.getName());
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

//	public VectorS getEdgeLabels() {
//		BDBCUR cur = new BDBCUR(link_string2id);
//		cur.first();
//
//		VectorS result = new VectorS(); 
//		String key;String value;
//		while((key = cur.key2()) != null){
//			value = cur.val2();
//			result.add(key);
//			cur.next();
//		}
//		return result;
//	}

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
	
	@Override 
	public int getNodeIdx(GraphId from){
		return getStoredIndex(this.node_string2id, makeKey(from));
	}
	//	@Override
	//	public Integer getNodeIdx( String flavor, String name) {
	//		return getNodeIdx(new GraphId(flavor,name));
	//	}
	@Override public SetI getNodeIdx( String flavor, String[] vs) {//int iSec
		SetI m= new SetI();
		for (String name: vs){
			m.add(getNodeIdx(new GraphId(flavor, name)));
		}
		return m;
	}
	@Override public String getNodeName(int idx){
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
	protected void addWalkInfoDistribution(int fromNodeIndex, int linkIndex, 
			int[] objectIndex, float[] totalWeightSoFar) {
		if (logger.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("Adding distribution for node "+fromNodeIndex+" link "+linkIndex+" (cumulative weights):");
			for (int i=0;i<objectIndex.length;i++) sb.append("\n\t"+objectIndex[i]+":"+totalWeightSoFar[i]);
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




	@Override public Distribution walk1(int from,int linkLabel){
		return getStoredDistribution(from, linkLabel);
	}


	@Override
	public Distribution walk1(GraphId from, String linkLabel) {
		int fromNodeIndex = getNodeIdx(from);
		if (fromNodeIndex < 0) return TreeDistribution.EMPTY_DISTRIBUTION;
		int linkIndex = getStoredIndex(this.link_string2id, linkLabel.getBytes());
		if (linkIndex < 0) return TreeDistribution.EMPTY_DISTRIBUTION;

		return getStoredDistribution(fromNodeIndex, linkIndex);
	}




	@Override public GraphId[] getGraphIds(){
		throw new UnsupportedOperationException("Don't even TRY to ask for an array of GraphIds!");
	}
//	public void setTime(int time) {
//		FSystem.dieNotImplemented();		
//	}
	@Override public void step(int ent, int rel, SetI dist) {
		Distribution d= getStoredDistribution(ent, rel);

		for (Iterator i=d.iterator(); i.hasNext(); ) {
			GraphId id = (GraphId)i.next();
			//double w = d.getLastWeight();
			dist.add(getNodeIdx(id));
		}
	}
	@Override public void step(int ent, int rel, double p0, MapID dist) {
		Distribution d= getStoredDistribution(ent, rel);

		for (Iterator i=d.iterator(); i.hasNext(); ) {
			GraphId id = (GraphId)i.next();
			//double w = d.getLastWeight();
			dist.plusOn(getNodeIdx(id), p0);
		}

	}

	
	/************** Template methods for local data implementation ***********/
	@Override
	protected void initLoad(int numLinks, int numNodes) {}

	@Override
	protected void initLinks() {
		putLink("",0); // add null link
	}

	@Override
	protected void addLink(int id, String link) {
		putLink(link,id);
	}

	@Override
	protected void initNodes() {
		putNode("",0); // add null nodeid
	}

	@Override
	protected void addNode(int id, String node) {
		putNode(node,id);
	}

	@Override
	protected void initWalkInfo() {}

//	@Override
//	protected void putWalkInfoLinks(int src, Set<Integer> links) {
//		// TODO Auto-generated method stub
//		
//	}

//	@Override
//	protected void addWalkInfoDistribution(int srcId, int linkId, int[] destId,
//			float[] totalWeightSoFar) {
//		// TODO Auto-generated method stub
//		
//	}

	@Override
	protected void finishWalkInfo() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void finishLoad() {
		tc.freeze(dbs);
		report();
	}

	@Override
	public void setTime(int time) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	/************** End template methods ****************/
}
