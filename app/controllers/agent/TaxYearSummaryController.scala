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

package controllers.agent

import audit.AuditingService
import audit.models.TaxYearSummaryResponseAuditModel
import auth.MtdItUser
import config.featureswitch.{CodingOut, FeatureSwitching, ForecastCalculation}
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import forms.utils.SessionKeys.{calcPagesBackPage, gatewayPage}
import implicits.ImplicitDateFormatter
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetailsErrorModel, FinancialDetailsModel}
import models.liabilitycalculation.viewmodels.TaxYearSummaryViewModel
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse, LiabilityCalculationResponseModel}
import models.nextUpdates.ObligationsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.TaxYearSummary

import java.net.URI
import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxYearSummaryController @Inject()(taxYearSummary: TaxYearSummary,
                                         val authorisedFunctions: AuthorisedFunctions,
                                         calculationService: CalculationService,
                                         financialDetailsService: FinancialDetailsService,
                                         incomeSourceDetailsService: IncomeSourceDetailsService,
                                         nextUpdatesService: NextUpdatesService,
                                         auditingService: AuditingService,
                                         dateService: DateService
                                         )(implicit val appConfig: FrontendAppConfig,
                                           val languageUtils: LanguageUtils,
                                           mcc: MessagesControllerComponents,
                                           implicit val ec: ExecutionContext,
                                           val itvcErrorHandler: AgentItvcErrorHandler)
  extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  lazy val agentTaxYearsUrl: String = controllers.routes.TaxYearsController.showAgentTaxYears().url
  lazy val agentHomeUrl: String = controllers.routes.HomeController.showAgent().url
  lazy val agentWhatYouOweUrl: String = controllers.routes.WhatYouOweController.showAgent().url

  val getCurrentTaxYearEnd: Int = {
    val currentDate: LocalDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }

  def show(taxYear: Int): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = false) flatMap { implicit mtdItUser =>
        withTaxYearFinancials(taxYear) { documentDetailsWithDueDates =>
          withObligationsModel(taxYear) { obligations =>
            calculationService.getLiabilityCalculationDetail(getClientMtditid, getClientNino, taxYear).map { liabilityCalcResponse =>
              view(liabilityCalcResponse, documentDetailsWithDueDates, taxYear, obligations, getBackURL(request.headers.get(REFERER)))
                .addingToSession(gatewayPage -> "taxYearSummary")
                .addingToSession(calcPagesBackPage -> "ITVC")(request)
            }
          }
        }
      }
  }

  private def getBackURL(referer: Option[String]): String = {
    referer.map(URI.create(_).getPath.equals(agentTaxYearsUrl)) match {
      case Some(true) => agentTaxYearsUrl
      case Some(false) if referer.map(URI.create(_).getPath.equals(agentWhatYouOweUrl)).get => agentWhatYouOweUrl
      case _ => agentHomeUrl
    }
  }

  private def showForecast(modelOpt: Option[TaxYearSummaryViewModel], taxYear: Int, currentTaxYear: Int) : Boolean = {
    val isCrystalised = modelOpt.flatMap(_.crystallised).contains(true)
    val isCurrentTaxYear = taxYear == currentTaxYear
    val forecastDataPresent = modelOpt.flatMap(_.forecastIncome).isDefined && modelOpt.flatMap(_.forecastIncomeTaxAndNics).isDefined
    isEnabled(ForecastCalculation) && modelOpt.isDefined && !isCrystalised && isCurrentTaxYear && forecastDataPresent
  }

  private def view(liabilityCalc: LiabilityCalculationResponseModel,
                   documentDetailsWithDueDates: List[DocumentDetailWithDueDate],
                   taxYear: Int,
                   obligations: ObligationsModel,
                   backUrl: String
                  )(implicit mtdItUser: MtdItUser[_]): Result = {
    liabilityCalc match {
      case liabilityCalc: LiabilityCalculationResponse =>
        val taxYearSummaryViewModel: TaxYearSummaryViewModel = TaxYearSummaryViewModel(liabilityCalc)
        auditingService.extendedAudit(TaxYearSummaryResponseAuditModel(
          mtdItUser, documentDetailsWithDueDates, obligations, Some(taxYearSummaryViewModel)))

        Logger("application").info(
          s"[Agent][TaxYearSummaryController][view][$taxYear]] Rendered Tax year summary page with Calc data")

        Ok(taxYearSummary(
          taxYear = taxYear,
          modelOpt = Some(taxYearSummaryViewModel),
          charges = documentDetailsWithDueDates,
          obligations = obligations,
          codingOutEnabled = isEnabled(CodingOut),
          backUrl = backUrl,
          isAgent = true,
          showForecastData = showForecast(Some(taxYearSummaryViewModel), taxYear, dateService.getCurrentTaxYearEnd(dateService.getCurrentDate))
        ))
      case error: LiabilityCalculationError if error.status == NOT_FOUND =>
        auditingService.extendedAudit(TaxYearSummaryResponseAuditModel(
          mtdItUser, documentDetailsWithDueDates, obligations, None))

        Logger("application").info(
          s"[Agent][TaxYearSummaryController][view][$taxYear]] Rendered Tax year summary page with No Calc data")

        Ok(taxYearSummary(
          taxYear = taxYear,
          modelOpt = None,
          charges = documentDetailsWithDueDates,
          obligations = obligations,
          codingOutEnabled = isEnabled(CodingOut),
          backUrl = backUrl,
          isAgent = true,
          showForecastData = true
        ))
      case _: LiabilityCalculationError =>
        Logger("application").error(
          s"[Agent][TaxYearSummaryController][view][$taxYear]] No new calc deductions data error found. Downstream error")
        itvcErrorHandler.showInternalServerError()
    }
  }

  private def withTaxYearFinancials(taxYear: Int)(f: List[DocumentDetailWithDueDate] => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
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

  private def withObligationsModel(taxYear: Int)(f: ObligationsModel => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    nextUpdatesService.getNextUpdates(
      fromDate = LocalDate.of(taxYear - 1, 4, 6),
      toDate = LocalDate.of(taxYear, 4, 5)
    ) flatMap {
      case obligationsModel: ObligationsModel => f(obligationsModel)
      case _ =>
        Logger("application").error(s"[TaxYearSummaryController][withObligationsModel] - Could not retrieve obligations for year: $taxYear")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

}
