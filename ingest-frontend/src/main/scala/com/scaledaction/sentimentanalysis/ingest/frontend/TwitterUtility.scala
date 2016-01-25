package com.scaledaction.sentimentanalysis.ingest.frontend

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import twitter4j._
import twitter4j.auth.AccessToken
import twitter4j.conf.ConfigurationBuilder

import scala.collection.JavaConverters._
import scala.collection._
import scala.util.Try

final case class Author(handle: String)

final case class Hashtag(name: String)

case class Tweet(author: Author, timestamp: Long, body: String) {
  def hashtags: Set[Hashtag] =
    body.split(" ").collect { case t if t.startsWith("#") => Hashtag(t) }.toSet
}

final object EmptyTweet extends Tweet(Author(""), 0L, "")

object CredentialsUtils {
  val config = ConfigFactory.load().getConfig("twitter")
  val consumerKey: String = config.getString("consumerKey")
  val consumerSecret: String = config.getString("consumerSecret")
  val tokenKey: String = config.getString("tokenKey")
  val tokenSecret: String = config.getString("tokenSecret")
}

object TwitterClient {

  def apply(): Twitter = {
    val factory = new TwitterFactory(new ConfigurationBuilder().build())
    val t = factory.getInstance()
    t.setOAuthConsumer(CredentialsUtils.consumerKey, CredentialsUtils.consumerSecret)
    t.setOAuthAccessToken(new AccessToken(CredentialsUtils.tokenKey, CredentialsUtils.tokenSecret))
    t
  }
}

class TwitterStreamClient(val actorSystem: ActorSystem) {
  val factory = new TwitterStreamFactory(new ConfigurationBuilder().build())
  val twitterStream = factory.getInstance()

  def init = {
    twitterStream.setOAuthConsumer(CredentialsUtils.consumerKey, CredentialsUtils.consumerSecret)
    twitterStream.setOAuthAccessToken(new AccessToken(CredentialsUtils.tokenKey, CredentialsUtils.tokenSecret))
    twitterStream.addListener(simpleStatusListener)
    twitterStream.sample
  }

  def simpleStatusListener = new StatusListener() {
    def onStatus(s: Status) {
      actorSystem.eventStream.publish(Tweet(Author(s.getUser.getScreenName), s.getCreatedAt.getTime, s.getText))
    }

    def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {}

    def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {}

    def onException(ex: Exception) {
      ex.printStackTrace
    }

    def onScrubGeo(arg0: Long, arg1: Long) {}

    def onStallWarning(warning: StallWarning) {}
  }

  def stop = {
    twitterStream.cleanUp
    twitterStream.shutdown
  }
}
