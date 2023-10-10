#!/bin/sh

set -xe

ROOT="$(cd $(dirname "$0")/../..; pwd)"
cd "$ROOT"

script/docker-build-python.sh
docker run --rm -i -t \
       -u `id -u`:`id -g` \
       -v "$(pwd)":/opt/workspace \
       record4s/python \
       python script/benchmark/visualize.py "$@"
