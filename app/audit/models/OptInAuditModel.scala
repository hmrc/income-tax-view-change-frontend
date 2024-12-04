package audit.models

import play.api.libs.json.{JsValue, Json, OFormat}

case class Outcome(
                  isSuccessful: Boolean,
                  failureCategory: Option[String],
                  failureReason: Option[String]
                  )

object Outcome {
  implicit val format: OFormat[Outcome] = Json.format[Outcome]
}

case class OptInAuditModel() extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.OptInQuarterlyReportingRequest

  override val auditType: String = enums.AuditType.OptInQuarterlyReportingRequest

  override val detail: JsValue = Json.toJson(this)
}

object OptInAuditModel {
  implicit val format: OFormat[OptInAuditModel] = Json.format[OptInAuditModel]
}
