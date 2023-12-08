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

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.BusinessNameForm
import models.incomeSourceDetails.{AddBusinessNameResponse, AddBusinessTradeResponse, AddIncomeSourceData}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.add.AddBusinessName

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessNameController @Inject()(authenticate: AuthenticationPredicate,
                                          val authorisedFunctions: AuthorisedFunctions,
                                          checkSessionTimeout: SessionTimeoutPredicate,
                                          val addBusinessView: AddBusinessName,
                                          val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                          val retrieveBtaNavBar: NavBarPredicate,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          incomeSourceDetailsService: IncomeSourceDetailsService,
                                          val sessionService: SessionService)
                                         (implicit val appConfig: FrontendAppConfig,
                                          implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          implicit override val mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils with JourneyChecker {

  lazy val backUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
  lazy val backUrlAgent: String = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
  lazy val checkDetailsBackUrl: String = controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment).url
  lazy val checkDetailsBackUrlAgent: String = controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment).url

  lazy val submitAction: Call = controllers.incomeSources.add.routes.AddBusinessNameController.submit()
  lazy val submitActionAgent: Call = controllers.incomeSources.add.routes.AddBusinessNameController.submitAgent()
  lazy val submitChangeAction: Call = controllers.incomeSources.add.routes.AddBusinessNameController.submitChange()
  lazy val submitChangeActionAgent: Call = controllers.incomeSources.add.routes.AddBusinessNameController.submitChangeAgent()

  lazy val redirect: Call = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)
  lazy val redirectAgent: Call = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)

  lazy val checkDetailsRedirect: Call = controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
  lazy val checkDetailsRedirectAgent: Call = controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)

  private lazy val journeyType: JourneyType = JourneyType(Add, SelfEmployment)

  private def getBusinessName(isChange: Boolean)
                             (implicit user: MtdItUser[_]): Future[Option[String]] = {
    if (isChange)
      sessionService.getMongoKeyTyped[AddBusinessNameResponse]().flatMap {
        case Right(Some(AddBusinessNameResponse(name))) =>
          Future.successful(Some(name))
        case Right(_) =>
          Future.failed(new Exception("Unable to extract AddBusinessTrade:"))
        case Left(ex) =>
          Future.failed(new Exception(s"Unable to extract AddBusinessTrade: ${ex}"))
      }
    else
      sessionService.createSession(journeyType.toString).flatMap {
        case true => Future.successful(None)
        case false => Future.failed(new Exception("Unable to create session 1"))
      }
  }

  private def getBusinessTrade(implicit user: MtdItUser[_]): Future[Option[String]] = {
    sessionService.getMongoKeyTyped[AddBusinessTradeResponse]().flatMap {
      case Right(Some(AddBusinessTradeResponse(name))) =>
        Future.successful(Some(name))
      case Left(ex) =>
        Future.failed(new Exception(s"Unable to create session 3: ${ex}"))
      case Right(Some(_)) | Right(None) =>
        Future.successful(None)
    }
  }


  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        isAgent = false,
        backUrl = backUrl,
        isChange = false
      )
  }

  def showAgent(): Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
            implicit mtdItUser =>
              handleRequest(
                isAgent = true,
                backUrl = backUrlAgent,
                isChange = false
              )
          }
    }

  def handleRequest(isAgent: Boolean, backUrl: String, isChange: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFSWithSessionCheck(JourneyType(Add, SelfEmployment)) {
      getBusinessName(isChange).flatMap {
        nameOpt =>
          val filledForm = nameOpt.fold(BusinessNameForm.form)(name =>
            BusinessNameForm.form.fill(BusinessNameForm(name)))
          val submitAction = getSubmitAction(isAgent, isChange)

          Future.successful {
            Ok(addBusinessView(filledForm, isAgent, submitAction, backUrl, useFallbackLink = true))
          }
      }.recover {
        case ex =>
          val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
          Logger("application").error(s"[AddBusinessNameController][handleRequest] ${ex.getMessage}")
          errorHandler.showInternalServerError()
      }
    }
  }

  private def getSubmitAction(isAgent: Boolean, isChange: Boolean) = {
    (isAgent, isChange) match {
      case (false, false) => submitAction
      case (true, false) => submitActionAgent
      case (false, true) => submitChangeAction
      case (true, true) => submitChangeActionAgent
    }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(isAgent = false, isChange = false)
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true, isChange = false)
        }
  }

  def submitChange: Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(isAgent = false, isChange = true)
  }

  def submitChangeAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true, isChange = true)
        }
  }

  def handleSubmitRequest(isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFS {
      val (backUrlLocal, submitActionLocal, redirectLocal) = (isAgent, isChange) match {
        case (false, false) => (backUrl, submitAction, redirect)
        case (true, false) => (backUrlAgent, submitActionAgent, redirectAgent)
        case (false, true) => (checkDetailsBackUrl, submitChangeAction, checkDetailsRedirect)
        case (true, true) => (checkDetailsBackUrlAgent, submitChangeActionAgent, checkDetailsRedirectAgent)
      }
      getBusinessTrade.flatMap {
        businessTradeOpt =>
          BusinessNameForm.checkBusinessNameWithTradeName(BusinessNameForm.form.bindFromRequest(), businessTradeOpt).fold(
            formWithErrors =>
              Future.successful {
                BadRequest(addBusinessView(formWithErrors,
                  isAgent,
                  submitActionLocal,
                  backUrlLocal,
                  useFallbackLink = true))
              },
            formData => {
              val redirect = Redirect(redirectLocal)
              sessionService.setMongoKey(AddIncomeSourceData.businessNameField, formData.name, journeyType).flatMap {
                case Right(result) if result => Future.successful(redirect)
                case Right(_) => Future.failed(new Exception("Mongo update call was not acknowledged"))
                case Left(exception) => Future.failed(exception)
              }
            }
          )
      }
    }
  }.recover {
    case exception =>
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"[AddBusinessNameController][handleSubmitRequest] ${exception.getMessage} - ${exception.getCause}")
      errorHandler.showInternalServerError()
  }

  def changeBusinessName(): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        isAgent = false,
        backUrl = checkDetailsBackUrl,
        isChange = true
      )
  }

  def changeBusinessNameAgent(): Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
            implicit mtdItUser =>
              handleRequest(
                isAgent = true,
                backUrl = checkDetailsBackUrlAgent,
                isChange = true
              )
          }
    }
}
