This is a really podunk test at the moment.  Improvements welcome.
-Katie



(1) Make a full .jar (ant dist-lib-full)
(2) Copy it here
(3) Do

$ java -cp ghirl2-full-tttttttt.jar -Dghirl.dbDir=. ghirl.graph.TextUI -graph cache-toy.bsh -query william | tail -21 > output.txt
$ diff output-goldstandard.txt output.txt

(4) If you get a result, you broke GHIRL -- do not commit!  