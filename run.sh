#!/usr/bin/env bash
docker build -t onellmweb-app .
# Build shaded jar and run Main

docker rm -f $(docker ps -aq)

docker run -p 8080:8080 onellmweb-app
