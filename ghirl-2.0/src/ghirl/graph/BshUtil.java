package ghirl.graph;

import ghirl.util.Config;

import java.io.*;

import bsh.EvalError;
import bsh.Interpreter;

public class BshUtil
{
	
	private static Object bshFile(Interpreter interp, String s) throws EvalError, IOException {
		String dbDir = ghirl.util.Config.getProperty("ghirl.dbDir");
		File bshfile = new File(s);
		if (!bshfile.exists()) {
			bshfile = new File(dbDir+File.separator+s);
			if (!bshfile.exists()) {
				throw new FileNotFoundException("No file "+s+" in working directory or ghirl.dbDir!");
			}
		}
		String parent =bshfile.getParent();
		if (parent != "") interp.eval("cd(\""+parent+"\");");
		return interp.source(bshfile.getName());
	}
	private static Object bshStatement(Interpreter interp, String s) throws EvalError {
		if (!s.startsWith("new")) s = "new "+s;
		return interp.eval(s);
	}
	public static <T> T toObject(String s, Class<T> expectedType) { 
		try {
			bsh.Interpreter interp = new bsh.Interpreter();
			interp.eval("import ghirl.graph.*;");
			interp.eval("import ghirl.learn.*;");
			Object raw_result;
			if (s.endsWith(".bsh")) {
				raw_result = bshFile(interp, s);
			} else {
				raw_result = bshStatement(interp, s);
			}

			if (!expectedType.isInstance(raw_result)) {
				throw new IllegalArgumentException(s+" did not produce "+expectedType);
			}
			return (T) raw_result;
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		} catch (bsh.EvalError e) {
			throw new IllegalStateException("Beanshell error for \""+s+"\":",e);
		}
	} 

	static public void main(String[] args)
	{
		for (int i=0; i<args.length; i++) {
			System.out.println(args[i]+" = '" + toObject(args[i],Object.class));
		}
	}
}
