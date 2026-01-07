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
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.GatewayPage.TaxYearSummaryPage
import forms.utils.SessionKeys.{calcPagesBackPage, gatewayPage}
import implicits.ImplicitDateFormatter
import models.admin._
import models.core.Nino
import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import models.liabilitycalculation.viewmodels.{CalculationSummary, TYSClaimToAdjustViewModel, TaxYearSummaryViewModel}
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse, LiabilityCalculationResponseModel}
import models.obligations.ObligationsModel
import models.taxyearsummary.{TaxYearSummaryChargeItem, TaxYearViewScenarios}
import play.api.Logger
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.mvc._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.TaxYearSummaryView

import java.net.URI
import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxYearSummaryController @Inject()(authActions: AuthActions,
                                         taxYearSummaryView: TaxYearSummaryView,
                                         taxYearSummaryService: TaxYearSummaryService,
                                         calculationService: CalculationService,
                                         financialDetailsService: FinancialDetailsService,
                                         itvcErrorHandler: ItvcErrorHandler,
                                         agentItvcErrorHandler: AgentItvcErrorHandler,
                                         nextUpdatesService: NextUpdatesService,
                                         messagesApi: MessagesApi,
                                         val languageUtils: LanguageUtils,
                                         val auditingService: AuditingService,
                                         claimToAdjustService: ClaimToAdjustService)
                                        (
                                          implicit val appConfig: FrontendAppConfig,
                                          dateService: DateServiceInterface,
                                          mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext
                                        ) extends FrontendController(mcc) with FeatureSwitching with I18nSupport with ImplicitDateFormatter with TransactionUtils {

  private def showForecast(calculationSummary: Option[CalculationSummary]): Boolean = {
    val isCrystallised = calculationSummary.exists(_.crystallised)
    val forecastDataPresent = calculationSummary.flatMap(_.forecastIncome).isDefined
    calculationSummary.isDefined && !isCrystallised && forecastDataPresent
  }

  private def renderView(liabilityCalc: LiabilityCalculationResponseModel,
                         previousCalc: Option[LiabilityCalculationResponseModel],
                         chargeItems: List[TaxYearSummaryChargeItem],
                         taxYear: Int,
                         obligations: ObligationsModel,
                         claimToAdjustViewModel: TYSClaimToAdjustViewModel,
                         backUrl: String,
                         origin: Option[String],
                         isAgent: Boolean
                        )(implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    (liabilityCalc, previousCalc, getLPP2Link(chargeItems, isAgent)) match {
      case (liabilityCalc: LiabilityCalculationResponse, None, Some(lpp2Url)) =>
        handleCalcSuccess(liabilityCalc, None, chargeItems, obligations, lpp2Url, claimToAdjustViewModel, taxYear, backUrl, origin, isAgent)

      case (liabilityCalc: LiabilityCalculationResponse, Some(previousCalc), Some(lpp2Url)) =>
        previousCalc match {
          case previousCalc: LiabilityCalculationResponse =>
            handleCalcSuccess(liabilityCalc, Some(previousCalc), chargeItems, obligations, lpp2Url, claimToAdjustViewModel, taxYear, backUrl, origin, isAgent)
          case error: LiabilityCalculationError =>
            handleCalcError(error, Some(liabilityCalc), chargeItems, obligations, lpp2Url, claimToAdjustViewModel, taxYear, backUrl, origin, isAgent)
        }

      case (error: LiabilityCalculationError, _, Some(lpp2Url)) =>
        handleCalcError(error, None, chargeItems, obligations, lpp2Url, claimToAdjustViewModel, taxYear, backUrl, origin, isAgent)

      case (_, _, None) =>
        if (isAgent) {
          Logger("application").error(s"[Agent][$taxYear]] No chargeReference supplied with second late payment penalty. Hand-off url could not be formulated")
          Future(agentItvcErrorHandler.showInternalServerError())
        }
        else {
          Logger("application").error(s"[$taxYear]] No chargeReference supplied with second late payment penalty. Hand-off url could not be formulated")

          Future(itvcErrorHandler.showInternalServerError())
        }
    }
  }

  private def handleCalcSuccess(latestCalc: LiabilityCalculationResponse,
                                previousCalc: Option[LiabilityCalculationResponse],
                                chargeItems: List[TaxYearSummaryChargeItem],
                                obligations: ObligationsModel,
                                lpp2Url: String,
                                claimToAdjustViewModel: TYSClaimToAdjustViewModel,
                                taxYear: Int,
                                backUrl: String,
                                origin: Option[String],
                                isAgent: Boolean
                               )(implicit hc: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[Result] = {
    val lang: Seq[Lang] = Seq(languageUtils.getCurrentLang)

    val calculationSummary =
      Some(CalculationSummary(
        formatErrorMessages(
          liabilityCalc = latestCalc,
          messagesProperty = messagesApi,
          isAgent = isAgent)(messagesApi.preferred(lang))
      ))

    val previousCalculationSummary =
      previousCalc
        .map(calc =>
          CalculationSummary(
            formatErrorMessages(
              liabilityCalc = calc,
              messagesProperty = messagesApi,
              isAgent = isAgent)(messagesApi.preferred(lang))
          )
        )

    val taxYearSummaryViewModel: TaxYearSummaryViewModel =
      TaxYearSummaryViewModel(
        calculationSummary = calculationSummary,
        previousCalculationSummary = previousCalculationSummary,
        charges = chargeItems,
        obligations = obligations,
        showForecastData = showForecast(calculationSummary),
        ctaViewModel = claimToAdjustViewModel,
        LPP2Url = lpp2Url,
        pfaEnabled = isEnabled(PostFinalisationAmendmentsR18)
      )

    lazy val ctaLink = controllers.claimToAdjustPoa.routes.AmendablePoaController.show(isAgent = isAgent).url

    auditingService.extendedAudit(TaxYearSummaryResponseAuditModel(mtdItUser, messagesApi, taxYearSummaryViewModel, latestCalc.messages))

    Logger("application").info(s"[$taxYear]] Rendered Tax year summary page with Calc data")

    for {
      scenarios <- taxYearSummaryService.determineCannotDisplayCalculationContentScenario(mtdItUser.nino, TaxYear(taxYear, taxYear + 1))
    } yield {
      Ok(taxYearSummaryView(
        taxYear = taxYear,
        viewModel = taxYearSummaryViewModel,
        backUrl = backUrl,
        origin = origin,
        isAgent = isAgent,
        ctaLink = ctaLink,
        taxYearViewScenarios = scenarios
      ))
    }
  }

  private def handleCalcError(error: LiabilityCalculationError,
                              validLatestCalculation: Option[LiabilityCalculationResponse],
                              chargeItems: List[TaxYearSummaryChargeItem],
                              obligations: ObligationsModel,
                              lpp2Url: String,
                              claimToAdjustViewModel: TYSClaimToAdjustViewModel,
                              taxYear: Int,
                              backUrl: String,
                              origin: Option[String],
                              isAgent: Boolean
                             )(implicit hc: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[Result] = {

    if (error.status == NO_CONTENT) {
      lazy val ctaLink = controllers.claimToAdjustPoa.routes.AmendablePoaController.show(isAgent = isAgent).url
      val lang: Seq[Lang] = Seq(languageUtils.getCurrentLang)

      val calculationSummary = validLatestCalculation match {
        case Some(calc) =>
          Some(CalculationSummary(
            formatErrorMessages(
              calc,
              messagesApi,
              isAgent)(messagesApi.preferred(lang))))
        case _ => None
      }

      val viewModel = TaxYearSummaryViewModel(
        calculationSummary,
        None,
        chargeItems,
        obligations,
        showForecastData = true,
        claimToAdjustViewModel,
        lpp2Url,
        isEnabled(PostFinalisationAmendmentsR18)
      )

      auditingService.extendedAudit(TaxYearSummaryResponseAuditModel(mtdItUser, messagesApi, viewModel))

      Logger("application").info(s"[$taxYear]] Rendered Tax year summary page with No Calc data")

      for {
        scenarios: TaxYearViewScenarios <- taxYearSummaryService.determineCannotDisplayCalculationContentScenario(mtdItUser.nino, TaxYear(taxYear, taxYear + 1))
      } yield {
        Ok(taxYearSummaryView(
          taxYear = taxYear,
          viewModel = viewModel,
          backUrl = backUrl,
          origin = origin,
          isAgent = isAgent,
          ctaLink = ctaLink,
          taxYearViewScenarios = scenarios
        ))
      }
    } else {
      if (isAgent) {
        Logger("application").error(s"[Agent][$taxYear]] No new calc deductions data error found. Downstream error")
        Future(agentItvcErrorHandler.showInternalServerError())
      }
      else {
        Logger("application").error(s"[$taxYear]] No new calc deductions data error found. Downstream error")
        Future(itvcErrorHandler.showInternalServerError())
      }
    }
  }

  private def getLPP2Link(chargeItems: List[TaxYearSummaryChargeItem], isAgent: Boolean): Option[String] = {
    val LPP2 = chargeItems.find(_.transactionType == SecondLatePaymentPenalty)
    LPP2 match {
      case Some(charge) => charge.chargeReference match {
        case Some(value) if isAgent => Some(appConfig.incomeTaxPenaltiesFrontendLPP2CalculationAgent(value))
        case Some(value) => Some(appConfig.incomeTaxPenaltiesFrontendLPP2Calculation(value))
        case None => None
      }
      case None => Some("")
    }
  }

  private def withTaxYearFinancials(taxYear: Int, isAgent: Boolean)(f: List[TaxYearSummaryChargeItem] => Future[Result])
                                   (implicit user: MtdItUser[_]): Future[Result] = {

    financialDetailsService.getFinancialDetails(taxYear, user.nino) flatMap {
      case financialDetails@FinancialDetailsModel(_, _, documentDetails, fd) =>

        val getChargeItem: DocumentDetail => Option[ChargeItem] = getChargeItemOpt(financialDetails = financialDetails.financialDetails)

        val chargeItemsNoPayments = documentDetails
          .filter(_.paymentLot.isEmpty)

        val chargeItemsCodingOut = chargeItemsNoPayments
          .filterNot(_.isNotCodingOutDocumentDetail)
          .flatMap(dd => getChargeItem(dd)
            .map(ci => TaxYearSummaryChargeItem.fromChargeItem(ci, dd.getDueDate())))

        val chargeItemsNoCodingOut: List[TaxYearSummaryChargeItem] = {
          chargeItemsNoPayments
            .filter(_.isNotCodingOutDocumentDetail)
            .flatMap(dd => getChargeItem(dd)
              .map(ci => TaxYearSummaryChargeItem.fromChargeItem(ci, financialDetails.findDueDateByDocumentDetails(dd))))
            .filterNot(_.originalAmount < 0)
            .filter(_.notCodedOutPoa(isEnabled(FilterCodedOutPoas)))
            .filter(ChargeItem.filterAllowedCharges(isEnabled(PenaltiesAndAppeals),
              FirstLatePaymentPenalty, SecondLatePaymentPenalty, LateSubmissionPenalty))
        }

        val chargeItemsLpi: List[TaxYearSummaryChargeItem] = {
          chargeItemsNoPayments
            .filter(_.isAccruingInterest)
            .flatMap(dd => getChargeItem(dd)
              .map(ci => TaxYearSummaryChargeItem.fromChargeItem(ci, dd.interestEndDate, isLatePaymentInterest = true)))
        }

        val chargeItemsCodingOutPaye: List[TaxYearSummaryChargeItem] = {
          chargeItemsCodingOut
            .filter(_.codedOutStatus.contains(models.financialDetails.Accepted))
            .filterNot(_.originalAmount <= 0)
        }

        val chargeItemsCodingOutFullyCollectedPoa: List[TaxYearSummaryChargeItem] =
          chargeItemsNoPayments
            .filter(_.isCodingOutFullyCollectedPoa(fd))
            .flatMap(dd => getChargeItem(dd)
              .map(ci => TaxYearSummaryChargeItem.fromChargeItem(ci, dd.getDueDate())))
            .filter(x => x.transactionType == PoaOneDebit || x.transactionType == PoaTwoDebit)

        val chargeItemsCodingOutNotPaye: List[TaxYearSummaryChargeItem] = {
          chargeItemsCodingOut
            .filterNot(_.codedOutStatus.contains(models.financialDetails.Accepted))
            .filterNot(_.originalAmount <= 0)
        }

        f(chargeItemsNoCodingOut ++ chargeItemsLpi ++ chargeItemsCodingOutPaye ++ chargeItemsCodingOutNotPaye ++ chargeItemsCodingOutFullyCollectedPoa)
      case FinancialDetailsErrorModel(NOT_FOUND, _) => f(List.empty)
      case _ if isAgent =>
        Logger("application").error(s"[Agent]Could not retrieve financial details for year: $taxYear")
        Future.successful(itvcErrorHandler.showInternalServerError())
      case _ =>
        Logger("application").error(s"Could not retrieve financial details for year: $taxYear")
        Future.successful(agentItvcErrorHandler.showInternalServerError())
    }
  }

  private def withObligationsModel(taxYear: Int, isAgent: Boolean)(f: ObligationsModel => Future[Result])
                                  (implicit user: MtdItUser[_]): Future[Result] = {
    nextUpdatesService.getAllObligationsWithinDateRange(
      fromDate = LocalDate.of(taxYear - 1, 4, 6),
      toDate = LocalDate.of(taxYear, 4, 5)
    ) flatMap {
      case obligationsModel: ObligationsModel => f(obligationsModel)
      case _ =>
        if (isAgent) {
          Logger("application").error(s"Could not retrieve obligations for year: $taxYear")
          Future.successful(agentItvcErrorHandler.showInternalServerError())
        } else {
          Logger("application").error(s"[Agent]Could not retrieve obligations for year: $taxYear")
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
                           (implicit user: MtdItUser[_], hc: HeaderCarrier,
                            ec: ExecutionContext): Future[Result] = {

    withTaxYearFinancials(taxYear, isAgent) { charges =>
      withObligationsModel(taxYear, isAgent) { obligationsModel =>
        val mtdItId: String = user.mtditid
        val nino: String = user.nino
        for {
          viewModel <- claimToAdjustViewModel(nino = Nino(value = user.nino), taxYear = taxYear)
          (latestCalcResponse, previousResponse) <- calculationService.getLatestAndPreviousCalculationDetails(mtdItId, nino, taxYear)
          view <- renderView(
            liabilityCalc = latestCalcResponse,
            previousCalc = previousResponse,
            chargeItems = charges,
            taxYear = taxYear,
            obligations = obligationsModel,
            claimToAdjustViewModel = viewModel,
            backUrl = if (isAgent) getAgentBackURL(user.headers.get(REFERER)) else getBackURL(user.headers.get(REFERER), origin),
            origin = origin, isAgent = isAgent
          )
        } yield {
          view
            .addingToSession(gatewayPage -> TaxYearSummaryPage.name)
            .addingToSession(calcPagesBackPage -> "ITVC")
        }
      }
    }
  }.recover {
    case ex: Throwable =>
      val errorHandler = if (isAgent) agentItvcErrorHandler else itvcErrorHandler
      Logger("application").error(s"${if (isAgent) "Agent" else "Individual"} - There was an error, status: - ${ex.getMessage} - ${ex.getCause} - ")
      errorHandler.showInternalServerError()
  }

  def renderTaxYearSummaryPage(taxYear: Int, origin: Option[String] = None): Action[AnyContent] =
    authActions.asMTDIndividual.async { implicit user =>
      if (taxYear.toString.matches("[0-9]{4}")) {
        handleRequest(taxYear, origin, isAgent = false)
      } else {
        Future.successful(itvcErrorHandler.showInternalServerError())
      }
    }

  def renderAgentTaxYearSummaryPage(taxYear: Int): Action[AnyContent] =
    authActions.asMTDPrimaryAgent.async { implicit user =>
      // TODO: restore taxYear validation
      handleRequest(taxYear, None, isAgent = true)
    }

  // Individual back urls
  private def taxYearsUrl(origin: Option[String]): String = controllers.routes.TaxYearsController.showTaxYears(origin).url

  def whatYouOweUrl(origin: Option[String]): String = controllers.routes.WhatYouOweController.show(origin).url

  def homeUrl(origin: Option[String]): String = controllers.routes.HomeController.show(origin).url

  // Agent back urls
  private lazy val agentTaxYearsUrl: String = controllers.routes.TaxYearsController.showAgentTaxYears().url
  private lazy val agentHomeUrl: String = controllers.routes.HomeController.showAgent().url
  private lazy val agentWhatYouOweUrl: String = controllers.routes.WhatYouOweController.showAgent().url


  def formatErrorMessages(liabilityCalc: LiabilityCalculationResponse, messagesProperty: MessagesApi, isAgent: Boolean)
                         (implicit messages: Messages): LiabilityCalculationResponse = {

    if (liabilityCalc.messages.isDefined) {
      val errorMessages = liabilityCalc.messages.get.getErrorMessageVariables(messagesProperty, isAgent)
      val translatedDateMessages = {
        models.liabilitycalculation.Messages.translateMessageDateVariables(errorMessages)(messages, this)
      }
      liabilityCalc.copy(messages = Some(liabilityCalc.messages.get.copy(errors = Some(translatedDateMessages))))
    } else {
      liabilityCalc
    }
  }

  private def claimToAdjustViewModel(nino: Nino, taxYear: Int)(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[TYSClaimToAdjustViewModel] = {
    claimToAdjustService.getPoaTaxYearForEntryPoint(nino).flatMap {
      case Right(value) => value match {
        case Some(value) if value.endYear == taxYear => Future.successful(TYSClaimToAdjustViewModel(Option(value)))
        case _ => Future.successful(TYSClaimToAdjustViewModel(None))
      }
      case Left(ex: Throwable) =>
        Logger("application").error(s"There was an error when getting the POA Entry point" +
          s" < cause: ${ex.getCause} message: ${ex.getMessage} >")
        Future.failed(ex)
    }
  }
}
