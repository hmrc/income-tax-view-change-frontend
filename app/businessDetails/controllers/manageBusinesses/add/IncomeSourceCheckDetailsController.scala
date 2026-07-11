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

package businessDetails.controllers.manageBusinesses.add

import businessDetails.auth.AuthActionsWithTriggeredMigrationCheck
import businessDetails.controllers.triggeredMigration.routes as triggeredMigrationRoutes
import businessDetails.enums.FailureCategory.ApiFailure
import businessDetails.models.audit.CreateIncomeSourceAuditModel
import businessDetails.models.createIncomeSource.CreateIncomeSourceResponse
import businessDetails.models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckDetailsViewModel, CheckPropertyViewModel}
import businessDetails.services.{CreateBusinessDetailsService, SessionService}
import businessDetails.utils.JourneyCheckerManageBusinesses
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import businessDetails.views.html.manageBusinesses.add.IncomeSourceCheckDetailsView
import common.auth.MtdItUser
import common.config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import common.enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment}
import common.enums.JourneyType.{Add, IncomeSourceJourneyType}
import common.enums.TriggeredMigration.TriggeredMigrationAdded
import common.models.admin.OverseasBusinessAddress
import common.models.core.NormalMode
import common.models.incomeSourceDetails.IncomeSourceDetailsModel
import common.services.AuditingService
import shared.enums.BeforeSubmissionPage
import shared.models.UIJourneySessionData

