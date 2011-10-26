package ghirl.graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import tokyocabinet.Util;

import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.util.StringUtil;

public abstract class AbstractCompactGraph implements Graph, ICompact {
    private static final Logger log = Logger.getLogger(AbstractCompactGraph.class);
	protected static final char SPACE_WORDCHAR = '\u0020';

    protected abstract void initLoad(int numLinks, int numNodes);
    
    protected abstract void initLinks();
    protected abstract void addLink(int id, String link);
    
    protected abstract void initNodes();
    protected abstract void addNode(int id, String node);
	
    /** Initialize walk info cache */
    protected abstract void initWalkInfo();
    protected abstract void putWalkInfoLinks(int src, Set<Integer> links);
    /** Add one node:link record to the walk info cache */
    protected abstract void addWalkInfoDistribution(int srcId, int linkId, int[]destId, float[] totalWeightSoFar);
    /** Finish initialization of immutable walk info cache */
    protected abstract void finishWalkInfo();
    
    protected abstract void finishLoad();
    
    public void load(File sizeFileName,File linkFileName,File nodeFileName,File walkFileName)
        throws IOException, FileNotFoundException 
    { 
        String line;
        String[] parts;

        // read the statistics on the array sizes
        LineNumberReader sizeIn = new LineNumberReader(new FileReader(sizeFileName));
        line = sizeIn.readLine();
        parts = line.split(" ");
        int numLinks = StringUtil.atoi(parts[0]);
        int numNodes = StringUtil.atoi(parts[1]);
        sizeIn.close();
        
        log.info("creating compact graph with "+numLinks+" links and "+numNodes+" nodes");
        initLoad(numLinks,numNodes);
        
        initLinks();
        LineNumberReader linkIn = new LineNumberReader(new FileReader(linkFileName));
        int id = 0;
        int linkcount=-1;
        String last=null;
        for (linkcount=0; (line = linkIn.readLine())!=null; linkcount++) {
        	addLink(++id, line);
            if (last != null && line.compareTo(last)<0) {
            	log.warn("Link file "+linkFileName+" must be in lexicographical order. Line "+linkcount+" '"
            			+line+"' should fall before previous line '"+last+"'");
            }
            last = line;
        }
        linkIn.close();
        if (linkcount != numLinks) {
        	log.warn("Size file has "+numLinks+" for the number of link types but "
        			+linkcount+" link types found in link file "+linkFileName
        			+". This could cause null pointer exceptions later; please fix!");
        }

        ProgressCounter npc = new ProgressCounter("loading "+nodeFileName,"lines");
        LineNumberReader nodeIn = new LineNumberReader(new FileReader(nodeFileName));
        id = 0;
        initNodes();
        int nodecount=-1;
        last = null;
        for (nodecount=0; (line = nodeIn.readLine())!=null; nodecount++) {
        	addNode(++id, line);
            npc.progress();
            if (last != null && line.compareTo(last)<0) {
            	log.warn("Node file "+nodeFileName+"  must be in lexicographical order. Line "+nodecount+" '"
            			+line+"' should fall before previous line '"+last.toString()+"'");
            }
        }
        if (nodecount != numNodes) {
        	log.warn("Size file has "+numNodes+" for the number of node names but "
        			+nodecount+" node names found in node file "+nodeFileName
        			+". This could cause null pointer exceptions later; please fix!");
        }
        npc.finished();
        nodeIn.close();

        ProgressCounter wpc = new ProgressCounter("loading "+walkFileName,"lines");
        LineNumberReader walkIn = new LineNumberReader(new FileReader(walkFileName));
        initWalkInfo(); // template call
        int l=0;
        Set<Integer> links = null; int lastSrc=-1; int srcId = -2;
		for (int linenum=0; (line = walkIn.readLine())!=null; linenum++) {  l++;      
			try {
				//			parts = line.split("\\s+");
				//			int cursor = 0;
				TokenData token = nextToken(line,0); //cursor=token.nextIndex;
				srcId = Util.atoi(token.token); 
	
				if (srcId != lastSrc) {
					if (links != null) 
						putWalkInfoLinks(lastSrc, links);
					links = new TreeSet<Integer>();
					lastSrc = srcId;
				}
	
				token = nextToken(line,token.nextIndex); //cursor = token.nextIndex;
				int linkId = Util.atoi(token.token); links.add(linkId);
				
				token = nextToken(line,token.nextIndex); //cursor = token.nextIndex;
				int numDest = Util.atoi(token.token);
				
				int[] destId = new int[numDest];
				
				float[] totalWeightSoFar = new float[numDest];
				float tw = (float)0.0;
				
				token = nextToken(line,token.nextIndex); //cursor = token.nextIndex;
				for (int k=0; token != null; k++) {
					TokenData weightToken = backToken(token.token,':',token.token.length());
					//				String[] destWeightParts = token.token.split(":");
					destId[k] = Util.atoi(token.token.substring(0,weightToken.nextIndex));
					tw += StringUtil.atof(weightToken.token);
					totalWeightSoFar[k] = tw;
					token = nextToken(line,token.nextIndex);
				}
				addWalkInfoDistribution(srcId, linkId, destId, totalWeightSoFar);
				wpc.progress();
			} catch (RuntimeException e) {
				log.error("Runtime error on line "+linenum+": "+line);
				throw(e);
			}
//            parts = line.split("\\s+");
//            try {
//	            int srcId = StringUtil.atoi(parts[0]);
//	            int linkId = StringUtil.atoi(parts[1]);
//	            int numDest = StringUtil.atoi(parts[2]);
//	            int[] destId = new int[numDest];
//	            float[] totalWeightSoFar = new float[numDest];
//	            float tw = (float)0.0;
//	            int k = 0;
//	            for (int i = 3; i < parts.length; i++) {
//	                String[] destWeightParts = parts[i].split(":");
//	                destId[k] = StringUtil.atoi(destWeightParts[0]); 
//	                tw += StringUtil.atof(destWeightParts[1]);
//	                totalWeightSoFar[k] = tw;
//	                k++;
//	            }
//	            addWalkInfoDistribution(srcId,linkId,destId,totalWeightSoFar); // template call
//	            wpc.progress();
//            } catch (IllegalArgumentException e) {
//            	log.error("Improper format for line "+l+": "+line,e);
//            	throw e;
//            }
        }
		putWalkInfoLinks(srcId,links);
        finishWalkInfo(); // template call
        wpc.finished();
        walkIn.close();
        
        finishLoad();
    }

