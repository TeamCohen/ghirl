java ghirl.graph.TextUI -graph memory-toy.bsh -query william | tail -21 > tmp.txt
echo test memory-toy.bsh against baseline
perl compare.pl tmp.txt g12-william-baseline.txt

java ghirl.graph.TextUI -graph cache-toy.bsh -query william | tail -21 > tmp.txt
echo test cache-toy.bsh against baseline
perl compare.pl tmp.txt g12-william-baseline.txt

java ghirl.graph.TextUI -graph disk-toy.bsh -query william | tail -21 > tmp.txt
echo test disk-toy.bsh against baseline
perl compare.pl tmp.txt g12-william-baseline.txt

java ghirl.graph.TextUI -graph memory-toy-g12.bsh -query william | tail -21 > tmp.txt
echo test memory-toy-g12.bsh against baseline
perl compare.pl tmp.txt g12-william-baseline.txt

java ghirl.graph.TextUI -graph nested-toy-g12.bsh -query william | tail -21 > tmp.txt
echo test nested-toy-g12.bsh against baseline
perl compare.pl tmp.txt g12-william-baseline.txt

java ghirl.graph.TextUI -graph memory-toy-g1.bsh -query william | tail -21 > tmp.txt
echo test memory-toy-g1.bsh against baseline
perl compare.pl tmp.txt g1-william-baseline.txt

java ghirl.graph.TextUI -graph nested-toy-g1.bsh -query william | tail -21 > tmp.txt
echo test nested-toy-g1.bsh against baseline
perl compare.pl tmp.txt g1-william-baseline.txt

