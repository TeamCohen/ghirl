/*
 *  Memstore.java
 *  
 *
 *  Created by Cathrin Weiss   
 *  http://www.ifi.uzh.ch/ddis/people/weiss/
 *	mailto:weiss@ifi.uzh.ch
 *  (c) University of Zurich, 2009
 //---------------------------------------------------------------------------
 // This work is licensed under the Creative Commons
 // Attribution-Noncommercial-Share Alike 3.0 Unported License. To view a copy
 // of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/
 // or send a letter to Creative Commons, 171 Second Street, Suite 300,
 // San Francisco, California, 94105, USA.
 //---------------------------------------------------------------------------
 *
 */

package ghirl.persistance;


import java.io.*;
import java.util.*;


import ghirl.graph.GraphId;
import ghirl.util.*;
public class Memstore implements IGraphStore {
	private native int addAnEdge(String a, String b, String c);
	private native int addAProp(String a, String b, String c);
	private native int addANode(String a, String b);
	private native String getProp(String a, String b);
	private native String[] getSet(String a, String b);
	private native String[] getNodes();
	private native String[] getLabels(String a);
	private native String getNode(String a);
	private native void writeAll();
	private native int containsNode(String a);
	private native void setBase(String a);

	static {
		System.loadLibrary("memstore");
    }
	
	public Memstore(){
	}
	
	public Memstore(String basename){
		setBase(basename);
	}
	
	public void add_Edge(String a, String b, String c){
		
		int abc = addAnEdge(a,b,c);

        
    }
	
	public void add_Node(String a, String b){
		int abc = addANode(a,b);
	}
	
	public void add_Prop(String a, String b, String c){
		int abc = addAProp(a,b,c);
	}
	
	public String get_Prop(String a, String b){
		return getProp(a,b);
	}
	
	public String get_Node(String a){
		return getNode(a);
	}
	
	public String[] getResultSetArray(String a, String b) {
		return getSet(a,b);
	}
	
	public String[] getNodesArray(){
		return getNodes();
	}

	public Set get_Labels(String a){
		Set labels = new HashSet<String>();
		String ls[] = getLabels(a);
		for (int i = 0; i < ls.length ; i++)
			labels.add(ls[i]);
		
		return labels;
	}
	
	public boolean contains_Node(String a){
		int abc = containsNode(a);
		return (abc > 0);
	}
	
	public void writeToDB(){
		// nothing
	}
	
	@Override
	public Iterator getNodesIterator() {
		String []nodes = getNodesArray();
		Set s = new HashSet<String>();
		for (int i = 0 ; i < nodes.length; i++)
			s.add(nodes[i]);
		return s.iterator();
	}
	@Override
	public Set getResultSet(String from, String linkLabel) {
		String []idStrings = getResultSetArray(from, linkLabel);
		if (idStrings == null)
			return Collections.EMPTY_SET;
		if (idStrings.length == 0)
			return Collections.EMPTY_SET;
		Set accum = new HashSet();
		for (int i = 0; i < idStrings.length; i++){
			accum.add(GraphId.fromString(idStrings[i]));
		}
		return accum;
	}
}

	

