/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.scaledaction.core.config

import java.net.InetAddress
import com.typesafe.config.{ Config, ConfigFactory }
import scala.util.{ Try, Success, Failure }

/**
 * Application settings. First attempts to acquire from the deploy environment.
 * If not exists, then from -D java system properties, else a default config.
 *
 * Settings in the environment such as: SPARK_HA_MASTER=local[10] is picked up first.
 *
 * Settings from the command line in -D will override settings in the deploy environment.
 * For example: sbt -Dspark.master="local[12]" run
 *
 * If you have not yet used Typesafe Config before, you can pass in overrides like so:
 *
 * {{{
 *   new Settings(ConfigFactory.parseString("""
 *      spark.master = "some.ip"
 *   """))
 * }}}
 *
 * Any of these can also be overriden by your own application.conf.
 *
 * @param conf Optional config for test
 */

protected class AppConfig(rootConfig: Config) {
  def getString(key: String): String = rootConfig.getString(key)
}

//final class AppConfig(conf: Option[Config] = None) extends Serializable {
trait HasAppConfig extends Serializable {

  val localAddress = InetAddress.getLocalHost.getHostAddress

  //  val rootConfig = conf match {
  //    case Some(c) => c.withFallback(ConfigFactory.load)
  //    case _ => ConfigFactory.load
  //  }
  val rootConfig = ConfigFactory.load

  //TODO - Document me. Returns the value obtained in the following order of precedence: environment variable, java system property, config file, specified default value
  /** Attempts to acquire from environment, then java system properties. */
  def getRequiredValue(envVarName: String, config: (Config, String), defaultValue: String): String = {
    val (configRoot, configName) = config
    val envVar = Try(sys.env(envVarName))
    envVar match {
      case Success(v) => v
      case Failure(_) => getRequiredValue(config, defaultValue)
    }
  }

  def getRequiredValue(config: (Config, String), defaultValue: String): String = {
    val (configRoot, configName) = config
    Try(configRoot.getString(configName)) getOrElse defaultValue
  }

  def getRequiredValue(envVarName: String, config: (Config, String), defaultValue: Int): Int = {
    val (configRoot, configName) = config
    val envVar = Try(sys.env(envVarName).toInt)
    envVar match {
      case Success(v) => v
      case Failure(_) => Try(configRoot.getInt(configName)) getOrElse defaultValue
    }
  }

  import akka.util.Timeout
  import scala.concurrent.duration._

  protected def stringToDuration(t: String): Timeout = {
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}
