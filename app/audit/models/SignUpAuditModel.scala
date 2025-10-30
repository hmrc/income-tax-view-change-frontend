package audit.models

import auth.MtdItUser
import play.api.libs.json.{JsValue, Json}
import audit.Utilities
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.ITSAStatus

case class SignUpAuditModel(signUpTaxYear: TaxYear,
                            currentYearItsaStatus: ITSAStatus,
                            nextYearItsaStatus: ITSAStatus)(implicit user: MtdItUser[_]) extends ExtendedAuditModel {
  override val transactionName: String = enums.TransactionName.SignUpTaxYearsPage
  override val auditType: String = enums.AuditType.SignUpTaxYearsPage

  override val detail: JsValue =
    Utilities.userAuditDetails(user) ++
      Json.obj(
        "signUpTaxYear" -> signUpTaxYear,
        "currentTaxYearItsaStatus" -> currentYearItsaStatus,
        "nextTaxYearItsaStatus" -> nextYearItsaStatus
      )
}
