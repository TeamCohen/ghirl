package ghirl.learn;

import java.util.*;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.gui.*;
import ghirl.graph.*;
import ghirl.util.*;

/**
 * An implementation of a Minorthird Instance that is based on a Distribution
 * over features. 
 */
public class DistributionInstance implements Instance, Visible
{
    private GraphId id;
    private Distribution d;
    private String subpop;

    public DistributionInstance(GraphId id,String subpop,Distribution d)
    { 
	    this.id=id; this.subpop=subpop; this.d=d;
    }

    public void add(double weight, Object obj){
        d.add( weight, obj);
    }
    public Object getSource() { return id; }
    public String getSubpopulationId() { return subpop; }
    public Feature.Looper featureIterator() { return new Feature.Looper(d.iterator()); }
    public Feature.Looper numericFeatureIterator() { return new Feature.Looper(d.iterator()); }
    public Feature.Looper binaryFeatureIterator() {return new Feature.Looper(new HashSet().iterator()); }
    public double getWeight(Feature f) { return d.getProbability(f);}
    public Viewer toGUI() { return new GUI.InstanceViewer(this); }
    public String toString() { return "[DistInstance "+id+","+subpop+": "+d+"]"; }
}
