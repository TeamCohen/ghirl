package ghirl.util;

import edu.cmu.minorthird.util.IOUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Given a file that includes Stanford dependency parses, this file creates
 * a corresponding input file to be loaded into Ghirl.
 *
 * You can try running this with the following input text file (containing one sentence):
 *
 * Both/DT the0AFL-CIO/JJ and/CC the0National0Association0of0Manufacturers/JJ are/VBP calling/VBG for/IN measures/NNS to/TO control/VB rising/VBG costs/NNS ,/, improve/VB quality/NN and/CC provide/VB care/NN to/TO the/DT 31/CD million/CD Americans/NNPS who/WP currently/RB lack/VBP health/NN insurance/NN ./.
 * [dep(the0AFL-2, Both-1), nsubj(calling-6, the0AFL-2), conj_and(the0AFL-2, the0National0Association0of0Manufacturers-4), aux(calling-6, are-5), prep_for(calling-6, measures-8), aux(control-10, to-9), xcomp(calling-6, control-10), amod(costs-12, rising-11), dobj(control-10, costs-12), conj_and(control-10, improve-14), dobj(improve-14, quality-15), conj_and(control-10, provide-17), dobj(provide-17, care-18), det(Americans-23, the-20), num(Americans-23, 31-21), number(31-21, million-22), prep_to(provide-17, Americans-23), nsubj(lack-26, who-24), advmod(lack-26, currently-25), rcmod(Americans-23, lack-26), nn(insurance-28, health-27), dobj(lack-26, insurance-28)]
 *
 * @Author: Einat Minkov
 */

public class Stanford2GraphFile
{
    String STOP_WORDS_FILE = "lib/stopwords.txt";  // may also want to represent numbers uniformly
    Set stopWords = new HashSet();
    boolean ELIMINATE_STOP_WORDS = true;
    boolean ADD_DIRECT_CONJ_EDGES = true;

    Set relationsKnown = new HashSet(); // record isa pos relations that were already output

    int SENT_ID;

    public Stanford2GraphFile() {
        populateStopWords();
        SENT_ID = 1;
    }

    private void populateStopWords(){
        try{
            BufferedReader br = new BufferedReader(new FileReader(new File(STOP_WORDS_FILE)));
            while (br.ready()) stopWords.add(br.readLine());
        }catch(Exception e){
            System.out.println("Error is accessing lib/stopwords.txt");
        }
    }


    public void processFile(File file) throws Exception{
        BufferedReader br = new BufferedReader(new FileReader(file));
        Map textPOS = new HashMap();
        Set SENT_entities = new HashSet();
        while (br.ready()){
            String line = br.readLine();
            if ((line.split(" ")[0]).indexOf("/")>-1){
                textPOS.clear();
                String[] toks = line.split(" ");
                for (int i=0; i<toks.length; i++){
                    String tokPOS = toks[i];
                    String word = tokPOS.split("/")[0]+"-"+(i+1);
                    String POSraw = "";
                    if (tokPOS.split("/").length>1) POSraw = tokPOS.split("/")[1];
                    String POS = POSraw.length()>=2? POSraw.substring(0,2) : "";
                    if (POS.length()>0 && POS!=null) textPOS.put(word,POS);
                }
            }

            Set sentRelations = new HashSet();
            Map conjEntities = new HashMap();

            if (line.startsWith("[")){
                SENT_entities.clear();
                line = cleanLine(line);
                //System.out.println(line);
                StringTokenizer st = new StringTokenizer(line);
                while (st.hasMoreTokens()){
                    String relation = st.nextToken();
                    while (relation.indexOf("-")>-1) relation = st.nextToken();
                    String entity1 = st.nextToken();
                    String entity2 = st.nextToken();
                    String parsed = relationToString2(relation,entity1,entity2,textPOS);
                    if (!relation.startsWith("conj")){
                        if (!relation.equals("contains"))
                            sentRelations.add(relation+" "+entity1+"-"+SENT_ID+ " "+entity2+"-"+SENT_ID);
                    }
                    else conjEntities.put(entity1,entity2);
                    if (parsed !=null) System.out.println(parsed);
                }
                SENT_ID++;
            }

            // do some extra processing...
            // whenever there is a conj - connect both arguments *directly* to
            // the verb/noun/associated argument (i.e., add an edge)
            if (ADD_DIRECT_CONJ_EDGES){
            for (Iterator it=conjEntities.keySet().iterator();it.hasNext();){
                String entity1 = (String)it.next();
                String entity2 = (String)conjEntities.get(entity1);
                //System.out.println("CONJUNCTIONS " + entity1 + " " + entity2);
                try{
                if (!textPOS.get(entity1).equals("VB")&&!textPOS.get(entity2).equals("VB")){
                    for (Iterator it2=sentRelations.iterator();it2.hasNext();){
                        String l = (String)it2.next();
                        if (l.indexOf(entity1)>0)
                            System.out.println("edge " + l.replaceAll(entity1,entity2));
                        if (l.indexOf(entity2)>0)
                            System.out.println("edge " + l.replaceAll(entity2,entity1));
                    }
                }} catch (Exception e) {;}
            }
            }
        }
    }


