package audit.models

import audit.Utilities
import auth.MtdItUser
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import models.incomeSourceDetails.viewmodels.{CeasedBusinessDetailsViewModel, DatesModel, ObligationsViewModel, ViewBusinessDetailsViewModel, ViewPropertyDetailsViewModel}
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
    case UkProperty => "UK property"
    case ForeignProperty => "Foreign property"
  }

  val quarterly: Seq[(Int, Seq[DatesModel])] = {
    ((if (obligations.quarterlyObligationsDatesYearOne.nonEmpty)
      Seq((obligations.quarterlyObligationsDatesYearOne.head.inboundCorrespondenceFrom.getYear, obligations.quarterlyObligationsDatesYearOne))
    else
      Seq()
      )
    ++
    (if (obligations.quarterlyObligationsDatesYearTwo.nonEmpty)
      Seq((obligations.quarterlyObligationsDatesYearTwo.head.inboundCorrespondenceFrom.getYear, obligations.quarterlyObligationsDatesYearTwo))
    else
      Seq()
      ))

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
          ),
        "quarterlyUpdates" ->
          quarterly.map { value =>
            Json.obj(
              "taxYear" -> s"${value._1}-${value._1+1}",
              "quarter" -> value._2.map { entry =>
                Json.obj(
                  "startDate" -> entry.inboundCorrespondenceFrom,
                  "endDate" -> entry.inboundCorrespondenceTo,
                  "deadline" -> entry.inboundCorrespondenceDue
                )
              }
            )
          },
        "EOPstatement" ->
          obligations.eopsObligationsDates.map { eops =>
            Json.obj(
              "taxYearStartDate" -> eops.inboundCorrespondenceFrom,
              "taxYearEndDate" ->  eops.inboundCorrespondenceDue,
              "deadline" -> eops.inboundCorrespondenceDue
            )
          },
        "finalDeclaration" ->
          obligations.finalDeclarationDates.map { finalDec =>
            Json.obj(
              "taxYearStartDate" -> finalDec.inboundCorrespondenceFrom,
              "taxYearEndDate" -> finalDec.inboundCorrespondenceDue,
              "deadline" -> finalDec.inboundCorrespondenceDue
            )
          }
      )
    //reportingMethod
  }
}
