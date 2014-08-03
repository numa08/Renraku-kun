package controllers

import play.api._
import play.api.mvc.{Results, Result, RequestHeader}
import scala.concurrent.Future
import models.TwitterAuthConfig
import org.pac4j.oauth.client.TwitterClient
import org.pac4j.core.client.Clients
import org.pac4j.play.Config

object Global extends GlobalSettings with TwitterAuthConfig {
  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = Future.successful(Results.InternalServerError)

  override def onStart(app: Application): Unit = {
    val ti = consumer
    val tc = new TwitterClient(ti.key, ti.secret)
    val cl = new Clients(callback, tc)
    Config.setClients(cl)
  }
}
