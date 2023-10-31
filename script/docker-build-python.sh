#!/bin/sh

ROOT="$(cd $(dirname "$0")/..; pwd)"
cd "$ROOT"

docker build "$@" -t 'record4s/python' script/python
