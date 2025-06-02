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

package controllers.manageBusinesses.manage

import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney._
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import models.admin.{AccountingMethodJourney, DisplayBusinessStartDate, OptInOptOutContentUpdateR17, ReportingFrequencyPage}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.core.IncomeSourceIdHash.{mkFromQueryString, mkIncomeSourceIdHash}
import models.core.{IncomeSourceId, IncomeSourceIdHash}
import models.incomeSourceDetails._
import models.incomeSourceDetails.viewmodels.ManageIncomeSourceDetailsViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.manage.ManageIncomeSourceDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageIncomeSourceDetailsController @Inject()(val view: ManageIncomeSourceDetails,
                                                    val authActions: AuthActions,
                                                    val itvcErrorHandler: ItvcErrorHandler,
                                                    val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                    val itsaStatusService: ITSAStatusService,
                                                    val dateService: DateService,
                                                    val calculationListService: CalculationListService,
                                                    val sessionService: SessionService)
                                                   (implicit val ec: ExecutionContext,
                                                    val mcc: MessagesControllerComponents,
                                                    val appConfig: FrontendAppConfig) extends FrontendController(mcc)
  with I18nSupport with JourneyCheckerManageBusinesses {

  private def getBackUrl(isAgent: Boolean): String =
    if (isAgent) {
      controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
    } else {
      controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
    }

  private def errorHandler(isAgent: Boolean): FrontendErrorHandler with ShowInternalServerError =
    if (isAgent) {
      itvcErrorHandlerAgent
    } else {
      itvcErrorHandler
    }


  def show(isAgent: Boolean,
           incomeSourceType: IncomeSourceType,
           id: Option[String]): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withSessionDataAndNewIncomeSourcesFS(IncomeSourceJourneyType(Manage, incomeSourceType), InitialPage) { _ =>
        incomeSourceType match {
          case SelfEmployment => id match {
            case Some(realId) => handleSoleTrader(realId, getBackUrl(isAgent), isAgent)
            case None => Logger("application")
              .error(s"no incomeSourceId supplied with SelfEmployment isAgent = $isAgent")
              Future.successful(errorHandler(isAgent).showInternalServerError())
          }
          case _ => handleProperty(
            sources = user.incomeSources,
            isAgent = isAgent,
            backUrl = getBackUrl(isAgent),
            incomeSourceType = incomeSourceType
          )
        }
      }
  }

  def showChange(incomeSourceType: IncomeSourceType,
                 isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withSessionDataAndNewIncomeSourcesFS(IncomeSourceJourneyType(Manage, incomeSourceType), InitialPage) { sessionData =>
        val incomeSourceIdStringOpt = sessionData.manageIncomeSourceData.flatMap(_.incomeSourceId)
        val incomeSourceIdOpt = incomeSourceIdStringOpt.map(id => mkIncomeSourceIdHash(IncomeSourceId(id)))
        val backUrl = controllers.manageBusinesses.manage.routes.CheckYourAnswersController.show(isAgent, incomeSourceType).url
        incomeSourceType match {
          case SelfEmployment => incomeSourceIdOpt match {
            case Some(realId) => handleSoleTrader(realId.hash, backUrl, isAgent)
            case None => Logger("application")
              .error(s"no incomeSourceId supplied with SelfEmployment isAgent = $isAgent")
              Future.successful(errorHandler(isAgent).showInternalServerError())
          }
          case _ => handleProperty(
            sources = user.incomeSources,
            isAgent = isAgent,
            backUrl = backUrl,
            incomeSourceType = incomeSourceType
          )
        }
      }
  }

  def handleSoleTrader(hashIdString: String, backUrl: String, isAgent: Boolean)
                      (implicit user: MtdItUser[_]): Future[Result] = {

    def setMongoKey(incomeSourceId: IncomeSourceId): Future[Boolean] = sessionService.setMongoKey(
      ManageIncomeSourceData.incomeSourceIdField,
      incomeSourceId.value,
      IncomeSourceJourneyType(Manage, SelfEmployment)
    ).flatMap {
      case Right(keySet) => Future.successful(keySet)
      case Left(exception) => Future.failed(exception)
    }

    val result = for {
      incomeSourceIdHash <- getIncomeSourceIdHash(hashIdString)
      incomeSourceId <- validateIncomeSourcesContainsIncomeSourceId(incomeSourceIdHash)
      _ <- setMongoKey(incomeSourceId)
      viewModel <- getManageIncomeSourceViewModel(
        sources = user.incomeSources,
        incomeSourceId = incomeSourceId,
        isAgent
      )
    } yield Ok(view(viewModel = viewModel,
      isAgent = isAgent,
      showStartDate = isEnabled(DisplayBusinessStartDate),
      showAccountingMethod = isEnabled(AccountingMethodJourney),
      showOptInOptOutContentUpdateR17 = isEnabled(OptInOptOutContentUpdateR17),
      showReportingFrequencyLink = isEnabled(ReportingFrequencyPage),
      backUrl = backUrl
    ))

    result.recover {
      case ex =>
        Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
        if (isAgent) {
          itvcErrorHandlerAgent.showInternalServerError()
        } else {
          itvcErrorHandler.showInternalServerError()
        }
    }
  }

  def handleProperty(sources: IncomeSourceDetailsModel,
                     isAgent: Boolean,
                     backUrl: String,
                     incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    getManageIncomeSourceViewModelProperty(sources = sources, isAgent = isAgent, incomeSourceType = incomeSourceType)
      .map { viewModel =>
        Ok(view(
          viewModel = viewModel,
          isAgent = isAgent,
          showStartDate = isEnabled(DisplayBusinessStartDate),
          showAccountingMethod = isEnabled(AccountingMethodJourney),
          showOptInOptOutContentUpdateR17 = isEnabled(OptInOptOutContentUpdateR17),
          showReportingFrequencyLink = isEnabled(ReportingFrequencyPage),
          backUrl = backUrl
        ))
      }.recover {
        case ex =>
          Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
          errorHandler(isAgent).showInternalServerError()
      }
  }

  private def getIncomeSourceIdHash(hashIdString: String): Future[IncomeSourceIdHash] = {
    val incomeSourceIdHash: Either[Throwable, IncomeSourceIdHash] = mkFromQueryString(hashIdString)
    incomeSourceIdHash match {
      case Left(exception: Exception) => Future.failed(exception)
      case Left(_) => Future.failed(new Error(s"Unexpected exception incomeSourceIdHash: <$incomeSourceIdHash>"))
      case Right(incomeSourceIdHash: IncomeSourceIdHash) => Future.successful(incomeSourceIdHash)
    }
  }

  private def validateIncomeSourcesContainsIncomeSourceId(incomeSourceIdHash: IncomeSourceIdHash)
                                                         (implicit user: MtdItUser[_]): Future[IncomeSourceId] = {
    val hashCompareResult: Either[Throwable, IncomeSourceId] = user.incomeSources.compareHashToQueryString(incomeSourceIdHash)
    hashCompareResult match {
      case Left(exception: Exception) => Future.failed(exception)
      case Left(_) => Future.failed(new Error(s"Unexpected exception incomeSourceIdHash: <$incomeSourceIdHash>"))
      case Right(incomeSourceId: IncomeSourceId) => Future.successful(incomeSourceId)
    }
  }

  private def getQuarterType(latencyDetails: Option[LatencyDetails],
                             quarterTypeElection: Option[QuarterTypeElection]): Option[QuarterReportingType] = {
    quarterTypeElection.flatMap(quarterTypeElection => {
      latencyDetails match {
        case Some(latencyDetails: LatencyDetails) =>
          val quarterIndicator = "Q"
          val currentTaxYearEnd = dateService.getCurrentTaxYearEnd.toString
          val showForLatencyTaxYear1 = (latencyDetails.taxYear1 == currentTaxYearEnd) && latencyDetails.latencyIndicator1.equals(quarterIndicator)
          val showForLatencyTaxYear2 = (latencyDetails.taxYear2 == currentTaxYearEnd) && latencyDetails.latencyIndicator2.equals(quarterIndicator)
          val showIfLatencyExpired = latencyDetails.taxYear2 < currentTaxYearEnd
          val showQuarterReportingType = showForLatencyTaxYear1 || showForLatencyTaxYear2 || showIfLatencyExpired
          if (showQuarterReportingType) quarterTypeElection.isStandardQuarterlyReporting else None
        case None => quarterTypeElection.isStandardQuarterlyReporting
      }
    })
  }

  private def getCrystallisationInformation(latencyDetails: Option[LatencyDetails])
                                           (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Option[List[Boolean]]] = {
    latencyDetails match {
      case Some(x) =>
        for {
          isTY1Crystallised <- calculationListService.determineTaxYearCrystallised(x.taxYear1.toInt)
          isTY2Crystallised <- calculationListService.determineTaxYearCrystallised(x.taxYear2.toInt)
        } yield {
          Some(List(isTY1Crystallised, isTY2Crystallised))
        }
      case _ =>
        Future.successful(None)
    }
  }

  private def variableViewModelSEBusiness(
                                           incomeSource: BusinessDetailsModel,
                                           latencyYearsQuarterly: LatencyYearsQuarterly,
                                           latencyYearsCrystallised: LatencyYearsCrystallised,
                                           useMTDForTaxYear1: Option[Boolean] = None,
                                           useMTDForTaxYear2: Option[Boolean] = None
                                         ): ManageIncomeSourceDetailsViewModel = {
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(incomeSource.incomeSourceId),
      incomeSource = incomeSource.incomeSource,
      tradingName = incomeSource.tradingName,
      tradingStartDate = incomeSource.tradingStartDate,
      address = incomeSource.address,
      isTraditionalAccountingMethod = incomeSource.cashOrAccruals,
      latencyYearsQuarterly = latencyYearsQuarterly,
      latencyYearsCrystallised = latencyYearsCrystallised,
      latencyDetails = incomeSource.latencyDetails,
      incomeSourceType = SelfEmployment,
      quarterReportingType = getQuarterType(incomeSource.latencyDetails, incomeSource.quarterTypeElection),
      useMTDForTaxYear1 = useMTDForTaxYear1,
      useMTDForTaxYear2 = useMTDForTaxYear2
    )
  }

  private def variableViewModelPropertyBusiness(incomeSource: PropertyDetailsModel,
                                                latencyYearsQuarterly: LatencyYearsQuarterly,
                                                latencyYearsCrystallised: LatencyYearsCrystallised,
                                                incomeSourceType: IncomeSourceType,
                                                useMTDForTaxYear1: Option[Boolean] = None,
                                                useMTDForTaxYear2: Option[Boolean] = None): ManageIncomeSourceDetailsViewModel = {
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(incomeSource.incomeSourceId),
      incomeSource = None,
      tradingName = None,
      tradingStartDate = incomeSource.tradingStartDate,
      address = None,
      isTraditionalAccountingMethod = incomeSource.cashOrAccruals,
      latencyYearsQuarterly = latencyYearsQuarterly,
      latencyYearsCrystallised = latencyYearsCrystallised,
      latencyDetails = incomeSource.latencyDetails,
      incomeSourceType = incomeSourceType,
      quarterReportingType = getQuarterType(incomeSource.latencyDetails, incomeSource.quarterTypeElection),
      useMTDForTaxYear1 = useMTDForTaxYear1,
      useMTDForTaxYear2 = useMTDForTaxYear2
    )
  }


  private def getManageIncomeSourceViewModel(
                                              sources: IncomeSourceDetailsModel,
                                              incomeSourceId: IncomeSourceId,
                                              isAgent: Boolean
                                            )(implicit user: MtdItUser[_],
                                              hc: HeaderCarrier, ec: ExecutionContext): Future[ManageIncomeSourceDetailsViewModel] = {

    val desiredIncomeSourceMaybe: Option[BusinessDetailsModel] = sources.businesses
      .filterNot(_.isCeased)
      .find(_.incomeSourceId == incomeSourceId.value)

    desiredIncomeSourceMaybe match {
      case Some(desiredIncomeSource) =>
        desiredIncomeSource.latencyDetails match {
          case Some(latencyDetails) =>
            handleLatencyAndCrystallisationDetails(desiredIncomeSource, latencyDetails)
          case None =>
            Future.successful(variableViewModelSEBusiness(
              incomeSource = desiredIncomeSource,
              latencyYearsQuarterly = LatencyYearsQuarterly(
                firstYear = Some(false),
                secondYear = Some(false)
              ),
              latencyYearsCrystallised = LatencyYearsCrystallised(
                firstYear = None,
                secondYear = None
              )
            ))
        }
      case None =>
        Future.failed(new Error("Unable to find income source"))
    }
  }

  private def getLatencyStatusesBasedOnFeatureSwitch(
                                  latencyDetails: LatencyDetails
                                )(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[(Boolean, Boolean)] = {
    if (isEnabled(OptInOptOutContentUpdateR17)) {
      Future.successful {
        val taxYear1Status = latencyDetails.latencyIndicator1 == "Q"
        val taxYear2Status = latencyDetails.latencyIndicator2 == "Q"
        (taxYear1Status, taxYear2Status)
      }
    } else {
      itsaStatusService.hasMandatedOrVoluntaryStatusForLatencyYears(Some(latencyDetails))
    }
  }


  private def handleLatencyAndCrystallisationDetails(
                                                      desiredIncomeSource: BusinessDetailsModel,
                                                      latencyDetails: LatencyDetails
                                                    )(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[ManageIncomeSourceDetailsViewModel] = {

    for {
      (latencyYearOneStatus, latencyYearTwoStatus) <- getLatencyStatusesBasedOnFeatureSwitch(latencyDetails)
      crystallisationData <- getCrystallisationInformation(Some(latencyDetails))
    } yield {
      val (maybeUseMTD1, maybeUseMTD2) =
        if (isEnabled(OptInOptOutContentUpdateR17))
          (Some(latencyYearOneStatus), Some(latencyYearTwoStatus))
        else
          (None, None)

      crystallisationData match {
        case None =>
          variableViewModelSEBusiness(
            incomeSource = desiredIncomeSource,
            latencyYearsQuarterly = LatencyYearsQuarterly(
              firstYear = Some(latencyYearOneStatus),
              secondYear = Some(latencyYearTwoStatus)
            ),
            latencyYearsCrystallised = LatencyYearsCrystallised(
              firstYear = None,
              secondYear = None
            ),
            useMTDForTaxYear1 = maybeUseMTD1,
            useMTDForTaxYear2 = maybeUseMTD2
          )

        case Some(crystallisationList: List[Boolean]) =>
          variableViewModelSEBusiness(
            incomeSource = desiredIncomeSource,
            latencyYearsQuarterly = LatencyYearsQuarterly(
              firstYear = Some(latencyYearOneStatus),
              secondYear = Some(latencyYearTwoStatus)
            ),
            latencyYearsCrystallised = LatencyYearsCrystallised(
              firstYear = crystallisationList.headOption,
              secondYear = crystallisationList.lastOption
            ),
            useMTDForTaxYear1 = maybeUseMTD1,
            useMTDForTaxYear2 = maybeUseMTD2
          )
      }
    }
  }


  private def getManageIncomeSourceViewModelProperty(sources: IncomeSourceDetailsModel,
                                                     incomeSourceType: IncomeSourceType,
                                                     isAgent: Boolean
                                                    )(implicit user: MtdItUser[_],
                                                      hc: HeaderCarrier,
                                                      ec: ExecutionContext): Future[ManageIncomeSourceDetailsViewModel] = {

    val desiredIncomeSourceMaybe: Option[PropertyDetailsModel] = {
      if (incomeSourceType == UkProperty) {
        sources.properties
          .filterNot(_.isCeased)
          .find(_.isUkProperty)
      } else {
        sources.properties
          .filterNot(_.isCeased)
          .find(_.isForeignProperty)
      }
    }

    desiredIncomeSourceMaybe match {
      case Some(desiredIncomeSource) =>
        desiredIncomeSource.latencyDetails match {
          case Some(latencyDetails) =>
            handleLatencyAndCrystallisationDetailsForProperty(desiredIncomeSource, latencyDetails, incomeSourceType)
          case None =>
            Future.successful(variableViewModelPropertyBusiness(
              incomeSource = desiredIncomeSource,
              latencyYearsQuarterly = LatencyYearsQuarterly(Some(false), Some(false)),
              latencyYearsCrystallised = LatencyYearsCrystallised(Some(false), Some(false)),
              incomeSourceType = incomeSourceType
            ))

        }
      case None =>
        Future.failed(new Error("Unable to find income source"))
    }
  }

  private def handleLatencyAndCrystallisationDetailsForProperty(
                                                                 desiredIncomeSource: PropertyDetailsModel,
                                                                 latencyDetails: LatencyDetails,
                                                                 incomeSourceType: IncomeSourceType
                                                               )(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[ManageIncomeSourceDetailsViewModel] = {

    for {
      (latencyYearOneStatus, latencyYearTwoStatus) <- getLatencyStatusesBasedOnFeatureSwitch(latencyDetails)
      crystallisationData <- getCrystallisationInformation(Some(latencyDetails))
    } yield {
      val maybeUseMTD1 = if (isEnabled(OptInOptOutContentUpdateR17)) Some(latencyYearOneStatus) else None
      val maybeUseMTD2 = if (isEnabled(OptInOptOutContentUpdateR17)) Some(latencyYearTwoStatus) else None

      crystallisationData match {
        case None =>
          variableViewModelPropertyBusiness(
            incomeSource = desiredIncomeSource,
            latencyYearsQuarterly = LatencyYearsQuarterly(Some(latencyYearOneStatus), Some(latencyYearTwoStatus)),
            latencyYearsCrystallised = LatencyYearsCrystallised(None, None),
            incomeSourceType = incomeSourceType,
            useMTDForTaxYear1 = maybeUseMTD1,
            useMTDForTaxYear2 = maybeUseMTD2
          )

        case Some(crystallisationList: List[Boolean]) =>
          variableViewModelPropertyBusiness(
            incomeSource = desiredIncomeSource,
            latencyYearsQuarterly = LatencyYearsQuarterly(Some(latencyYearOneStatus), Some(latencyYearTwoStatus)),
            latencyYearsCrystallised = LatencyYearsCrystallised(crystallisationList.headOption, crystallisationList.lastOption),
            incomeSourceType = incomeSourceType,
            useMTDForTaxYear1 = maybeUseMTD1,
            useMTDForTaxYear2 = maybeUseMTD2
          )
      }
    }
  }
}