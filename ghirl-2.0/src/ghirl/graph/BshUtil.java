package ghirl.graph;

import ghirl.util.Config;

import java.io.*;

public class BshUtil
{
	static public Object toObject(String s,Class expectedType) 
	{
		try {
			bsh.Interpreter interp = new bsh.Interpreter();
			interp.eval("import ghirl.graph.*;");
			interp.eval("import ghirl.learn.*;");
			if (s.endsWith(".bsh")) {
//				interp.set("bsh.cwd", Config.getProperty("ghirl.dbDir"));
				interp.eval("cd(ghirl.util.Config.getProperty(\"ghirl.dbDir\"));");
				interp.eval("print(bsh.cwd);");
				return interp.source(s);
			} else {
				if (!s.startsWith("new")) s = "new "+s;
				Object o = interp.eval(s);
				if (!expectedType.isInstance(o)) {
					throw new IllegalArgumentException(s+" did not produce "+expectedType);
				}
				return o;
			}
		} catch (IOException e) {
			System.out.println("=====bsh error====");
			System.out.println("the file '"+s+"' was not found");
			System.out.println("=====end bsh error====");
			throw new IllegalArgumentException("error parsing '"+s+"':\n"+e);
		} catch (bsh.EvalError e) {
			System.out.println("=====bsh error====");
			e.printStackTrace();
			System.out.println("=====end bsh error====");
			throw new IllegalArgumentException("error parsing '"+s+"':\n"+e);
		}
	} 

	static public void main(String[] args)
	{
		for (int i=0; i<args.length; i++) {
			System.out.println(args[i]+" = '" + toObject(args[i],Object.class));
		}
	}
}
