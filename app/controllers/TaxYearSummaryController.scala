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
import models.admin.*
import models.core.Nino
import models.financialDetails.*
import models.incomeSourceDetails.TaxYear
import models.liabilitycalculation.*
import models.liabilitycalculation.viewmodels.{CalculationSummary, TYSClaimToAdjustViewModel, TaxYearSummaryViewModel}
import models.obligations.ObligationsModel
import models.taxyearsummary.TaxYearSummaryChargeItem
import play.api.Logger
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.mvc.*
import services.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.TaxYearSummaryView

import java.net.URI
import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

//scalastyle:off
@Singleton
class TaxYearSummaryController @Inject()(
                                          authActions: AuthActions,
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
                                          claimToAdjustService: ClaimToAdjustService
                                        )(
                                          implicit val appConfig: FrontendAppConfig,
                                          dateService: DateServiceInterface,
                                          mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext
                                        ) extends FrontendController(mcc) with FeatureSwitching with I18nSupport with ImplicitDateFormatter with TransactionUtils {

  // Individual back urls
  private def taxYearsUrl(origin: Option[String]): String = controllers.routes.TaxYearsController.showTaxYears(origin).url

  private def whatYouOweUrl(origin: Option[String]): String = controllers.routes.WhatYouOweController.show(origin).url

  private def homeUrl(origin: Option[String]): String = controllers.routes.HomeController.show(origin).url

  // Agent back urls
  private lazy val agentTaxYearsUrl: String = controllers.routes.TaxYearsController.showAgentTaxYears().url
  private lazy val agentHomeUrl: String = controllers.routes.HomeController.showAgent().url
  private lazy val agentWhatYouOweUrl: String = controllers.routes.WhatYouOweController.showAgent().url

  def formatErrorMessages(
                           liabilityCalc: LiabilityCalculationResponse,
                           messagesProperty: MessagesApi,
                           isAgent: Boolean
                         )(implicit messages: Messages): LiabilityCalculationResponse = {

    liabilityCalc.messages match {
      case Some(value) =>
        val errorMessages =
          liabilityCalc.messages.get.getErrorMessageVariables(messagesProperty, isAgent)
        val translatedDateMessages =
          models.liabilitycalculation.Messages.translateMessageDateVariables(errorMessages)(messages, this)
        liabilityCalc.copy(messages = Some(liabilityCalc.messages.get.copy(errors = Some(translatedDateMessages))))
      case None =>
        liabilityCalc
    }
  }

  private[controllers] def showForecast(
                                         submissionChannel: Option[SubmissionChannel],
                                         calculationSummary: Option[CalculationSummary]
                                       ): Boolean = {

    val isMtd: Boolean =
      submissionChannel.contains(IsMTD)

    isMtd &&
      calculationSummary.exists { s =>
        !s.crystallised && s.forecastIncome.isDefined
      }
  }


  private def renderView(
                          liabilityCalc: LiabilityCalculationResponseModel,
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
        Logger("application").debug(s"[TaxYearSummaryController][renderView][$taxYear] Attempting to show successful latest calc view with no previous calc")
        handleCalcSuccess(
          mtdItId = mtdItUser.mtditid,
          nino = mtdItUser.nino,
          latestCalc = liabilityCalc,
          previousCalc = None,
          chargeItems = chargeItems,
          obligations = obligations,
          lpp2Url = lpp2Url,
          claimToAdjustViewModel = claimToAdjustViewModel,
          taxYear = taxYear,
          backUrl = backUrl,
          origin = origin,
          isAgent = isAgent
        )
      case (liabilityCalc: LiabilityCalculationResponse, Some(previousCalc), Some(lpp2Url)) =>
        previousCalc match {
          case previousCalc: LiabilityCalculationResponse =>
            Logger("application").debug(s"[TaxYearSummaryController][renderView][$taxYear] Attempting to show successful calc view for previous calc")
            handleCalcSuccess(
              mtdItId = mtdItUser.mtditid,
              nino = mtdItUser.nino,
              latestCalc = liabilityCalc,
              previousCalc = Some(previousCalc),
              chargeItems = chargeItems,
              obligations = obligations,
              lpp2Url = lpp2Url,
              claimToAdjustViewModel = claimToAdjustViewModel,
              taxYear = taxYear,
              backUrl = backUrl,
              origin = origin,
              isAgent = isAgent
            )
          case error: LiabilityCalculationError =>
            Logger("application").error(s"[TaxYearSummaryController][renderView][$taxYear] Unable to show calc view for previous calc")
            handleCalcError(
              mtdItId = mtdItUser.mtditid,
              nino = mtdItUser.nino,
              error = error,
              validLatestCalculation = Some(liabilityCalc),
              chargeItems = chargeItems,
              obligations = obligations,
              lpp2Url = lpp2Url,
              claimToAdjustViewModel = claimToAdjustViewModel,
              taxYear = taxYear,
              backUrl = backUrl,
              origin = origin,
              isAgent = isAgent
            )
        }
      case (error: LiabilityCalculationError, previousCalc, Some(lpp2Url)) =>
        Logger("application").error(s"[TaxYearSummaryController][renderView][$taxYear] Unable to show calc view for latest calc, PreviousCalc is defined: ${previousCalc.isDefined}")

        handleCalcError(
          mtdItId = mtdItUser.mtditid,
          nino = mtdItUser.nino,
          error = error,
          validLatestCalculation = None,
          chargeItems = chargeItems,
          obligations = obligations,
          lpp2Url = lpp2Url,
          claimToAdjustViewModel = claimToAdjustViewModel,
          taxYear = taxYear,
          backUrl = backUrl,
          origin = origin,
          isAgent = isAgent
        )
      case (_, _, None) if isAgent =>
        Logger("application").error(s"[Agent][$taxYear]] No chargeReference supplied with second late payment penalty. Hand-off url could not be formulated")
        Future(agentItvcErrorHandler.showInternalServerError())
      case (_, _, None) if !isAgent =>
        Logger("application").error(s"[$taxYear]] No chargeReference supplied with second late payment penalty. Hand-off url could not be formulated")
        Future(itvcErrorHandler.showInternalServerError())
    }
  }

  private def handleCalcSuccess(
                                 mtdItId: String,
                                 nino: String,
                                 latestCalc: LiabilityCalculationResponse,
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

    val calculationSummary: Option[CalculationSummary] =
      Some(CalculationSummary(
        formatErrorMessages(
          liabilityCalc = latestCalc,
          messagesProperty = messagesApi,
          isAgent = isAgent)(messagesApi.preferred(lang))
      ))

    val previousCalculationSummary: Option[CalculationSummary] =
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
        showForecastData = showForecast(latestCalc.submissionChannel, calculationSummary),
        ctaViewModel = claimToAdjustViewModel,
        LPP2Url = lpp2Url,
        pfaEnabled = isEnabled(PostFinalisationAmendmentsR18)
      )

    lazy val ctaLink = controllers.claimToAdjustPoa.routes.AmendablePoaController.show(isAgent = isAgent).url

    auditingService.extendedAudit(TaxYearSummaryResponseAuditModel(mtdItUser, messagesApi, taxYearSummaryViewModel, latestCalc.messages))

    Logger("application").info(s"[$taxYear]] Rendered Tax year summary page with Calc data")

    val selfAssessmentLink: Option[String] = mtdItUser.saUtr.map(sautr => s"https://www.tax.service.gov.uk/self-assessment/ind/$sautr/account/taxyear/$taxYear")
    val taxYearViewScenarios = taxYearSummaryService.determineCannotDisplayCalculationContentScenario(Some(latestCalc), TaxYear(taxYear - 1, taxYear))

    Future(
      Ok(taxYearSummaryView(
        taxYear = taxYear,
        viewModel = taxYearSummaryViewModel,
        backUrl = backUrl,
        origin = origin,
        isAgent = isAgent,
        ctaLink = ctaLink,
        taxYearViewScenarios = taxYearViewScenarios,
        showNoTaxCalc = latestCalc.calculation.isEmpty,
        viewTaxCalcLink = selfAssessmentLink,
        selfAssessmentLink = appConfig.selfAssessmentTaxReturnLink(isAgent),
        contactHmrcLink = appConfig.findHmrcContactsSALink()
      ))
    )
  }

  private def handleCalcError(
                               mtdItId: String,
                               nino: String,
                               error: LiabilityCalculationError,
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

      val calculationSummary: Option[CalculationSummary] =
        validLatestCalculation match {
          case Some(calc) =>
            Some(CalculationSummary(calc = formatErrorMessages(calc, messagesApi, isAgent)(messagesApi.preferred(lang))))
          case _ =>
            None
        }

      val viewModel =
        TaxYearSummaryViewModel(
          calculationSummary = calculationSummary,
          previousCalculationSummary = None,
          charges = chargeItems,
          obligations = obligations,
          showForecastData = true,
          ctaViewModel = claimToAdjustViewModel,
          LPP2Url = lpp2Url,
          pfaEnabled = isEnabled(PostFinalisationAmendmentsR18)
        )

      auditingService.extendedAudit(TaxYearSummaryResponseAuditModel(mtdItUser, messagesApi, viewModel))

      Logger("application").debug(s"[handleCalcError][$taxYear] Rendered Tax year summary page with No Calc data")

      val selfAssessmentLink: Option[String] =
        mtdItUser.saUtr.map(sautr => s"https://www.tax.service.gov.uk/self-assessment/ind/$sautr/account/taxyear/$taxYear")

      val taxYearViewScenarios =
        taxYearSummaryService.determineCannotDisplayCalculationContentScenario(Some(error), TaxYear(taxYear - 1, taxYear))

      Future(
        Ok(taxYearSummaryView(
          taxYear = taxYear,
          viewModel = viewModel,
          backUrl = backUrl,
          origin = origin,
          isAgent = isAgent,
          ctaLink = ctaLink,
          taxYearViewScenarios = taxYearViewScenarios,
          showNoTaxCalc = true,
          viewTaxCalcLink = selfAssessmentLink,
          selfAssessmentLink = appConfig.selfAssessmentTaxReturnLink(isAgent),
          contactHmrcLink = appConfig.findHmrcContactsSALink()
        ))
      )
    } else {
      lazy val ctaLink = controllers.claimToAdjustPoa.routes.AmendablePoaController.show(isAgent = isAgent).url
      val lang: Seq[Lang] = Seq(languageUtils.getCurrentLang)

      val calculationSummary: Option[CalculationSummary] =
        validLatestCalculation match {
          case Some(calc) =>
            Some(CalculationSummary(calc = formatErrorMessages(calc, messagesApi, isAgent)(messagesApi.preferred(lang))))
          case _ =>
            None
        }

      val viewModel =
        TaxYearSummaryViewModel(
          calculationSummary = calculationSummary,
          previousCalculationSummary = None,
          charges = chargeItems,
          obligations = obligations,
          showForecastData = true,
          ctaViewModel = claimToAdjustViewModel,
          LPP2Url = lpp2Url,
          pfaEnabled = isEnabled(PostFinalisationAmendmentsR18)
        )

      auditingService.extendedAudit(TaxYearSummaryResponseAuditModel(mtdItUser, messagesApi, viewModel))

      Logger("application").debug(s"[handleCalcError][$taxYear] Rendered Tax year summary page with No Calc data")

      val selfAssessmentLink: Option[String] =
        mtdItUser.saUtr.map(sautr => s"https://www.tax.service.gov.uk/self-assessment/ind/$sautr/account/taxyear/$taxYear")

      val taxYearViewScenarios =
        taxYearSummaryService.determineCannotDisplayCalculationContentScenario(Some(error), TaxYear(taxYear - 1, taxYear))

      Future(
        Ok(taxYearSummaryView(
          taxYear = taxYear,
          viewModel = viewModel,
          backUrl = backUrl,
          origin = origin,
          isAgent = isAgent,
          ctaLink = ctaLink,
          taxYearViewScenarios = taxYearViewScenarios,
          showNoTaxCalc = false,
          viewTaxCalcLink = selfAssessmentLink,
          selfAssessmentLink = appConfig.selfAssessmentTaxReturnLink(isAgent),
          contactHmrcLink = appConfig.findHmrcContactsSALink()
        ))
      )
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
      case None =>
        Some("")
    }
  }


  private def withTaxYearFinancials(taxYear: Int, isAgent: Boolean)(f: List[TaxYearSummaryChargeItem] => Future[Result])
                                   (implicit user: MtdItUser[_]): Future[Result] = {

    financialDetailsService.getFinancialDetails(taxYear, user.nino) flatMap {
      case financialDetails@FinancialDetailsModel(_, _, documentDetails, fd) =>

        val getChargeItem: DocumentDetail => Option[ChargeItem] =
          getChargeItemOpt(financialDetails = financialDetails.financialDetails)

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
            .filter(
              ChargeItem.filterAllowedCharges(
                isEnabled(PenaltiesAndAppeals),
                FirstLatePaymentPenalty,
                SecondLatePaymentPenalty,
                LateSubmissionPenalty
              )
            )
        }

        val chargeItemsLpi: List[TaxYearSummaryChargeItem] = {
          chargeItemsNoPayments
            .filter(_.latePaymentInterestAmount.exists(_ > 0))
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
        Logger("application").error(s"[withTaxYearFinancials][Agent] Could not retrieve financial details for year: $taxYear")
        Future(itvcErrorHandler.showInternalServerError())
      case _ =>
        Logger("application").error(s"[withTaxYearFinancials] Could not retrieve financial details for year: $taxYear")
        Future(agentItvcErrorHandler.showInternalServerError())
    }
  }

  private def withObligationsModel(taxYear: Int, isAgent: Boolean)(f: ObligationsModel => Future[Result])
                                  (implicit user: MtdItUser[_]): Future[Result] = {
    nextUpdatesService.getAllObligationsWithinDateRange(
      fromDate = LocalDate.of(taxYear - 1, 4, 6),
      toDate = LocalDate.of(taxYear, 4, 5)
    ) flatMap {
      case obligationsModel: ObligationsModel =>
        f(obligationsModel)
      case _ =>
        if (isAgent) {
          Logger("application").error(s"Could not retrieve obligations for year: $taxYear")
          Future(agentItvcErrorHandler.showInternalServerError())
        } else {
          Logger("application").error(s"[Agent]Could not retrieve obligations for year: $taxYear")
          Future(itvcErrorHandler.showInternalServerError())
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
          (latestCalcResponse: LiabilityCalculationResponseModel, previousResponse: Option[LiabilityCalculationResponseModel]) <- calculationService.getLatestAndPreviousCalculationDetails(mtdItId, nino, taxYear)
          backUrl = if (isAgent) getAgentBackURL(user.headers.get(REFERER)) else getBackURL(user.headers.get(REFERER), origin)
          view <- renderView(
            liabilityCalc = latestCalcResponse,
            previousCalc = previousResponse,
            chargeItems = charges,
            taxYear = taxYear,
            obligations = obligationsModel,
            claimToAdjustViewModel = viewModel,
            backUrl = backUrl,
            origin = origin,
            isAgent = isAgent
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

  private def claimToAdjustViewModel(nino: Nino, taxYear: Int)(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[TYSClaimToAdjustViewModel] = {
    claimToAdjustService.getPoaTaxYearForEntryPoint(nino).flatMap {
      case Right(value) => value match {
        case Some(value) if value.endYear == taxYear => Future(TYSClaimToAdjustViewModel(Option(value)))
        case _ => Future(TYSClaimToAdjustViewModel(None))
      }
      case Left(ex: Throwable) =>
        Logger("application").error(s"There was an error when getting the POA Entry point < cause: ${ex.getCause} message: ${ex.getMessage} >")
        Future.failed(ex)
    }
  }

  def renderTaxYearSummaryPage(taxYear: Int, origin: Option[String] = None): Action[AnyContent] =
    authActions.asMTDIndividual().async { implicit user =>
      if (taxYear.toString.matches("[0-9]{4}")) {
        handleRequest(taxYear, origin, isAgent = false)
      } else {
        Future(itvcErrorHandler.showInternalServerError())
      }
    }

  def renderAgentTaxYearSummaryPage(taxYear: Int): Action[AnyContent] =
    authActions.asMTDPrimaryAgent().async { implicit user =>
      // TODO: restore taxYear validation
      handleRequest(taxYear, None, isAgent = true)
    }

}
//scalastyle:on