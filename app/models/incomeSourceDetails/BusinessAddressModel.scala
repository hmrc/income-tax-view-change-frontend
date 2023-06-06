package models.incomeSourceDetails

import play.api.libs.json.{Json, OFormat}

case class Address(lines: Seq[String], postcode: Option[String]) {
  override def toString: String = s"${lines.mkString(", ")}${postcode.map(t => s", $t").getOrElse("")}"
}

case class BusinessAddressModel(auditRef: String,
                                address: Address)

object Address {
  implicit val format: OFormat[Address] = Json.format[Address]
}

object BusinessAddressModel {
  implicit val format: OFormat[BusinessAddressModel] = Json.format[BusinessAddressModel]
}
