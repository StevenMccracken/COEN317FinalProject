#!/bin/bash
cd target/classes
PID=$(pgrep rmiregistry)
kill -SIGTERM ${PID}