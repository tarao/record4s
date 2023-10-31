#!/bin/sh

set -xe

ROOT="$(cd $(dirname "$0")/..; pwd)"
cd "$ROOT"

script/docker-build-python.sh
docker run --rm -i -t \
       -u `id -u`:`id -g` \
       -e HOME=/opt/workspace \
       -v "$(pwd)":/opt/workspace \
       -p 8888:8888 \
       record4s/python
