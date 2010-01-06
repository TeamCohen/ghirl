package ghirl.graph;


import java.util.*;
import java.io.*;
import ghirl.util.*;
import edu.cmu.minorthird.util.ProgressCounter;
import com.wcohen.ss.*;
import com.wcohen.ss.lookup.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.api.*;


/**
 * Add links to a graph based on a SoftTFIDFDictionary. 
 *
 */

public class SoftDictAnnotator extends GraphAnnotator
{
    /**
     * @param softDictSaveFile must be a 'saveFile' created with the SoftTFIDFDictionary.saveAs method.
     */
    public SoftDictAnnotator(String linkLabel,String precondition,String softDictSaveFile,double minScore)
        throws IOException,FileNotFoundException
    {
        super(linkLabel,precondition,new SoftDictSearcher(SoftTFIDFDictionary.restore(new File(softDictSaveFile)),minScore));
    }

    public SoftDictAnnotator(String linkLabel,String precondition,SoftTFIDFDictionary dict,double minScore)
    {
        super(linkLabel,precondition,new SoftDictSearcher(dict,minScore));
    }

}
