/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package audit.models

import audit.Utilities.userAuditDetails
import auth.MtdItUser
import implicits.ImplicitDateParser
import models.liabilitycalculation.Messages
import models.liabilitycalculation.viewmodels.TaxYearSummaryViewModel
import models.obligations.ObligationWithIncomeType
import models.taxyearsummary.TaxYearSummaryChargeItem
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import utils.Utilities._

case class TaxYearSummaryResponseAuditModel(mtdItUser: MtdItUser[_],
                                            messagesApi: MessagesApi,
                                            taxYearSummaryViewModel: TaxYearSummaryViewModel,
                                            messages: Option[Messages] = None
                                           ) extends ExtendedAuditModel with ImplicitDateParser with PaymentSharedFunctions  {


  override val transactionName: String = enums.TransactionName.TaxYearOverviewResponse
  override val auditType: String = enums.AuditType.TaxYearOverviewResponse


  private val taxYearSummaryJson = {
    Json.obj() ++
      ("calculationDate", taxYearSummaryViewModel.calculationSummary.map(_.timestamp)) ++
      ("calculationAmount", taxYearSummaryViewModel.calculationSummary.map(_.taxDue)) ++
      ("isCrystallised", taxYearSummaryViewModel.calculationSummary.map(_.crystallised)) ++
      ("forecastAmount", taxYearSummaryViewModel.calculationSummary.map(_.forecastIncome))
  }

  private def unattendedCalcReason(): JsObject = {
    if (taxYearSummaryViewModel.calculationSummary.exists(_.unattendedCalc)) {
      Json.obj("calculationReason" -> "Unattended Calculation")
    } else {
      Json.obj("calculationReason" -> "customerRequest")
    }
  }

  private val errorMessages = {
    implicit val lang = Lang("GB")

    def isMultiLineErrorMessage(messageId: String): Boolean = messageId == "C15104" || messageId == "C15322" || messageId == "C159028"

    val (messageKeyPrefix, isAgent) = mtdItUser.userType match {
      case Some(Agent) => ("tax-year-summary.agent.message", true)
      case _ => ("tax-year-summary.message", false)
    }
    val msg = messages.map(_.getErrorMessageVariables(messagesApi, isAgent)).map(_.map(message => {
      val id = message.id
      val text = message.text
      val keyPrefix = s"$messageKeyPrefix.$id"

      if (isMultiLineErrorMessage(id)) {
        val result = messagesApi(s"$keyPrefix.1", text)

        (2 to 4).foldLeft(result)((r, v) => {
          val key = s"$keyPrefix.$v"
          if (messagesApi.isDefinedAt(key)) s"$r ${messagesApi(key, text)}," else r
        }).reverse.tail.reverse

      } else {
        messagesApi(keyPrefix, text)
      }
    }))

    Json.obj() ++ ("errors", msg)
  }


  private val calculationDetails = {
    Json.obj() ++
      unattendedCalcReason() ++
      ("income", taxYearSummaryViewModel.calculationSummary.map(_.income)) ++
      ("allowancesAndDeductions", taxYearSummaryViewModel.calculationSummary.map(_.deductions)) ++
      ("taxableIncome", taxYearSummaryViewModel.calculationSummary.map(_.totalTaxableIncome)) ++
      ("taxDue", taxYearSummaryViewModel.calculationSummary.map(_.taxDue)) ++
      errorMessages
  }

  private def paymentsJson(docDateDetail: TaxYearSummaryChargeItem): JsObject = {
    Json.obj("paymentType" -> getChargeType(docDateDetail, false),
      "underReview" -> docDateDetail.dunningLock,
      "status" -> docDateDetail.getChargePaidStatus) ++
      ("amount", Option(docDateDetail.originalAmount)) ++
      ("dueDate", docDateDetail.dueDate)
  }

  private val forecastJson: JsObject = Json.obj() ++
    ("income", taxYearSummaryViewModel.calculationSummary.map(_.forecastIncome)) ++
    ("taxableIncome", taxYearSummaryViewModel.calculationSummary.map(_.forecastIncome)) ++
    ("taxDue", taxYearSummaryViewModel.calculationSummary.map(_.forecastIncomeTaxAndNics)) ++
    ("totalAllowancesAndDeductions", taxYearSummaryViewModel.calculationSummary.map(_.forecastAllowancesAndDeductions))

  private def paymentsJsonLPI(docDateDetail: TaxYearSummaryChargeItem): JsObject = {
    Json.obj("paymentType" -> getChargeType(docDateDetail, latePaymentCharge = true),
      "underReview" -> docDateDetail.dunningLock,
      "status" -> docDateDetail.getInterestPaidStatus) ++
      ("amount", docDateDetail.accruingInterestAmount) ++
      ("dueDate", docDateDetail.dueDate)
  }

  private val paymentsDetails: Seq[JsObject] = taxYearSummaryViewModel.charges.map(paymentsJson) ++
    taxYearSummaryViewModel.charges.filter(_.accruingInterestAmount.isDefined).map(paymentsJsonLPI)

  private def getObligationsType(obligationType: String) = {
    obligationType match {
      case "Property" => "Property income"
      case "Business" => "Business"
      case "Crystallisation" => "All income sources"
      case _ => obligationType
    }
  }

  private def getUpdateType(updateType: String) = {
    updateType match {
      case "Quarterly" => "Quarterly Update"
      case "Crystallisation" => "Final Declaration"
      case _ => updateType
    }
  }

  private def updatesJson(updates: ObligationWithIncomeType): JsObject = {
    Json.obj("updateType" -> getUpdateType(updates.obligation.obligationType),
      "incomeSource" -> getObligationsType(updates.incomeType)) ++
      ("dateSubmitted", updates.obligation.dateReceived)
  }

  private val updatesDetail: Seq[JsObject] = taxYearSummaryViewModel.obligations.allDeadlinesWithSource(true)(mtdItUser).map(updatesJson)

  override val detail: JsValue = {
    userAuditDetails(mtdItUser) ++
      Json.obj("taxYearOverview" -> taxYearSummaryJson,
        "forecast" -> forecastJson,
        "calculation" -> calculationDetails,
        "payments" -> paymentsDetails,
        "updates" -> updatesDetail)
  }
}
