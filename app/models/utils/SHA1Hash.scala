package models.utils

import scala.util.control.Exception._
import java.security.MessageDigest

trait SHA1Hash {

  def hash(seed : String) : Either[Throwable, String] = allCatch either {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(seed.getBytes)
    val hashValue = messageDigest.digest()
    hashValue.map(_.formatted("%02x"))
             .mkString
  }
}
