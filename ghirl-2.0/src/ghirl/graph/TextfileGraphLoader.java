package ghirl.graph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import org.apache.log4j.Logger;

public class TextfileGraphLoader extends GraphLoader {
	private static final Logger logger = Logger.getLogger(TextfileGraphLoader.class);
	protected Writer writer;
	protected boolean newThing=false;
	public TextfileGraphLoader(MutableGraph g) { super(g); }
	public TextfileGraphLoader(MutableGraph g, File tofile) throws IOException {
		super(g);
		this.writer = new FileWriter(tofile);
	}
	
	public void setTextfile(File tofile) throws IOException {
		this.writer = new FileWriter(tofile);
	}
	
	@Override
	public boolean loadLine(String line) {
		if (super.loadLine(line)) {
			if (newThing) {
				try {
					logger.debug("Writing "+line);
					this.writer.write(line);
					if (!line.endsWith("\n")) this.writer.write("\n");
					this.writer.flush();
				} catch (IOException e) {
					throw new IllegalStateException("Couldn't write to output file!",e);
				}
			}
			newThing = false;
			return true;
		} else return false;
	}
	
	@Override
	protected GraphId createNode(GraphId id, String content) {
		newThing = true;
		return super.createNode(id,content);
	}
	@Override
	protected void addEdge(String linkLabel, GraphId from, GraphId to) {
		if (!newThing) {
			Set dests = graph.followLink(from, linkLabel);
			if (dests == null || !dests.contains(to)) newThing = true;
			graph.melt();
		}
		super.addEdge(linkLabel, from, to);
	}
}
