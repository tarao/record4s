#!/bin/env bash

set -xe

ROOT="$(cd $(dirname "$0")/..; pwd)"
cd "$ROOT"

[ -d benchmark ] && mv benchmark benchmark.bak

script/benchmark.sh

script/benchmark.sh -0 -p runtime \
                    -t record4s \
                    -t map \
                    -f Creation \
                    -f FieldAccessSize
script/benchmark.sh -0 -p compiletime \
                    -t record4s \
                    -t map \
                    -f CompileCreation \
                    -f CompileUpdate \
                    -f CompileFieldAccessSize
script/benchmark.sh -0 -p caseclass \
                    -t record4s \
                    -t map \
                    -t caseclass \
                    -f Creation \
                    -f Update \
                    -f FieldAccessSize
script/benchmark.sh -0 -p arrayrecord \
                    -t record4s \
                    -t record4s_arrayrecord \
                    -t caseclass \
                    -t map \
                    -f FieldAccessSize \
                    -f Creation \
                    -f Update \
                    -f CompileFieldAccessSize

cp benchmark/*.svg doc/img/benchmark
