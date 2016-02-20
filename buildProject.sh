#!/bin/bash

sbt assembly
cp -u ingest-frontend/target/scala-2.10/sentiment-ingest-frontend-assembly-1.0.jar ingest-frontend/docker
cp -u ingest-backend/target/scala-2.10/sentiment-ingest-backend-assembly-1.0.jar ingest-backend/docker 
cp -n ingest-backend/src/main/resources/tweet-corpus/negative.gz ingest-backend/docker
cp -n ingest-backend/src/main/resources/tweet-corpus/positive.gz ingest-backend/docker
docker build -t scaledaction/sentiment-analysis-ingest-frontend ingest-frontend/docker
docker build -t scaledaction/sentiment-analysis-ingest-backend ingest-backend/docker
