package ghirl.graph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.log4j.Logger;

public class TeeGraphLoader extends GraphLoader {
		private static final Logger logger = Logger.getLogger(TeeGraphLoader.class);
		protected Writer writer;
		public TeeGraphLoader(MutableGraph g) { super(g); }
		public TeeGraphLoader(MutableGraph g, File tofile) throws IOException {
			super(g);
			this.writer = new FileWriter(tofile);
		}
		public TeeGraphLoader(MutableGraph g, Writer w) {
			super(g);
			this.writer = w;
		}
		
		public void setTextfile(File tofile) throws IOException {
			this.writer = new FileWriter(tofile);
		}
		public void flush() throws IOException {
			this.writer.flush();
		}
		public void close() throws IOException {
			this.writer.close();
		}
		
		@Override
		public boolean loadLine(String line) {
			if (super.loadLine(line)) {
				try {
					logger.debug("Writing "+line);
					this.writer.write(line);
					if (!line.endsWith("\n")) this.writer.write("\n");
					this.writer.flush();
				} catch (IOException e) {
					throw new IllegalStateException("Couldn't write to output file!",e);
				}
				return true;
			} else return false;
		}
}
