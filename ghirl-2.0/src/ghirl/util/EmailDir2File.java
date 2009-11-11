package ghirl.util;

import edu.cmu.minorthird.util.IOUtil;

import java.io.*;
import java.util.*;

/**
 * This code does the following:
 *
 * 1. Pre-process a directory of emails into an input graph file that can be fed into ghirl.
 * (named "emailAsGraph.txt")
 *
 * Note that you may want to adapt header parsing slightly to your files format. For example, if the recipient names
 * in your files include commas (e.g.: Minkov, Einat) and are separated by ';', then you can modify the
 * parseRecipients method (which currently considers commas as a recipient separator).
 * The current considered formats should fit Enron emails.
 *
 * 2. Output the header-truncated the email files into a prallel directory
 * (named as the original directory with a "_Ghirl" suffix). It is recommended to feed the modified files
 * into Ghirl to avoid re-processing the header as text by the Lucene indexing implemented in Ghirl.
 *
 * Note that is is possible to eliminate reply lines in this process. The boolean variable 'inclueReplyLines'
 * controls this property.
 *
 * This code doesn't explicitly model relations between date expressions. However, it includes the infra-structure
 * required to add this to the graph file (addDateEdges method).
 *
 * @Author: Einat Minkov
 *
 */

public class EmailDir2File
{
        private String doc;
        private Date date = new Date();
        private int day, month, year;
        private Set sender = new HashSet();
        private Set recipients = new HashSet();
        private List text = new LinkedList();

        private static final String FILE_TYPE = "FILE";
        private static final String TEXT_TYPE = "TEXT";
        private static String PERSON_TYPE = "PERSON";
        private static String EMAIL_TYPE = "EMAIL-ADDRESS";
        private static String DATE_TYPE = "DATE";

        private static boolean inclueReplyLines = true;


        public EmailDir2File() {;}

        public EmailDir2File(String doc) {
            this.doc = doc;
            this.processHeader();}

        public class Recipient{
            private String emailAddress;
            private String name;

            public Recipient(String name,String emailAddress){
                this.emailAddress = emailAddress;
                this.name = name;  }

            public String getEmailAddress(){return emailAddress;}
            public String getName(){return name;}
        }

        // Getters
        public Set getSender(){ return sender; }
        public Set getRecipients(){ return recipients; }
        public Date getDate(){ return date; }
        public String getDateString(){ return new String(month+"/"+day+"/"+year); }


        private void processHeader(){
            try{
            BufferedReader in = new BufferedReader(new StringReader(doc));
            String line = in.readLine();
            boolean fromFilled=false,dateFilled=false,toFilled=false,ccFilled=false;
            boolean keepReading = true;
            while (line!=null && keepReading){
                String first;
                try{ first = ((line.split("[\\s\\t]+")[0]).toLowerCase());}
                catch (Exception e) { first = ""; }
                if (first.startsWith("from:") && !fromFilled){
                    sender=parseSender(line);  fromFilled=true; }
                else if (first.startsWith("to:") && !toFilled){
                    recipients=parseRecipients(line); toFilled=true; }
                else if ((first.startsWith("cc:") || first.startsWith("bcc:")) && !ccFilled){
                    recipients.addAll(parseRecipients(line)); ccFilled=true; }
                else if ((first.startsWith("sent:") || first.startsWith("date:") || first.startsWith("time:")) && !dateFilled){
                    date=parseDate(line); dateFilled=true; }
                else if (line.length()>0){
                    boolean endOfOriginalMsg = isEndOfOriginalMsg(line);
                    if (endOfOriginalMsg && !inclueReplyLines) keepReading=false;
                    if (!((isReplyLine(line) || endOfOriginalMsg) && !inclueReplyLines))
                        text.add(line);
                }
                line = in.readLine();
                }
            }
            catch (Exception e){
                System.out.println("lucille.email.process()" + doc + e.toString());
            }
        }

        // a parser of recipients lines that fits the current format.
        private Set parseSender(String line){
            if ((line.startsWith("From"))) line = line.substring(6).trim();
            Set senders = new HashSet();
            String[] tokens = line.split("[\\s\\t]+");
            String name="", emailAddress="", emailAddress2="";
            int j=0;
            if (tokens.length>3){
                if ((new String(tokens[1]+tokens[2]+tokens[3])).equals("onbehalfof")){
                    emailAddress=tokens[0];
                    for (int i=4; i<tokens.length; i++){
                        String tok = strip(tokens[i]);
                        if (tok.matches("^(.+)@(.+)$")){ emailAddress2 = tok; j=i;}
                    }
                    name = strip(tokens[4]);
                    for (int i=5;i<j;i++) name = name.concat(" ").concat(tokens[i]);
                }
            }
            if (emailAddress.length()==0){
                for (int i=0; i<tokens.length; i++){
                    String tok = strip(tokens[i]);
                    if (tok.matches("^(.+)@(.+)$")){ emailAddress = tok; j=i;}
                }
                if (j!=0) name = strip(tokens[0]);
                for (int i=1;i<j;i++) name = name.concat(" ").concat(strip(tokens[i]));
            }
            senders.add(new Recipient(name.trim().toLowerCase(),emailAddress.toLowerCase()));
            if (emailAddress2.length()>0) senders.add(new Recipient(name.toLowerCase(),emailAddress2));
            return senders;
        }


