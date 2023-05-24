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

package controllers

import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.BusinessNameForm
import forms.incomeSources.add.BusinessStartDateForm
import forms.utils.SessionKeys
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.AddBusiness
import services.IncomeSourceDetailsService
import views.html.incomeSources.add.AddBusinessStartDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessStartDateController @Inject()(authenticate: AuthenticationPredicate,
                                               val authorisedFunctions: AuthorisedFunctions,
                                               checkSessionTimeout: SessionTimeoutPredicate,
                                               businessStartDateForm: BusinessStartDateForm,
                                               retrieveNino: NinoPredicate,
                                               val addBusinessStartDate: AddBusinessStartDate,
                                               val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                               val retrieveBtaNavBar: NavBarPredicate,
                                               val itvcErrorHandler: ItvcErrorHandler,
                                               incomeSourceDetailsService: IncomeSourceDetailsService)
                                              (implicit val appConfig: FrontendAppConfig,
                                               implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                               implicit override val mcc: MessagesControllerComponents,
                                               val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  lazy val backUrl: String = routes.AddBusinessStartDateCheckController.show().url
  lazy val backUrlAgent: String = routes.AddBusinessStartDateCheckController.showAgent().url
  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        isAgent = false,
        backUrl = backUrl
      )
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              backUrl = backUrlAgent
            )
        }
  }

  def handleRequest(isAgent: Boolean, backUrl: String)
                   (implicit user: MtdItUser[_], ec: ExecutionContext, messages: Messages): Future[Result] = {

    val postAction = {
      if(isAgent) routes.AddBusinessStartDateController.submitAgent()
      else routes.AddBusinessStartDateController.submit()
    }

    val errorHandler: ShowInternalServerError = {
      if(isAgent) itvcErrorHandlerAgent
      else itvcErrorHandler
    }

      if (isDisabled(IncomeSources)) {
        Future.successful(
          Redirect(controllers.routes.HomeController.show())
        )
      } else {
        Future.successful(
          Ok(addBusinessStartDate(
            form = businessStartDateForm.apply(user, messages),
            postAction = postAction,
            backUrl = backUrl,
            isAgent = isAgent
          )(user, messages))
        )
      } recover {
        case ex: Exception =>
          Logger("application").error(s"${if (isAgent) "[Agent]"}" +
            s"[AddBusinessStartDateController][handleRequest] - Error: ${ex.getMessage}")
          errorHandler.showInternalServerError()
      }

  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      businessStartDateForm.apply.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(
            BadRequest(addBusinessStartDate(
              form = formWithErrors,
              postAction = routes.AddBusinessStartDateController.submit(),
              backUrl = backUrl,
              isAgent = false
            ))
          ),
        formData =>
          Future.successful(
            Redirect(routes.AddBusinessStartDateCheckController.show())
              .addingToSession(SessionKeys.businessStartDate -> formData.date.toString)
          )
      )
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            businessStartDateForm.apply.bindFromRequest().fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(addBusinessStartDate(
                    form = formWithErrors,
                    postAction = routes.AddBusinessStartDateController.submitAgent(),
                    backUrl = backUrlAgent,
                    isAgent = true
                  ))
                ),
              formData =>
                Future.successful(
                  Redirect(routes.AddBusinessStartDateCheckController.showAgent())
                    .addingToSession(SessionKeys.businessStartDate -> formData.date.toString))
            )
        }
  }
}
