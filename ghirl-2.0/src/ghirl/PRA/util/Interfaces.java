/**
 * 
 */
package ghirl.PRA.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import org.w3c.dom.Element;

/**
 * Interfaces let container to operate on abstract objects
 * @author nlao
 *
 */
public class Interfaces {

	public static interface IFromFile {
/*	public static VectorS fromFile(String fn);

	public static VectorS fromFile(String fn, int iCol);
	public static VectorS fromFile(String fn, int iCol, String sep);
	public static VectorS fromLine(String x, String sep) ;*/
	}
	public static interface IParseLine {
		public boolean parseLine(String line);	
	}
	public static interface IPlusObjOn {
		public Object plusObjOn(Object x);	
	}
	public static interface IPlusObj {
		public Object plusObj(Object x);	
	}
	public static interface IMinusObjOn {
		public Object minusObjOn(Object x);	
	}	
	public static interface IMultiplyOn {
		public Object multiplyOn(Double x);	
	}
	public static interface IMultiply {
		public Object multiply(Double x);
	}
	public static interface ILength {
		public int length();
	}
	public static interface IGetStringByString {
		public String getString(String name);
	}
	
	public static interface IGetStringByInt {
		public String getString(int id);
	}
	public static interface IGetIntByString {
		public Integer getInt(String name);
	}
	public static interface ISetDoubleByString {
		public void setDouble(String name,Double d);
	}
	public static interface ISetIntByString {
		public void setInt(String name, Integer i);
	}
	
	public static interface IGetObjByString {
		public Object getObj(String name);
	}
	public static interface IGetObjByStringInt {
		public Object getObj(String name, int id);
	}
	public static interface IGetDoubleByString {
		public Double getDouble(String name);
	}
	public static interface IGetBooleanByString {
		public Boolean getBoolean(String name);
	}
	public static interface IGetDoubleByInt {
		public Double getDouble(Integer i);
	}
	public static interface IGetClass {	
		//	public  java.lang.Class getClass();	
	}
	
	public static interface ISerializableObj {	
		//http://java.sun.com/developer/technicalArticles/Programming/serialization/

		//private void onWriteObject(ObjectOutputStream out) throws IOException
		//out.defaultWriteObject(); 
		//private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
		
	}
	

	public static interface ICloneable {
		public Object clone()  ;//throws CloneNotSupportedException ;
	}
	public static interface IAddOn {
		public Object addOn(Object x);
		
	}
	public static interface IAdd {
		public Object add(Object x);
	}
	
	/**
	 * write an object into what ever format the user want
	 * @author nlao
	 *
	 */
	public static interface  IWrite {
		public BufferedWriter write(BufferedWriter writer );//throws IOException;
	}
	public static interface IParseXML {		
		public  Object parseXML(Element e);
	}
	public static interface IToXML {		
		public  String toXML();
	}
	
	public static interface IRead {
		public BufferedReader read(BufferedReader reader );// throws IOException;
	}
	
	
}
