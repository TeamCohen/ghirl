my $file1 = shift;
my $file2 = shift;

open(F,$file1) || die;
while (<F>) { 
    my($s,$o) = split;
    $score1{$o} = $s;
}
open(G,$file2) || die;
while (<G>) { 
    my($s,$o) = split;
    $score2{$o} = $s;
}
my @errors = ();
foreach my $o (keys(%score1)) {
#    print "keys 1: $o $score1{$o} $score2{$o}\n";
    push(@errors, "$file1($o)=$score1{$o}  !=  $file2($o)=$score2{$o}")
	unless $score1{$o}==$score2{$o}
}
foreach my $o (keys(%score2)) {
#    print "keys 2: $o $score1{$o} $score2{$o}\n";
    push(@errors, "$file1($o)=$score1{$o}  !=  $file2($o)=$score2{$o}")
	unless $score1{$o}==$score2{$o}
}
print scalar(@errors)," errors comparing $file1 vs $file2\n";
print "detailed error listing:\n",join("\n",@errors)
    if (@errors>0);
 
