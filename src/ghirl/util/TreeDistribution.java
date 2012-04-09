package ghirl.util;

import java.util.*;

/**
 * A binary tree implementation of Distribution.
 * Only Comparable objects can be stored in this Distribution.
 *
 * <p>This isn't a balanced tree implementation, so systematically
 * ordered additions/deletions should be avoided.
 */
public class TreeDistribution extends Distribution
{
	public static final Distribution EMPTY_DISTRIBUTION = new TreeDistribution();

	private Random random = new Random(0);
	int numberOfElements = 0;

	// indicates if the 'theLastWeight' variable has a meaningful
	// value.  one should set this to true whenever you set
	// theLastWeight
	private boolean theLastWeightIsValid = false;

	//
	// a node in the binary tree
	//
	final private static class TreeNode 
	{
		TreeNode left=null, right=null; 
		// depth of this tree
		int depth = 0;
		Comparable obj;
		// localWeight is the weight associated with the object stored
		// in this node.
		double localWeight;
		// totalWeight is the total weight of all objects stored in
		// the tree rooted at this node.
		double totalWeight;

		public TreeNode(double w,Object obj)
		{
			if (!(obj instanceof Comparable)) 
				throw new IllegalArgumentException("objects in TreeDistribution must be Comparable!: "+obj);
			this.obj = (Comparable)obj;
			this.localWeight = this.totalWeight = w;
			this.depth = 1;
		}
		// make sure the totalWeight is consistent, assuming the left
		// and right subtrees are consistent
		final void updateWeights() 
		{
			totalWeight = localWeight + safeTotalWeight(left) + safeTotalWeight(right);
			depth = Math.max( safeDepth(left), safeDepth(right) )+1;
		}
		final static double safeTotalWeight(TreeNode node) { return node==null ? 0 : node.totalWeight; }
		public String toString() { return "tn("+localWeight+"/"+totalWeight+", d="+depth+", "+obj+")"; };
	}

	final static int safeDepth(TreeNode node) { return node==null ? 0 : node.depth; }

	// root of the tree that stores this distribution
	private TreeNode root = null;

	//
	// pretty-printer, for debugging
	//
	public String toDebugString() 
	{ 
		StringBuffer buf = new StringBuffer();
		toDebugString(buf,0,root); 
		return buf.toString();
	}
	private void toDebugString(StringBuffer buf,int tab,TreeNode root)
	{
		for (int i=0; i<tab; i++) buf.append("|  ");
		if (root==null) buf.append(".\n"); 
		else {
			buf.append("root: "+root.toString()+"\n");
			toDebugString(buf,tab+1,root.left);
			toDebugString(buf,tab+1,root.right);
		}
	}

	//
	// constructors
	//

	public TreeDistribution() { ; }

	/** Convenience method to create a singleton distribution. */
	public TreeDistribution(Object obj) { add(1.0,obj); }

	//
	// addition to the tree
	//

	final public void add(double weight,Object obj)
	{
		root = treeAdd(weight,obj,root);
		//checkReducedTree("removing",obj);
	}

	final private TreeNode treeAdd(double w, Object obj, TreeNode root)
	{
		if (root==null) {
			theLastWeightIsValid = true;
			theLastWeight = w;
			numberOfElements++;
			return new TreeNode(w,obj);
		} else {
			int cmp = root.obj.compareTo(obj);
			if (cmp<0) {
				root.left = treeAdd(w,obj,root.left);
				root = rebalanceTree(root);
			} else if (cmp>0) {
				root.right = treeAdd(w,obj,root.right);
				root = rebalanceTree(root);
			} else { 
				root.localWeight += w;
				theLastWeightIsValid = true;
				theLastWeight = root.localWeight;
			}
			root.updateWeights();
			return root;
		}
	}

	final private TreeNode rebalanceTree(TreeNode root)
	{
		int leftDepth = safeDepth(root.left);
		int rightDepth = safeDepth(root.right);
		//System.err.println("rebalance: "+leftDepth+"/"+rightDepth);
		if (leftDepth > 2*rightDepth+100 || rightDepth>2*leftDepth+100) {
			//System.err.println("rebalancing tree: L="+leftDepth+", R="+rightDepth);
			List accum = new ArrayList();
			treeToList(root,accum);
			Collections.shuffle(accum);
			TreeDistribution balanced = new TreeDistribution();
			for (Iterator i=accum.iterator(); i.hasNext(); ) {
				TreeNode nd = (TreeNode)i.next();
				balanced.add( nd.localWeight, nd.obj );
			}
			return balanced.root;
		} else {
			return root;
		}
	}

