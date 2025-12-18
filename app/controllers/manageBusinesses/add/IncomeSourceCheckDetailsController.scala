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

package controllers.manageBusinesses.add

import audit.AuditingService
import audit.models.CreateIncomeSourceAuditModel
import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.BeforeSubmissionPage
import enums.IncomeSourceJourney._
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.TriggeredMigration.TriggeredMigrationAdded
import models.UIJourneySessionData
import models.core.NormalMode
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckDetailsViewModel, CheckPropertyViewModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CreateBusinessDetailsService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.add.IncomeSourceCheckDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceCheckDetailsController @Inject()(val incomeSourceCheckDetailsView: IncomeSourceCheckDetailsView,
                                                   val authActions: AuthActions,
                                                   val businessDetailsService: CreateBusinessDetailsService,
                                                   val auditingService: AuditingService,
                                                   val sessionService: SessionService,
                                                   val itvcErrorHandler: ItvcErrorHandler,
                                                   val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                  (implicit val ec: ExecutionContext,
                                                   val mcc: MessagesControllerComponents,
                                                   val appConfig: FrontendAppConfig) extends FrontendController(mcc)
  with JourneyCheckerManageBusinesses with I18nSupport {


  private lazy val errorRedirectUrl: (Boolean, IncomeSourceType, Boolean) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType, isTriggeredMigration: Boolean) =>
    if (isAgent) routes.IncomeSourceNotAddedController.showAgent(incomeSourceType, isTriggeredMigration).url
    else routes.IncomeSourceNotAddedController.show(incomeSourceType, isTriggeredMigration).url

  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        incomeSourceType
      )(implicitly, itvcErrorHandler)
  }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      handleRequest(
        sources = mtdItUser.incomeSources,
        isAgent = true,
        incomeSourceType
      )(implicitly, itvcErrorHandlerAgent)
  }

  private def handleRequest(sources: IncomeSourceDetailsModel,
                            isAgent: Boolean,
                            incomeSourceType: IncomeSourceType)
                           (implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] =
    withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), journeyState = BeforeSubmissionPage) { sessionData =>
      val backUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(isAgent, NormalMode, incomeSourceType).url
      val postAction: Call = if (isAgent) controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.submitAgent(incomeSourceType) else {
        controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.submit(incomeSourceType)
      }
      getViewModel(incomeSourceType, sessionData)(user) match {
        case Some(viewModel) =>
          Future.successful {
            Ok(
              incomeSourceCheckDetailsView(
                viewModel,
                postAction = postAction,
                isAgent,
                backUrl = backUrl
              )
            )
          }
        case None =>
          val agentPrefix = if (isAgent) "[Agent]" else ""
          Logger("application").error(agentPrefix +
            s"Unable to construct view model for $incomeSourceType")
          Future.successful {
            errorHandler.showInternalServerError()
          }
      }
    }.recover {
      case ex: Throwable =>
        val agentPrefix = if (isAgent) "[Agent]" else ""
        Logger("application").error(agentPrefix +
          s"Unexpected exception ${ex.getMessage} - ${ex.getCause}")
        errorHandler.showInternalServerError()
    }

  private def getViewModel(incomeSourceType: IncomeSourceType, sessionData: UIJourneySessionData)
                          (implicit user: MtdItUser[_]): Option[CheckDetailsViewModel] = {
    if (incomeSourceType == SelfEmployment) getBusinessModel(sessionData) else getPropertyModel(incomeSourceType, sessionData)
  }

  private def getPropertyModel(incomeSourceType: IncomeSourceType, sessionData: UIJourneySessionData): Option[CheckPropertyViewModel] = {
    val dateStartedOpt = sessionData.addIncomeSourceData.flatMap(_.dateStarted)
    dateStartedOpt match {
      case Some(dateStarted) =>
        Some(
          CheckPropertyViewModel(
            tradingStartDate = dateStarted,
            incomeSourceType = incomeSourceType
          )
        )
      case _ => None
    }
  }

  private def getBusinessModel(sessionData: UIJourneySessionData)(implicit user: MtdItUser[_]): Option[CheckBusinessDetailsViewModel] = {

    sessionData.addIncomeSourceData.flatMap { addIncomeSourceData =>
      for {
        address <- addIncomeSourceData.address
        accountingPeriodEndDate <- addIncomeSourceData.accountingPeriodEndDate
        businessTrade <- addIncomeSourceData.businessTrade
        businessAddressLine1 <- address.lines.headOption
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
        businessCountryCode = addIncomeSourceData.countryCode
      )
    }
  }


  def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleSubmit(isAgent = false, incomeSourceType)
  }

  def submitAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      handleSubmit(isAgent = true, incomeSourceType)
  }

  private def handleSubmit(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), BeforeSubmissionPage) { sessionData =>
      val redirectUrl: (Boolean, IncomeSourceType, Boolean) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType, isTriggeredMigration: Boolean) => {
        if (isTriggeredMigration) {
          controllers.triggeredMigration.routes.CheckHmrcRecordsController.show(isAgent, Some(TriggeredMigrationAdded(incomeSourceType).toString)).url
        } else {
          controllers.manageBusinesses.add.routes.IncomeSourceReportingFrequencyController.show(isAgent, isChange = false, incomeSourceType).url
        }
      }

      val triggeredMigrationOpt = sessionData.triggeredMigrationSessionData.map(_.isTriggeredMigrationJourney)

      val viewModel = getViewModel(incomeSourceType, sessionData)

      (viewModel, triggeredMigrationOpt) match {
        case (Some(viewModel), Some(isTriggeredMigration)) =>
          businessDetailsService.createRequest(viewModel) flatMap {
            case Right(CreateIncomeSourceResponse(id)) =>

              auditingService.extendedAudit(
                CreateIncomeSourceAuditModel(incomeSourceType, viewModel, None, None, Some(CreateIncomeSourceResponse(id)))
              )

              val saveAddIncomeSourceDataToMongo =
                for {
                  result: Boolean <-
                    sessionService.setMongoData(
                      sessionData.copy(
                        addIncomeSourceData = sessionData.addIncomeSourceData.map(_.copy(incomeSourceId = Some(id)))
                      )
                    )
                } yield {
                  result
                }

              saveAddIncomeSourceDataToMongo.flatMap {
                case true => Future.successful(Redirect(redirectUrl(isAgent, incomeSourceType, isTriggeredMigration)))
                case false => Future.failed(new Exception("Mongo update call was not acknowledged"))
              }
            case Left(ex) =>
              auditingService.extendedAudit(
                CreateIncomeSourceAuditModel(incomeSourceType, viewModel, Some(enums.FailureCategory.ApiFailure), Some(ex.getMessage), None)
              )
              Future.failed(ex)
          }
        case (None, Some(isTriggeredMigration)) =>
          val agentPrefix = if (isAgent) "[Agent]" else ""
          Logger("application").error(agentPrefix +
            s"Unable to construct view model for $incomeSourceType")
          Future.successful {
            Redirect(errorRedirectUrl(isAgent, incomeSourceType, isTriggeredMigration))
          }
        case (Some(_), None) =>
          Logger("application").error("Missing triggered migration session data")
          // TODO: defaulting to false for now. it'll need to be handled by using the downstream triggered migration flag once available
          Future.successful(Redirect(errorRedirectUrl(isAgent, incomeSourceType, false)))
      }
    }
  }.recover {
    case ex: Exception =>
      Logger("application").error(s"${ex.getMessage}")
      // TODO: defaulting to false for now. it'll need to be handled by using the downstream triggered migration flag once available
      Redirect(errorRedirectUrl(isAgent, incomeSourceType, false))
  }
}
