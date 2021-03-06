#!/bin/bash

set -e

sbt -Dversion="$1" docker:publishLocal

git tag "$1"

docker save "tracker-server:$1" | pv -s 200000000 | ssh cloud 'docker load'
ssh cloud "./docker/tracker-server_recreate.sh $1"
