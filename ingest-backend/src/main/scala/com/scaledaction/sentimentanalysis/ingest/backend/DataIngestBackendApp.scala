package com.scaledaction.sentimentanalysis.ingest.backend

import com.scaledaction.core.cassandra.HasCassandraConfig
import com.scaledaction.core.kafka.HasKafkaConfig
import com.scaledaction.core.spark.{ SparkUtils, HasSparkConfig }
import kafka.serializer.StringDecoder
import org.apache.spark.mllib.classification.{ ClassificationModel, NaiveBayes }
import org.apache.spark.mllib.feature.HashingTF
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{ SQLContext, SaveMode }
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{ Seconds, StreamingContext, Time }
import org.apache.spark.{ SparkConf, SparkContext }
import org.slf4j.LoggerFactory
import scala.io.Source
import spray.json.{ JsString, JsonParser }

case class Tweet(tweet: String, score: Double, batchtime: Long, tweet_text: String, query: String)

object DataIngestBackendApp extends HasCassandraConfig with HasKafkaConfig with HasSparkConfig {

  def main(args: Array[String]) {
    val log = LoggerFactory.getLogger("main")

    val cassandraConfig = getCassandraConfig

    val kafkaConfig = getKafkaConfig

    val sparkConfig = getSparkConfig

    val sc = SparkUtils.getActiveOrCreateSparkContext(cassandraConfig, sparkConfig.master, "DataIngestBackend")
    val htf = new HashingTF(10000)
    val model = createModel(sc, htf)

    val ssc = SparkUtils.getActiveOrCreateStreamingContext(sc)
    val sqlContext = SQLContext.getOrCreate(sc)

    val ratingsStream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaConfig.kafkaParams, kafkaConfig.topics)

    ratingsStream.foreachRDD {
      import sqlContext.implicits._

      (message: RDD[(String, String)], batchTime: Time) => {
        // convert each RDD from the batch into a DataFrame
        val df = message.map(_._2).map(tweet => {
          val json = JsonParser(tweet).asJsObject
          (json.fields.get("text"), json.fields.get("query")) match {
            case (Some(JsString(text)), Some(JsString(query))) =>
              (tweet, text, new LabeledPoint(0, htf.transform(text.toLowerCase.split(" "))), query)
            case _ => (tweet, "", new LabeledPoint(0, htf.transform("".split(""))), "")
          }
        }).map(t => {
          val tweet = t._1
          val text = t._2
          println(s"Writing tweet:  $text")
          val point = t._3
          val score = model.predict(point.features)
          val query = t._4
          Tweet(tweet, score, batchTime.milliseconds, text, query)
        }).toDF("tweet", "score", "batchtime", "tweet_text", "query")

        df.write.format("org.apache.spark.sql.cassandra")
          .mode(SaveMode.Append)
          .options(Map("keyspace" -> cassandraConfig.keyspace, "table" -> "tweets"))
          .save()
      }
    }

    ssc.start()
    ssc.awaitTermination()
  }

  private def createModel(sc: SparkContext, htf: HashingTF): ClassificationModel = {
    val (positiveData, pSource) = dataFromResource(sc, htf, "/tweet-corpus/positive.gz", 1)
    val (negativeData, nSource) = dataFromResource(sc, htf, "/tweet-corpus/negative.gz", 0)
    val training = positiveData.union(negativeData)
    try {
      NaiveBayes.train(training, lambda = 1.0, modelType = "multinomial")
    } finally {
      pSource.close
      nSource.close
    }
  }

  private def dataFromResource(sc: SparkContext, htf: HashingTF, path: String, label: Int): (RDD[LabeledPoint], Source) = {
    val (rdd, source) = textFileFromResource(sc, path)
    val data = rdd.map { text => new LabeledPoint(label, htf.transform(text.split(" "))) }
    (data, source)
  }

  private def textFileFromResource(sc: SparkContext, path: String): (RDD[String], Source) = {
    val stream = getClass.getResourceAsStream(path)
    val source = Source.fromInputStream(stream, "ISO-8859-1")
    val rdd = sc.parallelize(source.getLines.toSeq)
    (rdd, source)
  }
}
