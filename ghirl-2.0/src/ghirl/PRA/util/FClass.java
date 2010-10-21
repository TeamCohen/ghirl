/**
 * 
 */
package ghirl.PRA.util;

/**
 * @author nlao
 *
 */
public class FClass {
	public static Object newInstance(Class cInSamePackage, String c)
	throws Exception{
		String cn= cInSamePackage.getPackage().getName()+"."+c;
		return Class.forName(cn).newInstance();
	}	
	public static Object newValue(Class c){
		try{			
			Object o=c.newInstance();
			return o;
		}
		catch (Exception e){
			System.out.println(e.getClass().getName());
			e.printStackTrace();			
		}
		return null;
	}
}
