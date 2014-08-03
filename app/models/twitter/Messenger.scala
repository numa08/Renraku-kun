package models.twitter

import twitter4j.Twitter
import akka.actor.Actor

case class DirectMessage(id : Long, text : String)

class Messenger(tw : Twitter) extends Actor {

  def receive = {
    case DirectMessage(id, text) => sender ! tw.directMessages().sendDirectMessage(id, text)
  }
}
