package ghirl.persistance;
import ghirl.graph.GraphId;

import java.util.Iterator;

import tokyocabinet.BDBCUR;

public class TokyoValueIterator implements Iterator<GraphId> {
	private BDBCUR cursor;
	public TokyoValueIterator(BDBCUR cursor) {
		this.cursor = cursor;
		this.cursor.first();
	}
	public void remove() { throw new UnsupportedOperationException("Can't remove from a Tokyo Cabinet iterator."); }
	public boolean hasNext() {
		return (cursor.key2() != null);
	}
	public GraphId next() {
		String value = cursor.val2();
		cursor.next();
		return GraphId.fromString(value);
	}
}