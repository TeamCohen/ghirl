package ghirl.util;

import ghirl.graph.GraphId;

import java.util.*;

/**
 * A probability distribution of objects that can be sampled from.
 */
public abstract class Distribution implements Iterable
{
    /** caches last weight returned  */
    protected double theLastWeight = 0;

    /** Add the specified object, and give if the appropriate weight
     * in the distribution.  The distribution will always be normalized
     * by the total weight of all objects.  If the object already exists,
     * then its weight will be incremented.
     */
    abstract public void add(double weight,Object obj);

    /** Return the weight associated with this object.
     */
    abstract public double getWeight(Object obj);

    /** Remove an equivalent object.
     */
    abstract public Object remove(Object obj);

    /** Return an iterator over all objects.
     */
    abstract public Iterator iterator();

    /** Return the number of objects that will be produced by the
     * iterator() */
    abstract public int size();


    /** Return the weight associated with the last object
     * that was either: returned with 'get', removed,
     * or returned by an Iterator.
     */
    abstract public double getLastWeight();

    /** Returns the sample probability associated with the last object
     * that was returned with 'get', removed, or returned by an
     * Iterator.  This is the same as getLastWeight/getTotalWeight.
     */
    final public double getLastProbability() { return getLastWeight()/getTotalWeight(); }

    /** Return the total weight of all objects in the distribution */
    abstract public double getTotalWeight();

    /** Pick an object at random, according to its probability. */
    abstract public Object sample(Random rand);

    /** Return the probability of sampling an object */
    final public double getProbability(Object obj)
    {
	return getWeight(obj)/getTotalWeight();
    }


    /** Add everything in the given distribution to this one,
     * in such a way that the total weight of the objects in the given
     * distribution is w.
     */
    final public void addAll(double w,Distribution d)
    {
        double norm = w/d.getTotalWeight();
        for (Iterator i = d.iterator(); i.hasNext(); ) {
            Object obj = i.next();
            double objW = d.getLastWeight();
            add(objW*norm, obj);
        }
    }


    /** Return the set of objects returned by the iterator
     * ordered by decreasing weight
     */
    public Iterator orderedIterator(){
        return orderedIterator(true);
    }


    /** Return the set of objects returned by the iterator
     * ordered by weight (an ascending order is useful for dists of ranks)
     */
    public Iterator orderedIterator(boolean descending)
    {
        final int desc = descending? 1: -1;
        TreeSet set = new TreeSet(new Comparator() {
            public int compare(Object a,Object b) {
                double diff = getWeight(b) - getWeight(a);
                if (diff!=0) return diff*desc>0 ? +1 : -1;
                return a.toString().compareTo(b.toString());
            }
            });
        for (Iterator i = iterator(); i.hasNext(); ) {
            set.add( i.next() );
        }
        return new MyOtherIterator(set.iterator());
    }


    // converts an iterator over Objects and
    // keeps theLastWeight up-to-date
    final private class MyOtherIterator implements Iterator {
        private Iterator i;
        public MyOtherIterator(Iterator i) { this.i = i; }
        public void remove() { throw new UnsupportedOperationException("can't remove!"); }
        public boolean hasNext() { return i.hasNext(); }
        public Object next() {
            Object o = i.next();
            //System.out.println("getting weight of "+o+" in MyOtherIterator");
            theLastWeight = getWeight(o);
            return o;
        }
    }

     /** Given a distribution of weighted objects, return
      * another distribution of the objects ranks. In case several objects
      * have the same weight, the effective rank is considered (middle of
      * the weight block)
     */
   public Distribution rankDistribution()
   {
        Distribution result = new TreeDistribution();
        Iterator i=orderedIterator();
        double lastWeight = 0;
        Set objectsInBlock = new HashSet();
        int lastRankProcessed = 0;
        while (i.hasNext()){
            Object o = i.next();
            double weight = getLastWeight();
            if (weight!=lastWeight){
                // process the current block
                double rankDeFacto = lastRankProcessed + ((double)objectsInBlock.size()+1)/2;
                for (Iterator it=objectsInBlock.iterator(); it.hasNext();) {
                    result.add(rankDeFacto,it.next());
                }
                lastRankProcessed += objectsInBlock.size();
                objectsInBlock.clear();
                lastWeight = weight;
            }
            objectsInBlock.add(o);
        }
        // process the last block
        double rankDeFacto = lastRankProcessed + (objectsInBlock.size()+1)/2;
        for (Iterator it=objectsInBlock.iterator(); it.hasNext();)
        result.add(rankDeFacto,it.next());

        return result;
    }


