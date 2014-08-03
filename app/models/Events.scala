package models

import play.api.db.DB
import anorm._
import play.api.Play.current
import models.utils.SHA1Hash
import play.api.Logger

case class Event(id : String, title : String, ownerId : Long)

object Event extends SHA1Hash {

  def apply(title : String, ownerId : Long) : Event = {
    val id = hash(System.currentTimeMillis().toString) match {
      case Left(e) => {
        Logger.error("Hash generate error", e)
        System.currentTimeMillis().toString
      }
      case Right(s) => s
    }

    new Event(id, title, ownerId)
  }

}

object Events {

  val rowToEvent = (row : Row)  => Event(row[String]("id"), row[String]("title"), row[Long]("owner_id"))

  def my(ownerId : Long) : List[Event] = DB.withConnection{ implicit c =>
    SQL("select * from Event where owner_id = {oi}")
     .on('oi -> ownerId)
     .map(row => rowToEvent(row))
     .list()
     .toList
  }

  def create(title : String, ownerId : Long) : Unit = {
    val event = Event(title, ownerId)
    DB.withConnection{ implicit  c =>
      SQL("insert into Event(id, title, owner_id) values ({i},{t}, {od})")
       .on('i -> event.id,
           't -> event.title,
           'od -> event.ownerId)
       .executeUpdate()
    }
  }

  def event(id : String) : Option[Event] = DB.withConnection{ implicit c =>
    SQL("select * from Event where id = {i}")
     .on('i -> id)
     .map(row => rowToEvent(row))
     .singleOpt()
  }
}