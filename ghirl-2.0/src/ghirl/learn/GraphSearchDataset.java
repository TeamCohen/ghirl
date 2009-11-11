package ghirl.learn;

import java.util.*;
import edu.cmu.minorthird.classify.*;

/** 
 * The analog of a dataset - basically, a set of GraphSearchExamples.
 *
 * Mostly pilfered from minorthird.classify.BasicDataset.
 */

public class GraphSearchDataset
{
    private List list = new ArrayList();

    public void add(GraphSearchExample example) { list.add(example); }
    public GraphSearchExample.Looper iterator() { return new GraphSearchExample.Looper(list); }
    public int size() { return list.size(); }
    //
    // for experiments
    //
    public void shuffle(Random r) { Collections.shuffle(list,r); }
    public void shuffle() { shuffle(new Random(0)); }
    public GraphSearchDataset shallowCopy() 
    { 
	GraphSearchDataset copy = new GraphSearchDataset();
	for (GraphSearchExample.Looper i=iterator(); i.hasNext(); ) copy.add( i.nextExample() );
	return copy;
    }
    public Split split(final Splitter splitter)
    {
      splitter.split(list.iterator());
      return new Split() {
	      public int getNumPartitions() { return splitter.getNumPartitions(); }
	      public GraphSearchDataset getTrain(int k) { return invertIteration(splitter.getTrain(k)); }
	      public GraphSearchDataset getTest(int k) { return invertIteration(splitter.getTest(k)); }
	  };
    }
   private GraphSearchDataset invertIteration(Iterator i)
   {
      GraphSearchDataset copy = new GraphSearchDataset();
      while (i.hasNext()) copy.add((GraphSearchExample)i.next());
      return copy;
   }

    /** A partitioning into train/test partitions. */
    public interface Split 
    {
	/** Return the number of partitions */
	public int getNumPartitions();
	/** Return a dataset containing the training cases in the k-th split */
	public GraphSearchDataset getTrain(int k);
	/** Return a dataset containing the test cases in the k-th split */
	public GraphSearchDataset getTest(int k);
    }

}
