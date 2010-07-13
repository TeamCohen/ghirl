#!/usr/local/bin/perl -w

my $degreeFile = shift;
unless (defined($degreeFile)) {
    die "usage: degreeFile nodeFile";
}
open(DF,"$degreeFile") || die "can't open $degreeFile";

@NodeId = ();
$id=0;
while (<>) {
    chomp;
    $NodeId[++$id]=$_;
}
#print $#NodeId;
while(<DF>) {
    chomp;
#    print $_;
    my($degree,$nodenumber)=split;
    unless (defined($degree)) { die "Degree not defined"; }
    unless (defined($nodenumber)) { die "Node number not defined"; }
    unless (defined($NodeId[$nodenumber])) { die "NodeId not defined"; }
    print "$degree $NodeId[$nodenumber]\n";
}
close(DF);
