#!/bin/bash

USAGE="$0 <N_NODES> <JAR> <CLASS> <ARGV>"

if [[ "$1" -eq "-h" ]]; then
    echo $USAGE
else

    OWN_DIR=`dirname "${BASH_SOURCE[0]}"`
    OWN_DIR_ABS_PATH=`readlink -f "$OWN_DIR"`
    ROOT_DIR=${ROOT_DIR:-$OWN_DIR_ABS_PATH}
    IGNITE="${ROOT_DIR}/ignite.sh"
    TEMPLATE="${ROOT_DIR}/template.sh"
    TMP="${TMP:-$HOME/tmp}"
    N_NODES="$1"; shift
    JAR="`readlink -f $1`"; shift
    CLASS="$1"; shift
    ARGV="$@"

    mkdir -p "$TMP"
    SCRIPT="`mktemp --tmpdir=$TMP`"
    printf "`cat $TEMPLATE`" "$JAR" "$CLASS" > "$SCRIPT"
    chmod +rx "$SCRIPT"

    export JOB_NAME=${JOB_NAME:-$CLASS}

    "$IGNITE" "$N_NODES" "$SCRIPT" "$ARGV"
fi
