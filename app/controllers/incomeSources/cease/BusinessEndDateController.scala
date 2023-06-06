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

package controllers.incomeSources.cease

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.incomeSources.cease.BusinessEndDateForm
import forms.utils.SessionKeys.{ceaseBusinessEndDate, ceaseUKPropertyEndDate}
import models.incomeSourceDetails.viewmodels.CeaseBusinessDetailsViewModel
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.incomeSources.cease.BusinessEndDate
import views.html.errorPages.CustomNotFoundError

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessEndDateController @Inject()(val authenticate: AuthenticationPredicate,
                                          val authorisedFunctions: FrontendAuthorisedFunctions,
                                          val checkSessionTimeout: SessionTimeoutPredicate,
                                          val businessEndDateForm: BusinessEndDateForm,
                                          val incomeSourceDetailsService: IncomeSourceDetailsService,
                                          val retrieveBtaNavBar: NavBarPredicate,
                                          val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                          val retrieveNino: NinoPredicate,
                                          val businessEndDate: BusinessEndDate,
                                          val customNotFoundErrorView: CustomNotFoundError)
                                         (implicit val appConfig: FrontendAppConfig,
                                          implicit override val mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(businessStartDate: Option[String] = None, origin: Option[String] = None): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          LocalDate.parse(businessStartDate.get),
          origin
        )
    }

  def showAgent(businessStartDate: Option[String]): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              LocalDate.parse(businessStartDate.get)
            )
        }
  }

  def handleRequest(isAgent: Boolean, businessStartDate: LocalDate, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val backUrl: String = if (isAgent) controllers.incomeSources.cease.routes.CeaseUKPropertyController.showAgent().url else
      controllers.incomeSources.cease.routes.CeaseUKPropertyController.show().url
    val postAction: Call = if (isAgent) controllers.incomeSources.cease.routes.BusinessEndDateController.submitAgent(Some(businessStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))) else
      controllers.incomeSources.cease.routes.BusinessEndDateController.submit(Some(businessStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    if (incomeSourcesEnabled) {
      Future.successful(Ok(businessEndDate(
        BusinessEndDateForm = businessEndDateForm.apply(user, businessStartDate = Some(businessStartDate)),
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl,
        origin = origin)(user, messages)))
    } else {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting BusinessEndDate page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  def submit(businessStartDate: Option[String]): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      businessEndDateForm.apply(user, Some(LocalDate.parse(businessStartDate.get))).bindFromRequest().fold(
        hasErrors => Future.successful(BadRequest(businessEndDate(
          BusinessEndDateForm = hasErrors,
          postAction = controllers.incomeSources.cease.routes.BusinessEndDateController.submit(businessStartDate),
          backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url,
          isAgent = false
        ))),
        validatedInput =>
          Future.successful(Redirect(controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.show())
            .addingToSession(ceaseBusinessEndDate -> validatedInput.date.toString))
      )
  }

  def submitAgent(businessStartDate: Option[String]): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            businessEndDateForm.apply(mtdItUser, Some(LocalDate.parse(businessStartDate.get))).bindFromRequest().fold(
              hasErrors => Future.successful(BadRequest(businessEndDate(
                BusinessEndDateForm = hasErrors,
                postAction = controllers.incomeSources.cease.routes.BusinessEndDateController.submitAgent(businessStartDate),
                backUrl = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url,
                isAgent = true
              ))),
              validatedInput =>
                Future.successful(Redirect(controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.showAgent())
                  .addingToSession(ceaseBusinessEndDate -> validatedInput.date.toString))
            )
        }
  }
}
