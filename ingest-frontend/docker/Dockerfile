FROM scaledaction/dev-base:1.4.1 

MAINTAINER Scaled Action

COPY sentiment-ingest-frontend-assembly-1.0.jar /

ENTRYPOINT ["java", "-cp", "sentiment-ingest-frontend-assembly-1.0.jar", "com.scaledaction.sentimentanalysis.ingest.frontend.DataIngestFrontendApp"]