    public String relationToString(String relation, String a, String b, Map textPOS){
        a = a.toLowerCase();
        b = b.toLowerCase();
        String a_val = mentionVal(a);
        String b_val = mentionVal(b);
        if (ELIMINATE_STOP_WORDS){
            if (stopWords.contains(a_val) || stopWords.contains(b_val)) return null;
        }
        if (a_val.trim().length()==0 || b_val.trim().length()==0) return null;

        String POS_a = (String)textPOS.get(a);
        String POS_b = (String)textPOS.get(b);
        if (POS_a==null) POS_a="UNK";
        if (POS_b==null) POS_b="UNK";

        //String term_isa_POS1 = "edge isa TERM$" + a.split("-")[0] + " " + POS_a;
        //String term_isa_POS2 = "edge isa TERM$" + b.split("-")[0] + " " + POS_b;

        String term_isa_POS1 = "edge isa " + mentionVal(a) + " " + POS_a;
        String term_isa_POS2 = "edge isa " + mentionVal(b) + " " + POS_b;

        a = "TEXT$"+a+"-"+SENT_ID;
        b = "TEXT$"+b+"-"+SENT_ID;

        String node_a = "node " + a + " " + a_val + "\n";
        String node_b = "node " + b + " " + b_val + "\n";

        String relStr = new String();
        if (!relationsKnown.contains(node_a.trim())){
            relStr = relStr.concat(node_a);
            relationsKnown.add(node_a.trim());
        }
        if (!relationsKnown.contains(node_b.trim())){
            relStr = relStr.concat(node_b);
            relationsKnown.add(node_b.trim());
        }
        relStr = relStr.concat("edge " + relation + " " + a + " " + b);
        // add POS information
        if (!relationsKnown.contains(term_isa_POS1.trim())){
            relStr = relStr.concat("\n" + term_isa_POS1);
            relationsKnown.add(term_isa_POS1.trim());
        }
        if (!relationsKnown.contains(term_isa_POS2.trim())) {
            relStr = relStr.concat("\n" + term_isa_POS2);
            relationsKnown.add(term_isa_POS2.trim());
        }
        return relStr;
    }


     public String relationToString2(String relation, String a, String b, Map textPOS){
        a = a.toLowerCase();
        b = b.toLowerCase();
        String a_val = mentionVal(a);
        String b_val = mentionVal(b);
        if (ELIMINATE_STOP_WORDS){
            if (stopWords.contains(a_val) || stopWords.contains(b_val)) return null;
        }
        if (a_val.trim().length()==0 || b_val.trim().length()==0) return null;

        String POS_a = (String)textPOS.get(a);
        String POS_b = (String)textPOS.get(b);
        if (POS_a==null) POS_a="UNK";
        if (POS_b==null) POS_b="UNK";

        //String term_isa_POS1 = "edge isa TERM$" + a.split("-")[0] + " " + POS_a;
        //String term_isa_POS2 = "edge isa TERM$" + b.split("-")[0] + " " + POS_b;

        String term_isa_POS1 = "edge isa " + mentionVal(a) + " " + POS_a;
        String term_isa_POS2 = "edge isa " + mentionVal(b) + " " + POS_b;

        a = a+"-"+SENT_ID;
        b = b+"-"+SENT_ID;

        String node_a = "edge contains " + a + " " + a_val + "\n";
        String node_b = "edge contains " + b + " " + b_val + "\n";

        String relStr = new String();
        if (!relationsKnown.contains(node_a.trim())){
            relStr = relStr.concat(node_a);
            relationsKnown.add(node_a.trim());
        }
        if (!relationsKnown.contains(node_b.trim())){
            relStr = relStr.concat(node_b);
            relationsKnown.add(node_b.trim());
        }

        relStr = relStr.concat("edge " + relation + " " + a + " " + b);
        // add POS information
        if (!relationsKnown.contains(term_isa_POS1.trim())){
            relStr = relStr.concat("\n" + term_isa_POS1);
            relationsKnown.add(term_isa_POS1.trim());
        }
        if (!relationsKnown.contains(term_isa_POS2.trim())) {
            relStr = relStr.concat("\n" + term_isa_POS2);
            relationsKnown.add(term_isa_POS2.trim());
        }
        return relStr;
    }


    private String cleanLine(String line){
        line = line.replaceAll("\\[","").replaceAll("\\]","");
        line = line.replaceAll("\\("," ").replaceAll("\\)"," ");
        line = line.replaceAll("0 -","0-");
        line = line.replaceAll(",","");
        return line;
    }

    private String mentionVal(String str){
        return str.split("-")[0];
    }


    public static void main(String[] args) throws Exception{
        File file = null;
        try{ file = new File(args[0]);
        } catch(Exception e){ System.out.println("USAGE: INPUT-PARSE-FILE"); }

        Stanford2GraphFile s = new Stanford2GraphFile();
        s.processFile(file);
    }

}
