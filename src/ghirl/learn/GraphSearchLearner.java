package ghirl.learn;

import ghirl.graph.*;
import edu.cmu.minorthird.classify.*;


public interface GraphSearchLearner
{
    public void setGraph(Graph graph);
    public void setInstanceMachine(InstanceMachine machine);
    public InstanceMachine getInstanceMachine();
    public Walker getWalker();
    public GraphSearcher batchTrain(GraphSearchDataset dataset);
}
