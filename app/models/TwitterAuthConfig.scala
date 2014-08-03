package models

import play.api.Configuration
import com.typesafe.config.ConfigFactory
import java.io.File
import models.TwitterAuthConfig.ConfigurationError

trait TwitterAuthConfig {

  lazy val consumer : TwitterConsumer = {
    val config = Configuration(ConfigFactory.parseFileAnySyntax(new File("conf/pac4j.conf")))
    (for {
      key <- config.getString("twitter.api.key")
      secret <- config.getString("twitter.api.secret")
    } yield {
      TwitterConsumer(key, secret)
    }).getOrElse(throw new ConfigurationError("conf/pac4j.conf is not define or format is invalid"))
  }

  lazy val callback = {
    val config = Configuration(ConfigFactory.parseFileAnySyntax(new File("conf/pac4j.conf")))
    (for {
      callback <- config.getString("twitter.api.callback")
    } yield {
      callback
    }).getOrElse(throw new ConfigurationError("conf/pac4j.conf is not define or format is invalid"))
  }
}

object TwitterAuthConfig {
  class ConfigurationError(m: String) extends RuntimeException(m)
}
case class TwitterConsumer(key: String, secret: String)