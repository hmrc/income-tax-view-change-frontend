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
import audit.models.TaxYearSummaryResponseAuditModel
import auth.MtdItUser
import config.featureswitch.{CodingOut, FeatureSwitching, ForecastCalculation}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates._
import forms.utils.SessionKeys.{calcPagesBackPage, gatewayPage}
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetailsErrorModel, FinancialDetailsModel}
import models.liabilitycalculation.viewmodels.TaxYearSummaryViewModel
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse, LiabilityCalculationResponseModel}
import models.nextUpdates.ObligationsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CalculationService, FinancialDetailsService, NextUpdatesService}
import views.html.TaxYearSummary

import java.net.URI
import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxYearSummaryController @Inject()(taxYearSummaryView: TaxYearSummary,
                                         authenticate: AuthenticationPredicate,
                                         calculationService: CalculationService,
                                         checkSessionTimeout: SessionTimeoutPredicate,
                                         financialDetailsService: FinancialDetailsService,
                                         itvcErrorHandler: ItvcErrorHandler,
                                         retrieveIncomeSourcesNoCache: IncomeSourceDetailsPredicateNoCache,
                                         retrieveNino: NinoPredicate,
                                         nextUpdatesService: NextUpdatesService,
                                         val retrieveBtaNavBar: NavBarPredicate,
                                         val auditingService: AuditingService)
                                        (implicit val appConfig: FrontendAppConfig,
                                         mcc: MessagesControllerComponents,
                                         val executionContext: ExecutionContext)
  extends BaseController with FeatureSwitching with I18nSupport {

  val action: ActionBuilder[MtdItUser, AnyContent] = checkSessionTimeout andThen authenticate andThen
    retrieveNino andThen retrieveIncomeSourcesNoCache andThen retrieveBtaNavBar

  private def showForecast(modelOpt: Option[TaxYearSummaryViewModel]): Boolean = {
    val isCrystalised = modelOpt.flatMap(_.crystallised).contains(true)
    val forecastDataPresent = modelOpt.flatMap(_.forecastIncome).isDefined && modelOpt.flatMap(_.forecastIncomeTaxAndNics).isDefined
    isEnabled(ForecastCalculation) && modelOpt.isDefined && !isCrystalised && forecastDataPresent
  }

  private def view(liabilityCalc: LiabilityCalculationResponseModel,
                   documentDetailsWithDueDates: List[DocumentDetailWithDueDate],
                   taxYear: Int,
                   obligations: ObligationsModel,
                   codingOutEnabled: Boolean,
                   backUrl: String,
                   origin: Option[String]
                  )(implicit mtdItUser: MtdItUser[_]): Result = {
    liabilityCalc match {
      case liabilityCalc: LiabilityCalculationResponse =>
        val taxYearSummaryViewModel: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalc)
        auditingService.extendedAudit(TaxYearSummaryResponseAuditModel(
          mtdItUser, documentDetailsWithDueDates, obligations, Some(taxYearSummaryViewModel)))

        Logger("application").info(
          s"[TaxYearSummaryController][view][$taxYear]] Rendered Tax year summary page with Calc data")

        Ok(taxYearSummaryView(
          taxYear = taxYear,
          modelOpt = Some(taxYearSummaryViewModel),
          charges = documentDetailsWithDueDates,
          obligations = obligations,
          codingOutEnabled = codingOutEnabled,
          backUrl = backUrl,
          showForecastData = showForecast(Some(taxYearSummaryViewModel)),
          origin = origin
        ))
      case error: LiabilityCalculationError if error.status == NOT_FOUND =>
        auditingService.extendedAudit(TaxYearSummaryResponseAuditModel(
          mtdItUser, documentDetailsWithDueDates, obligations, None))

        Logger("application").info(
          s"[TaxYearSummaryController][view][$taxYear]] Rendered Tax year summary page with No Calc data")

        Ok(taxYearSummaryView(
          taxYear = taxYear,
          modelOpt = None,
          charges = documentDetailsWithDueDates,
          obligations = obligations,
          codingOutEnabled = codingOutEnabled,
          backUrl = backUrl,
          showForecastData = true,
          origin = origin
        ))
      case _: LiabilityCalculationError =>
        Logger("application").error(
          s"[TaxYearSummaryController][view][$taxYear]] No new calc deductions data error found. Downstream error")
        itvcErrorHandler.showInternalServerError()
    }
  }

  private def withTaxYearFinancials(taxYear: Int)(f: List[DocumentDetailWithDueDate] => Future[Result])
                                   (implicit user: MtdItUser[AnyContent]): Future[Result] = {

    financialDetailsService.getFinancialDetails(taxYear, user.nino) flatMap {
      case financialDetails@FinancialDetailsModel(_, _, documentDetails, _) =>
        val docDetailsNoPayments = documentDetails.filter(_.paymentLot.isEmpty)
        val docDetailsCodingOut = docDetailsNoPayments.filter(_.isCodingOutDocumentDetail(isEnabled(CodingOut)))
        val documentDetailsWithDueDates: List[DocumentDetailWithDueDate] = {
          docDetailsNoPayments.filter(_.isNotCodingOutDocumentDetail).filter(_.originalAmountIsNotZero).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, financialDetails.findDueDateByDocumentDetails(documentDetail),
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        val documentDetailsWithDueDatesForLpi: List[DocumentDetailWithDueDate] = {
          docDetailsNoPayments.filter(_.isLatePaymentInterest).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, documentDetail.interestEndDate, isLatePaymentInterest = true,
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        val documentDetailsWithDueDatesCodingOutPaye: List[DocumentDetailWithDueDate] = {
          docDetailsCodingOut.filter(dd => dd.isPayeSelfAssessment && dd.originalAmountIsNotZero).map(
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
        Logger("application").error(s"[TaxYearSummaryController][withTaxYearFinancials] - Could not retrieve financial details for year: $taxYear")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  private def withObligationsModel(taxYear: Int)(implicit user: MtdItUser[AnyContent]) = {
    nextUpdatesService.getNextUpdates(
      fromDate = LocalDate.of(taxYear - 1, 4, 6),
      toDate = LocalDate.of(taxYear, 4, 5)
    )
  }

  private def getBackURL(referer: Option[String], origin: Option[String]): String = {
    referer.map(URI.create(_).getPath.equals(taxYearsUrl(origin))) match {
      case Some(true) => taxYearsUrl(origin)
      case Some(false) if referer.map(URI.create(_).getPath.equals(whatYouOweUrl(origin))).get => whatYouOweUrl(origin)
      case _ => homeUrl(origin)
    }
  }

  private def showTaxYearSummary(taxYear: Int, origin: Option[String]): Action[AnyContent] = action.async {
    implicit user =>
      withTaxYearFinancials(taxYear) { charges =>
        withObligationsModel(taxYear) flatMap {
          case obligationsModel: ObligationsModel =>
            val codingOutEnabled = isEnabled(CodingOut)
            calculationService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map { liabilityCalcResponse =>
              view(liabilityCalcResponse, charges, taxYear, obligationsModel, codingOutEnabled,
                backUrl = getBackURL(user.headers.get(REFERER), origin), origin = origin)
                .addingToSession(gatewayPage -> "taxYearSummary")
                .addingToSession(calcPagesBackPage -> "ITVC")
            }
          case _ => Future.successful(itvcErrorHandler.showInternalServerError())
        }
      }
  }

  def renderTaxYearSummaryPage(taxYear: Int, origin: Option[String] = None): Action[AnyContent] = {
    if (taxYear.toString.matches("[0-9]{4}")) {
      showTaxYearSummary(taxYear, origin)
    } else {
      action.async { implicit request =>
        Future.successful(itvcErrorHandler.showInternalServerError())
      }
    }
  }

  def taxYearsUrl(origin: Option[String]): String = controllers.routes.TaxYearsController.showTaxYears(origin).url

  def whatYouOweUrl(origin: Option[String]): String = controllers.routes.WhatYouOweController.show(origin).url


  def homeUrl(origin: Option[String]): String = controllers.routes.HomeController.show(origin).url
}
