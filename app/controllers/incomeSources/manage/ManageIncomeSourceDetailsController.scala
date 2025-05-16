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

package controllers.incomeSources.manage

import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney._
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.core.IncomeSourceIdHash.mkFromQueryString
import models.core.{IncomeSourceId, IncomeSourceIdHash}
import models.incomeSourceDetails._
import models.incomeSourceDetails.viewmodels.ManageIncomeSourceDetailsViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyChecker
import views.html.incomeSources.manage.ManageIncomeSourceDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageIncomeSourceDetailsController @Inject()(val view: ManageIncomeSourceDetails,
                                                    val authActions: AuthActions,
                                                    val itsaStatusService: ITSAStatusService,
                                                    val dateService: DateService,
                                                    val calculationListService: CalculationListService,
                                                    val sessionService: SessionService)
                                                   (implicit val ec: ExecutionContext,
                                                    val itvcErrorHandler: ItvcErrorHandler,
                                                    val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                    val mcc: MessagesControllerComponents,
                                                    val appConfig: FrontendAppConfig) extends FrontendController(mcc)
    with I18nSupport with JourneyChecker {


  def show(isAgent: Boolean,
           incomeSourceType: IncomeSourceType,
           id: Option[String]): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withSessionData(IncomeSourceJourneyType(Manage, incomeSourceType), InitialPage) { _ =>
        incomeSourceType match {
          case SelfEmployment => id match {
            case Some(realId) => handleSoleTrader(realId, isAgent)
            case None => Logger("application")
              .error(s"no incomeSourceId supplied with SelfEmployment isAgent = $isAgent")
              Future.successful(if (isAgent) {
                itvcErrorHandlerAgent.showInternalServerError()
              } else {
                itvcErrorHandler.showInternalServerError()
              })
          }
          case _ => handleProperty(
            sources = user.incomeSources,
            isAgent = isAgent,
            backUrl = controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(isAgent).url,
            incomeSourceType = incomeSourceType
          )
        }
      }
  }

  def handleSoleTrader(hashIdString: String,
                       isAgent: Boolean)
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
      backUrl = controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(isAgent).url
    ))

    result.recover{
      case ex =>
        Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
        if (isAgent) {
          itvcErrorHandlerAgent.showInternalServerError()
        } else {
          itvcErrorHandler.showInternalServerError()
        }
    }
  }


  def handleProperty(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String,
                     incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    getManageIncomeSourceViewModelProperty(sources = sources, isAgent = isAgent, incomeSourceType = incomeSourceType)
      .map { viewModel =>
        Ok(view(
          viewModel = viewModel,
          isAgent = isAgent,
          backUrl = backUrl
        ))
      }.recover {
        case ex =>
          Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
          if (isAgent) {
            itvcErrorHandlerAgent.showInternalServerError()
          } else {
            itvcErrorHandler.showInternalServerError()
          }
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

  private def variableViewModelSEBusiness(incomeSource: BusinessDetailsModel, itsaStatus: Boolean, crystallisationTaxYear1: Option[Boolean],
                                          crystallisationTaxYear2: Option[Boolean]): ManageIncomeSourceDetailsViewModel = {
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(incomeSource.incomeSourceId),
      incomeSource = incomeSource.incomeSource,
      tradingName = incomeSource.tradingName,
      tradingStartDate = incomeSource.tradingStartDate,
      address = incomeSource.address,
      isTraditionalAccountingMethod = incomeSource.cashOrAccruals,
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = None,
        secondYear = Some(itsaStatus)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = crystallisationTaxYear1,
        secondYear = crystallisationTaxYear2
      ),
      latencyDetails = incomeSource.latencyDetails,
      incomeSourceType = SelfEmployment,
      quarterReportingType = getQuarterType(incomeSource.latencyDetails, incomeSource.quarterTypeElection)
    )
  }

  private def variableViewModelPropertyBusiness(incomeSource: PropertyDetailsModel,
                                                itsaStatus: Boolean,
                                                crystallisationTaxYear1: Option[Boolean],
                                                crystallisationTaxYear2: Option[Boolean],
                                                incomeSourceType: IncomeSourceType): ManageIncomeSourceDetailsViewModel = {
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(incomeSource.incomeSourceId),
      incomeSource = None,
      tradingName = None,
      tradingStartDate = incomeSource.tradingStartDate,
      address = None,
      isTraditionalAccountingMethod = incomeSource.cashOrAccruals,
      latencyYearsQuarterly = LatencyYearsQuarterly(
        firstYear = None,
        secondYear = Some(itsaStatus)
      ),
      latencyYearsCrystallised = LatencyYearsCrystallised(
        firstYear = crystallisationTaxYear1,
        secondYear = crystallisationTaxYear2
      ),
      latencyDetails = incomeSource.latencyDetails,
      incomeSourceType = incomeSourceType,
      quarterReportingType = getQuarterType(incomeSource.latencyDetails, incomeSource.quarterTypeElection)
    )
  }


  private def getManageIncomeSourceViewModel(sources: IncomeSourceDetailsModel,
                                             incomeSourceId: IncomeSourceId,
                                             isAgent: Boolean)
                                            (implicit user: MtdItUser[_],
                                             hc: HeaderCarrier,
                                             ec: ExecutionContext): Future[ManageIncomeSourceDetailsViewModel] = {

    val desiredIncomeSourceMaybe: Option[BusinessDetailsModel] = sources.businesses
      .filterNot(_.isCeased)
      .find(businessDetailsModel => businessDetailsModel.incomeSourceId == incomeSourceId.value)

    if (desiredIncomeSourceMaybe.isDefined) {
      itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear(user.nino).flatMap {
        case true =>
          getCrystallisationInformation(desiredIncomeSourceMaybe.get.latencyDetails).flatMap {
            case None => Future(variableViewModelSEBusiness(
              incomeSource = desiredIncomeSourceMaybe.get,
              itsaStatus = true,
              crystallisationTaxYear1 = None,
              crystallisationTaxYear2 = None))
            case Some(crystallisationData: List[Boolean]) =>
              Future(
                variableViewModelSEBusiness(
                  incomeSource = desiredIncomeSourceMaybe.get,
                  itsaStatus = true,
                  crystallisationTaxYear1 = crystallisationData.headOption,
                  crystallisationTaxYear2 = crystallisationData.lastOption
                ))
          }
        case false =>
          Future(
            variableViewModelSEBusiness(
              incomeSource = desiredIncomeSourceMaybe.get,
              itsaStatus = false,
              crystallisationTaxYear1 = None,
              crystallisationTaxYear2 = None)
          )
      }
    } else {
      Future.failed(
        new Error("Unable to find income source")
      )
    }
  }

  private def getManageIncomeSourceViewModelProperty(sources: IncomeSourceDetailsModel,
                                                     incomeSourceType: IncomeSourceType,
                                                     isAgent: Boolean)
                                                    (implicit user: MtdItUser[_],
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

    if (desiredIncomeSourceMaybe.isDefined) {
      itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear(user.nino).flatMap {
        case true =>
          getCrystallisationInformation(desiredIncomeSourceMaybe.get.latencyDetails).flatMap {
            case None => Future(variableViewModelPropertyBusiness(
              incomeSource = desiredIncomeSourceMaybe.get,
              itsaStatus = true,
              crystallisationTaxYear1 = None,
              crystallisationTaxYear2 = None,
              incomeSourceType = incomeSourceType))
            case Some(crystallisationData: List[Boolean]) =>
              Future(
                variableViewModelPropertyBusiness(
                  incomeSource = desiredIncomeSourceMaybe.get,
                  itsaStatus = true,
                  crystallisationTaxYear1 = crystallisationData.headOption,
                  crystallisationTaxYear2 = crystallisationData.lastOption,
                  incomeSourceType = incomeSourceType
                ))
          }
        case false =>
          Future(
            variableViewModelPropertyBusiness(
              incomeSource = desiredIncomeSourceMaybe.get,
              itsaStatus = false,
              crystallisationTaxYear1 = None,
              crystallisationTaxYear2 = None,
              incomeSourceType = incomeSourceType)
          )
      }
    } else {
      Future.failed(
        new Error("Unable to find income source")
      )
    }
  }
}