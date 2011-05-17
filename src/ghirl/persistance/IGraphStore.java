/*
 *  IGraphStore.java
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

import ghirl.graph.Closable;
import ghirl.graph.GraphId;
import ghirl.util.*;
public interface IGraphStore extends Closable {
	
	/**
	 * 
	 * @param from
	 * @param linkLabel
	 * @param to
	 */
	public void add_Edge(String from, String linkLabel, String to);
	
	public void add_Node(String key, String node);
	
	public void add_Prop(String node, String property, String value);
	
	public String get_Prop(String node, String property); 
	
	public String get_Node(String key);
	
	public String[] getResultSetArray(String node, String linkLabel);	

	public Set getResultSet(String node, String linkLabel);
	
	public String[] getNodesArray();
	
	public Iterator getNodesIterator();
	
	public Set get_Labels(String key);	
	
	public boolean contains_Node(String key);
	
	public void writeToDB();
	
	public void close();

	class MyKeyIteratorAdaptor implements Iterator
	{
		private Iterator it;
		public MyKeyIteratorAdaptor(Iterator it) { this.it=it; }
		public void remove() { it.remove(); }
		public boolean hasNext() { return it.hasNext(); }
		public Object next() { return GraphId.fromString((String)it.next()); }
	}
}

	

