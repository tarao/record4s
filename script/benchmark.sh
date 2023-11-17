#!/bin/env bash

set -xe

ROOT="$(cd $(dirname "$0")/..; pwd)"
cd "$ROOT"

OUT_DIR="benchmark"
OPTIONS="-t 1"

sbt=sbt

OUTPUTS=()
TARGETS=()
FEATURES=()
PREFIX=''
SUFFIX=''

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
        index: [ .params.size // "", .params.index // "", .params.repetitions // "", .benchmark | sub("^.*[^0-9](?<x>[0-9]*)$"; "\(.x)") ] | map(select(. | length > 0) | tonumber)[0],
        score: (.primaryMetric.rawData[] | map({value:.}))[] | .value
    } ]'
}

while :
do
    [ "$1" = "-0" ] && {
        # Skip JMH run but accumulate outpus and plot the charts
        # This is expecting the following workflow:
        #   1. run `script/benchmark.sh <project> <target> <feature>` one by one
        #   2. run `script/benchmark.sh -0 to accumulate all the results and plot the charts
        shift
        sbt=:
        continue
    }

    [ "$1" = "-1" ] && { # just run once for test
        shift
        OPTIONS="-wi 0 -i 1 -t 1 -f 1"
        continue
    }

    [ "$1" = "-3" ] && { # just run few iterations for test
        shift
        OPTIONS="-wi 0 -i 3 -t 1 -f 1"
        continue
    }

    [ "$1" = '-t' ] && [ -n "$2" ] && {
        TARGETS+=("$2")
        shift; shift
        continue
    }

    [ "$1" = '-f' ] && [ -n "$2" ] && {
        FEATURES+=("$2")
        shift; shift
        continue
    }

    [ "$1" = '-p' ] && [ -n "$2" ] && {
        PREFIX="${2}_"
        shift; shift
        continue
    }

    [ "$1" = '-s' ] && [ -n "$2" ] && {
        SUFFIX="_${2}"
        shift; shift
        continue
    }

    break
done

declare -A PROJECTS=(
    ['record4s']='benchmark_3'
    ['record4s_arrayrecord']='benchmark_3'
    ['caseclass']='benchmark_3'
    ['map']='benchmark_3'
    ['shapeless']='benchmark_2_13'
    ['scalarecords']='benchmark_2_11'
)

[ ${#TARGETS[@]} = 0 ] && {
    TARGETS=(
        'record4s'
        'record4s_arrayrecord'
        'map'
        'caseclass'
        'shapeless'
        'scalarecords'
    )
}

[ ${#FEATURES[@]} = 0 ] && {
    FEATURES=(
        'Creation'
        'Update'
        'Concatenation'
        'FieldAccess'
        'FieldAccessSize'
        'FieldAccessPoly'

        'CompileCreation'
        'CompileCreationAndAccess'
        'CompileCreationAndAccessRep'
        'CompileUpdate'
        'CompileUpdateRep'
        'CompileConcatenation'
        'CompileFieldAccess'
        'CompileFieldAccessSize'
    )
}

run_feature() {
    FEATURE="$1"
    for target in ${TARGETS[@]}; do
        run "${PROJECTS[$target]}" "$target" "${FEATURE}"
    done

    CHART_INPUT="${OUT_DIR}/${PREFIX}${FEATURE}${SUFFIX}.json"
    CHART_OUTPUT="${OUT_DIR}/${PREFIX}${FEATURE}${SUFFIX}.svg"

    for output in ${OUTPUTS[@]}; do
        jq '.[]' "${output}"
    done | to_json_rows > "${CHART_INPUT}"

    script/benchmark/visualize.sh "${FEATURE}" "${CHART_INPUT}" "${CHART_OUTPUT}"

    OUTPUTS=()
}

for feature in ${FEATURES[@]}; do
    run_feature "$feature"
done
