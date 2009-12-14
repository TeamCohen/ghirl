#!/usr/local/bin/perl -w

#convert a basic graph from old format to new format
#old format: each line is one of:
#
# node nodeName
# edge relation nodeName1 nodeName2
#
#new format has four files: 
#
# 1) in the sizesFile, there is one line of the form:
# n m
# where N is the number of links and M is the number of nodes.
#
# 2) in the linkFile, each line is a relation name (link label), and
# the n-th label has id n (where n starts from 1)
#
# 3) in the nodeFile, each line is a node name, and the n-th
# node has id n (where n starts from 1)
#
# 4) in the rowFile, each line is 
#
#  nodeId relationId nodeId1:w ... nodeIdK:w 
#
# where w is a floating point weight.  The weights from a link don't
# need to be normalized to sum to one.
#
# This code assumes the list of nodes and the list of outlinks for
# each node can fit in memory, and that you want to add inverses of
# each link.  The most expensive step is sorting the edges, which 
# is done using the unix sort routine in a tmpfile

my $sizeFile = shift;
my $linkFile = shift;
my $nodeFile = shift;
my $rowFile = shift;
my $tmpFile = shift;

unless (defined($tmpFile)) {
    die "usage: sizeFile linkFile nodeFile rowFile tmpFile [oldGraphFile1.txt oldGraphFile2.txt ...]";
}

open(SF,">$sizeFile") || die "can't write $sizeFile";
open(NF,">$nodeFile") || die "can't write $nodeFile";
open(LF,">$linkFile") || die "can't write $linkFile";
open(RF,">$rowFile") || die "can't write $rowFile";
open(TF,">$tmpFile") || die "can't write $tmpFile";

%IsNode = ();
%IsLabel = ();
my $ctr = 0;
while (<>) {
    #print "echo $_";
    chop;
    if (/^node\s/) {
	my($keyword,$nodeName) = split;
	$nodeName = normalizeNodeName($nodeName);
	$IsNode{$nodeName}++;
    }
    if (/^edge/) {
	#format could be 'edge:count rel fromNode toNode'
	#or just 'edge rel fromNode toNode'
	my($keywordAndCount,$linkLabel,$nodeName1,$nodeName2) = split;
	my($keyword,$count) = split(/:/,$keywordAndCount);
	$count = 1 unless defined($count);
	$nodeName1 = normalizeNodeName($nodeName1);
	$nodeName2 = normalizeNodeName($nodeName2);
	$IsNode{$nodeName1}++;
	$IsNode{$nodeName2}++;
	$IsLabel{$linkLabel}++;
	$IsLabel{$linkLabel."Inverse"}++;
	print TF $nodeName1," ",$linkLabel," $count ",$nodeName2,"\n";
	print TF $nodeName2," ",$linkLabel,"Inverse $count ",$nodeName1,"\n",
    }
    print "- pass 1 finished $ctr lines\n" if ++$ctr % 100000 == 0;
}
close(TF);


print "- done pass 1, sorting links\n";
%LabelId = ();
$id=0;
foreach my $lab (sort(keys(%IsLabel))) {
    $LabelId{$lab} = ++$id;
    print LF $lab,"\n";
}
close(LF);

print "- done pass 1, sorting nodes\n";
%NodeId = ();
$id=0;
foreach my $n (sort(keys(%IsNode))) {
    $NodeId{$n} = ++$id;
    print NF $n,"\n";
}
close(NF);

print SF scalar(keys(%IsLabel))," ",$id,"\n";
close(SF);

print "- begin pass 2, sorting and converting\n";
$ctr = 0;
open(SFT, "sort $tmpFile|") || die "can't sort $tmpFile";
my $lastSrc = '';
my $lastLabel = '';
my @Dsts = ();
while (<SFT>) {
    print "- pass 2 finished $ctr lines\n" if ++$ctr % 100000 == 0;
    my($src,$label,$count,$dst) = split;
    if ($src ne $lastSrc || $label ne $lastLabel) {
	flushEdges($lastSrc,$lastLabel,@Dsts)
	    if $lastSrc;
	@Dsts = ()
    }
    push(@Dsts,($NodeId{$dst}.':'.$count));
    $lastSrc = $src;
    $lastLabel = $label;
    print "- pass 1 finished $ctr lines\n" if ++$ctr % 100000 == 0;
}
flushEdges($lastSrc,$lastLabel,@Dsts);
close(SFT);
close(RF);

sub flushEdges {
    my($src,$lab,@dstIds) = @_;
    $srcId = $NodeId{$src};
    $linkId = $LabelId{$lab};
    print RF $srcId," ",$linkId," ",scalar(@dstIds)," ",join(' ',@dstIds),"\n";
}

sub normalizeNodeName {
    my($name) = @_;
    return ($name =~ /\$/) ? $name : '$'.$name;
}

