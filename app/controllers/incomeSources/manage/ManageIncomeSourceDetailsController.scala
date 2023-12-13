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
import config.featureswitch.{FeatureSwitching, TimeMachineAddYear}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{JourneyType, Manage}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.core.IncomeSourceIdHash.mkFromQueryString
import models.core.{IncomeSourceId, IncomeSourceIdHash}
import models.incomeSourceDetails._
import models.incomeSourceDetails.viewmodels.ManageIncomeSourceDetailsViewModel
import play.api.Logger
import play.api.mvc._
import services._
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.{IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.manage.ManageIncomeSourceDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageIncomeSourceDetailsController @Inject()(val view: ManageIncomeSourceDetails,
                                                    val checkSessionTimeout: SessionTimeoutPredicate,
                                                    val authenticate: AuthenticationPredicate,
                                                    val authorisedFunctions: AuthorisedFunctions,
                                                    val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                                    val itvcErrorHandler: ItvcErrorHandler,
                                                    implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                    val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                    val itsaStatusService: ITSAStatusService,
                                                    val dateService: DateService,
                                                    val retrieveBtaNavBar: NavBarPredicate,
                                                    val calculationListService: CalculationListService,
                                                    val sessionService: SessionService)
                                                   (implicit val ec: ExecutionContext,
                                                    implicit override val mcc: MessagesControllerComponents,
                                                    val appConfig: FrontendAppConfig)
  extends ClientConfirmedController with FeatureSwitching with IncomeSourcesUtils with JourneyChecker {


  def showUkProperty: Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        incomeSourceIdHashMaybe = None,
        backUrl = controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(false).url,
        incomeSourceType = UkProperty
      )
  }

  def showUkPropertyAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              sources = mtdItUser.incomeSources,
              isAgent = true,
              backUrl = controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(true).url,
              None,
              incomeSourceType = UkProperty
            )
        }
  }

  def showForeignProperty: Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        incomeSourceIdHashMaybe = None,
        backUrl = controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(false).url,
        incomeSourceType = ForeignProperty
      )
  }

  def showForeignPropertyAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              sources = mtdItUser.incomeSources,
              isAgent = true,
              backUrl = controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(true).url,
              None,
              incomeSourceType = ForeignProperty
            )
        }
  }

  def showSoleTraderBusiness(hashIdString: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      withIncomeSourcesFS {
        val incomeSourceIdHashMaybe: Option[IncomeSourceIdHash] = mkFromQueryString(hashIdString).toOption

        val incomeSourceId: IncomeSourceId = incomeSourceIdHashMaybe.flatMap(x => user.incomeSources.compareHashToQueryString(x))
          .getOrElse(throw new Error(s"No incomeSourceId found for user with hash: [$hashIdString]"))
        sessionService.createSession(JourneyType(Manage, SelfEmployment).toString).flatMap { _ =>
          sessionService.setMongoKey(ManageIncomeSourceData.incomeSourceIdField, incomeSourceId.value, JourneyType(Manage, SelfEmployment)).flatMap {
            case Right(_) => handleRequest(
              sources = user.incomeSources,
              isAgent = false,
              backUrl = controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(false).url,
              incomeSourceIdHashMaybe = incomeSourceIdHashMaybe,
              incomeSourceType = SelfEmployment
            )
            case Left(exception) => Future.failed(exception)
          }
        }.recover {
          case exception =>
            Logger("application").error(s"[ManageIncomeSourceDetailsController][showSoleTraderBusiness] ${exception.getMessage}")
            itvcErrorHandler.showInternalServerError()
        }
      }
  }

  def showSoleTraderBusinessAgent(hashIdString: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            withIncomeSourcesFS {
              val incomeSourceIdHashMaybe: Option[IncomeSourceIdHash] = mkFromQueryString(hashIdString).toOption
              val result = handleRequest(
                sources = mtdItUser.incomeSources,
                isAgent = true,
                backUrl = controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(true).url,
                incomeSourceIdHashMaybe = incomeSourceIdHashMaybe,
                incomeSourceType = SelfEmployment
              )

              val incomeSourceId: IncomeSourceId = incomeSourceIdHashMaybe.flatMap(x => mtdItUser.incomeSources.compareHashToQueryString(x))
                .getOrElse(throw new Error(s"No incomeSourceId found for user with hash: [$hashIdString]"))
              sessionService.createSession(JourneyType(Manage, SelfEmployment).toString).flatMap { _ =>
                sessionService.setMongoKey(ManageIncomeSourceData.incomeSourceIdField, incomeSourceId.value, JourneyType(Manage, SelfEmployment)).flatMap {
                  case Right(_) => result
                  case Left(exception) => Future.failed(exception)
                }
              }
            }
        }
  }

  private def getQuarterType(latencyDetails: Option[LatencyDetails], quarterTypeElection: Option[QuarterTypeElection]): Option[QuarterReportingType] = {
    quarterTypeElection.flatMap(quarterTypeElection => {
      latencyDetails match {
        case Some(latencyDetails: LatencyDetails) =>
          val quarterIndicator = "Q"
          val currentTaxYearEnd = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear)).toString
          val showForLatencyTaxYear1 = (latencyDetails.taxYear1 == currentTaxYearEnd) && latencyDetails.latencyIndicator1.equals(quarterIndicator)
          val showForLatencyTaxYear2 = (latencyDetails.taxYear2 == currentTaxYearEnd) && latencyDetails.latencyIndicator2.equals(quarterIndicator)
          val showQuarterReportingType = showForLatencyTaxYear1 || showForLatencyTaxYear2
          if (showQuarterReportingType) quarterTypeElection.isStandardQuarterlyReporting else None
        case None => quarterTypeElection.isStandardQuarterlyReporting
      }
    })
  }

  private def getCrystallisationInformation(latencyDetails: Option[LatencyDetails])
                                           (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[List[Boolean]]] = {
    latencyDetails match {
      case Some(x) =>
        for {
          isTY1Crystallised <- calculationListService.isTaxYearCrystallised(x.taxYear1.toInt, isEnabled(TimeMachineAddYear))
          isTY2Crystallised <- calculationListService.isTaxYearCrystallised(x.taxYear2.toInt, isEnabled(TimeMachineAddYear))
        } yield {
          Some(List(isTY1Crystallised.get, isTY2Crystallised.get))
        }
      case _ =>
        Future.successful(None)
    }
  }

  private def variableViewModelSEBusiness(incomeSource: BusinessDetailsModel, itsaStatus: Boolean, crystallisationTaxYear1: Option[Boolean],
                                          crystallisationTaxYear2: Option[Boolean]): ManageIncomeSourceDetailsViewModel = {
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(incomeSource.incomeSourceId),
      tradingName = incomeSource.tradingName,
      tradingStartDate = incomeSource.tradingStartDate,
      address = incomeSource.address,
      businessAccountingMethod = incomeSource.cashOrAccruals,
      itsaHasMandatedOrVoluntaryStatusCurrentYear = itsaStatus,
      taxYearOneCrystallised = crystallisationTaxYear1,
      taxYearTwoCrystallised = crystallisationTaxYear2,
      latencyDetails = incomeSource.latencyDetails,
      incomeSourceType = SelfEmployment,
      quarterReportingType = getQuarterType(incomeSource.latencyDetails, incomeSource.quarterTypeElection)
    )
  }

  private def variableViewModelPropertyBusiness(incomeSource: PropertyDetailsModel, itsaStatus: Boolean, crystallisationTaxYear1: Option[Boolean],
                                                crystallisationTaxYear2: Option[Boolean], incomeSourceType: IncomeSourceType): ManageIncomeSourceDetailsViewModel = {
    ManageIncomeSourceDetailsViewModel(
      incomeSourceId = mkIncomeSourceId(incomeSource.incomeSourceId),
      tradingName = None,
      tradingStartDate = incomeSource.tradingStartDate,
      address = None,
      businessAccountingMethod = incomeSource.cashOrAccruals,
      itsaHasMandatedOrVoluntaryStatusCurrentYear = itsaStatus,
      taxYearOneCrystallised = crystallisationTaxYear1,
      taxYearTwoCrystallised = crystallisationTaxYear2,
      latencyDetails = incomeSource.latencyDetails,
      incomeSourceType = incomeSourceType,
      quarterReportingType = getQuarterType(incomeSource.latencyDetails, incomeSource.quarterTypeElection)
    )
  }


  private def getManageIncomeSourceViewModel(sources: IncomeSourceDetailsModel, incomeSourceId: IncomeSourceId, isAgent: Boolean)
                                            (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, ManageIncomeSourceDetailsViewModel]] = {

    val desiredIncomeSourceMaybe: Option[BusinessDetailsModel] = sources.businesses
      .filterNot(_.isCeased)
      .find(businessDetailsModel => businessDetailsModel.incomeSourceId == incomeSourceId.value)

    if (desiredIncomeSourceMaybe.isDefined) {
      itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear.flatMap {
        case true =>
          getCrystallisationInformation(desiredIncomeSourceMaybe.get.latencyDetails).flatMap {
            case None => Future(Right(variableViewModelSEBusiness(
              incomeSource = desiredIncomeSourceMaybe.get,
              itsaStatus = true,
              crystallisationTaxYear1 = None,
              crystallisationTaxYear2 = None)))
            case Some(crystallisationData: List[Boolean]) =>
              Future(Right(
                variableViewModelSEBusiness(
                  incomeSource = desiredIncomeSourceMaybe.get,
                  itsaStatus = true,
                  crystallisationTaxYear1 = crystallisationData.headOption,
                  crystallisationTaxYear2 = crystallisationData.lastOption
                )))
          }
        case false =>
          Future(Right(
            variableViewModelSEBusiness(
              incomeSource = desiredIncomeSourceMaybe.get,
              itsaStatus = false,
              crystallisationTaxYear1 = None,
              crystallisationTaxYear2 = None)
          ))
      }
    } else {
      Future(Left(
        new Error("Unable to find income source")
      ))
    }
  }

  private def getManageIncomeSourceViewModelProperty(sources: IncomeSourceDetailsModel, incomeSourceType: IncomeSourceType, isAgent: Boolean)
                                                    (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, ManageIncomeSourceDetailsViewModel]] = {
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
      itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear.flatMap {
        case true =>
          getCrystallisationInformation(desiredIncomeSourceMaybe.get.latencyDetails).flatMap {
            case None => Future(Right(variableViewModelPropertyBusiness(
              incomeSource = desiredIncomeSourceMaybe.get,
              itsaStatus = true,
              crystallisationTaxYear1 = None,
              crystallisationTaxYear2 = None,
              incomeSourceType = incomeSourceType)))
            case Some(crystallisationData: List[Boolean]) =>
              Future(Right(
                variableViewModelPropertyBusiness(
                  incomeSource = desiredIncomeSourceMaybe.get,
                  itsaStatus = true,
                  crystallisationTaxYear1 = crystallisationData.headOption,
                  crystallisationTaxYear2 = crystallisationData.lastOption,
                  incomeSourceType = incomeSourceType
                )))
          }
        case false =>
          Future(Right(
            variableViewModelPropertyBusiness(
              incomeSource = desiredIncomeSourceMaybe.get,
              itsaStatus = false,
              crystallisationTaxYear1 = None,
              crystallisationTaxYear2 = None,
              incomeSourceType = incomeSourceType)
          ))
      }
    } else {
      Future(Left(
        new Error("Unable to find income source")
      ))
    }
  }

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String, incomeSourceIdHashMaybe: Option[IncomeSourceIdHash],
                    incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    val incomeSourceIdMaybe: Option[IncomeSourceId] = incomeSourceIdHashMaybe.flatMap(x => user.incomeSources.compareHashToQueryString(x))

    withSessionData(JourneyType(Manage, incomeSourceType)) { _ =>
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
            Logger("application").error(s"[ManageIncomeSourceDetailsController][extractIncomeSource] unable to find income source: $error. isAgent = $isAgent")
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