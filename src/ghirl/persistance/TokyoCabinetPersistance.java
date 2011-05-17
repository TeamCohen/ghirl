package ghirl.persistance;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import tokyocabinet.BDB;

public class TokyoCabinetPersistance {
	public static final HashMap<Character,Integer> MODES = new HashMap<Character,Integer>();
	public static final char
		MODE_READ = 'r',
		MODE_WRITE= 'w',
		MODE_APPEND='a';
	static {
		MODES.put(MODE_READ,   BDB.OREADER);
		MODES.put(MODE_WRITE,  BDB.OWRITER | BDB.OTRUNC | BDB.OCREAT);
		MODES.put(MODE_APPEND, BDB.OWRITER | BDB.OCREAT);
	}
	public char mode;
	private Logger logger;
	
	public TokyoCabinetPersistance() {
		this.logger = Logger.getLogger(TokyoCabinetPersistance.class);
	}
	public TokyoCabinetPersistance(Logger l) {
		this.logger = l;
	}

	public BDB initDB(String fqpath, int mode) throws IOException {
		BDB db;
		try {
			db = new BDB();
			logger.debug("Opening Tokyo Cabinet at "+fqpath+" in mode "+mode);
			if(db.open(fqpath,mode)) return db;
			int code = db.ecode();
			switch(code) {
			case BDB.ENOFILE:
				throw new FileNotFoundException("Couldn't open database at "+fqpath+": "+BDB.errmsg(db.ecode()));
			case BDB.ENOPERM:
				throw new IOException("Permission denied on "+fqpath);
			}
			throw new IllegalStateException("Couldn't open database at "+fqpath+": "+BDB.errmsg(db.ecode()));
		} catch (UnsatisfiedLinkError e) {
			throw new IllegalStateException("Tokyo Cabinet requires \"-Djava.library.path=usr/local/lib\""
					+"(or correct path to tokyocabinet.dylib) on command line or in VM arguments.",e);			
		}
	}
	public void freeze(List<BDB> dbs) {
		if(mode != MODE_READ) {
			for (BDB db: dbs) {
				if (!db.sync())
					logger.error("EXCEPTION NEEDED: problem syncing Tokyo Cabinet: "+BDB.errmsg(db.ecode()));
			}
		}
	}
	public void close(List<BDB> dbs) {
		for (BDB db: dbs) {
			if (!db.close())
				logger.error("Problem closing database: "+BDB.errmsg(db.ecode()));
		}
		logger.info("All "+dbs.size()+" databases closed.");
	}
}