	@Override
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
	

	protected class TokenData {
		public String token;
		public int nextIndex;
		public TokenData(String t, int n) {token=t; nextIndex=n;}
	}
	protected TokenData nextToken(String line, int startAt) {
		int len = line.length();
		int i=startAt; if (i>=len) return null;
		StringBuilder sb = new StringBuilder();
		//1: skip delimiter chars at the head of the string
		for (;line.charAt(i) <= SPACE_WORDCHAR; i++) if ((i+1)>=len) return null;
		//2: accumulate chars until we see a delim char
		for (;i<len;i++) {
			char c = line.charAt(i);
			if (c > SPACE_WORDCHAR) sb.append(c);
			else break;
		}
		return new TokenData(sb.toString(),i);
	}
	protected TokenData backToken(String line, char delim, int startAt) {
		int len = line.length();
		int i=startAt; if (i<=0) return null;
		if (i>=len) i=len-1;
		StringBuilder sb = new StringBuilder();
		//1: skip delimiter chars at the head of the string
		for (;line.charAt(i) == delim; i--) if ((i-1)<0) return null;
		//2: accumulate chars until we see a delim char
		for (;i>=0;i--) {
			char c = line.charAt(i);
			if (c != delim) sb.append(c);
			else break;
		}
		return new TokenData(sb.reverse().toString(),i);
	}
}
