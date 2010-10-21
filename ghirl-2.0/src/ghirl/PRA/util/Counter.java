package ghirl.PRA.util;

public class Counter {
	public Counter(int nInterval, char c){
		this.nInterval=nInterval;
		this.c=c;
	}
	public Counter(int nInterval){
		this(nInterval, '.');
	}
	
	public int nInterval;
	public int n=0;
	public char c;
	
	public void step(){
		++n;
		if (n % nInterval==0)
			System.out.print(c);
		
	}
	public void step(char c1){
		System.out.print(c1);
		step();
	}
}
