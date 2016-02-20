FROM scaledaction/dev-base:1.4.1 

MAINTAINER Scaled Action

COPY sentiment-ingest-backend-assembly-1.0.jar /
COPY negative.gz /tweet-corpus/negative.gz
COPY positive.gz /tweet-corpus/positive.gz

ENTRYPOINT ["java", "-cp", "/usr/local/spark/lib/spark-assembly-1.4.1-hadoop2.6.0.jar:/spark-streaming-kafka_2.10-1.4.1.jar:sentiment-ingest-backend-assembly-1.0.jar", "com.scaledaction.sentimentanalysis.ingest.backend.DataIngestBackendApp"]
