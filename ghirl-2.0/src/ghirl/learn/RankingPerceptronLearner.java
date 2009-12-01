package ghirl.learn;

import ghirl.util.*;
import ghirl.graph.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.transform.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;

import java.util.*;
import java.io.*;
import org.apache.log4j.*;

/**
 * A perceptron-based ranking method.
 */

public class RankingPerceptronLearner extends BatchBinaryClassifierLearner 
{
	private static Logger log = Logger.getLogger(RankingPerceptronLearner.class);

	private int numEpochs;
	boolean pushAllExamples;
	private InstanceTransform transformer;
	private boolean transform2Logs;

	public RankingPerceptronLearner() 
	{ 
		this(200,false,true);
	}
	public RankingPerceptronLearner(int numEpochs)
	{
		this(numEpochs,false,true);
	}

	public RankingPerceptronLearner(int numEpochs,boolean pushAllExamples,boolean transform2Logs) 
	{ 
		this.numEpochs=numEpochs;
		this.pushAllExamples=pushAllExamples;
		this.transform2Logs = transform2Logs;
		if (transform2Logs) transformer = new LogTransform();
		else transformer = new IdentityTransform();
	}

	public Classifier batchTrain(Dataset originalData) 
	{
		Dataset data = transformer.transform(originalData);
		new ViewerFrame("transformed dataset",new SmartVanillaViewer(data));
		Hyperplane h = new Hyperplane();
		Hyperplane s = new Hyperplane();
		Map bySubpopMap = ClassifierRerankerLearner.sortBySubpop(data);
		ProgressCounter pc = new ProgressCounter("perceptron training", "epoch", numEpochs);
		int numUpdates = 0;
		for (int e=0; e<numEpochs; e++) {
	    int epochUpdates = 0;
	    log.info("epoch "+e+"/"+numEpochs);
	    for (Iterator i=bySubpopMap.keySet().iterator(); i.hasNext(); ) {
            String subpop = (String)i.next();
            List subdata = (List)bySubpopMap.get(subpop);
            log.info("subpop "+subpop+" has "+subdata.size()+" examples");
            int subpopUpdates = batchTrainSubPop( subpop, h, s, subdata );
            log.info("subpop "+subpop+" caused "+subpopUpdates+" updates");
            numUpdates += subpopUpdates;
            epochUpdates += subpopUpdates;
	    }
	    pc.progress();
	    log.info("epoch "+e+"/"+numEpochs+" forced "+epochUpdates+" updates");
		}
		pc.finished();
		// turn sum hyperplane into an average
		s.multiply( 1.0/((double)numUpdates) );
		return new TransformingClassifier(s, transformer);
	}

	// return the number of times h has been updated
	private int batchTrainSubPop( String subpop, Hyperplane h, Hyperplane s, List subdata )
	{
		int updates = 0;
		sortByScore(h,subdata);
		if (pushAllExamples) {
            boolean positiveExampleEncountered = false;
            boolean negativeExampleEncountered = false;
            for (int i=0; i<subdata.size(); i++) {
                Example exi = (Example)subdata.get(i);
                if (exi.getLabel().isNegative() && !positiveExampleEncountered) {
                    // try and push this example down
                    //System.out.print(" neg too hi @ rank "+(i+1));
                    h.increment( exi, -1.0 );
                } else if (exi.getLabel().isPositive() && negativeExampleEncountered) {
                    // try and push this example up
                    //System.out.print(" pos too lo @ rank "+(i+1));
                    h.increment( exi, +1.0 );
                }
                if (exi.getLabel().isPositive()) positiveExampleEncountered=true;
                if (exi.getLabel().isNegative()) negativeExampleEncountered=true;
            }
            s.increment( h );
            updates++;
		} else {
            // look for highest positive example that is below a negative example
            // and the highest negative example
            boolean negativeExampleEncountered = false;
            int highestBadPositiveExample = -1, highestNegativeExample = -1;
            for (int i=0; i<subdata.size(); i++) {
                    Example exi = (Example)subdata.get(i);
                    if (exi.getLabel().isNegative() && !negativeExampleEncountered) {
                        highestNegativeExample = i;
                        log.debug("highest negative example = "+highestNegativeExample);
                    }
                    if (exi.getLabel().isPositive() && negativeExampleEncountered && highestBadPositiveExample<0) {
                        highestBadPositiveExample = i;
                        log.debug("highest positive example after a negative example = "+highestBadPositiveExample);
                    }
                    if (exi.getLabel().isNegative()) negativeExampleEncountered=true;
            }
            if (highestBadPositiveExample>=0) {
                    if (log.isDebugEnabled()) log.debug("neg example at "+highestNegativeExample+" outranks "+highestBadPositiveExample);
                    Example neg = (Example)subdata.get(highestNegativeExample);
                    Example pos = (Example)subdata.get(highestBadPositiveExample);
                    h.increment( neg, -1.0 );
                    h.increment( pos, +1.0 );
            }
                s.increment( h );
                updates++;
		}
		return updates;
	}
	private void sortByScore( final Hyperplane h, List subdata )
	{
		Collections.sort( subdata, new Comparator() {
				public int compare(Object a,Object b) {
					Example exa = (Example)a;
					Example exb = (Example)b;
					double diff = h.score( exb ) - h.score( exa );
					int cmp = diff>0 ? +1 : (diff<0 ? -1: 0 );
					if (cmp!=0) return cmp;
					// sort negative examples above positive if scores are the same
					return (int)(exa.getLabel().numericLabel() - exb.getLabel().numericLabel());
				}
	    });
		if (log.isDebugEnabled()) {
            log.debug("sorted by score: ");
            java.text.DecimalFormat fmt = new java.text.DecimalFormat("0.000");
            for (int i=0; i<subdata.size(); i++) {
                    Example exi = (Example)subdata.get(i);
                    log.debug(fmt.format(h.score(exi))+"\t"+exi.getSource());
            }
		}
	}

    public String toString(){
        return "[Classifier: RankingPerceptronLearner]";
    }

	//
	// machinery for transforming probabilities to log probabilities
	//

	static final public class IdentityTransform extends AbstractInstanceTransform implements Serializable
	{
		static private final long serialVersionUID = 1;
		private final int CURRENT_VERSION_NUMBER = 1;
		public Instance transform(Instance instance) { return instance; }
	}


	static final public class LogTransform extends AbstractInstanceTransform implements Serializable
	{
		static private final long serialVersionUID = 1;
		private final int CURRENT_VERSION_NUMBER = 1;
		public Instance transform(Instance instance) {
            return new LogTransformInstance(instance);
        }
	}

	static final public class LogTransformInstance implements Instance, Serializable
	{
		static private final long serialVersionUID = 1;
		private final int CURRENT_VERSION_NUMBER = 1;
		private Instance inner;
		public LogTransformInstance(Instance inner) { this.inner=inner; }
		public Object getSource() { return inner.getSource(); }
		public String getSubpopulationId() { return inner.getSubpopulationId(); }
		public Iterator<Feature> featureIterator() { return inner.featureIterator(); }
		public Iterator<Feature> binaryFeatureIterator() { return inner.binaryFeatureIterator(); }
		public Iterator<Feature> numericFeatureIterator() { return inner.numericFeatureIterator(); }
		public double getWeight(Feature f) { return Math.log(inner.getWeight(f)); }
		public Viewer toGUI() { return new GUI.InstanceViewer(this); }
		public String toString() { return "[LogTransform "+inner+"]"; }
		public int numFeatures() { return inner.numFeatures(); }
	}
}
