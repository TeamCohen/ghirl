#bash script to define classpath for ghirl

#redefine this as needed
GHIRL=/usr0/wcohen/code/ghirl-2.0 

#using katie's jar
#export CLASSPATH=${GHIRL}/lib/je-3.3.82.jar:${GHIRL}/lib/lucene.jar:${GHIRL}/lib/minorThird-20071114.jar:${GHIRL}/build:${GHIRL}

#using my patched version of the jar
#export CLASSPATH=${GHIRL}/lib/je-3.3.82.jar:${GHIRL}/lib/lucene.jar:${GHIRL}/lib/minorThird-20071114a.jar:${GHIRL}/build:${GHIRL}

#using newest jar
#export CLASSPATH=${GHIRL}/lib/je-3.3.82.jar:${GHIRL}/lib/lucene.jar:${GHIRL}/lib/m3rd_20091201.jar/:${GHIRL}/build:${GHIRL}

#using live minorthird
export CLASSPATH=${GHIRL}/lib/je-3.3.82.jar:${GHIRL}/lib/lucene.jar:${GHIRL}/build:${GHIRL}
. $MINORTHIRD/script/setup.linux