	//
	// access to objects in the tree.  weight of zero would be mean
	// the object was not present.
	//

	final public double getWeight(Object obj)
	{
		return treeGetWeight(obj,root);
	}

	final private double treeGetWeight(Object obj, TreeNode root)
	{
		if (root==null) return 0;
		else {
			int cmp = root.obj.compareTo(obj);
			if (cmp<0) return treeGetWeight(obj,root.left);
			else if (cmp>0) return treeGetWeight(obj,root.right);
			else return root.localWeight; 
		}
	}

	//
	// remove objects from the tree
	//

	// cache the object removed by treeRemove
	private Object theRemovedObject = null;

	final public Object remove(Object obj)
	{
		//System.out.println("before removal:\n"+toDebugString());
		root = treeRemove(obj, root);
		//checkReducedTree("removing",obj);
		return theRemovedObject;
	}

	final private void checkReducedTree(String lastOp,Object operand)
	{
		Comparable last = null;
		for (Iterator i = iterator(); i.hasNext(); ) {
			Object obj = i.next();
			if (last!=null && last.compareTo(obj)<=0) {
				System.out.println(toDebugString());
				System.out.println(last+" <= "+obj+": "+last.compareTo(obj));
				throw new IllegalStateException("tree out of order after "+lastOp+" "+operand);
			}
			last = (Comparable)obj;
		}
	}

	final private TreeNode treeRemove(Object obj, TreeNode root)
	{
		if (root==null) return null;
		else {
			int cmp = root.obj.compareTo(obj);
			if (cmp<0) {
				root.left = treeRemove(obj,root.left);
				root.updateWeights();
				return root;
			} else if (cmp>0) {
				root.right = treeRemove(obj,root.right);
				root.updateWeights();
				return root;
			} else {
			        // we're definitely removing something, so record that
                                numberOfElements--;
                            
				// easy cases first...
				if (root.left==null) return root.right;
				else if (root.right==null) return root.left;

				// pick an extremal node from the left or right
				// subtree - randomly chosen
				boolean goLeft = (random.nextDouble()>0.5);
				if (goLeft) root.left = removeMax(root.left);
				else root.right = removeMin(root.right);

				// replace the contents of the root with the
				// contents of the removed node
				theLastWeightIsValid = true;
				theLastWeight = root.localWeight;
				theRemovedObject = root.obj;
				root.localWeight = removedExtremalNode.localWeight;
				root.obj = removedExtremalNode.obj;
				root.updateWeights();
				return root;
			}
		}
	}

	//
	// snip off the minimum/maximum tree nodes, and
	// return the result.
	//

	private TreeNode removedExtremalNode = null;

	final private TreeNode removeMin(TreeNode root)
	{
		if (root==null) {
			throw new IllegalStateException("oops");
		} else if (root.left!=null) {
			root.left = removeMin(root.left);
			root.updateWeights();
			return root;
		} else {
			removedExtremalNode = root;
			return root.right;
		}
	}

	final private TreeNode removeMax(TreeNode root)
	{
		if (root==null) {
			throw new IllegalStateException("oops");
		} else if (root.right!=null) {
			root.right = removeMax(root.right);
			root.updateWeights();
			return root;
		} else {
			removedExtremalNode = root;
			return root.left;
		}
	}

	/** Return an iterator over all objects.
	 */
	final public Iterator iterator() 
	{
		List accum = new ArrayList();
		treeToList(root, accum);
		return new MyIterator(accum.iterator());
	}

	final public int size()
	{
		return numberOfElements;
	}

	final private void treeToList(TreeNode root,List accum)
	{
		if (root==null) return;
		else {
			treeToList(root.left,accum);
			accum.add( root );
			treeToList(root.right,accum);
		}
	}