        private Set parseRecipients(String line){
            Set recipients = new HashSet();
            if ((line.startsWith("To") || line.startsWith("Cc") || line.startsWith("Bcc")))
                line = line.substring(3).trim();
            StringTokenizer st = new StringTokenizer(line);
            String name="", emailAddress="";
            while (st.hasMoreTokens()){
                String tok=st.nextToken().trim();
                if (tok.endsWith(";") || tok.endsWith(",") || !st.hasMoreTokens() || tok.matches("^(.+)@(.+)$")){
                    tok = strip(tok);
                    if (tok.matches("^(.+)@(.+)$")) emailAddress = tok;
                    else name = name.concat(" ").concat(tok);
                    recipients.add(new Recipient(name.trim().toLowerCase(),emailAddress.toLowerCase()));
                    name=""; emailAddress="";
                }
                else name = name.concat(" ").concat(strip(tok));
            }
            return recipients;
        }


        private Date parseDate(String line){
            StringTokenizer st = new StringTokenizer(line);
            Calendar cal = new GregorianCalendar();
            String[] months = new String[]{"jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec"};
            while (st.hasMoreTokens()){
                String tok = strip(((String)st.nextToken()));
                if (tok.matches("\\d+/\\d+/\\d+")){
                    String[] splitTok = tok.split("/");
                    month = (new Integer(splitTok[0])).intValue();
                    day = (new Integer(splitTok[1])).intValue();
                    year = (new Integer(splitTok[2])).intValue();
                }
                else if (tok.matches("\\d{2}")) day=Integer.parseInt(tok);
                else if (tok.matches("\\d")) day=Integer.parseInt(tok);
                else if (tok.matches("[12]\\d{3}")) year=Integer.parseInt(tok);
                else{ for (int i=0; i<months.length;i++)
                        if (tok.toLowerCase().startsWith(months[i])) month=i+1;
                }
            }
            cal.set(year,month-1,day);
            Date date = new Date(cal.getTimeInMillis());
            return date;
        }



        // strip the string from attached punctuation marks
        public String strip(String w){
            w = w.toLowerCase();
            if (w.matches("\\p{Punct}+")) return "";
            boolean done = false;
            while (!done && w.length()>0){
                if (w.substring(0,1).matches("\\p{Punct}"))
                    w = w.substring(1,w.length());
                else done = true;
                if (w.length()>0){
                    if (w.substring(w.length()-1,w.length()).matches("\\p{Punct}")){
                        w = w.substring(0,w.length()-1);
                        done = false;}
                }
            }
            if (w.startsWith("mailto:")) w=w.substring(7); //relevant for recipients lines
            return w;
        }


    private boolean isReplyLine(String line){
        if (line.length()>0){
            if (line.startsWith(">")) return true;
        }
        return false;
    }

    private boolean isEndOfOriginalMsg(String line){
        if (line.length()>0){
            if (line.endsWith("wrote:")) return true;
            else if ((strip(line).equals("original message"))) return true;
        }
        return false;
    }



    private static void printRelation(String relation, String srcObject, String targetObject, BufferedWriter bw) throws IOException{
        bw.write("edge "+ relation+" "+srcObject+" "+targetObject); bw.newLine();
    }

    private static void printNode(String nodeName, BufferedWriter bw) throws IOException{
        if (nodeName.startsWith(TEXT_TYPE)){
            String text = nodeName.substring(5).replaceAll("_"," ");
            bw.write("node " + nodeName + " " + text);
        }
        else bw.write("node "+ nodeName);
        bw.newLine();
    }



    private static String getText(File file)
    {
        String documentContents = "";
        try {
        documentContents = IOUtil.readFile(file);
        } catch (Exception ex) {
        System.err.println("Error accessing file "+file+": "+ex);
        documentContents = ex.toString();
        }
        return documentContents;
    }

    private static boolean isNew(Set knownEntities, String entity){
        if (!knownEntities.contains(entity)) {
            knownEntities.add(entity);
            return true;        }
        return false;
    }


    private double deltaDays(Date start, Date end){
        long millis_per_day = 1000*60*60*24;
        long deltaMillis = end.getTime()-start.getTime();
        return deltaMillis/millis_per_day;
    }


