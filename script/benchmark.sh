#!/bin/sh

set -xe

ROOT="$(cd $(dirname "$0")/..; pwd)"
cd "$ROOT"

OUT_DIR="target/benchmark"
OPTIONS="-wi 10 -i 20 -t 1 -f 10"

run() {
    PROJECT=$1
    TARGET=$2
    FEATURE=$3

    mkdir -p "${OUT_DIR}/${TARGET}"
    OUTPUT="../../${OUT_DIR}/${TARGET}/${FEATURE}.json"

    sbt "${PROJECT} / Jmh / run $OPTIONS ${TARGET}.${FEATURE} -rf json -rff \"${OUTPUT}\""
}

[ "$1" = "-1" ] && { # just run once for test
    shift
    OPTIONS="-wi 0 -i 1 -t 1 -f 1"
}

[ -n "$1" ] && [ -n "$2" ] && [ -n "$3" ] && {
    run "$1" "$2" "$3"
    exit
}

run "benchmark_3" "record4s" "FieldAccess"
