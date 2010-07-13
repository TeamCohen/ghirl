#!/bin/bash
if [ $# -eq 0 ] 
then
    echo "USAGE: $0 rowfile entityfile"
    echo "       where an entity is a node or a link"
    exit
fi

set -x
ROW=$1
NODE=$2
awk 'BEGIN { pid=1; accum=0 } { if ($1 == pid) {accum += $3} else { print accum,pid; pid=$1; accum=0 }}' $ROW | sort -rn > ${NODE}-degree
./_idsToLabels.pl ${NODE}-degree $NODE > l${NODE}-degree