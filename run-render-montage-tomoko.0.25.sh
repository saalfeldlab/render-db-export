#!/bin/bash

OWN_DIR=`dirname "${BASH_SOURCE[0]}"`
ABS_DIR=`readlink -f "$OWN_DIR"`

FLINTSTONE=$ABS_DIR/flintstone/flintstone.sh
JAR=$PWD/target/render-align-0.0.1-SNAPSHOT.jar # this jar must be accessible from the cluster
CLASS=org.janelia.saalfeldlab.renderalign.RenderMontages
N_NODES=20

SERVER="http://tem-services.int.janelia.org:8080/render-ws/v1"
OWNER="hessh"
PROJECT="tomoko_Santomea11172016_04_A3_T1"
STACK="n_10_l_9_tfac_m9_xlfac_0_ylfac_0_xfac_2_yfac_5_deg_2"
#STACK="n_10_l_9_tfac_m5_xlfac_0_ylfac_0_xfac_2_yfac_5_deg_2"
#STACK="TOS_1_20948_fine_nbrs_10_lambda_6p5_deg_1"
X="0"
Y="0"
WIDTH="7493"
HEIGHT="5460"
#WIDTH="7069"
#HEIGHT="5083"
#WIDTH="239959.0"
#HEIGHT="148704.0"
SCALE="1.0"
OUTPUT="/nrs/saalfeld/tmp/spark-export/test/"
ZSCALE="2"

ARGV="-S $SERVER -u $OWNER -p $PROJECT -s $STACK -x $X -y $Y -w $WIDTH -h $HEIGHT -t $SCALE -o $OUTPUT -z $ZSCALE"
#ARGV="-S $SERVER -u $OWNER -p $PROJECT -s $STACK -t $SCALE -o $OUTPUT"

mkdir -p $OUTPUT

SPARK_VERSION=rc TERMINATE=1 $FLINTSTONE $N_NODES $JAR $CLASS $ARGV
