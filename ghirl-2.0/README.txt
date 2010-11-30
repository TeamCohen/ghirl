GHIRL 2.0: Moving Forward
November 2009 - Katie Rivard krivard@andrew.cmu.edu katie@rivard.org

If you need to use cutting-edge GHIRL features such as Hexastore (TBA) or a 
current copy of Minorthird, or you are adding new features to GHIRL, this is 
probably the distribution for you.


Requirements:
 - Java 1.6, although it can probably run in 1.5 if you care to tweak build.xml


Installation/Getting Started:

If using Eclipse, be sure to label src/ as a source folder, and add all the .jar
files in lib/ to the build path.

If using Hexastore or TokyoCabinet, copy build.properties.orig to build.properties
locally, then edit the relevant section of build.properties to fit your system.
Note that while our antfile will build Hexastore, you must build and install
the Java bindings for TokyoCabinet externally.  See 
	http://1978th.net/tokyocabinet/
for details.


===============
Troubleshooting
===============
*** If you are building ghirl2 and seeing this error:

> bash-3.2$ ant clean
> Buildfile: build.xml
>
> init:
>  [taskdef] Could not load definitions from resource
> net/sf/antcontrib/antcontrib.properties. It could not be found.

Then you do not have the ant-contrib package installed with your version of 
ant. ant-contrib defines a looping target which is being used in ghirl2's 
buildfile to package the external libraries.  To install ant-contrib to your 
own system, go to

http://sourceforge.net/projects/ant-contrib/

And click the big green button.  I think the current version is 1.0b3, which 
has been stable since November 2006.  Copy the ant-contrib JAR to the lib 
directory of your ant install, or, if using Eclipse, go to your preferences and
 do Ant -> Runtime -> Classpath -> Global Entries, click the "Add External 
JARs..." button and select the ant-contrib JAR.

*** If you are running the "verify" or other test targets in Eclipse, and you
get tokyocabinet errors to the tune of "Wrong ELF class" (i.e. it *is* locating
the tokyocabinet libraries, but it's expecting libraries for a different
architecture.  This problem only occurs on dual-architecture machines)

(1) Go to Window -> Preferences -> Java -> Installed JREs
Here you want to check that you have a version of java installed of the same
architecture as the tokyocabinet libraries. If you used a different java
to build the tokyocabinet JNI than you normally use in Eclipse, you may have
to add another JRE to this panel.  Once you have identified which installed
JRE matches your tokyocabinet libraries, proceed.

(2) Go to Window -> Show View -> Ant
If it is not already there, add the GHIRL build.xml.  Right click and select
"Run As -> External Tools Configurations...".  Select the JRE tab. Select
the JRE that matches your tokyocabinet libraries.  This sets the JRE which is
used to execute ant, which is the same one that is used to run junit using the
ant targets.

(3) It should work now; run your target.  If you still get a "Wrong ELF class"
error, seek additional help.
