/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import audit.AuditingService
import audit.models.TaxYearOverviewResponseAuditModel
import auth.MtdItUser
import config.featureswitch.{CodingOut, FeatureSwitching, TxmEventsApproved}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates._
import forms.utils.SessionKeys
import models.calculation._
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetailsErrorModel, FinancialDetailsModel}
import models.nextUpdates.ObligationsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CalculationService, FinancialDetailsService, NextUpdatesService}
import views.html.TaxYearOverview

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxYearOverviewController @Inject()(taxYearOverviewView: TaxYearOverview,
                                          authenticate: AuthenticationPredicate,
                                          calculationService: CalculationService,
                                          checkSessionTimeout: SessionTimeoutPredicate,
                                          financialDetailsService: FinancialDetailsService,
                                          itvcErrorHandler: ItvcErrorHandler,
                                          retrieveIncomeSourcesNoCache: IncomeSourceDetailsPredicateNoCache,
                                          retrieveNino: NinoPredicate,
                                          nextUpdatesService: NextUpdatesService,
                                          val retrieveBtaNavBar: BtaNavBarPredicate,
                                          val auditingService: AuditingService)
                                     (implicit val appConfig: FrontendAppConfig,
                                      mcc: MessagesControllerComponents,
                                      val executionContext: ExecutionContext)
  extends BaseController with FeatureSwitching with I18nSupport {

  val action: ActionBuilder[MtdItUser, AnyContent] = checkSessionTimeout andThen authenticate andThen
    retrieveNino andThen retrieveIncomeSourcesNoCache andThen retrieveBtaNavBar

  private def withTaxYearFinancials(taxYear: Int)(f: List[DocumentDetailWithDueDate] => Future[Result])
                                   (implicit user: MtdItUser[AnyContent]): Future[Result] = {

    financialDetailsService.getFinancialDetails(taxYear, user.nino) flatMap {
      case financialDetails@FinancialDetailsModel(_, documentDetails, _) =>
        val docDetailsNoPayments = documentDetails.filter(_.paymentLot.isEmpty)
        val docDetailsCodingOut = docDetailsNoPayments.filter(_.isCodingOutDocumentDetail(isEnabled(CodingOut)))
        val documentDetailsWithDueDates: List[DocumentDetailWithDueDate] = {
          docDetailsNoPayments.filter(_.isNotCodingOutDocumentDetail).filter(_.originalAmountIsNotZero).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, financialDetails.getDueDateFor(documentDetail),
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        val documentDetailsWithDueDatesForLpi: List[DocumentDetailWithDueDate] = {
          docDetailsNoPayments.filter(_.latePaymentInterestAmount.isDefined).filter(_.latePaymentInterestAmountIsNotZero).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, documentDetail.interestEndDate, isLatePaymentInterest = true,
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        val documentDetailsWithDueDatesCodingOutPaye: List[DocumentDetailWithDueDate] = {
          docDetailsCodingOut.filter(dd => dd.isPayeSelfAssessment && dd.amountCodedOutIsNotZero).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, financialDetails.getDueDateFor(documentDetail),
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        val documentDetailsWithDueDatesCodingOut: List[DocumentDetailWithDueDate] = {
          docDetailsCodingOut.filter(dd => !dd.isPayeSelfAssessment && dd.originalAmountIsNotZero).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, financialDetails.getDueDateFor(documentDetail),
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        f(documentDetailsWithDueDates ++ documentDetailsWithDueDatesForLpi ++ documentDetailsWithDueDatesCodingOutPaye ++ documentDetailsWithDueDatesCodingOut)
      case FinancialDetailsErrorModel(NOT_FOUND, _) => f(List.empty)
      case _ =>
        Logger("application").error(s"[TaxYearOverviewController][withTaxYearFinancials] - Could not retrieve financial details for year: $taxYear")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  private def withObligationsModel(taxYear: Int)(implicit user: MtdItUser[AnyContent]) = {
    nextUpdatesService.getNextUpdates(
      fromDate = LocalDate.of(taxYear - 1, 4, 6),
      toDate = LocalDate.of(taxYear, 4, 5)
    )
  }

  private def getBackURL(referer: Option[String]): String = {
    referer.map(_.contains(backHomeUrl)) match {
      case Some(true) => backHomeUrl
      case _ => backTaxYearsUrl
    }
  }

  private def showTaxYearOverview(taxYear: Int): Action[AnyContent] = action.async {
    implicit user =>
      calculationService.getCalculationDetail(user.nino, taxYear) flatMap {
        case CalcDisplayModel(_, calcAmount, calculation, _) =>
          withTaxYearFinancials(taxYear) { chargesValue =>
            withObligationsModel(taxYear) map {
              case obligationsModel: ObligationsModel =>
                if (isEnabled(TxmEventsApproved)) {
                  auditingService.extendedAudit(TaxYearOverviewResponseAuditModel(user, calculation, chargesValue, obligationsModel))
                }
                val codingOutEnabled = isEnabled(CodingOut)
                Ok(taxYearOverviewView(taxYear, overviewOpt = Some(CalcOverview(calculation)),
                  charges = chargesValue, obligations = obligationsModel, codingOutEnabled = codingOutEnabled,
                  btaNavPartial = user.btaNavPartial, backUrl = getBackURL(user.headers.get(REFERER)))
                ).addingToSession(SessionKeys.chargeSummaryBackPage -> "taxYearOverview")
              case _ => itvcErrorHandler.showInternalServerError()
            }
          }
        case CalcDisplayNoDataFound =>
          val codingOutEnabled = isEnabled(CodingOut)
          withTaxYearFinancials(taxYear) { chargesValue =>
            withObligationsModel(taxYear) map {
              case obligationsModel: ObligationsModel => Ok(taxYearOverviewView(taxYear, overviewOpt = None, charges = chargesValue,
                obligations = obligationsModel, codingOutEnabled = codingOutEnabled,
                btaNavPartial = user.btaNavPartial, backUrl = getBackURL(user.headers.get(REFERER))))
                .addingToSession(SessionKeys.chargeSummaryBackPage -> "taxYearOverview")
              case _ => itvcErrorHandler.showInternalServerError()
            }
          }
        case CalcDisplayError =>
          Logger("application").error(s"[CalculationController][showTaxYearOverview] - Could not retrieve calculation for year $taxYear")
          Future.successful(itvcErrorHandler.showInternalServerError())
      }
  }

  def renderTaxYearOverviewPage(taxYear: Int): Action[AnyContent] = {
    if (taxYear > 0) {
        showTaxYearOverview(taxYear)
    } else {
      action.async { implicit request =>
        Future.successful(itvcErrorHandler.showInternalServerError())
      }
    }
  }

  lazy val backTaxYearsUrl: String = controllers.routes.TaxYearsController.viewTaxYears().url
  lazy val backHomeUrl: String = controllers.routes.HomeController.home().url
}

