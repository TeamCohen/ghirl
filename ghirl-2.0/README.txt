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



Troubleshooting:
If you are building ghirl2 and seeing this error:

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