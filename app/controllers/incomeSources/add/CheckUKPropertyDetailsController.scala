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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import implicits.ImplicitDateFormatter
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.viewmodels.CheckUKPropertyViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CreateBusinessDetailsService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import uk.gov.hmrc.play.language.LanguageUtils
import utils.IncomeSourcesUtils
import utils.IncomeSourcesUtils.getUKPropertyDetailsFromSession
import views.html.incomeSources.add.CheckUKPropertyDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckUKPropertyDetailsController @Inject()(val checkUKPropertyDetails: CheckUKPropertyDetails,
                                                 val checkSessionTimeout: SessionTimeoutPredicate,
                                                 val authenticate: AuthenticationPredicate,
                                                 val authorisedFunctions: AuthorisedFunctions,
                                                 val retrieveNino: NinoPredicate,
                                                 val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                 val businessDetailsService: CreateBusinessDetailsService,
                                                 val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                 val createBusinessDetailsService: CreateBusinessDetailsService,
                                                 val retrieveBtaNavBar: NavBarPredicate)
                                                (implicit val appConfig: FrontendAppConfig,
                                                 mcc: MessagesControllerComponents,
                                                 val ec: ExecutionContext,
                                                 val itvcErrorHandler: ItvcErrorHandler,
                                                 val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                 val languageUtils: LanguageUtils)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with ImplicitDateFormatter with IncomeSourcesUtils {

  def getBackUrl(isAgent: Boolean): String = {
    if (isAgent) controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.showAgent().url else
      controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.show().url
  }

  def getSubmitUrl(isAgent: Boolean): Call = {
    if (isAgent) controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.submitAgent() else
      controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.submit()
  }

  def getUKPropertyReportingMethodUrl(isAgent: Boolean, id: String): Call = {
    if (isAgent) controllers.incomeSources.add.routes.UKPropertyReportingMethodController.showAgent(id) else
      controllers.incomeSources.add.routes.UKPropertyReportingMethodController.show(id)
  }

  def getErrorHandler(isAgent: Boolean): FrontendErrorHandler with ShowInternalServerError = {
    if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
  }

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
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
      getUKPropertyDetailsFromSession(user).toOption match {
        case Some(checkUKPropertyViewModel: CheckUKPropertyViewModel) =>
          Future.successful(Ok(
            checkUKPropertyDetails(viewModel = checkUKPropertyViewModel,
              isAgent = isAgent,
              backUrl = backUrl,
              postAction = postAction)))
        case None => Logger("application").error(
          s"[CheckUKPropertyDetailsController][handleRequest] - Error: Unable to build UK property details")
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
      getUKPropertyDetailsFromSession(user) match {
        case Right(checkUKPropertyViewModel: CheckUKPropertyViewModel) =>
          businessDetailsService.createUKProperty(checkUKPropertyViewModel).map {
            case Left(ex) => Logger("application").error(
              s"[CheckUKPropertyDetailsController][handleRequest] - Unable to create income source: ${ex.getMessage}")
              withIncomeSourcesRemovedFromSession {
                if (isAgent) Redirect(controllers.incomeSources.add.routes.UKPropertyNotAddedController.showAgent())
                else Redirect(controllers.incomeSources.add.routes.UKPropertyNotAddedController.show())
              }
            case Right(CreateIncomeSourceResponse(id)) =>
              val redirectUrl = getUKPropertyReportingMethodUrl(isAgent, id)
              withIncomeSourcesRemovedFromSession {
                Redirect(redirectUrl)
              }
          }.recover {
            case ex: Throwable =>
              Logger("application").error(
                s"[CheckUKPropertyDetailsController][handleRequest] - Error while processing request: ${ex.getMessage}")
              withIncomeSourcesRemovedFromSession {
                if (isAgent) Redirect(controllers.incomeSources.add.routes.UKPropertyNotAddedController.showAgent())
                else Redirect(controllers.incomeSources.add.routes.UKPropertyNotAddedController.show())
              }
          }
        case Left(ex: Throwable) =>
          Logger("application").error(
            s"[CheckUKPropertyDetailsController][handleSubmit] - Error: Unable to build UK property details on submit ${ex.getMessage}")
          Future.successful {
            withIncomeSourcesRemovedFromSession {
              errorHandler.showInternalServerError()
            }
          }
      }
    }
  }
}