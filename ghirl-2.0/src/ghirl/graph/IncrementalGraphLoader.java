package ghirl.graph;

import java.io.*;
import java.util.*;

import edu.cmu.minorthird.util.*;

/**
 * Contains a main program for incrementally loading a MutableGraph from a file-based storage.  
 * 
 * @author William Cohen
 */


public class IncrementalGraphLoader implements CommandLineProcessor.Configurable
{
    private String graphName = null;
    private boolean clearBeforeLoading = false;
    private List filesToLoad = new ArrayList();

    // allow command-line configuration
    public class MyCLP extends BasicCommandLineProcessor 
    {
        public void load(String s) { filesToLoad.add(new File(s)); }
        public void clear() { clearBeforeLoading = true; }
        public void graph(String s) { graphName = s; }
        public void usage()
        {
            System.out.println("-graph NAME    graph to extend");
            System.out.println("-clear         empty graph before loading anything");
            System.out.println("-load FILE     load given file - may be used multiple times on command line");
        }
    }
    public CommandLineProcessor getCLP() { return new MyCLP(); }
    
    public void checkArguments()
    {
        if (graphName==null) {
            System.out.println("error: must specify -graph NAME with persistant graphs");
        }
    }

    public MutableGraph doLoading() throws IOException
    {
        if (graphName!=null) {
            char mode = (clearBeforeLoading) ? 'w' : 'a';
            MutableGraph g = new TextGraph(graphName, mode);
            GraphLoader gloader = new GraphLoader(g);
            for (Iterator i=filesToLoad.iterator(); i.hasNext(); ) {
                File f = (File)i.next();
                gloader.load( f );
            }
            g.freeze();
            return g;
        } 
        return null;
    }


    static public void main(String[] args) throws IOException
    {
        IncrementalGraphLoader iloader = new IncrementalGraphLoader();
        iloader.getCLP().processArguments(args);
        iloader.checkArguments();
        iloader.doLoading();
    }
}