    /** Copy only the highest-ranking N objects */
    public Distribution copyTopN(int n)
    {
        Distribution result = new TreeDistribution();
        Iterator i=orderedIterator();
        while (i.hasNext() && n>0) {
            Object o = i.next();
            result.add( getLastWeight(), o);
            n--;
        }
        return result;
    }

    /** Make a shallow copy of the distribution */
    abstract public Distribution copy();

    /** Output a printable representation of the distribution */
    public String format()
    {
	java.text.DecimalFormat fmt = new java.text.DecimalFormat("0.000");
	StringBuffer buf = new StringBuffer();
	for (Iterator i=orderedIterator(); i.hasNext(); ) {
	    Object obj = i.next();
	    double w = getLastWeight();
	    buf.append(fmt.format(w*100));
	    buf.append("\t");
	    buf.append(obj.toString());
	    buf.append("\n");
	}
	return buf.toString();
    }


     /** Return the abs. value of the distribution, in terms of weight (sqrt(Sum(w_i^2))
     * used by cosine similarity.
     */
    final public double getAbsWeight()
    {
        double sqrs = 0;
        for (Iterator i=iterator();i.hasNext();)
            sqrs += Math.pow(getWeight(i.next()),2);
        return Math.sqrt(sqrs);
    }



     /** Following is a set of similarity/distance metric functions, comparing with
     * another distribution
     */


     /** Kullback-Leibler divergence:
      *  a measure of the difference between two probability distributions: from a
      * "true" probability distribution P to an arbitrary probability distribution Q.
      * Although it is often intuited as a distance metric, the KL divergence
      * is not a true metric since it is not symmetric (hence 'divergence').
      *
      * The Skew measure (Lee 99, 'Measures of Distributional Similarity'), accounts for
      * zeros in Q by mixing in a small amount of P (in the log's denominator).
      * For alpha=1, this is exactly KL-div.
      * This measure has been shown to be superior to pure KL-div in Lee 2001, 'On the
      * effectiveness of the skew divergence for statistical language analysis' and also
      * in Hughes and Ramage 2007, 'Lexical Semantic Relatedness with Random GraphWalks'
      * Implemented here is Zero-KL variation by Hughes amd Ramagae 07, which builds on and
      * was found better than Lee's measure.
      *
      * Note that the measure may improve tuning gamma.
      **/
     public double KLSkewDiv(Distribution Q){
        double gamma = 40;
        if (Q.size()!=this.size()){
            System.out.println("KL-div. requires distributions to be of the same space (size)");
            return 0;
        } else {
            double div = 0;
            double totalWeightP = getTotalWeight();
            double totalWeightQ = Q.getTotalWeight();
            for (Iterator i=this.iterator(); i.hasNext();){
                GraphId node = (GraphId)i.next();
                double nodeProbQ = Q.getWeight(node)/totalWeightQ;
                double nodeProbP = this.getWeight(node)/totalWeightP;
                if (nodeProbQ>0)
                    //KL-div exactly
                    div += nodeProbP*(Math.log(nodeProbP/nodeProbQ)/Math.log(2));
                else
                    div += nodeProbP*gamma;
            }
            return div;
        }
     }

     public double consineSim(Distribution Q){
        if (Q.size()!=this.size()){
            System.out.println("Cosine sim. requires distributions to be of the same space (size)");
            return 0;
        } else {
            double sim = 0;
            for (Iterator i=this.iterator(); i.hasNext();){
                GraphId node = (GraphId)i.next();
                sim += Q.getWeight(node) * this.getWeight(node);
            }
            sim = sim/(getAbsWeight()*Q.getAbsWeight());
            return sim;
        }
     }

}
