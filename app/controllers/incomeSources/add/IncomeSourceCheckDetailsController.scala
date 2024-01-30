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

package controllers.incomeSources.add

import audit.AuditingService
import audit.models.CreateIncomeSourceAuditModel
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney.{BeforeSubmissionPage, IncomeSourceType, SelfEmployment}
import enums.JourneyType.{Add, JourneyType}
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckDetailsViewModel, CheckPropertyViewModel}
import models.incomeSourceDetails.{AddIncomeSourceData, BusinessDetailsModel, IncomeSourceDetailsModel, UIJourneySessionData}
import play.api.Logger
import play.api.mvc._
import services.{CreateBusinessDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.add.IncomeSourceCheckDetails

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceCheckDetailsController @Inject()(val checkDetailsView: IncomeSourceCheckDetails,
                                                   val authorisedFunctions: AuthorisedFunctions,
                                                   val businessDetailsService: CreateBusinessDetailsService,
                                                   val auditingService: AuditingService,
                                                   auth: AuthenticatorPredicate)
                                                  (implicit val ec: ExecutionContext,
                                                   implicit override val mcc: MessagesControllerComponents,
                                                   val appConfig: FrontendAppConfig,
                                                   implicit val sessionService: SessionService,
                                                   implicit val itvcErrorHandler: ItvcErrorHandler,
                                                   implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController
  with IncomeSourcesUtils with FeatureSwitching with JourneyChecker {

  def show(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        handleRequest(
          sources = user.incomeSources,
          isAgent = isAgent,
          incomeSourceType
        )
  }

  def submit(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        handleSubmit(isAgent, incomeSourceType)
  }

  private def handleRequest(sources: IncomeSourceDetailsModel,
                            isAgent: Boolean,
                            incomeSourceType: IncomeSourceType)
                           (implicit user: MtdItUser[_]): Future[Result] = withSessionData(JourneyType(Add, incomeSourceType), journeyState = BeforeSubmissionPage) { sessionData =>

    getViewModel(incomeSourceType, sessionData)(user) match {
      case Some(viewModel) =>
        Future.successful {
          Ok(checkDetailsView(
            viewModel,
            postAction = postAction(isAgent, incomeSourceType),
            isAgent,
            backUrl = backUrl(isAgent, false, incomeSourceType)
          ))
        }
      case None =>
        val agentPrefix = if (isAgent) "[Agent]" else ""
        Logger("application").error(agentPrefix +
          s"[IncomeSourceCheckDetailsController][handleRequest]: Unable to construct view model for $incomeSourceType")
        Future.successful {
          errorHandler(isAgent).showInternalServerError()
        }
    }
  }.recover {
    case ex: Throwable =>
      val agentPrefix = if (isAgent) "[Agent]" else ""
      Logger("application").error(agentPrefix +
        s"[IncomeSourceCheckDetailsController][handleRequest]: Unexpected exception ${ex.getMessage} - ${ex.getCause}")
      errorHandler(isAgent).showInternalServerError()
  }

  private def getViewModel(incomeSourceType: IncomeSourceType, sessionData: UIJourneySessionData)
                          (implicit user: MtdItUser[_]): Option[CheckDetailsViewModel] = {
    if (incomeSourceType == SelfEmployment) getBusinessModel(sessionData) else getPropertyModel(incomeSourceType, sessionData)
  }

  private def getPropertyModel(incomeSourceType: IncomeSourceType, sessionData: UIJourneySessionData): Option[CheckPropertyViewModel] = {
    val accountingMethodOpt = sessionData.addIncomeSourceData.flatMap(_.incomeSourcesAccountingMethod)
    val dateStartedOpt = sessionData.addIncomeSourceData.flatMap(_.dateStarted)
    (dateStartedOpt, accountingMethodOpt) match {
      case (Some(dateStarted), Some(accountingMethod)) =>
        Some(CheckPropertyViewModel(
          tradingStartDate = dateStarted,
          cashOrAccrualsFlag = accountingMethod,
          incomeSourceType = incomeSourceType
        ))
      case (_, _) =>
        None
    }
  }

  private def getBusinessModel(sessionData: UIJourneySessionData)(implicit user: MtdItUser[_]): Option[CheckBusinessDetailsViewModel] = {
    val userActiveBusinesses: List[BusinessDetailsModel] = user.incomeSources.businesses.filterNot(_.isCeased)
    val showAccountingMethodPage: Boolean = userActiveBusinesses.isEmpty

    sessionData.addIncomeSourceData.flatMap { addIncomeSourceData =>
      for {
        address <- addIncomeSourceData.address
        accountingPeriodEndDate <- addIncomeSourceData.accountingPeriodEndDate
        businessTrade <- addIncomeSourceData.businessTrade
        businessAddressLine1 <- address.lines.headOption
        incomeSourcesAccountingMethod <- addIncomeSourceData.incomeSourcesAccountingMethod
      } yield CheckBusinessDetailsViewModel(
        businessName = addIncomeSourceData.businessName,
        businessStartDate = addIncomeSourceData.dateStarted,
        accountingPeriodEndDate = accountingPeriodEndDate,
        businessTrade = businessTrade,
        businessAddressLine1 = businessAddressLine1,
        businessAddressLine2 = address.lines.lift(1),
        businessAddressLine3 = address.lines.lift(2),
        businessAddressLine4 = address.lines.lift(3),
        businessPostalCode = address.postcode,
        businessCountryCode = addIncomeSourceData.countryCode,
        incomeSourcesAccountingMethod = Some(incomeSourcesAccountingMethod),
        cashOrAccrualsFlag = incomeSourcesAccountingMethod,
        showedAccountingMethod = showAccountingMethodPage
      )
    }
  }




  private def handleSubmit(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {
    withSessionData(JourneyType(Add, incomeSourceType), BeforeSubmissionPage) { sessionData =>

      val redirectUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
        routes.IncomeSourceReportingMethodController.show(isAgent, incomeSourceType).url

      val viewModel = getViewModel(incomeSourceType, sessionData)

      viewModel match {
        case Some(viewModel) =>
          businessDetailsService.createRequest(viewModel).flatMap {
            case Right(CreateIncomeSourceResponse(id)) =>
              auditingService.extendedAudit(
                CreateIncomeSourceAuditModel(incomeSourceType, viewModel, None, None, Some(CreateIncomeSourceResponse(id)))
              )
              sessionService.setMongoData(
                sessionData.copy(
                  addIncomeSourceData =
                    sessionData.addIncomeSourceData.map(
                      _.copy(
                        incomeSourceId = Some(id)
                      )
                    )
                )
              ) flatMap {
                case true => Future.successful(Redirect(redirectUrl(isAgent, incomeSourceType)))
                case false => Future.failed(new Exception("Mongo update call was not acknowledged"))
              }
            case Left(ex) =>
              auditingService.extendedAudit(
                CreateIncomeSourceAuditModel(incomeSourceType, viewModel, Some(enums.FailureCategory.ApiFailure), Some(ex.getMessage), None)
              )
              Future.failed(ex)
          }
        case None =>
          val agentPrefix = if (isAgent) "[Agent]" else ""
          Logger("application").error(agentPrefix +
            s"[IncomeSourceCheckDetailsController][handleSubmit]: Unable to construct view model for $incomeSourceType")
          Future.successful {
            Redirect(errorRedirectUrl(isAgent, incomeSourceType))
          }
      }
    }
  }.recover {
    case ex: Exception =>
      Logger("application").error(s"[IncomeSourceCheckDetailsController][handleSubmit]: ${ex.getMessage}")
      Redirect(errorRedirectUrl(isAgent, incomeSourceType))
  }

  private lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private lazy val errorRedirectUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
    routes.IncomeSourceNotAddedController.show(isAgent, incomeSourceType).url

  private lazy val backUrl: (Boolean, Boolean, IncomeSourceType) => String = (isAgent, isChange, incomeSourceType) =>
    routes.IncomeSourcesAccountingMethodController.show(isAgent, isChange, incomeSourceType).url

  private lazy val postAction: (Boolean, IncomeSourceType) => Call = (isAgent, incomeSourceType) =>
    routes.IncomeSourceCheckDetailsController.submit(isAgent, incomeSourceType)
}
