#!/bin/env bash

set -xe

ROOT="$(cd $(dirname "$0")/..; pwd)"
cd "$ROOT"

OUT_DIR="benchmark"
OPTIONS="-t 1"

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
    $sbt "${PROJECT} / Jmh / run $OPTIONS [.]${TARGET}[.]${FEATURE}[.] -rf json -rff \"../../${OUTPUT}\""
    OUTPUTS+=("$OUTPUT")
}

to_json_rows() {
    jq -s '[ .[] | {
        benchmark:.benchmark,
        target: .benchmark | sub("^benchmark[.](?<t>[^.]+)[.].*$"; "\(.t)"),
        feature: .benchmark | sub("^benchmark[.][^.]+[.](?<f>[^.]+)[.].*$"; "\(.f)"),
        index: [ .params.size // "", .benchmark | sub("^.*[^0-9](?<x>[0-9]*)$"; "\(.x)") ] | map(select(. | length > 0) | tonumber)[0],
        score: (.primaryMetric.rawData[] | map({value:.}))[] | .value
    } ]'
}

[ "$1" = "-0" ] && {
    # Skip JMH run but accumulate outpus and plot the charts
    # This is expecting the following workflow:
    #   1. run `script/benchmark.sh <project> <target> <feature>` one by one
    #   2. run `script/benchmark.sh -0 to accumulate all the results and plot the charts
    shift
    sbt=:
}

[ "$1" = "-1" ] && { # just run once for test
    shift
    OPTIONS="-wi 0 -i 1 -t 1 -f 1"
}

[ "$1" = "-3" ] && { # just run few iterations for test
    shift
    OPTIONS="-wi 0 -i 3 -t 1 -f 1"
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

[ -n "$1" ] && [ -z "$2" ] && {
    run_feature "$1"
    exit
}

run_feature "Creation"
run_feature "Update"
run_feature "FieldAccess"
run_feature "FieldAccessSize"
run_feature "FieldAccessPoly"

run_feature "CompileCreation"
