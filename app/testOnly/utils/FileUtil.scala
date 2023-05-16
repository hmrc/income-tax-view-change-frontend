package testOnly.utils

import models.core.Nino
import play.api.Play
import testOnly.models.{UserCredentials, UserRecord}
import utils.DelegatedEnrolmentValues

import scala.util.Try

object FileUtil {

  def getUsersFromFile(path: String): Either[Throwable, List[UserRecord]] = {
    Try {
      val resource = Play.getClass.getResourceAsStream(path)
      val lines = scala.io.Source.fromInputStream(resource).mkString("").split("\n").toList
      lines.flatMap(line => {
        Try {
          val recs = line.split('$')
          UserRecord(recs(0), recs(1), recs(2), recs(3))
        }.toOption
      })
    }.toEither
  }

  def getUtrBtyNino(nino: String): Either[Throwable, Option[String]] = {
    FileUtil.getUsersFromFile("/data/users.txt") match {
      case Left(ex) => Left(ex)
      case Right(records: Seq[UserRecord]) => Right(records.find(_.nino == nino).map(_.utr))
    }
  }

  def getUserCredentials(nino: String): Either[Throwable, UserCredentials] = {
    FileUtil.getUsersFromFile("/data/users.txt") match {
      case Left(ex) => Left(ex)
      case Right(records: Seq[UserRecord]) =>
        records.find(record => record.nino == nino) match {
          case None =>
            Left(new RuntimeException("Can not find user by nino"))
          case Some(record) =>
            Right(
              UserCredentials(credId = UserCredentials.credId,
                affinityGroup = "Individual",
                confidenceLevel = 250,
                credentialStrength = "strong",
                Role = "User",
                enrolmentData = EnrolmentValues(record.mtditid, record.utr),
                delegatedEnrolmentData = None
              )
            )
        }
    }
  }

  def getAgentCredentials(nino: Nino): Either[Throwable, UserCredentials] = {
    FileUtil.getUsersFromFile("/data/users.txt") match {
      case Left(ex) => Left(ex)
      case Right(records) =>
        records.find(record => record.nino == nino.value) match {
          case None =>
            Left(new RuntimeException("Can not find user by nino"))
          case Some(record) =>
            Right(
              UserCredentials(credId = UserCredentials.credId, //"6528180096307862",
                affinityGroup = "Agent",
                confidenceLevel = 250,
                credentialStrength = "strong",
                Role = "User",
                enrolmentData = EnrolmentValues(record.mtditid, record.utr),
                delegatedEnrolmentData = Some(DelegatedEnrolmentValues(record.mtditid, record.utr))
              )
            )
        }
    }
  }

}
