package ghirl.learn;

import java.io.*;
import java.util.Iterator;

import ghirl.graph.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;

public class StochasticGradientLinearRegressor 
{
    private static final boolean SQUARE_NORM_PENALTY=false;
    private static final boolean NORM_PENALTY=true;

    static public class Batch extends BatchBinaryClassifierLearner
    {
	private Dataset testDataset = null;
	int epochs = 10;
	private Hyperplane theHyperplane;
	private StochasticGradientLinearRegressor.Online onlineRegressor = 
	    new StochasticGradientLinearRegressor.Online();

	public Batch() {;}

	public void setTestDataset(Dataset testDataset) { this.testDataset=testDataset; }
	public int getEpochs() { return epochs; }
	public void setEpochs(int epochs) { this.epochs=epochs; }
	public double getPenalty(double p) { return onlineRegressor.getPenalty(); }
	public void setPenalty(double p) { onlineRegressor.setPenalty(p); }

	public Classifier batchTrain(Dataset dataset)
	{
	    onlineRegressor.reset();
	    for (int i=0; i<epochs; i++) {
		dataset.shuffle();
		onlineRegressor.setLearningRate( 1.0/(i+1.0) );
		for (Iterator<Example> j=dataset.iterator(); j.hasNext(); ) {
		    Example ex = j.next();
		    onlineRegressor.addExample(ex);
		}
		theHyperplane = onlineRegressor.getHyperplane();
		System.out.print("epoch "+i+" norm: "+onlineRegressor.getHyperplaneNorm()+
				 " RMSE: "+rootMeanSquaredError(theHyperplane,dataset));
		if (testDataset!=null) 
		    System.out.print(" testRMSE: "+rootMeanSquaredError(theHyperplane,testDataset));
		System.out.println();
	    }
	    return theHyperplane;
	}

	public Hyperplane getHyperplane() 
	{ 
	    return theHyperplane; 
	}

	public double estimateRMSE(Dataset d,Splitter splitter)
	{
	    double rmseSum = 0;
	    Dataset.Split split = d.split(splitter);
	    for (int i=0; i<split.getNumPartitions(); i++) {
		Dataset train = split.getTrain(i);
		Dataset test = split.getTest(i);
		setTestDataset(test);
		Hyperplane w = (Hyperplane)new DatasetClassifierTeacher(train).train(this);
		double rmse = rootMeanSquaredError( w, test );
		System.out.println("rmse on split "+(i+1)+"/"+split.getNumPartitions()+": "+rmse);
		rmseSum += rmse;
	    }
	    return rmseSum/split.getNumPartitions();
	}
    }

    /** StochasticGradientLinearRegressor.Online is an online regression method. */
    static public class Online extends OnlineBinaryClassifierLearner
    {
	private Hyperplane w_t; 
	private double learningRate = 0.1;
	private double penalty = 1.0;
	private double normSquared = 0;

	public Online() {;}

	public Online(double learningRate,double penalty)
	{
	    this.learningRate = learningRate; 
	    this.penalty = penalty;
	}

	public double getLearningRate() { return learningRate; }
	public void setLearningRate(double rate) { this.learningRate = rate; }
	public void setPenalty(double p) { this.penalty = p;}
	public double getPenalty() { return penalty; }

	public void reset() 
	{
	    w_t = new Hyperplane();
	    normSquared = 0;
	}
	public void addExample(Example example)
	{
	    // this will be used to keep the hyperplane weights from getting too big
	    double norm = Math.sqrt(normSquared); 

	    double y_t = example.getLabel().posWeight();
	    double predicted_y_t = w_t.score(example.asInstance());
	    double error = (y_t - predicted_y_t);

	    /*
	    // virtually normalize examples to unit length
	    double exampleNormSquared = 0;
	    for (Feature.Looper i=example.featureIterator(); i.hasNext(); ) {
		Feature f = i.nextFeature();
		double e_f = example.getWeight(f);
		exampleNormSquared += e_f*e_f;
	    }
	    double exampleNorm = Math.sqrt(exampleNormSquared);
	    predicted_y_t /= exampleNorm;
	    */

	    //System.out.println("y = "+y_t+" yhat = "+predicted_y_t);
	    for (Iterator<Feature> i=example.featureIterator(); i.hasNext(); ) {
		Feature f = i.next();
		double w_f = w_t.featureScore(f);
		double e_f = example.getWeight(f); // was divided by exampleNormSquared;
		double delta = error * e_f;
		if (NORM_PENALTY && norm!=0) delta -= (penalty*w_f/norm);
		else if (SQUARE_NORM_PENALTY) delta += (penalty*w_f);
		double correction =  delta * learningRate;
		w_t.increment( f, correction );
		//System.out.println("incremented "+f+"` by "+correction+" to "+w_t.featureScore(f));
		normSquared -= w_f*w_f;
		normSquared += (w_f+correction)*(w_f+correction);
		//if (norm!=0) System.out.println("  - delta "+delta+" "+f+" penalty: "+(penalty*w_f/norm));
	    }

	}
	public Classifier getClassifier() 
	{
	    return getHyperplane();
	}

	public Hyperplane getHyperplane()
	{
	    return w_t;
	}

	public double getHyperplaneNorm()
	{
	    return Math.sqrt(normSquared);
	}

	public String toString() { return "[StochasticGradientLinearRegressor.Online]"; }
    }

    static public double rootMeanSquaredError(Hyperplane w,Dataset d)
    {
	double sum = 0;
	double tot = 0;
	for (Iterator<Example> i=d.iterator(); i.hasNext(); ) {
	    Example ex = i.next();
	    double y = ex.getLabel().posWeight();
	    double predicted = w.score(ex.asInstance());
	    sum += (y-predicted)*(y-predicted);
	    tot++;
	}
	return Math.sqrt(sum/tot);
    }

    static public void main(String[] args) throws IOException
    {
	Dataset d = (Dataset)IOUtil.loadSerialized(new File(args[0]));
	StochasticGradientLinearRegressor.Batch regressor = new StochasticGradientLinearRegressor.Batch();
	double rmse = regressor.estimateRMSE(d,new CrossValSplitter());
	System.out.println("average rmse: "+rmse);
    }
}
