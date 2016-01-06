# Twitter Stream Sentiment Analysis on the Fast Data Stack
A Reactive Application that ingests Twitter streams, performs Sentiment Analysis, provides for queries and visualizes the results

* Streams tweets using the Twitter stream API to [Akka](http://http://akka.io/)
* Captures the tweets and places them in [Kafka](http://kafka.apache.org)
* Buffers them until they are picked up by [Spark](http://spark.apache.org)
* Performs Sentiment Analysis and stores tweets and opinions in [Cassandra](http://cassandra.apache.org)
* Makes the data queryable via SQL using [Ignite](https://ignite.apache.org)
* Visualizes the query results using [Zeppelin](https://zeppelin.incubator.apache.org)

ScaledAction pipeline
![ScaledAction pipeline](https://github.com/scaledaction/sentiment-analysis/blob/images/images/pipeline1.png)


# Deployment via dtk

# Visualization with Zeppelin Notebook
![Twitter + Watson high level architecture](https://raw.githubusercontent.com/abajwa-hw/zeppelin-stack/master/screenshots/4.png)


