#!/bin/bash

OWN_DIR=`dirname "${BASH_SOURCE[0]}"`
ABS_DIR=`readlink -f "$OWN_DIR"`

FLINTSTONE=$ABS_DIR/flintstone/flintstone.sh
JAR=$PWD/target/render-align-0.0.1-SNAPSHOT.jar # this jar must be accessible from the cluster
CLASS=org.janelia.saalfeldlab.renderalign.RenderMontages
N_NODES=20

SERVER="http://tem-services.int.janelia.org:8080/render-ws/v1"
OWNER="hessh"
PROJECT="fly_em_pre_iso"
STACK="Test_slab_1_98401_fine_nbrs_20"
X="-6"
Y="-32"
WIDTH="37160"
HEIGHT="7227"
#WIDTH="239959.0"
#HEIGHT="148704.0"
SCALE="0.25"
OUTPUT="/nrs/saalfeld/tmp/spark-export/test/"
ZSCALE="4"

ARGV="-S $SERVER -u $OWNER -p $PROJECT -s $STACK -x $X -y $Y -w $WIDTH -h $HEIGHT -t $SCALE -o $OUTPUT -z $ZSCALE"
#ARGV="-S $SERVER -u $OWNER -p $PROJECT -s $STACK -t $SCALE -o $OUTPUT"

mkdir -p $OUTPUT

SPARK_VERSION=rc TERMINATE=1 $FLINTSTONE $N_NODES $JAR $CLASS $ARGV
