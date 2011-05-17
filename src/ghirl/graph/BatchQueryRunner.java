package ghirl.graph;

import ghirl.util.Distribution;
import ghirl.util.TreeDistribution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;

import ml.options.OptionSet;
import ml.options.Options;
import ml.options.Options.Multiplicity;
import ml.options.Options.Separator;

import org.apache.log4j.Logger;

import bsh.EvalError;
import bsh.Interpreter;

public class BatchQueryRunner {
	private static final GraphIdResultProcessor DEFAULT_PROCESSOR = new GraphIdResultProcessor() {
		public boolean processAsDistribution() { return true; }
		public Distribution processResults(Distribution d, Graph g) { return d; }
		public Distribution processResult(GraphId i) { return new TreeDistribution(i); }
	};
	private static final Logger logger= Logger.getLogger(BatchQueryRunner.class);
	private File queryFile, outFile, resultsProcessorBshfile, verboseFile;
	private boolean verbose=false;
	private Graph graph;
	private GraphIdResultProcessor resultsProcessor = DEFAULT_PROCESSOR;
	
	public BatchQueryRunner(String ... args) {
		Options options = new Options(args,0);
		options.getSet()
			.addOption("query", Separator.BLANK)
			.addOption("out",Separator.BLANK,Multiplicity.ZERO_OR_ONE)
			.addOption("v",Separator.BLANK,Multiplicity.ZERO_OR_ONE)
			.addOption("processor",Separator.BLANK,Multiplicity.ZERO_OR_ONE)
			.addOption("graph",Separator.BLANK);
		if(!options.check()) {
			StringBuilder sb = new StringBuilder("{");
			for(String arg : args) sb.append("'").append(arg).append("', ");
			sb.reverse().replace(0, 1, "}").reverse();
			System.err.println(sb.toString());
			System.err.println(options.getCheckErrors());
			System.err.println("Usage:\n\t" +
					"-query queryFile\n\t" +
					"[-out outFile]\n\t" +
					"[-processor processorFile]\n\t" +
					"[-v verboseOutputFile]\n\t"+
					"-graph \"<graph options>\"\n");
			System.exit(0);
		} OptionSet set = options.getSet();
		
		this.setQueryFile(new File(set.getOption("query").getResultValue(0)));
		if (set.isSet("out")) {
			this.setOutFile(new File(set.getOption("out").getResultValue(0)));
		} else { this.setOutFile(new File("batchQueryOutput.txt")); }
		if (set.isSet("v")) {
			this.verboseFile = new File(set.getOption("v").getResultValue(0));
			this.verbose = true;
		}
		if (set.isSet("processor")) {
			this.setResultsProcessorBshfile(new File(set.getOption("processor").getResultValue(0)));
		}
		this.setGraph(
				GraphFactory.makeGraph(
						set.getOption("graph").getResultValue(0)
						.replaceAll("^\"", "")
						.replaceAll("\"$", "")
						.split(" ")));
	}
	
	public BatchQueryRunner() {}
	private String getNodeList(Distribution results) {
		StringBuilder sb = new StringBuilder();
		for (Iterator it = results.iterator(); it.hasNext();) { GraphId node = (GraphId) it.next();
			sb.append("\n\t").append(node.toString());
			if (node.getFlavor().equals(TextGraph.TEXT_TYPE))
				sb.append("\t").append(graph.getTextContent(node));
		}
		return sb.toString();
	}
	public void run() {
		logger.info("Running queries from "+queryFile.getPath()+" on graph "+graph.toString()+";\n"
				+ " writing output to file "+outFile.getPath());
		Writer writer=null, verboseWriter = null;
		BufferedReader reader=null;
		
		if (!queryFile.exists()) {
			logger.error("Query file "+queryFile.getAbsolutePath()+" does not exist!");
			return;
		}
		
		try {
			writer = new FileWriter(outFile);
			if (verbose) verboseWriter = new FileWriter(verboseFile);
			reader = new BufferedReader(new FileReader(queryFile));
			String line;
			writer.write("# raw results, # final results (as filtered by "
					+ (resultsProcessor.equals(DEFAULT_PROCESSOR) ? "the default processor" : resultsProcessorBshfile.getPath())
					+ ")\n");
			int nqueries=0;
			while ( (line = reader.readLine()) != null) { line = line.trim();
				if (line.startsWith("#")) continue;
				 nqueries++;
				logger.info("Query: "+line);
				if (verbose) verboseWriter.write("# "+line+"\n");
				Distribution rawResults = graph.asQueryDistribution(line);
				Distribution finalResults;
				if (resultsProcessor.processAsDistribution()) {
					logger.debug("Processing distribution...");
					finalResults = resultsProcessor.processResults(rawResults, graph);
				} else {
					logger.debug("Processing one at a time...");
					finalResults = new TreeDistribution();
					for (Iterator it = rawResults.iterator(); it.hasNext();) { GraphId node = (GraphId) it.next();
						Distribution d = resultsProcessor.processResult(node);
						finalResults.addAll(d.getTotalWeight(), d);
					}
				}
				if (logger.isDebugEnabled()) { // don't want to call the iterators if we're not debugging
					logger.debug(rawResults.size()+ " raw results:"+getNodeList(rawResults));
					logger.debug(finalResults.size()+ " final results:"+getNodeList(finalResults));
				} else {
					logger.info(rawResults.size()+" raw results");
					logger.info(finalResults.size()+" final results");
				}
				if(verbose) {
					for(Iterator it = finalResults.iterator(); it.hasNext(); ) {
						GraphId node = (GraphId) it.next();
						verboseWriter.write(node.toString());
						verboseWriter.write("\n");
					}
				}
				writer.write(rawResults.size()+","+finalResults.size()+"\n");
			}
			logger.info(nqueries+" queries completed.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null)
			try { writer.close(); } catch (IOException e) {
				e.printStackTrace();
			}
		if (reader != null)
			try { reader.close(); } catch (IOException e) {
				e.printStackTrace();
			}
		if (verboseWriter != null)
			try { verboseWriter.close(); } catch (IOException e) {
				e.printStackTrace();
			}
	}
	public File getQueryFile() {
		return queryFile;
	}
	public void setQueryFile(File queryFile) {
		this.queryFile = queryFile;
	}
	public File getOutFile() {
		return outFile;
	}
	public void setOutFile(File outFile) {
		this.outFile = outFile;
	}
	public File getResultsProcessorBshfile() {
		return resultsProcessorBshfile;
	}
	public void setResultsProcessorBshfile(File resultsProcessorBshfile) {
		this.resultsProcessorBshfile = resultsProcessorBshfile;
		Interpreter interpreter = new Interpreter(); 
		try {
			resultsProcessor = (GraphIdResultProcessor) interpreter.eval(new FileReader(resultsProcessorBshfile));
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Invalid results processor setting:",e);
		} catch (EvalError e) {
			throw new IllegalStateException("Couldn't instantiate results processor:",e);
		}
	}
	public Graph getGraph() {
		return graph;
	}
	public void setGraph(Graph graph) {
		this.graph = graph;
	}
	public static void main(String[] args) {
		new BatchQueryRunner(args).run();
	}
	
	public interface GraphIdResultProcessor {
		public Distribution processResults(Distribution results, Graph g);
		public Distribution processResult(GraphId result);
		public boolean processAsDistribution();
	}
}
