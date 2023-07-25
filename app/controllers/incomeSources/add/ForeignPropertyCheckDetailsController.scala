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
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.utils.SessionKeys._
import implicits.ImplicitDateFormatter
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.incomeSourceDetails.viewmodels.CheckForeignPropertyViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CreateBusinessDetailsService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import uk.gov.hmrc.play.language.LanguageUtils
import utils.IncomeSourcesUtils
import utils.IncomeSourcesUtils.getForeignPropertyDetailsFromSession
import views.html.incomeSources.add.ForeignPropertyCheckDetails

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ForeignPropertyCheckDetailsController @Inject()(val checkForeignPropertyDetails: ForeignPropertyCheckDetails,
                                                      val checkSessionTimeout: SessionTimeoutPredicate,
                                                      val authenticate: AuthenticationPredicate,
                                                      val authorisedFunctions: AuthorisedFunctions,
                                                      val retrieveNino: NinoPredicate,
                                                      val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                      val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                      val retrieveBtaNavBar: NavBarPredicate,
                                                      val businessDetailsService: CreateBusinessDetailsService)
                                                     (implicit val ec: ExecutionContext,
                                                      implicit override val mcc: MessagesControllerComponents,
                                                      val appConfig: FrontendAppConfig,
                                                      implicit val itvcErrorHandler: ItvcErrorHandler,
                                                      val languageUtils: LanguageUtils,
                                                      implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with ImplicitDateFormatter with IncomeSourcesUtils {

  def getBackUrl(isAgent: Boolean): String = {
    if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.showAgent().url else
      controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.show().url
  }

  def getSubmitUrl(isAgent: Boolean): Call = {
    if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.submitAgent() else
      controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.submit()
  }

  def getUKPropertyReportingMethodUrl(isAgent: Boolean, id: String): Call = {
    if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.showAgent(id) else
      controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.show(id)
  }

  def getForeignPropertyReportingMethodUrl(isAgent: Boolean, id: String): Call = {
    if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.showAgent(id) else
      controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.show(id)
  }

  def getErrorHandler(isAgent: Boolean): FrontendErrorHandler with ShowInternalServerError = {
    if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
  }

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      println(s"\nCHECK PAGE SHOW: ALL user properties ON PAGE LOAD: ${user.incomeSources.properties}\n")

      println(s"\nCHECK PAGE SHOW: FOREIGN PROPERTIES ON PAGE LOAD: ${user.incomeSources.properties.find(_.isForeignProperty)}\n")

      handleRequest(
        isAgent = false
      )
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true
            )
        }
  }

  def handleRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val backUrl = getBackUrl(isAgent)
    val postAction = getSubmitUrl(isAgent)
    val errorHandler = getErrorHandler(isAgent)

    withIncomeSourcesFS {
      getForeignPropertyDetailsFromSession(user).toOption match {
        case Some(checkForeignPropertyViewModel: CheckForeignPropertyViewModel) =>
          Future.successful(Ok(
            checkForeignPropertyDetails(viewModel = checkForeignPropertyViewModel,
              isAgent = isAgent,
              backUrl = backUrl,
              postAction = postAction)))
        case None => Logger("application").error(
          s"[CheckForeignPropertyDetailsController][handleRequest] - Error: Unable to build Foreign property details")
          Future.successful(errorHandler.showInternalServerError())
      }
    }
  }

  def submit(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmit(isAgent = false)
  }

  def submitAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmit(isAgent = true)
        }
  }

  def handleSubmit(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val errorHandler = getErrorHandler(isAgent)

    withIncomeSourcesFS {
      getForeignPropertyDetailsFromSession(user) match {
        case Right(checkForeignPropertyViewModel: CheckForeignPropertyViewModel) =>
          businessDetailsService.createForeignProperty(checkForeignPropertyViewModel).map {
            case Left(ex) => Logger("application").error(
              s"[CheckForeignPropertyDetailsController][handleRequest] - Unable to create income source: ${ex.getMessage}")
              withIncomeSourcesRemovedFromSession {
                if (isAgent) Redirect(controllers.incomeSources.add.routes.ForeignPropertyBusinessNotAddedErrorController.showAgent())
                else Redirect(controllers.incomeSources.add.routes.ForeignPropertyBusinessNotAddedErrorController.show())
              }
            case Right(CreateIncomeSourceResponse(id)) =>
              val redirectUrl = getForeignPropertyReportingMethodUrl(isAgent, id)
              withIncomeSourcesRemovedFromSession {
                Redirect(redirectUrl)
              }
          }.recover {
            case ex: Throwable =>
              Logger("application").error(
                s"[CheckForeignPropertyDetailsController][handleRequest] - Error while processing request: ${ex.getMessage}")
              withIncomeSourcesRemovedFromSession {
                if (isAgent) Redirect(controllers.incomeSources.add.routes.ForeignPropertyBusinessNotAddedErrorController.showAgent())
                else Redirect(controllers.incomeSources.add.routes.ForeignPropertyBusinessNotAddedErrorController.show())
              }
          }
        case Left(ex: Throwable) =>
          Logger("application").error(
            s"[CheckForeignPropertyDetailsController][handleSubmit] - Error: Unable to build Foreign property details on submit ${ex.getMessage}")
          Future.successful {
            withIncomeSourcesRemovedFromSession {
              errorHandler.showInternalServerError()
            }
          }
      }
    }
  }

//  lazy val foreignPropertyAccountingMethodUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.show().url
//  lazy val agentForeignPropertyAccountingMethodUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.showAgent().url
//  lazy val backUrlIndividual: String = foreignPropertyAccountingMethodUrl
//  lazy val backUrlAgent: String = agentForeignPropertyAccountingMethodUrl
//
//  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean)
//                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
//
//
//
//    val backUrl: String = if (isAgent) backUrlAgent else backUrlIndividual
//    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
//    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.submitAgent() else {
//      controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.submit()
//    }
//
//    if (isDisabled(IncomeSources)) {
//      if (isAgent) Future.successful(Redirect(controllers.routes.HomeController.showAgent)) else Future.successful(Redirect(controllers.routes.HomeController.show()))
//    } else {
//        getDetails(user) map {
//          case Right(viewModel) =>
//            Ok(checkForeignPropertyDetails(
//              viewModel,
//              postAction = postAction,
//              isAgent,
//              backUrl = backUrl
//            ))
//          case Left(ex) =>
//            if (isAgent) {
//              Logger("application").error(
//                s"[Agent][ForeignPropertyCheckDetailsController][handleRequest] - Error: ${ex.getMessage}")
//              errorHandler.showInternalServerError()
//            } else {
//              Logger("application").error(
//                s"[ForeignPropertyCheckDetailsController][handleRequest] - Error: ${ex.getMessage}")
//              errorHandler.showInternalServerError()
//            }
//      } recover {
//        case ex: Exception =>
//          if (isAgent) {
//            Logger("application").error(
//              s"[Agent][ForeignPropertyCheckDetailsController][handleRequest] - Error: Unable to construct Future ${ex.getMessage}")
//            errorHandler.showInternalServerError()
//          } else {
//            Logger("application").error(
//              s"[ForeignPropertyCheckDetailsController][handleRequest] - Error: Unable to construct Future ${ex.getMessage}")
//            errorHandler.showInternalServerError()
//          }
//      }
//    }
//  }
//
//  def getDetails(implicit user: MtdItUser[_]): Future[Either[Throwable, CheckForeignPropertyViewModel]] = {
//
//    case class MissingKey(msg: String)
//
//    val errors: Seq[String] = Seq(
//      user.session.data.get(foreignPropertyStartDate).orElse(Option(MissingKey("MissingKey: addForeignPropertyStartDate"))),
//      user.session.data.get(addForeignPropertyAccountingMethod).orElse(Option(MissingKey("MissingKey: addForeignPropertyAccountingMethod")))
//    ).collect {
//      case Some(MissingKey(msg)) => MissingKey(msg)
//    }.map(e => e.msg)
//
//    val result: Option[CheckForeignPropertyViewModel] = for {
//      foreignPropertyStartDate <- user.session.data.get(foreignPropertyStartDate).map(LocalDate.parse)
//      cashOrAccrualsFlag <- user.session.data.get(addForeignPropertyAccountingMethod)
//    } yield {
//      CheckForeignPropertyViewModel(
//        tradingStartDate = foreignPropertyStartDate,
//        cashOrAccrualsFlag = cashOrAccrualsFlag)
//    }
//    Future.successful(
//      result match {
//        case Some(checkForeignPropertyViewModel) =>
//          Right(checkForeignPropertyViewModel)
//        case None =>
//          Left(new IllegalArgumentException(s"Missing required session data: ${errors.mkString(" ")}"))
//      }
//    )
//  }
//
//  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
//    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
//    implicit user =>
//      println(s"\nCHECK PAGE SHOW: ALL user properties ON PAGE LOAD: ${user.incomeSources.properties}\n")
//
//      println(s"\nCHECK PAGE SHOW: UK PROPERTIES ON PAGE LOAD: ${user.incomeSources.properties.find(_.isUkProperty)}\n")
//
//      handleRequest(
//        sources = user.incomeSources,
//        isAgent = false
//      )
//  }
//
//  def showAgent(): Action[AnyContent] = Authenticated.async {
//    implicit request =>
//      implicit user =>
//        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
//          implicit mtdItUser =>
//            handleRequest(
//              sources = mtdItUser.incomeSources,
//              isAgent = true
//            )
//        }
//  }
//
//  def submit(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
//    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
//    implicit user =>
//      handleSubmit(isAgent = false)
//  }
//
//  def submitAgent: Action[AnyContent] = Authenticated.async {
//    implicit request =>
//      implicit user =>
//        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
//          implicit mtdItUser =>
//            handleSubmit(isAgent = true)
//        }
//  }
//
//  private val sessionKeys = Seq(foreignPropertyStartDate, addForeignPropertyAccountingMethod)
//
//  private lazy val errorUrl: String = controllers.incomeSources.add.routes.ForeignPropertyBusinessNotAddedErrorController.show().url
//  private lazy val agentErrorUrl: String = controllers.incomeSources.add.routes.ForeignPropertyBusinessNotAddedErrorController.showAgent().url
//
//  def handleSubmit(isAgent: Boolean)(implicit user: MtdItUser[AnyContent], request: Request[AnyContent]): Future[Result] = {
//    getDetails(user) flatMap {
//      case Right(viewModel: CheckForeignPropertyViewModel) =>
//        businessDetailsService.createForeignProperty(viewModel).map {
//          case Left(ex) => if (isAgent) {
//            Logger("application").error(
//              s"[CheckBusinessDetailsController][handleRequest] - Unable to create income source: ${ex.getMessage}")
//            Redirect(agentErrorUrl)
//          }
//          else
//          {
//            Logger("application").error(
//              s"[CheckBusinessDetailsController][handleRequest] - Unable to create income source: ${ex.getMessage}")
//            Redirect(errorUrl)
//          }
//
//          case Right(CreateIncomeSourceResponse(id)) =>
//            if (isAgent) Redirect(controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.showAgent(id).url).withSession(user.session -- sessionKeys)
//            else Redirect(controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.show(id).url).withSession(user.session -- sessionKeys)
//        }
//      case Left(_) => if(isAgent){
//        Logger("application").error(
//          s"[CheckBusinessDetailsController][submit] - Error: Unable to build view model on submit")
//        Future.successful(Redirect(agentErrorUrl))
//      } else {
//        Logger("application").error(
//          s"[CheckBusinessDetailsController][submit] - Error: Unable to build view model on submit")
//        Future.successful(Redirect(errorUrl))
//      }
//    }
//  }
}
