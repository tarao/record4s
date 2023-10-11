#!/bin/env bash

set -xe

ROOT="$(cd $(dirname "$0")/..; pwd)"
cd "$ROOT"

OUT_DIR="benchmark"
OPTIONS="-wi 10 -i 20 -t 1 -f 10"

sbt=sbt

OUTPUTS=()

file_path() {
    PROJECT=$1
    TARGET=$2
    FEATURE=$3

    echo "modules/${PROJECT}/src/main/scala/benchmark/${TARGET}/${FEATURE}.scala"
}

run() {
    PROJECT=$1
    TARGET=$2
    FEATURE=$3

    FILE=$(file_path "$PROJECT" "$TARGET" "$FEATURE")
    [ -r "$FILE" ] || return 0

    mkdir -p "${OUT_DIR}/${TARGET}"
    OUTPUT="${OUT_DIR}/${TARGET}/${FEATURE}.json"

    $sbt "${PROJECT} / clean"
    $sbt "${PROJECT} / Jmh / run $OPTIONS ${TARGET}.${FEATURE} -rf json -rff \"../../${OUTPUT}\""
    OUTPUTS+=("$OUTPUT")
}

to_json_rows() {
    jq -s '[ .[] | {
        benchmark:.benchmark,
        target: .benchmark | sub("^benchmark[.](?<t>[^.]+)[.].*$"; "\(.t)"),
        feature: .benchmark | sub("^benchmark[.][^.]+[.](?<f>[^.]+)[.].*$"; "\(.f)"),
        index: .benchmark | sub("^.*[^0-9](?<x>[0-9]+)$"; "\(.x)") | tonumber,
        score: (.primaryMetric.rawData[] | map({value:.}))[] | .value
    } ]'
}

[ "$1" = "-0" ] && { # dry run
    shift
    sbt=:
}

[ "$1" = "-1" ] && { # just run once for test
    shift
    OPTIONS="-wi 0 -i 1 -t 1 -f 1"
}

[ -n "$1" ] && [ -n "$2" ] && [ -n "$3" ] && {
    run "$1" "$2" "$3"

    CHART_INPUT="${OUT_DIR}/${FEATURE}_${TARGET}.json"
    CHART_OUTPUT="${OUT_DIR}/${FEATURE}_${TARGET}.svg"

    for output in ${OUTPUTS[@]}; do
        jq '.[]' "${output}"
    done | to_json_rows > "${CHART_INPUT}"

    script/benchmark/visualize.sh "${FEATURE}" "${CHART_INPUT}" "${CHART_OUTPUT}"

    exit
}

run_feature() {
    FEATURE="$1"
    run "benchmark_3"    "record4s"     "${FEATURE}"
    run "benchmark_3"    "caseclass"    "${FEATURE}"
    run "benchmark_3"    "map"          "${FEATURE}"
    run "benchmark_2_13" "shapeless"    "${FEATURE}"
    run "benchmark_2_11" "scalarecords" "${FEATURE}"

    CHART_INPUT="${OUT_DIR}/${FEATURE}.json"
    CHART_OUTPUT="${OUT_DIR}/${FEATURE}.svg"

    for output in ${OUTPUTS[@]}; do
        jq '.[]' "${output}"
    done | to_json_rows > "${CHART_INPUT}"

    script/benchmark/visualize.sh "${FEATURE}" "${CHART_INPUT}" "${CHART_OUTPUT}"

    OUTPUTS=()
}

run_feature "Creation"
run_feature "Update"
run_feature "FieldAccess"
