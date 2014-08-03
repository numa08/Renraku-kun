package controllers

import play.api.mvc._
import models.{Members, Events, TwitterAuthConfig}
import org.pac4j.play.scala.ScalaController
import twitter4j.{User, TwitterFactory}
import org.pac4j.oauth.profile.twitter.TwitterProfile
import play.api.data.{Forms, Form}
import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import models.twitter.{DirectMessage, Messenger, SearchByName, UserSearch}
import twitter4j.auth.AccessToken
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Logger

object Application extends ScalaController with TwitterAuthConfig {

  lazy val twitter = {
    val tw = new TwitterFactory().getInstance()
    tw.setOAuthConsumer(consumer.key, consumer.secret)
    tw
  }

  val eventForm = Form("title" -> Forms.nonEmptyText)

  val memberForm = Form("members" -> Forms.nonEmptyText)

  val messageForm = Form("message" -> Forms.nonEmptyText)

  def index = Action { request =>
    val newSession = getOrCreateSessionId(request)
    val content = Option(getUserProfile(request))
                  .collect{case p : TwitterProfile => p}
                  .fold {
                    val loginLocation = getRedirectAction(request, newSession, "TwitterClient", "/").getLocation
                    views.html.login_twitter(loginLocation)
                  }{p =>
                    val events = Events.my(p.getId.toLong)
                    views.html.events(events, eventForm)
                  }

    Ok(views.html.index(content))
  }

  def newEvent = RequiresAuthentication("TwitterClient") { p =>
    val twitterProfile = p.asInstanceOf[TwitterProfile]
    Action {implicit request =>
      eventForm.bindFromRequest().fold(
        error => {
          val content = views.html.events(Events.my(twitterProfile.getId.toLong), error)
          BadRequest(views.html.index(content))
        },
        title => {
          Events.create(title, twitterProfile.getId.toLong)
          Redirect(routes.Application.index())
        }
       )
    }
  }

  def event(id : String) = RequiresAuthentication("TwitterClient") {p =>
    Action{implicit request =>
      Events.event(id)
            .fold(BadRequest("Event not find"))
            { ev =>
              val members = Members.onEvent(ev)
              val content = views.html.event(ev, members,  memberForm, messageForm)
              Ok(content)
            }
    }
  }

  def deleteMember(id : String, mId : Long) = TODO

  def sendMessage(id : String) = RequiresAuthentication("TwitterClient"){ p =>
    val twitterProfile = p.asInstanceOf[TwitterProfile]
    Action { implicit request =>
      messageForm.bindFromRequest().fold(
        error => {
          Events.event(id)
                .fold(Redirect(routes.Application.event(id))) { e =>
            val members = Members.onEvent(e)
            val content = views.html.event(e, members, memberForm, error)
            BadRequest(content)
          }
        },
        message => {
          implicit val timeout = Timeout(10 seconds)
          val ac = new AccessToken(twitterProfile.getAccessToken, twitterProfile.getAccessSecret)
          twitter.setOAuthAccessToken(ac)
          val system = ActorSystem("SendMessage")
          val messenger = system.actorOf(Props(new Messenger(twitter)))
          Members.onEvent(id)
                 .map(m => DirectMessage(m.id, message))
                 .foreach(m => messenger ! m)

          Redirect(routes.Application.event(id))
        }
      )
    }
  }

  def addMember(id : String) = RequiresAuthentication("TwitterClient") { p =>
    val twitterProfile = p.asInstanceOf[TwitterProfile]
    Action { implicit request =>
      memberForm.bindFromRequest().fold(
        error => {
           val result = Events.event(id)
            .fold(Redirect(routes.Application.event(id))) { e =>
            val members = Members.onEvent(e)
            val content = views.html.event(e, members, error, messageForm)
            BadRequest(content)
          }
          result
        },
        members => {
          implicit val timeout = Timeout(10 seconds)
          val ac = new AccessToken(twitterProfile.getAccessToken, twitterProfile.getAccessSecret)
          twitter.setOAuthAccessToken(ac)
          val system = ActorSystem("SearchUser")
          val users = system.actorOf(Props(new UserSearch(twitter)))
          members.split(",")
                 .map(n => (users ? SearchByName(n)).mapTo[User])
                 .toList
                 .foreach{ f =>
                    f.onSuccess{
                      case u : User => Members.join(u.getId, u.getScreenName, id)
                    }
                    Await.result(f, Duration.Inf)
                 }
          Redirect(routes.Application.event(id))
        }
      )
    }
  }
}