import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceCheckDetailsController @Inject()(val incomeSourceCheckDetailsView: IncomeSourceCheckDetailsView,
                                                   val authActions: AuthActionsWithTriggeredMigrationCheck,
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

  def show(incomeSourceType: IncomeSourceType, isTriggeredMigration: Boolean): Action[AnyContent] = authActions.asMTDIndividual(isTriggeredMigration).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        incomeSourceType,
        isTriggeredMigration
      )(implicitly, itvcErrorHandler)
  }

  def showAgent(incomeSourceType: IncomeSourceType, isTriggeredMigration: Boolean): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient(isTriggeredMigration).async  {
    implicit mtdItUser =>
      handleRequest(
        sources = mtdItUser.incomeSources,
        isAgent = true,
        incomeSourceType,
        isTriggeredMigration
      )(implicitly, itvcErrorHandlerAgent)
  }

  private def handleRequest( @unused sources: IncomeSourceDetailsModel,
                            isAgent: Boolean,
                            incomeSourceType: IncomeSourceType,
                            isTriggeredMigration: Boolean)
                           (implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] =
    withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), journeyState = BeforeSubmissionPage) { sessionData =>
      val backUrl: String = routes.AddIncomeSourceStartDateController.show(isAgent, NormalMode, incomeSourceType, isTriggeredMigration).url
      val postAction: Call = if (isAgent) routes.IncomeSourceCheckDetailsController.submitAgent(incomeSourceType, isTriggeredMigration) else {
        routes.IncomeSourceCheckDetailsController.submit(incomeSourceType, isTriggeredMigration)
      }
      getViewModel(incomeSourceType, sessionData)(user) match {
        case Some(viewModel) =>
          Future.successful {
            Ok(
              incomeSourceCheckDetailsView(
                viewModel,
                postAction = postAction,
                isAgent,
                backUrl = backUrl,
                isTriggeredMigration = isTriggeredMigration,
                overseasBusinessAddressEnabled = isEnabled(OverseasBusinessAddress)
              )
            )
          }
        case None =>
          val agentPrefix = if (isAgent) "[Agent]" else ""
          Logger("application").warn(agentPrefix +
            s"Unable to construct view model for $incomeSourceType")
          sessionService.clearSession(sessionData.sessionId).flatMap { _ =>
            journeyRestartUrl(isTriggeredMigration)(user)
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
            incomeSourceType = incomeSourceType,
            idempotencyKey = sessionData.addIncomeSourceData.flatMap(_.idempotencyKey)
          )
        )
      case _ => None
    }
  }

  private def getBusinessModel(sessionData: UIJourneySessionData)(implicit @unused user: MtdItUser[_]): Option[CheckBusinessDetailsViewModel] = {

    sessionData.addIncomeSourceData.flatMap { addIncomeSourceData =>
      for {
        address <- addIncomeSourceData.address
        accountingPeriodEndDate <- addIncomeSourceData.accountingPeriodEndDate
        businessTrade <- addIncomeSourceData.businessTrade
        businessAddressLine1 <- address.lines.headOption
      } yield {
        val isAddingNewAddress = addIncomeSourceData.chooseSoleTraderAddress.exists(_.newAddress)
        val isNoAddressOnFile = addIncomeSourceData.chooseSoleTraderAddress.isEmpty
        CheckBusinessDetailsViewModel(
          businessName = addIncomeSourceData.businessName,
          businessStartDate = addIncomeSourceData.dateStarted,
          accountingPeriodEndDate = accountingPeriodEndDate,
          businessTrade = businessTrade,
          businessAddressLine1 = businessAddressLine1,
          businessAddressLine2 = address.lines.lift(1),
          businessAddressLine3 = address.lines.lift(2),
          businessAddressLine4 = address.lines.lift(3),
          businessPostalCode = address.postcode,
          businessCountryCode = addIncomeSourceData.address.flatMap(_.country.flatMap(_.code)),
          businessCountryName = address.country.flatMap(_.name),
          addressId = addIncomeSourceData.addressLookupId.orElse(addIncomeSourceData.addressId),
          idempotencyKey = addIncomeSourceData.idempotencyKey,
          isAddingNewAddress = isAddingNewAddress,
          isNoAddressOnFile = isNoAddressOnFile
        )
      }
    }
  }


  def submit(incomeSourceType: IncomeSourceType, isTriggeredMigration: Boolean): Action[AnyContent] = authActions.asMTDIndividual(isTriggeredMigration).async {
    implicit user =>
      handleSubmit(isAgent = false, incomeSourceType, isTriggeredMigration)
  }

  def submitAgent(incomeSourceType: IncomeSourceType, isTriggeredMigration: Boolean): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient(isTriggeredMigration).async  {
    implicit mtdItUser =>
      handleSubmit(isAgent = true, incomeSourceType, isTriggeredMigration)
  }

  private def handleSubmit(isAgent: Boolean, incomeSourceType: IncomeSourceType, isTriggeredMigration: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), BeforeSubmissionPage) { sessionData =>
      val redirectUrl: (Boolean, IncomeSourceType, Boolean) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType, isTriggeredMigration: Boolean) => {
        if (isTriggeredMigration) {
          triggeredMigrationRoutes.CheckHmrcRecordsController.show(isAgent, Some(TriggeredMigrationAdded(incomeSourceType).toString)).url
        } else {
          routes.IncomeSourceReportingFrequencyController.show(isAgent, isChange = false, incomeSourceType).url
        }
      }

      val viewModel = getViewModel(incomeSourceType, sessionData)

      viewModel match {
        case Some(viewModel) =>
          businessDetailsService.createRequest(viewModel) flatMap {
            case Right(CreateIncomeSourceResponse(id)) =>

              auditingService.extendedAudit(
                CreateIncomeSourceAuditModel(incomeSourceType, viewModel, None, None, Some(CreateIncomeSourceResponse(id)), isTrigMig = isTriggeredMigration)
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
                CreateIncomeSourceAuditModel(incomeSourceType, viewModel, Some(ApiFailure), Some(ex.getMessage), None, isTrigMig = isTriggeredMigration)
              )
              Future.failed(ex)
          }
        case None =>
          val agentPrefix = if (isAgent) "[Agent]" else ""
          Logger("application").error(agentPrefix +
            s"Unable to construct view model for $incomeSourceType")
          Future.successful {
            Redirect(errorRedirectUrl(isAgent, incomeSourceType, isTriggeredMigration))
          }
      }
    }
  }.recover {
    case ex: Exception =>
      Logger("application").error(s"${ex.getMessage}")
      Redirect(errorRedirectUrl(isAgent, incomeSourceType, !user.incomeSources.isConfirmedUser))
  }
}
