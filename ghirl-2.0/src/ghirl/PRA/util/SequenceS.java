package ghirl.PRA.util;


/**
 * this is a sequence of strings
 * which is special because we know how to 
 * convert it from/to other types
 * @author nlao
 *
 */
public class SequenceS extends TSequence<String>{ 	
	//private String fn;
	
	/*public SequenceS(String fn){
		super(String.class, fn);
	}*/
	public SequenceS(Iterable<String> v ) {
		super(String.class,v);
	}
	public <T1> SequenceS(Iterable v, Pipe<T1,String> f) {
		super(String.class,v,f);
	}
	public SequenceS(String fn ) {
		super(fn);
	}
	public static PipeSI pipeSI= new PipeSI();
	public static PipeSD pipeSD= new PipeSD();
	
	public TSequence<Integer> toSeqI(){
		return new TSequence<Integer>(Integer.class,this, pipeSI);
	}
	public TSequence<Double> toSeqD(){
		return new TSequence<Double>(Double.class,this, pipeSD);
	}

}