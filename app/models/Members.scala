package models

import play.api.db.DB
import play.api.Play.current
import anorm._

object Members {
  def onEvent(ev : Event) : List[Member] = Members.onEvent(ev.id)

  def onEvent(id : String) : List[Member] = DB.withConnection{ implicit c =>
    SQL("select * from Member where event = {eid}")
      .on('eid -> id)
      .map{row => Member(row[Long]("id"), row[String]("name"), row[String]("event"))}
      .list()
      .toList
  }

  def join(id : Long, name : String, eventId : String) : Unit = {
    val member = Member(id, name, eventId)
    DB.withConnection{ implicit c =>
      SQL("insert into Member(id, event, name) values ({i}, {eid}, {n})")
      .on('i -> member.id,
          'eid -> member.eventId,
          'n -> member.name)
      .executeUpdate()
    }
  }
}

case class Member(id : Long, name : String, eventId : String)
