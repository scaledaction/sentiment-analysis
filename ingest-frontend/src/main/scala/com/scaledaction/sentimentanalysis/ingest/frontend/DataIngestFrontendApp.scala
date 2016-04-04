package com.scaledaction.sentimentanalysis.ingest.frontend

import akka.actor._
import akka.event.Logging
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._
import com.scaledaction.core.kafka.HasKafkaConfig
import java.util.concurrent.{ ExecutorService, Executors }
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerRecord, Callback, RecordMetadata }
import scala.concurrent._
import spray.json._
import spray.json.DefaultJsonProtocol._

object DataIngestFrontendApp extends HasKafkaConfig {

  case class TweetToKafka(query: String, text: String)

  object TweetToKafka {
    implicit val tweetToKafkaJson = jsonFormat2(TweetToKafka.apply)
  }

  def main(args: Array[String]) {
    val query = args.mkString(" ")
    if (query.trim.isEmpty) {
      println("No query was given!!")
      sys.exit(1)
    }
    println(s"Running with query: $query")

    val execService: ExecutorService = Executors.newCachedThreadPool()
    implicit val system = ActorSystem("DataIngestFrontend")
    implicit val ec = ExecutionContext.fromExecutorService(execService)
    implicit val materializer = ActorFlowMaterializer()(system)
    val log = Logging(system, DataIngestFrontendApp.getClass.getName)

    val kafkaConfig = getKafkaConfig

    val g = FlowGraph { implicit b =>
      import akka.stream.scaladsl.FlowGraphImplicits._

      val producer = new KafkaProducer[String, String](kafkaConfig.toProducerProperties)

      // Create and start a TwitterStreamClient that publishes on the event bus
      val twitterStream = new TwitterStreamClient(system)
      twitterStream.init

      // Create a Source with an actor that listens to the event bus
      val tweets: Source[Tweet] = Source(Props[StatusPublisherActor])
      val filter = Flow[Tweet].filter(t => t.body.contains(query))

      val out = Sink.foreach[Tweet]({
        case (tweet) =>
          {
            val tweetJson = TweetToKafka(query, tweet.body).toJson.toString
            println("Receiving tweet: " + tweetJson)
            val record = new ProducerRecord[String, String](kafkaConfig.topic, tweetJson)
            producer.send(record, new Callback {
              override def onCompletion(result: RecordMetadata, exception: Exception) {
                if (exception != null) {
                  log.warning(s"Failed to send: ${tweet.body}", exception)
                } else {
                  log.info(s"Sent tweet ${tweet.body}")
                }
              }
            })
          }
      })

      tweets ~> filter ~> out
    }

    g.run()
  }
}
