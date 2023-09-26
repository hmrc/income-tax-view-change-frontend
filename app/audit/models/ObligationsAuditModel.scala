package audit.models

import audit.Utilities
import auth.MtdItUser
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import models.incomeSourceDetails.viewmodels.{CeasedBusinessDetailsViewModel, ObligationsViewModel, ViewBusinessDetailsViewModel, ViewPropertyDetailsViewModel}
import play.api.libs.json.{JsValue, Json}

case class ObligationsAuditModel(incomeSourceType: IncomeSourceType,
                                 obligations: ObligationsViewModel,
                                 businessName: Option[String]
                                )(implicit user: MtdItUser[_]) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.Obligations

  override val auditType: String = enums.AuditType.Obligations

  val journey: String = incomeSourceType match {
    case SelfEmployment => "SE"
    case UkProperty => "UKPROPERTY"
    case ForeignProperty => "FOREIGNPROPERTY"
  }

  val name: String = incomeSourceType match {
    case SelfEmployment => businessName.getOrElse("Self Employment Business")
    case UkProperty => ???
    case ForeignProperty => ???
  }

  override val detail: JsValue = {
    Utilities.userAuditDetails(user) ++
      Json.obj(
        "journeyType" -> journey,
        "incomeSourceInfo" ->
          Json.obj(
            "businessName" -> name,
            "reportingMethod" -> "",
            "taxYear" -> s"${obligations.currentTaxYear}-${obligations.currentTaxYear+1}"
          )
      )
    //quarterlyUpdates
    //EOPStatement
    //finalDeclaration
  }

}