	// converts an iterator over TreeNode's to one over objects, which
	// keeps theLastWeight up-to-date
	final private class MyIterator implements Iterator {
		private Iterator i;
		private TreeNode node;
		public MyIterator(Iterator i) { this.i = i; }
		public void remove() { i.remove(); }
		public boolean hasNext() { return i.hasNext(); }
		public Object next() { 
			node = (TreeNode)i.next(); 
			theLastWeightIsValid = true;
			theLastWeight = node.localWeight; 
			return node.obj; 
		}
	}

	final public double getLastWeight() 
	{ 
		if (!theLastWeightIsValid) throw new IllegalStateException("no lastWeight to return");
		return theLastWeight; 
	}

	final public double getTotalWeight() 
	{ 
		return root==null ? 0.0 : root.totalWeight; 
	}

	final public Distribution copy() 
	{
		Distribution result = new TreeDistribution();
		result.addAll(1.0, this);
		return result;
	}


	//
	// sample from the tree
	//

	public Object sample(Random rand)
	{
		double r = rand.nextDouble() * root.totalWeight;
		return treeSample(root, r);
	}

	public String toString() 
	{
		StringBuffer buf = new StringBuffer();
		buf.append("[TreeDist:");
		for (Iterator i=iterator(); i.hasNext(); ) {
			Object obj = i.next();
			buf.append(" "+obj.toString()+":"+getLastWeight());
		}
		buf.append("]");
		return buf.toString();
	}

	// sample from the tree rooted at this node
	private Object treeSample(TreeNode root, double r)
	{
		if (root==null) {
			throw new IllegalStateException("this shouldn't happen!");
		}
		double leftTot = TreeNode.safeTotalWeight(root.left);
		double rightTot = TreeNode.safeTotalWeight(root.right);
		double local = root.localWeight;
		if (r<leftTot) return treeSample(root.left, r);
		else if (r>leftTot+local) return treeSample(root.right, r-leftTot-local);
		else {
			theLastWeightIsValid = true;
			theLastWeight = root.localWeight;
			return root.obj;
		}
	}

	//
	// test code
	//

	public static void main(String arg[])
	{
		TreeDistribution d = new TreeDistribution();
		for (int i=0; i<10; i++) {
			System.out.println("adding "+i);
			d.add(1.0,new Integer(i));
			System.out.println("tree:\n" + d.toDebugString());
		}
		/*
	Random r = new Random();
	TreeDistribution d = new TreeDistribution();
	// check adds
	for (int i=0; i<10; i++) {
	    Integer k = new Integer(r.nextInt(100));
	    double w = r.nextInt(10)+1;
	    System.out.println("adding w,k = "+w+","+k);
	    d.add(w,k);
	}
	System.out.println("tree:\n" + d.toDebugString());
	// check lastWeight
	Set accum = new HashSet();
	for (Iterator i=d.iterator(); i.hasNext(); ) {
	    Integer k = (Integer)i.next();
	    double w = d.getLastWeight();
	    System.out.println("retrieved w,k = "+w+","+k);
	    double w2 = d.getWeight(k);
	    System.out.println("check w2=w: "+w2+" = "+w);
	    if (k.intValue()%2!=0) {
		accum.add(k);
	    }
	}
	// check remove
	for (Iterator i=accum.iterator(); i.hasNext(); ) {
	    Integer k = (Integer)i.next();
	    Integer k2 = (Integer)d.remove(k);
	    System.out.println("check k2=k: "+k2+" = "+k);
	}
	System.out.println("odd tree:\n" + d.toDebugString());
	// check sampling, accumulating weights
	d = new TreeDistribution();
	for (int i=0; i<10; i+=2) {
	    d.add(i+1, new Integer(i));
	}
	for (int i=1; i<10; i+=2) {
	    d.add(i+1, new Integer(i));
	}
	for (int i=5; i<10; i++) {
	    d.add(i+2, new Integer(i));
	}
	//
	System.out.println("tree:\n"+d.toDebugString());
	int[] sample = new int[10];
	for (int m=0; m<10000; m++) {
	    Integer k = (Integer)d.sample(r);
	    sample[ k.intValue() ]++;
	}
	for (int i=0; i<10; i++) {
	    System.out.println("i: "+i
			       +"\tex: "+d.getProbability(new Integer(i))
			       +"\tobs: "+sample[i]/10000.0);
	}
		 */
	}
}
