/*
 * Copyright 2024 HM Revenue & Customs
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

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import forms.manageBusinesses.add.{AddProprertyForm => form}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.{AuthenticatorPredicate, IncomeSourcesUtils}
import views.html.manageBusinesses.add.AddProperty

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class AddPropertyController @Inject()(auth: AuthenticatorPredicate,
                                      val addProperty: AddProperty,
                                      val authorisedFunctions: FrontendAuthorisedFunctions)
                                     (implicit val appConfig: FrontendAppConfig,
                                      mcc: MessagesControllerComponents,
                                      val ec: ExecutionContext,
                                      val itvcErrorHandler: ItvcErrorHandler,
                                      val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with I18nSupport with IncomeSourcesUtils{

  private def getBackUrl(isAgent: Boolean): String = if(isAgent) {
    controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
  } else {
    controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
  }

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      handleRequest(isAgent)
  }

  def handleRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {
    val postAction = controllers.manageBusinesses.add.routes.AddPropertyController.submit(isAgent)
    Future.successful(Ok(addProperty(form.apply, isAgent, Some(getBackUrl(isAgent)), postAction)))
  }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) { implicit user =>
    handleSubmitRequest(
      isAgent = isAgent
    )
  }

  private def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {
    val postAction = controllers.manageBusinesses.add.routes.AddPropertyController.submit(isAgent)
    form.apply.bindFromRequest().fold(
      formWithErrors =>
        Future.successful {
          BadRequest(
            addProperty(
              isAgent = isAgent,
              form = formWithErrors,
              backUrl = Some(getBackUrl(isAgent)),
              postAction = postAction
            )
          )
        },
      formData =>
        handleValidForm(formData, isAgent)
    )
  }

  private def handleValidForm(validForm: form,
                              isAgent: Boolean)
                             (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    val formResponse: Option[String] = validForm.toFormMap(form.response).headOption
    val ukPropertyUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(isAgent, isChange = false, UkProperty).url
    val foreignPropertyUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(isAgent, isChange = false, ForeignProperty).url

    formResponse match {
      case Some(form.responseUK) => Future.successful(Redirect(ukPropertyUrl))
      case Some(form.responseForeign) => Future.successful(Redirect(foreignPropertyUrl))
      case _ =>
        Logger("application").error(s"Unexpected response, isAgent = $isAgent")
        Future.successful(showInternalServerError(isAgent))
    }
  }
}
