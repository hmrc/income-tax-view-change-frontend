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

package controllers

import audit.AuditingService
import audit.models.TaxYearSummaryResponseAuditModel
import auth.MtdItUser
import config.featureswitch.{CodingOut, FeatureSwitching, ForecastCalculation, MFACreditsAndDebits, R7bTxmEvents}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.GatewayPage.TaxYearSummaryPage
import forms.utils.SessionKeys.{calcPagesBackPage, gatewayPage}
import models.financialDetails.MfaDebitUtils.filterMFADebits
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetailsErrorModel, FinancialDetailsModel}
import models.liabilitycalculation.viewmodels.TaxYearSummaryViewModel
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse, LiabilityCalculationResponseModel, Message}
import models.nextUpdates.ObligationsModel
import play.api.Logger
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.mvc._
import services.{CalculationService, DateService, FinancialDetailsService, IncomeSourceDetailsService, NextUpdatesService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
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
                                         incomeSourceDetailsService: IncomeSourceDetailsService,
                                         retrieveIncomeSourcesNoCache: IncomeSourceDetailsPredicateNoCache,
                                         retrieveNino: NinoPredicate,
                                         nextUpdatesService: NextUpdatesService,
                                         messages: MessagesApi,
                                         val authorisedFunctions: AuthorisedFunctions,
                                         val retrieveBtaNavBar: NavBarPredicate,
                                         val auditingService: AuditingService)
                                        (implicit val appConfig: FrontendAppConfig,
                                         dateService: DateService,
                                         val agentItvcErrorHandler: AgentItvcErrorHandler,
                                         mcc: MessagesControllerComponents,
                                         val ec: ExecutionContext)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  val action: ActionBuilder[MtdItUser, AnyContent] = checkSessionTimeout andThen authenticate andThen
    retrieveNino andThen retrieveIncomeSourcesNoCache andThen retrieveBtaNavBar

  private def showForecast(modelOpt: Option[TaxYearSummaryViewModel]): Boolean = {
    val isCrystalised = modelOpt.flatMap(_.crystallised).contains(true)
    val forecastDataPresent = modelOpt.flatMap(_.forecastIncome).isDefined
    isEnabled(ForecastCalculation) && modelOpt.isDefined && !isCrystalised && forecastDataPresent
  }

  private def view(liabilityCalc: LiabilityCalculationResponseModel,
                   documentDetailsWithDueDates: List[DocumentDetailWithDueDate],
                   taxYear: Int,
                   obligations: ObligationsModel,
                   codingOutEnabled: Boolean,
                   backUrl: String,
                   origin: Option[String],
                   isAgent: Boolean
                  )(implicit mtdItUser: MtdItUser[_]): Result = {
    liabilityCalc match {
      case liabilityCalc: LiabilityCalculationResponse =>
        val taxYearSummaryViewModel: TaxYearSummaryViewModel = TaxYearSummaryViewModel(formatErrorMessages(liabilityCalc, messages)(Lang("GB")))
        auditingService.extendedAudit(TaxYearSummaryResponseAuditModel(
          mtdItUser, documentDetailsWithDueDates, obligations, Some(taxYearSummaryViewModel), isEnabled(R7bTxmEvents)))

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
          origin = origin,
          isAgent = isAgent
        ))
      case error: LiabilityCalculationError if error.status == NOT_FOUND =>
        auditingService.extendedAudit(TaxYearSummaryResponseAuditModel(
          mtdItUser, documentDetailsWithDueDates, obligations, None, isEnabled(R7bTxmEvents)))

        Logger("application").info(
          s"[TaxYearSummaryController][view][$taxYear]] Rendered Tax year summary page with No Calc data")

        Ok(taxYearSummaryView(
          taxYear = taxYear,
          modelOpt = None,
          charges = documentDetailsWithDueDates,
          obligations = obligations,
          codingOutEnabled = codingOutEnabled,
          backUrl = backUrl,
          showForecastData = isEnabled(ForecastCalculation),
          origin = origin,
          isAgent = isAgent
        ))
      case _: LiabilityCalculationError if isAgent =>
        Logger("application").error(
          s"[Agent][TaxYearSummaryController][view][$taxYear]] No new calc deductions data error found. Downstream error")
        agentItvcErrorHandler.showInternalServerError()
      case _: LiabilityCalculationError =>
        Logger("application").error(
          s"[TaxYearSummaryController][view][$taxYear]] No new calc deductions data error found. Downstream error")
        itvcErrorHandler.showInternalServerError()
    }
  }

  private def withTaxYearFinancials(taxYear: Int, isAgent: Boolean)(f: List[DocumentDetailWithDueDate] => Future[Result])
                                   (implicit user: MtdItUser[AnyContent]): Future[Result] = {

    financialDetailsService.getFinancialDetails(taxYear, user.nino) flatMap {
      case financialDetails@FinancialDetailsModel(_, documentDetails, _) =>
        val docDetailsNoPayments = documentDetails.filter(_.paymentLot.isEmpty)
        val docDetailsCodingOut = docDetailsNoPayments.filter(_.isCodingOutDocumentDetail(isEnabled(CodingOut)))
        val documentDetailsWithDueDates: List[DocumentDetailWithDueDate] = {
          docDetailsNoPayments
            .filter(_.isNotCodingOutDocumentDetail)
            .filter(_.originalAmountIsNotZeroOrNegative)
            .map(
              documentDetail => DocumentDetailWithDueDate(documentDetail, financialDetails.findDueDateByDocumentDetails(documentDetail),
                dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId), codingOutEnabled = isEnabled(CodingOut),
                isMFADebit = financialDetails.isMFADebit(documentDetail.transactionId)))
        }.filter(documentDetailWithDueDate => filterMFADebits(isEnabled(MFACreditsAndDebits), documentDetailWithDueDate))
        val documentDetailsWithDueDatesForLpi: List[DocumentDetailWithDueDate] = {
          docDetailsNoPayments.filter(_.isLatePaymentInterest).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, documentDetail.interestEndDate, isLatePaymentInterest = true,
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        val documentDetailsWithDueDatesCodingOutPaye: List[DocumentDetailWithDueDate] = {
          docDetailsCodingOut.filter(dd => dd.isPayeSelfAssessment && dd.originalAmountIsNotZeroOrNegative).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, financialDetails.getDueDateFor(documentDetail),
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        val documentDetailsWithDueDatesCodingOut: List[DocumentDetailWithDueDate] = {
          docDetailsCodingOut.filter(dd => !dd.isPayeSelfAssessment && dd.originalAmountIsNotZeroOrNegative).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, financialDetails.getDueDateFor(documentDetail),
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        f(documentDetailsWithDueDates ++ documentDetailsWithDueDatesForLpi ++ documentDetailsWithDueDatesCodingOutPaye ++ documentDetailsWithDueDatesCodingOut)
      case FinancialDetailsErrorModel(NOT_FOUND, _) => f(List.empty)
      case _ if isAgent =>
        Logger("application").error(s"[TaxYearSummaryController][withTaxYearFinancials] - Could not retrieve financial details for year: $taxYear")
        Future.successful(agentItvcErrorHandler.showInternalServerError())
      case _ =>
        Logger("application").error(s"[Agent][TaxYearSummaryController][withTaxYearFinancials] - Could not retrieve financial details for year: $taxYear")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  private def withObligationsModel(taxYear: Int, isAgent: Boolean)(f: ObligationsModel => Future[Result])
                                  (implicit user: MtdItUser[AnyContent]): Future[Result] = {
    nextUpdatesService.getNextUpdates(
      fromDate = LocalDate.of(taxYear - 1, 4, 6),
      toDate = LocalDate.of(taxYear, 4, 5)
    ) flatMap {
      case obligationsModel: ObligationsModel => f(obligationsModel)
      case _ =>
        if (isAgent) {
          Logger("application").error(s"[TaxYearSummaryController][withObligationsModel] - Could not retrieve obligations for year: $taxYear")
          Future.successful(agentItvcErrorHandler.showInternalServerError())
        } else {
          Logger("application").error(s"[Agent][TaxYearSummaryController][withObligationsModel] - Could not retrieve obligations for year: $taxYear")
          Future.successful(itvcErrorHandler.showInternalServerError())
        }
    }
  }

  private def getBackURL(referer: Option[String], origin: Option[String]): String = {
    referer.map(URI.create(_).getPath.equals(taxYearsUrl(origin))) match {
      case Some(true) => taxYearsUrl(origin)
      case Some(false) if referer.map(URI.create(_).getPath.equals(whatYouOweUrl(origin))).get => whatYouOweUrl(origin)
      case _ => homeUrl(origin)
    }
  }

  private def getAgentBackURL(referer: Option[String]): String = {
    referer.map(URI.create(_).getPath.equals(agentTaxYearsUrl)) match {
      case Some(true) => agentTaxYearsUrl
      case Some(false) if referer.map(URI.create(_).getPath.equals(agentWhatYouOweUrl)).get => agentWhatYouOweUrl
      case _ => agentHomeUrl
    }
  }

  private def handleRequest(taxYear: Int, origin: Option[String], isAgent: Boolean)
                           (implicit user: MtdItUser[AnyContent], hc: HeaderCarrier,
                            ec: ExecutionContext, messages: Messages): Future[Result] = {
    withTaxYearFinancials(taxYear, isAgent) { charges =>
      withObligationsModel(taxYear, isAgent) { obligationsModel =>
        val codingOutEnabled: Boolean = isEnabled(CodingOut)
        val mtdItId: String = if (isAgent) getClientMtditid else user.mtditid
        val nino: String = if (isAgent) getClientNino else user.nino
        calculationService.getLiabilityCalculationDetail(mtdItId, nino, taxYear).map { liabilityCalcResponse =>
          view(liabilityCalcResponse, charges, taxYear, obligationsModel, codingOutEnabled,
            backUrl = if (isAgent) getAgentBackURL(user.headers.get(REFERER)) else getBackURL(user.headers.get(REFERER), origin),
            origin = origin, isAgent = isAgent)
            .addingToSession(gatewayPage -> TaxYearSummaryPage.name)
            .addingToSession(calcPagesBackPage -> "ITVC")
        }
      }
    }
  }

  def renderTaxYearSummaryPage(taxYear: Int, origin: Option[String] = None): Action[AnyContent] = action.async {
    implicit user =>
      if (taxYear.toString.matches("[0-9]{4}")) {
        handleRequest(taxYear, origin, false)
      } else {
        Future.successful(itvcErrorHandler.showInternalServerError())
      }
  }

  def renderAgentTaxYearSummaryPage(taxYear: Int): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (taxYear.toString.matches("[0-9]{4}")) {
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = false) flatMap { implicit mtdItUser =>
          handleRequest(taxYear, None, true)
        }
      } else {
        Future.successful(agentItvcErrorHandler.showInternalServerError())
      }
  }

  // Individual back urls
  def taxYearsUrl(origin: Option[String]): String = controllers.routes.TaxYearsController.showTaxYears(origin).url

  def whatYouOweUrl(origin: Option[String]): String = controllers.routes.WhatYouOweController.show(origin).url

  def homeUrl(origin: Option[String]): String = controllers.routes.HomeController.show(origin).url

  // Agent back urls
  lazy val agentTaxYearsUrl: String = controllers.routes.TaxYearsController.showAgentTaxYears.url
  lazy val agentHomeUrl: String = controllers.routes.HomeController.showAgent.url
  lazy val agentWhatYouOweUrl: String = controllers.routes.WhatYouOweController.showAgent.url

  def formatErrorMessages(liabilityCalc: LiabilityCalculationResponse, messagesProperty: MessagesApi)(implicit lang: Lang): LiabilityCalculationResponse = {

    val errMessages = liabilityCalc.messages.get.errorMessages.map(msg => {
      val key = "tax-year-summary.message." + msg.id
      val nMsg = Message(id = msg.id, text = "")
      if (messagesProperty.isDefinedAt(key))
        nMsg.text = msg.text diff messagesProperty(key)
      nMsg
    })
    liabilityCalc.copy(messages = Some(liabilityCalc.messages.get.copy(errors = Some(errMessages))))
  }
}
