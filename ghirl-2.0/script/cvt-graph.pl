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
# is done using the unix sort routine in a tmpfile.  For very large 
# tmpfiles (>100MB), the file is split by increasingly longer string prefixes
# until no file exceeds the maximum, then each file is sorted separately and
# the results concatenated.  Intermediate tmpfiles are stored in
# ${tmpfile}-tmpfiles/.  You must have 2x the free disk space as the size
# of $tmpfile to do this sort.

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
my $pass = 1;
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
    print "- pass $pass finished $ctr lines\n" if ++$ctr % 100000 == 0;
}
close(TF);


print "- done pass $pass, sorting links\n";
%LabelId = ();
$id=0;
foreach my $lab (sort(keys(%IsLabel))) {
    $LabelId{$lab} = ++$id;
    print LF $lab,"\n";
}
close(LF);

print "- done pass $pass, sorting nodes\n";
%NodeId = ();
$id=0;
foreach my $n (sort(keys(%IsNode))) {
    $NodeId{$n} = ++$id;
    print NF $n,"\n";
}
close(NF);

print SF scalar(keys(%IsLabel))," ",$id,"\n";
close(SF);

$pass = 2;
print "- begin pass $pass, sorting and converting\n";
# limit to 100MB by default -- do we need a cmdline op for this?
sortTmpfileLargefileHandling($tmpFile, 100);
open(SFT, "$tmpFile") || die "can't open $tmpFile after sort";



$ctr = 0;
my $lastSrc = '';
my $lastLabel = '';
my @Dsts = ();
while (<SFT>) {
#    print "- pass $pass finished $ctr lines\n" if ++$ctr % 100000 == 0;
    my($src,$label,$count,$dst) = split;
    if ($src ne $lastSrc || $label ne $lastLabel) {
	flushEdges($lastSrc,$lastLabel,@Dsts)
	    if $lastSrc;
	@Dsts = ()
    }
    push(@Dsts,($NodeId{$dst}.':'.$count));
    $lastSrc = $src;
    $lastLabel = $label;
    print "- pass $pass finished $ctr lines\n" if ++$ctr % 100000 == 0;
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

############### Piecewise sorting for large files:
sub sortTmpfileLargefileHandling {
    my $filename = shift;
    my $tmpdir = "${filename}-tempfiles";
    my $max_size = scalar(shift);
    if(!defined($max_size)) { $max_size = 10; }
    print "Splitting with maximum filesize ${max_size}MB\n";
    
    my $pass = 1;
    $max_pass = 10;
    my %preinfo = ();
    open($tmpname, $filename);
    my @remaining = getLargeFiles($max_size,($filename => $tmpname));
    close($tmpname);
    undef($tmpname);
    if ($#remaining >= 0) {
	%preinfo = splitFile($filename, $pass, $tmpdir); $ni = scalar(keys(%preinfo));
	print "#####################\n$ni new files:\n\t";
	foreach my $key (sort(keys(%preinfo))) { print "$key, "; } print "\n";
	@remaining = getLargeFiles($max_size,%preinfo);
    } else {
	mkdir($tmpdir);
	system("mv $filename $tmpdir/.tmp");
	open($tmpname, "$tmpdir/.tmp");
	%preinfo = (".tmp" => $tmpname);
    }
    while ($#remaining >= 0) {
	print scalar(@remaining)," files over ${max_size}MB after pass $pass\n";
	foreach my $key (sort(@remaining)) {
	    if(exists($preinfo{$key})) {
		close($preinfo{$key});
		undef $preinfo{$key};
		delete $preinfo{$key};
	    }
	    open($tmpname,"$tmpdir/$key");
	    @stuff = stat($tmpname);
	    print "\t$key\t$stuff[7]\n";
	    close($tmpname);
	    undef $tmpname;
	}
	
	$pass += 1; if($pass > $max_pass) { last; }
	%info = splitFiles($tmpdir, $pass, @remaining); $ni = scalar(keys(%info));
	
	foreach my $key (keys(%info)) { $preinfo{$key} = $info{$key}; }
	if ($ni>1) {
	    print "#####################\n$ni new files:\n\t";
	    foreach my $key (sort(keys(%info))) { print "$key, "; } print "\n";
	}
	
	@remaining = getLargeFiles($max_size,%info);
	undef %info;
    }
    print "Done splitting at pass $pass. Beginning sort and merge:\n";
    #foreach my $key (sort(keys(%preinfo))) { print "\t$key\n"; }
    mergePrefixFiles("$filename",$tmpdir,%preinfo);
#    system("rm -r $tmpdir");
}

sub splitFiles {
    my ($tmpdir, $pren, @splitinfo) = @_;
    my %this_pass = ();
    foreach my $key (@splitinfo) {
	($safekey = $key) =~ s/\?/\\\?/g;
	($safekey = $key) =~ s/\$/\\\$/g;
	$cmd = "mv $tmpdir/$safekey $tmpdir/.tmp";
	#print "$cmd\n";
	print "Moving $key to $tmpdir/.tmp\n";
	system($cmd) and die "Couldn't move file to temporary location: $!";
	%subinfo = splitFile("$tmpdir/.tmp",$pren, $tmpdir);
	# ew ew ew
	foreach my $k (keys %subinfo) { $this_pass{$k} = $subinfo{$k}; }
    }
    return %this_pass;
}

sub splitFile {
    my ($filename,$pren,$tmpdir) = @_;
    my %preinfo = ();
    print "Splitting $filename on $pren characters\n";
    my $tempfiles = $tmpdir; # "${filename}-tempfiles";
    mkdir($tempfiles);
    open(FILE,$filename) or die "Couldn't get file to split: $filename $!";
    while(<FILE>) {
	chop;
	my $pre = substr($_, 1, $pren);
	if (/^TEXT/) {
	    $pre = substr($_, 0, $pren+5);
	}
	unless(defined($preinfo{$pre})) {
	    open($tmpname,">$tempfiles/$pre");
	    $preinfo{$pre} = $tmpname;
	    undef $tmpname;
	}
	print { $preinfo{$pre} } "$_\n";
    }
    close(FILE);
    return %preinfo;
}

sub mergePrefixFiles {
    my ($tmpfile,$tmpdir,%preinfo) = @_; 
    print "Merging ",scalar(keys(%preinfo))," files in ${tmpdir}.\n";
    my $N=0;
    open(TT,">$tmpfile") or die "Can't make temp file $tmpfile for concatenating sorted files! $!";
    foreach my $key (sort(keys(%preinfo))) {
	print ":$key";
	#print ".";
	close($preinfo{$key});
	($safekey = $key) =~ s/\?/\\\?/g;
	($safekey = $key) =~ s/\$/\\\$/g;
# silly shell; don't include *all* the files
	open(SPREF, "sort $tmpdir/$safekey|") or die "Can't sort prefix file $safekey $!";
	while (<SPREF>) { print TT $_; }
	close(SPREF);
    }
    close(TT);
    print "\nDone with piecewise sort.\n";
}

sub getLargeFiles {
    my ($maxsize, %files) = @_;
    my @largeFiles = ();
    print "Looking for files over ${maxsize}MB:\n";
    foreach my $prename (keys(%files)) {
	$pre = $files{$prename};
	@prestat = stat($pre) or die "Can't stat file handle for $prename: $!\n";
	$size = $prestat[7] / 1000 / 1000;
	if ($size > $maxsize) {
	    #print " #($size)$prename# ";
	    push(@largeFiles, $prename);
	} 
	print ".";
    } print "\n";
    return @largeFiles;
}
