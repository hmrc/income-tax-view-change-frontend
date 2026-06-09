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

package businessDetails.controllers.manageBusinesses.add

import businessDetails.controllers.manageBusinesses.routes as manageBusinessesRoutes
import businessDetails.forms.manageBusinesses.add.AddProprertyForm as form
import businessDetails.utils.IncomeSourcesUtils
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import businessDetails.views.html.manageBusinesses.add.AddPropertyView
import common.auth.{AuthActions, MtdItUser}
import common.config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import common.enums.IncomeSourceJourney.{ForeignProperty, UkProperty}
import common.models.core.NormalMode

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class AddPropertyController @Inject()(authActions: AuthActions,
                                      val addProperty: AddPropertyView,
                                      val itvcErrorHandler: ItvcErrorHandler,
                                      val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                     (implicit val appConfig: FrontendAppConfig,
                                      mcc: MessagesControllerComponents,
                                      val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with IncomeSourcesUtils {

  private def getBackUrl(isAgent: Boolean): String = if(isAgent) {
    manageBusinessesRoutes.ManageYourBusinessesController.showAgent().url
  } else {
    manageBusinessesRoutes.ManageYourBusinessesController.show().url
  }

  def show(isAgent: Boolean, isTrigMig: Boolean = false): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent, isTrigMig).async {
    implicit user => handleRequest(isAgent, isTrigMig)
  }

  def handleRequest(isAgent: Boolean, isTrigMig: Boolean = false)(implicit user: MtdItUser[_]): Future[Result] = {
    val postAction = routes.AddPropertyController.submit(isAgent, isTrigMig)
    Future.successful(Ok(addProperty(form.apply, isAgent, Some(getBackUrl(isAgent)), postAction)))
  }

  def submit(isAgent: Boolean, isTrigMig: Boolean = false): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent, isTrigMig).async { implicit user =>
    handleSubmitRequest(isAgent = isAgent, isTrigMig)
  }

  private def handleSubmitRequest(isAgent: Boolean, isTrigMig: Boolean = false)(implicit user: MtdItUser[_]): Future[Result] = {
    val postAction = routes.AddPropertyController.submit(isAgent, isTrigMig)
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
        handleValidForm(formData, isAgent, isTrigMig)
    )
  }

  private def handleValidForm(validForm: form,
                              isAgent: Boolean,
                              isTrigMig: Boolean = false)
                             (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    val formResponse: Option[String] = validForm.toFormMap(form.response).headOption
    val ukPropertyUrl: String = routes.AddIncomeSourceStartDateController.show(isAgent, mode = NormalMode, UkProperty, isTriggeredMigration = isTrigMig).url
    val foreignPropertyUrl: String = routes.AddIncomeSourceStartDateController.show(isAgent, mode = NormalMode, ForeignProperty, isTriggeredMigration = isTrigMig).url

    formResponse match {
      case Some(form.responseUK) => Future.successful(Redirect(ukPropertyUrl))
      case Some(form.responseForeign) => Future.successful(Redirect(foreignPropertyUrl))
      case _ =>
        Logger("application").error(s"Unexpected response, isAgent = $isAgent")
        val errorHandler = if(isAgent) itvcErrorHandlerAgent else itvcErrorHandler
        Future.successful(errorHandler.showInternalServerError())
    }
  }
}
