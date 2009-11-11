GHIRL 1.0: A Brief Dusting Off
November 2009 - Katie Rivard krivard@andrew.cmu.edu katie@rivard.org

If you need to use GHIRL as-is, this is probably the distribution for you.  It works the same way the NIES, Hexastore, and archived versions did.

Changes:
 - Includes all necessary libraries
    * minorThird-20071114.jar
    * je-3.3.82 (this may be significantly newer than you're used to.  It requires more memory but doesn't have buggy file locks)
    * lucene.jar
 - Shiny new build file.  Targets you probably want are:
    * compile
    * all (clean,compile)
    * dist-lib-solo (ghirl-dstamp.jar, just GHIRL)
    * dist-lib-full (ghirl-full-dstamp.jar, GHIRL and m3rd and lucene and sleepycat)
    * dist-custom -Dmainclass=YOUR.DESIRED.MAIN.CLASS.HERE (executable ghirl-custom-mainclass.jar)
       - Be forewarned; this is an all-in-one jar (about 8MB)

Requirements:
 - Java 1.6, although it can probably run in 1.5 if you care to tweak build.xml

