package ghirl.graph;

import java.io.*;

/** Object that identifies a node in a graph.  A flavor is sort of
 * like an XML namespace, so that nodes from multiple graphs can
 * be thrown together.
*/
public class GraphId implements Comparable,Serializable
{
    public static final String DEFAULT_FLAVOR = "";
    public static char FLAVOR_SEPARATOR = '$';

    private final String flavor,shortName;

    public GraphId(String flavor,String shortName) { this.flavor=flavor; this.shortName=shortName; }

    /** Return a string that says what type of object this id refers to. 
     */
    final public String getFlavor() { return flavor; }

    /** Return a string that identifies the object uniquely among all
     * objects of the same 'flavor' 
     */
    final public String getShortName() { return shortName; }

    final public int sizeInBytes() { return flavor.length() + shortName.length() + 8; }

    //
    // these guys are hashable and comparable also
    //

    final public int hashCode() { return flavor.hashCode() ^ shortName.hashCode(); }

    final public int compareTo(Object o) 
    { 
	GraphId b = (GraphId)o;
	if (b==null) return +1; // why is this needed?
	int cmp1 = flavor.compareTo(b.flavor);
	if (cmp1!=0) return cmp1;
	return shortName.compareTo(b.shortName);
    }

    final public boolean equals(Object o) 
    {
	return compareTo(o)==0;
    }

    public String toString() 
    { 
	return toString(flavor,shortName); 
    }

    static public String toString(String flavor,String shortName)
    {
				return flavor+FLAVOR_SEPARATOR+shortName; 
    }

    static public GraphId fromString( String s )
    {
	String flavor="",shortName="";
	int k = s.indexOf( FLAVOR_SEPARATOR );
	if (k>=0) { flavor = s.substring(0,k); shortName=s.substring(k+1); }
	else { flavor = DEFAULT_FLAVOR; shortName = s; }
	return new GraphId(flavor,shortName);
    }
}
