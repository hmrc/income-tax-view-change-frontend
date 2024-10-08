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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney._
import enums.JourneyType.{JourneyType, Manage}
import models.admin.CalendarQuarterTypes
import models.core.IncomeSourceId.mkIncomeSourceId
import models.core.IncomeSourceIdHash.{mkFromQueryString, mkIncomeSourceIdHash}
import models.core.{IncomeSourceId, IncomeSourceIdHash}
import models.incomeSourceDetails._
import models.incomeSourceDetails.viewmodels.ManageIncomeSourceDetailsViewModel
import play.api.Logger
import play.api.mvc._
import services._
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.manage.ManageIncomeSourceDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageIncomeSourceDetailsController @Inject()(val view: ManageIncomeSourceDetails,
                                                    val authorisedFunctions: AuthorisedFunctions,
                                                    val itvcErrorHandler: ItvcErrorHandler,
                                                    implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                    val itsaStatusService: ITSAStatusService,
                                                    val dateService: DateService,
                                                    val calculationListService: CalculationListService,
                                                    val sessionService: SessionService,
                                                    val auth: AuthenticatorPredicate)
                                                   (implicit val ec: ExecutionContext,
                                                    implicit override val mcc: MessagesControllerComponents,
                                                    val appConfig: FrontendAppConfig)
  extends ClientConfirmedController with FeatureSwitching with IncomeSourcesUtils with JourneyCheckerManageBusinesses {


  def show(isAgent: Boolean, incomeSourceType: IncomeSourceType, id: Option[String]): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      withSessionData(JourneyType(Manage, incomeSourceType), InitialPage) { _ =>
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
          case _ => handleRequest(
            sources = user.incomeSources,
            isAgent = isAgent,
            incomeSourceIdHashMaybe = None,
            backUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show(isAgent).url,
            incomeSourceType = incomeSourceType
          )
        }
      }
  }

  def handleSoleTrader(hashIdString: String, isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val incomeSourceIdHash: Either[Throwable, IncomeSourceIdHash] = mkFromQueryString(hashIdString)
    incomeSourceIdHash match {
      case Left(exception: Exception) => Future.failed(exception)
      case Left(_) => Future.failed(new Error(s"Unexpected exception incomeSourceIdHash: <$incomeSourceIdHash>"))
      case Right(incomeSourceIdHash: IncomeSourceIdHash) =>

        val hashCompareResult: Either[Throwable, IncomeSourceId] = user.incomeSources.compareHashToQueryString(incomeSourceIdHash)

        hashCompareResult match {
          case Left(exception: Exception) => Future.failed(exception)
          case Left(_) => Future.failed(new Error(s"Unexpected exception incomeSourceIdHash: <$incomeSourceIdHash>"))
          case Right(incomeSourceId: IncomeSourceId) =>
            sessionService.setMongoKey(ManageIncomeSourceData.incomeSourceIdField, incomeSourceId.value, JourneyType(Manage, SelfEmployment)).flatMap {
              case Right(_) => handleRequest(
                sources = user.incomeSources,
                isAgent = isAgent,
                backUrl = controllers.manageBusinesses.routes.ManageYourBusinessesController.show(isAgent).url,
                incomeSourceIdHashMaybe = Some(incomeSourceIdHash),
                incomeSourceType = SelfEmployment
              )
              case Left(exception) => Future.failed(exception)
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
    }
  }

  private def getQuarterType(latencyDetails: Option[LatencyDetails], quarterTypeElection: Option[QuarterTypeElection])
                            (implicit user: MtdItUser[_]): Option[QuarterReportingType] = {
    if (isEnabled(CalendarQuarterTypes)) {
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
    } else None
  }

  private def getCrystallisationInformation(latencyDetails: Option[LatencyDetails])
                                           (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[List[Boolean]]] = {
    latencyDetails match {
      case Some(x) =>
        for {
          isTY1Crystallised <- calculationListService.isTaxYearCrystallised(x.taxYear1.toInt)
          isTY2Crystallised <- calculationListService.isTaxYearCrystallised(x.taxYear2.toInt)
        } yield {
          Some(List(isTY1Crystallised, isTY2Crystallised))
        }
      case _ =>
        Future.successful(None)
    }
  }

  private def variableViewModelSEBusiness(incomeSource: BusinessDetailsModel,
                                          latencyYearsQuarterly: LatencyYearsQuarterly,
                                          latencyYearsCrystallised: LatencyYearsCrystallised)(implicit user: MtdItUser[_]): ManageIncomeSourceDetailsViewModel = {
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
      quarterReportingType = getQuarterType(incomeSource.latencyDetails, incomeSource.quarterTypeElection)
    )
  }

  private def variableViewModelPropertyBusiness(incomeSource: PropertyDetailsModel,
                                                latencyYearsQuarterly: LatencyYearsQuarterly,
                                                latencyYearsCrystallised: LatencyYearsCrystallised,
                                                incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): ManageIncomeSourceDetailsViewModel = {
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
      quarterReportingType = getQuarterType(incomeSource.latencyDetails, incomeSource.quarterTypeElection)
    )
  }


  private def getManageIncomeSourceViewModel(
                                              sources: IncomeSourceDetailsModel,
                                              incomeSourceId: IncomeSourceId,
                                              isAgent: Boolean
                                            )(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, ManageIncomeSourceDetailsViewModel]] = {

    val desiredIncomeSourceMaybe: Option[BusinessDetailsModel] = sources.businesses
      .filterNot(_.isCeased)
      .find(_.incomeSourceId == incomeSourceId.value)

    desiredIncomeSourceMaybe match {
      case Some(desiredIncomeSource) =>
        desiredIncomeSource.latencyDetails match {
          case Some(latencyDetails) =>
            handleLatencyAndCrystallisationDetails(desiredIncomeSource, latencyDetails)
          case None =>
            Future.successful(Right(variableViewModelSEBusiness(
              incomeSource = desiredIncomeSource,
              latencyYearsQuarterly = LatencyYearsQuarterly(
                firstYear = Some(false),
                secondYear = Some(false)
              ),
              latencyYearsCrystallised = LatencyYearsCrystallised(
                firstYear = None,
                secondYear = None
              )
            )))
        }
      case None =>
        Future.successful(Left(new Error("Unable to find income source")))
    }
  }

  private def handleLatencyAndCrystallisationDetails(
                                                      desiredIncomeSource: BusinessDetailsModel,
                                                      latencyDetails: LatencyDetails
                                                    )(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, ManageIncomeSourceDetailsViewModel]] = {

    for {
      (latencyYearOneStatus, latencyYearTwoStatus) <- itsaStatusService.hasMandatedOrVoluntaryStatusForLatencyYears(Some(latencyDetails))
      crystallisationData <- getCrystallisationInformation(Some(latencyDetails))
    } yield crystallisationData match {
      case None =>
        Right(variableViewModelSEBusiness(
          incomeSource = desiredIncomeSource,
          latencyYearsQuarterly = LatencyYearsQuarterly(
            firstYear = Some(latencyYearOneStatus),
            secondYear = Some(latencyYearTwoStatus)
          ),
          latencyYearsCrystallised = LatencyYearsCrystallised(
            firstYear = None,
            secondYear = None
          )
        ))

      case Some(crystallisationList: List[Boolean]) =>
        Right(variableViewModelSEBusiness(
          incomeSource = desiredIncomeSource,
          latencyYearsQuarterly = LatencyYearsQuarterly(
            firstYear = Some(latencyYearOneStatus),
            secondYear = Some(latencyYearTwoStatus)
          ),
          latencyYearsCrystallised = LatencyYearsCrystallised(
            firstYear = crystallisationList.headOption,
            secondYear = crystallisationList.lastOption
          )
        ))
    }
  }


  private def getManageIncomeSourceViewModelProperty(
                                                      sources: IncomeSourceDetailsModel,
                                                      incomeSourceType: IncomeSourceType,
                                                      isAgent: Boolean
                                                    )(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, ManageIncomeSourceDetailsViewModel]] = {

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
            Future.successful(Right(variableViewModelPropertyBusiness(
              incomeSource = desiredIncomeSource,
              latencyYearsQuarterly = LatencyYearsQuarterly(Some(false), Some(false)),
              latencyYearsCrystallised = LatencyYearsCrystallised(Some(false), Some(false)),
              incomeSourceType = incomeSourceType
            )))

        }
      case None =>
        Future.successful(Left(new Error("Unable to find income source")))
    }
  }

  private def handleLatencyAndCrystallisationDetailsForProperty(
                                                                 desiredIncomeSource: PropertyDetailsModel,
                                                                 latencyDetails: LatencyDetails,
                                                                 incomeSourceType: IncomeSourceType
                                                               )(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, ManageIncomeSourceDetailsViewModel]] = {

    for {
      (latencyYearOneStatus, latencyYearTwoStatus) <- itsaStatusService.hasMandatedOrVoluntaryStatusForLatencyYears(Some(latencyDetails))
      crystallisationData <- getCrystallisationInformation(Some(latencyDetails))
    } yield crystallisationData match {
      case None =>
        Right(variableViewModelPropertyBusiness(
          incomeSource = desiredIncomeSource,
          latencyYearsQuarterly = LatencyYearsQuarterly(Some(latencyYearOneStatus), Some(latencyYearTwoStatus)),
          latencyYearsCrystallised = LatencyYearsCrystallised(None, None),
          incomeSourceType = incomeSourceType
        ))
      case Some(crystallisationList: List[Boolean]) =>
        Right(variableViewModelPropertyBusiness(
          incomeSource = desiredIncomeSource,
          latencyYearsQuarterly = LatencyYearsQuarterly(Some(latencyYearOneStatus), Some(latencyYearTwoStatus)),
          latencyYearsCrystallised = LatencyYearsCrystallised(crystallisationList.headOption, crystallisationList.lastOption),
          incomeSourceType = incomeSourceType
        ))
    }
  }


  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String, incomeSourceIdHashMaybe: Option[IncomeSourceIdHash],
                    incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    withSessionData(JourneyType(Manage, incomeSourceType), journeyState = InitialPage) { _ =>

      val hashCompareResult: Option[Either[Throwable, IncomeSourceId]] = incomeSourceIdHashMaybe.map(x => user.incomeSources.compareHashToQueryString(x))

      hashCompareResult match {
        case Some(Left(exception: Exception)) => Future.failed(exception)
        case _ =>
          val incomeSourceIdMaybe: Option[IncomeSourceId] = IncomeSourceId.toOption(hashCompareResult)

          for {
            value <- if (incomeSourceType == SelfEmployment) {
              getManageIncomeSourceViewModel(sources = sources, incomeSourceId = incomeSourceIdMaybe
                .getOrElse(throw new Error(s"No incomeSourceId found for user with hash: [${incomeSourceIdHashMaybe.map(x => x.hash)}]")), isAgent = isAgent)
            } else {
              getManageIncomeSourceViewModelProperty(sources = sources, isAgent = isAgent, incomeSourceType = incomeSourceType)
            }
          } yield {
            value match {
              case Right(viewModel) =>
                Ok(view(viewModel = viewModel,
                  isAgent = isAgent,
                  backUrl = backUrl
                ))
              case Left(error) =>
                Logger("application")
                  .error(s"unable to find income source: $error. isAgent = $isAgent")
                if (isAgent) {
                  itvcErrorHandlerAgent.showInternalServerError()
                } else {
                  itvcErrorHandler.showInternalServerError()
                }
            }
          }
      }
    }
  }

  def showChange(incomeSourceType: IncomeSourceType, isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      withSessionData(JourneyType(Manage, incomeSourceType), InitialPage) { sessionData =>
        val incomeSourceIdStringOpt = sessionData.manageIncomeSourceData.flatMap(_.incomeSourceId)
        val incomeSourceIdOpt = incomeSourceIdStringOpt.map(id => mkIncomeSourceIdHash(IncomeSourceId(id)))
        incomeSourceType match {
          case SelfEmployment => incomeSourceIdOpt match {
            case Some(realId) => handleSoleTrader(realId.hash, isAgent)
            case None => Logger("application")
              .error(s"no incomeSourceId supplied with SelfEmployment isAgent = $isAgent")
              Future.successful(if (isAgent) {
                itvcErrorHandlerAgent.showInternalServerError()
              } else {
                itvcErrorHandler.showInternalServerError()
              })
          }
          case _ => handleRequest(
            sources = user.incomeSources,
            isAgent = isAgent,
            incomeSourceIdHashMaybe = None,
            backUrl = controllers.manageBusinesses.manage.routes.CheckYourAnswersController.show(isAgent, incomeSourceType).url,
            incomeSourceType = incomeSourceType
          )
        }
      }
  }
}