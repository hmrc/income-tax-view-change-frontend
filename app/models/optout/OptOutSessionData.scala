package models.optout

import models.incomeSourceDetails.CeaseIncomeSourceData
import play.api.libs.json.{Json, OFormat}

case class OptOutSessionData(intent: Option[String]) {

  val intentField: String = "intent"

  def getJSONKeyPath(name: String): String = s"optOutSessionData.$name"
}

object OptOutSessionData {
  implicit val format: OFormat[OptOutSessionData] = Json.format[OptOutSessionData]
}
