package ghirl.PRA.util;
import java.util.Random;
public class FRand {
	public static Random rand = new Random(2);
	public static boolean drawBoolean(double p){
		return rand.nextDouble()<p;
	}
	public static int drawBinary(double p){
		return drawBoolean(p)?1:0;
	}
	
	// assume min=0
	public static int drawInt(int max){
		return (int) Math.floor(rand.nextDouble()*max);
	}
	public static int drawInt(int min, int max){
		return drawInt(max-min)+min;
	}
}
