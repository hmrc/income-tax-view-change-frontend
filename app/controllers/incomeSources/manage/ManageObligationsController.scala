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

import audit.AuditingService
import audit.models.ObligationsAuditModel
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney._
import enums.JourneyType.{JourneyType, Manage}
import forms.utils.SessionKeys
import models.incomeSourceDetails.{ManageIncomeSourceData, PropertyDetailsModel}
import models.incomeSourceDetails.TaxYear.getTaxYearModel
import play.api.Logger
import play.api.mvc._
import services.{IncomeSourceDetailsService, NextUpdatesService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils
import views.html.incomeSources.manage.ManageObligations

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManageObligationsController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                            val authenticate: AuthenticationPredicate,
                                            val authorisedFunctions: AuthorisedFunctions,
                                            val retrieveNino: NinoPredicate,
                                            val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                            implicit val itvcErrorHandler: ItvcErrorHandler,
                                            implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            val incomeSourceDetailsService: IncomeSourceDetailsService,
                                            val retrieveBtaNavBar: NavBarPredicate,
                                            val obligationsView: ManageObligations,
                                            val auditingService: AuditingService,
                                            val sessionService: SessionService,
                                            nextUpdatesService: NextUpdatesService)
                                           (implicit val ec: ExecutionContext,
                                            implicit override val mcc: MessagesControllerComponents,
                                            val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils {


  def showSelfEmployment(changeTo: String, taxYear: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      withIncomeSourcesFS {
        sessionService.getMongoKey(ManageIncomeSourceData.incomeSourceIdField, JourneyType(Manage, SelfEmployment)).flatMap {
          case Right(incomeSourceIdMayBe) =>
            handleRequest(
              mode = SelfEmployment,
              isAgent = false,
              taxYear,
              changeTo,
              incomeSourceIdMayBe
            )
          case Left(exception) => Future.failed(exception)
        }
      }.recover {
        case exception =>
          Logger("application").error(s"[ManageObligationsController][showSelfEmployment] ${exception.getMessage}")
          showInternalServerError(isAgent = false)
      }
  }

  def showAgentSelfEmployment(changeTo: String, taxYear: String): Action[AnyContent] = authenticatedAction(isAgent = true) {
    implicit user =>
      withIncomeSourcesFS {
        sessionService.getMongoKey(ManageIncomeSourceData.incomeSourceIdField, JourneyType(Manage, SelfEmployment)).flatMap {
          case Right(incomeSourceIdMayBe) =>
            handleRequest(
              mode = SelfEmployment,
              isAgent = true,
              taxYear,
              changeTo,
              incomeSourceIdMayBe
            )
          case Left(exception) => Future.failed(exception)
        }
      }.recover {
        case exception =>
          Logger("application").error(s"[ManageObligationsController][showAgentSelfEmployment] ${exception.getMessage}")
          showInternalServerError(isAgent = true)
      }
  }

  def showUKProperty(changeTo: String, taxYear: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        mode = UkProperty,
        isAgent = false,
        taxYear,
        changeTo,
        None
      )
  }

  def showAgentUKProperty(changeTo: String, taxYear: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              mode = UkProperty,
              isAgent = true,
              taxYear,
              changeTo,
              None
            )
        }
  }

  def showForeignProperty(changeTo: String, taxYear: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        mode = ForeignProperty,
        isAgent = false,
        taxYear,
        changeTo,
        None
      )
  }

  def showAgentForeignProperty(changeTo: String, taxYear: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              mode = ForeignProperty,
              isAgent = true,
              taxYear,
              changeTo,
              None
            )
        }
  }

  def handleRequest(mode: IncomeSourceType, isAgent: Boolean, taxYear: String, changeTo: String, incomeSourceId: Option[String])(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    if (isDisabled(IncomeSources)) {
      if (isAgent) Future.successful(Redirect(controllers.routes.HomeController.showAgent))
      else Future.successful(Redirect(controllers.routes.HomeController.show()))
    }
    else {
      val backUrl: String = getBackurl(isAgent, mode, changeTo, taxYear)
      val postUrl: Call = if (isAgent) controllers.incomeSources.manage.routes.ManageObligationsController.agentSubmit() else controllers.incomeSources.manage.routes.ManageObligationsController.submit()

      val addedBusinessName: String = getBusinessName(mode, incomeSourceId)

      getTaxYearModel(taxYear) match {
        case Some(years) =>
          if (changeTo == "annual" || changeTo == "quarterly") {
            getIncomeSourceId(mode, incomeSourceId, isAgent = isAgent) match {
              case Left(error) => showError(isAgent, {
                error.getMessage
              })
              case Right(value) =>
                nextUpdatesService.getObligationsViewModel(value, showPreviousTaxYears = false) map { viewModel =>
                  auditingService.extendedAudit(ObligationsAuditModel(
                    incomeSourceType = mode,
                    obligations = viewModel,
                    businessName = addedBusinessName,
                    changeTo,
                    years
                  ))
                  if (isAgent) Ok(obligationsView(viewModel, addedBusinessName, years, changeTo, isAgent, backUrl, postUrl))
                  else Ok(obligationsView(viewModel, addedBusinessName, years, changeTo, isAgent, backUrl, postUrl))
                }
            }
          }
          else {
            showError(isAgent, "invalid changeTo mode provided")
          }
        case None => showError(isAgent, "invalid tax year provided")
      }
    }
  }

  def getBackurl(isAgent: Boolean, incomeSourceType: IncomeSourceType, changeTo: String, taxYear: String): String = {
    routes.ConfirmReportingMethodSharedController.show(
      taxYear = taxYear,
      changeTo = changeTo,
      isAgent = isAgent,
      incomeSourceType = incomeSourceType
    ).url
  }

  def showError(isAgent: Boolean, message: String)(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    Logger("application").error(
      s"${if (isAgent) "[Agent]"}[ManageObligationsController][handleRequest] - $message")
    if (isAgent) Future.successful(itvcErrorHandlerAgent.showInternalServerError())
    else Future.successful(itvcErrorHandler.showInternalServerError())
  }

  def getBusinessName(mode: IncomeSourceType, incomeSourceId: Option[String])(implicit user: MtdItUser[_]): String = {
    (mode, incomeSourceId) match {
      case (SelfEmployment, Some(incomeSourceId)) =>
        val businessDetailsParams = for {
          addedBusiness <- user.incomeSources.businesses.find(x => x.incomeSourceId.contains(incomeSourceId))
          businessName <- addedBusiness.tradingName
        } yield (addedBusiness, businessName)
        businessDetailsParams match {
          case Some((_, name)) => name
          case None => "Not Found"
        }
      case (UkProperty, _) => "UK property"
      case (ForeignProperty, _) => "Foreign property"
      case _ => "Not Found"
    }
  }

  def getIncomeSourceId(incomeSourceType: IncomeSourceType, id: Option[String], isAgent: Boolean)(implicit user: MtdItUser[_]): Either[Throwable, String] = {
    (incomeSourceType, id) match {
      case (SelfEmployment, Some(id)) => Right(id)
      case _ =>
        val placeholder = if (incomeSourceType == UkProperty)
          incomeSourceDetailsService.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(isUkProperty = true)
        else
          incomeSourceDetailsService.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(isUkProperty = false)

        placeholder match {
          case Right(property: PropertyDetailsModel) => Right(property.incomeSourceId)
          case Left(error: Error) => Left(error)
          case _ => Left(new Error(s"Unknown error. IncomeSourceType: $incomeSourceType"))
        }
    }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      Future.successful(Redirect(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(false)))
  }

  def agentSubmit: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            Future.successful(Redirect(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(true)))
        }
  }

  private def authenticatedAction(isAgent: Boolean
                                 )(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent)
      Authenticated.async {
        implicit request =>
          implicit user =>
            getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
              authenticatedCodeBlock(mtdItUser)
            }
      }
    else
      (checkSessionTimeout andThen authenticate andThen retrieveNino
        andThen retrieveIncomeSources andThen retrieveBtaNavBar).async { implicit user =>
        authenticatedCodeBlock(user)
      }
  }
}