    // Define date relations
    private void addDateEdges(Set datesSet, BufferedWriter bw){
        int dateCardinality = datesSet.size();
        Date[] dates = new Date[datesSet.size()];
        int i=0;
        //Fill in all dates into a sorted array
        for (Iterator it=datesSet.iterator(); it.hasNext();)
            dates[i] = (Date)it.next();
        Arrays.sort(dates);
        //Add crossing edges btw dates. At current config - only going back in time.
        for (i=dateCardinality-1; i>0; i--){
            int j=1, lastGap=0;
            //System.out.println(dates[i-j].toString());
            while (j<i && lastGap<30){
                Date from = dates[i];
                Date to = dates[i-j];
                //System.out.println(dates[i-j].toString() + " " + dates[i].toString());
                double diff = deltaDays(dates[i-j],dates[i]);
                //System.out.println("delta_days " + deltaDays(dates[i-j],dates[i]));
                if (diff<2){
                    // print to file an edge between from and to dates, with relation 'date_diff_2'
                }
                else if (diff<4){
                    // print to file an edge between from and to dates, with relation 'date_diff_3'
                }
                else if (diff<7){
                    // etc
                }
                else if (diff<15){
                    // etc
                }
                lastGap = (int)diff;  j++;
            }
        }
    }


    public static void main(String[] args){
        File emailDir = null;
        Set knownEntities = new HashSet();
        Set dates = new HashSet();
        try{
            emailDir = new File(args[0]);
            new File(args[0]+"_Ghirl").mkdir();
            File graphFile = new File("email_as_graph.txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(graphFile));
            String[] files = emailDir.list();
            for (int i=0; i<files.length; i++){
                File file = new File(emailDir+"/"+files[i]);
                File noHeaderFile = new File(emailDir+"_Ghirl/"+files[i]);
                EmailDir2File e2f = new EmailDir2File(getText(file));

                // Write the file parsed data into the Ghirl graph file

                // define file
                String fileNode = new String(FILE_TYPE+"$"+file.toString());
                printNode(fileNode,bw);
                // recipients
                for (Iterator it=e2f.getRecipients().iterator();it.hasNext();){
                    Recipient recipient = (Recipient)it.next();
                    String rec_name = recipient.getName().replaceAll(" ","_");
                    String recNode = new String(TEXT_TYPE+"$"+rec_name);
                    String rec_email = recipient.getEmailAddress();
                    boolean newName = isNew(knownEntities,rec_name);
                    boolean newEmail = isNew(knownEntities,rec_email);
                    if (rec_name.length()>0){
                        if (newName){
                            printNode(recNode,bw);
                            printRelation("isa",recNode,PERSON_TYPE,bw);
                        }
                        printRelation("sentTo",fileNode,recNode,bw);
                    }
                    if (rec_email.length()>0){
                        if (newEmail){
                            printRelation("isa",rec_email,EMAIL_TYPE,bw);
                            printRelation("sentToEmail",fileNode,rec_email,bw);
                        }
                    }
                    if ((rec_name.length()>0) && (rec_email.length()>0)){
                        if (newName || newEmail){
                            printRelation("alias",recNode,rec_email,bw);
                        }
                    }
                }
                // sender
                for (Iterator it=e2f.getSender().iterator();it.hasNext();){
                    Recipient sender = (Recipient)it.next();
                    String sender_name = sender.getName().replaceAll(" ","_");
                    String senderNode = new String(TEXT_TYPE+"$"+sender_name);
                    String sender_email = sender.getEmailAddress();
                    boolean newName = isNew(knownEntities,sender_name);
                    boolean newEmail = isNew(knownEntities,sender_email);
                    if (sender_name.length()>0){
                        if (newName) {
                            printNode(senderNode,bw);
                            printRelation("isa",senderNode,PERSON_TYPE,bw); }
                        printRelation("sentFrom",fileNode,senderNode,bw);
                    }
                    if (newEmail){
                        if (isNew(knownEntities,sender_email)) {
                            printRelation("isa",sender_email,EMAIL_TYPE,bw); }
                        printRelation("sentFromEmail",fileNode,sender_email,bw);
                    }
                    if ((sender_name.length()>0) && (sender_email.length()>0)){
                        if (newName || newEmail){
                            printRelation("alias",senderNode,sender_email,bw);
                        }
                    }
                }
                // date
                String date = e2f.getDateString();
                if (isNew(knownEntities,date)) {
                    printRelation("isa",date,DATE_TYPE,bw);
                    dates.add(e2f.getDate());
                }
                printRelation("sentOnDate",fileNode,date,bw);

                // Create a similar file without the header (so that the header will not be handled by Lucene)
                BufferedWriter bwTmp = new BufferedWriter(new FileWriter(noHeaderFile));
                for (int j=0; j<e2f.text.size();j++){
                    bwTmp.write((String)e2f.text.get(j)+"\n");
                }
                bwTmp.close();
            }

            // can add date relations here.
            // addDateEdges(dates,bw);

            bw.close();

        } catch (Exception e){
            System.out.println("Please pecify a valid directory containing your email files.");
            System.out.println(e.toString());
        }
    }

}
