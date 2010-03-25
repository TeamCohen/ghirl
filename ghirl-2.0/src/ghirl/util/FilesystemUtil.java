package ghirl.util;

import java.io.File;

import org.apache.log4j.Logger;

public class FilesystemUtil {
	private static final Logger logger = Logger.getLogger(FilesystemUtil.class);
	/** Recursively delete files anchored at <code>f</code>
	 * 
	 * @param f Directory or file to delete.
	 */
	public static void rm_r(File f)
    {
        if (f.isDirectory()) {
		    File[] g = f.listFiles();
		    for (int i=0; i<g.length; i++) rm_r(g[i]);
        }
        boolean deleted = f.delete();
        logger.info("deleted: "+f+" "+(deleted?"-done":"-not present!"));
    }
}
