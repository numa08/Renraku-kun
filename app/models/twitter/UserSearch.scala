package models.twitter

import twitter4j.Twitter
import akka.actor.Actor

case class SearchById(id : Long)
case class SearchByName(name : String)

class UserSearch(twitter : Twitter) extends Actor {

  def receive = {
    case SearchById(id) => {
      val user = twitter.users.showUser(id)
      sender ! user
    }
    case SearchByName(name) => {
      val user = twitter.users.showUser(name)
      sender ! user
    }
  }
}
