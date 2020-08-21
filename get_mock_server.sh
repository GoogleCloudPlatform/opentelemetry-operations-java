#!/bin/bash
VERSION=v2-alpha
BINARY=mock_server-x64-linux-$VERSION
if ! [ -e $BINARY ]; then
  curl -L -o $BINARY https://github.com/googleinterns/cloud-operations-api-mock/releases/download/$VERSION/$BINARY
  chmod +x $BINARY
fi

ln -sf $PWD/$BINARY mock_server
export MOCKSERVER=$PWD/$BINARY
