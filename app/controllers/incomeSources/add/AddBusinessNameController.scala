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
import enums.IncomeSourceJourney.{AddJourney, JourneyType, SE, SelfEmployment}
import forms.incomeSources.add.BusinessNameForm
import forms.utils.SessionKeys
import models.incomeSourceDetails.AddIncomeSourceData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.AddBusinessName

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessNameController @Inject()(authenticate: AuthenticationPredicate,
                                          val authorisedFunctions: AuthorisedFunctions,
                                          checkSessionTimeout: SessionTimeoutPredicate,
                                          retrieveNino: NinoPredicate,
                                          val addBusinessView: AddBusinessName,
                                          val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                          val retrieveBtaNavBar: NavBarPredicate,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          incomeSourceDetailsService: IncomeSourceDetailsService,
                                          val sessionService: SessionService)
                                         (implicit val appConfig: FrontendAppConfig,
                                          implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          implicit override val mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils {

  lazy val backUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
  lazy val backUrlAgent: String = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
  lazy val checkDetailsBackUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url
  lazy val checkDetailsBackUrlAgent: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url

  lazy val submitAction: Call = controllers.incomeSources.add.routes.AddBusinessNameController.submit()
  lazy val submitActionAgent: Call = controllers.incomeSources.add.routes.AddBusinessNameController.submitAgent()
  lazy val submitChangeAction: Call = controllers.incomeSources.add.routes.AddBusinessNameController.submitChange()
  lazy val submitChangeActionAgent: Call = controllers.incomeSources.add.routes.AddBusinessNameController.submitChangeAgent()

  lazy val redirect: Call = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)
  lazy val redirectAgent: Call = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)

  lazy val checkDetailsRedirect: Call = controllers.incomeSources.add.routes.CheckBusinessDetailsController.show()
  lazy val checkDetailsRedirectAgent: Call = controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent()


  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
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

  private def createSession(journeyType:JourneyType, isChange: Boolean)(implicit user: MtdItUser[_]): Future[Boolean] = {
    if (isChange) Future { true } else sessionService.createSession(journeyType.toString)

  }

  def handleRequest(isAgent: Boolean, backUrl: String, isChange: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    withIncomeSourcesFS {
//      sessionService.getMongo("add-is-se").map {
//        case Right(dataOpt) =>
//          val businessNameOpt = dataOpt.flatMap(_.addIncomeSourceData.flatMap(_.businessName))
//          val filledForm = businessNameOpt.fold(BusinessNameForm.form)(name => BusinessNameForm.form.fill(BusinessNameForm(name)))
//          val submitAction = getSubmitAction(isAgent: Boolean, isChange: Boolean)
//          Ok(addBusinessView(filledForm, isAgent, submitAction, backUrl, useFallbackLink = true))
//
//        case Left(error) =>
//          Logger("application").error(s"[AddBusinessNameController][handleRequest] $error")
//          errorHandler.showInternalServerError()
//      }

      val journeyType = JourneyType(AddJourney, SE)
      createSession(journeyType, isChange) flatMap(
        result => {
          sessionService.getMongoKey("businessName", journeyType).map {
            case Right(nameOpt) =>
              //          val businessNameOpt = dataOpt.flatMap(_.addIncomeSourceData.flatMap(_.businessName))
              val filledForm = nameOpt.fold(BusinessNameForm.form)(name => BusinessNameForm.form.fill(BusinessNameForm(name)))
              val submitAction = getSubmitAction(isAgent: Boolean, isChange: Boolean)
              Ok(addBusinessView(filledForm, isAgent, submitAction, backUrl, useFallbackLink = true))
            //SessionKeys
            case Left(error) =>
              Logger("application").error(s"[AddBusinessNameController][handleRequest] $error")
              errorHandler.showInternalServerError()
          }
        }
      )

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

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
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

  def submitChange: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
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
    val (backUrlLocal, submitActionLocal, redirectLocal) = (isAgent, isChange) match {
      case (false, false) => (backUrl, submitAction, redirect)
      case (true, false) => (backUrlAgent, submitActionAgent, redirectAgent)
      case (false, true) => (checkDetailsBackUrl, submitChangeAction, checkDetailsRedirect)
      case (true, true) => (checkDetailsBackUrlAgent, submitChangeActionAgent, checkDetailsRedirectAgent)
    }
//    AddIncomeSourceData.businessNameField
    val journeyType = JourneyType(AddJourney, SE)
    sessionService.getMongoKey(AddIncomeSourceData.businessNameField, journeyType).flatMap {
      case Right(businessTradeName) =>
        BusinessNameForm.checkBusinessNameWithTradeName(BusinessNameForm.form.bindFromRequest(), businessTradeName).fold(
          formWithErrors =>
            Future.successful(
              Ok(addBusinessView(formWithErrors,
                isAgent,
                submitActionLocal,
                backUrlLocal,
                useFallbackLink = true))),

          formData => {
            val redirect = Redirect(redirectLocal)
            sessionService.setMongoKey("businessName", formData.name, journeyType).flatMap {
              case Right(result) if result => {
                println(Console.RED + "getting businessNAme field" + Console.WHITE)
                sessionService.getMongoKey("businessName", journeyType).flatMap {
                  case Right(businessName) => {
                  println(Console.RED + "businesstradename  ffs:" + businessName + Console.WHITE)
                    Future.successful(redirect)
                  }
                  case Left(err) => println("fatal err:" + err)
                    Future.failed(err)
                }
              }
              case Right(_) => Future.failed(new Exception("failed.."))
              case Left(exception) => Future.failed(exception)
            }
          }
        )

      case Left(exception) => Future.failed(exception)
    }
  }.recover {
    case exception =>
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"[AddBusinessNameController][handleSubmitRequest] ${exception.getMessage}")
      errorHandler.showInternalServerError()
  }

  def changeBusinessName(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
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
