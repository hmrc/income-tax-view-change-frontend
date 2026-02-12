/*
 * Copyright 2026 HM Revenue & Customs
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

import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, ItvcErrorHandler, ShowInternalServerError}
import config.featureswitch.FeatureSwitching
import forms.manageBusinesses.add.ChooseSoleTraderAddressForm
import jakarta.inject.Singleton
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.IncomeSourcesUtils
import views.html.manageBusinesses.add.ChooseSoleTraderAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import config.FrontendAppConfig
import models.admin.OverseasBusinessAddress

@Singleton
class ChooseSoleTraderAddressController @Inject()(
                                                    val itvcErrorHandler: ItvcErrorHandler,
                                                    val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                    view: ChooseSoleTraderAddressView,
                                                    authActions: AuthActions
                                                 )(implicit val appConfig: FrontendAppConfig,
                                                   val mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext) extends FrontendController(mcc) with FeatureSwitching with IncomeSourcesUtils with I18nSupport {

  lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) =>
    if (isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler

  private def backUrl(isAgent: Boolean): String = if (isAgent) controllers.routes.HomeController.showAgent().url else controllers.routes.HomeController.show().url //change in nav ticket

  def show(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent = isAgent).async { implicit user =>
    /*TODO currently if there are no business addresses then this will redirect the user to the home page.
    * in the nav ticket we will want to likely redirect to the enter business address page instead
    */
    if(isEnabled(OverseasBusinessAddress)){
      val businessAddresses = user.incomeSources.getAllUniqueBusinessAddressesWithIndex
      Future(Ok(view(
        postAction = controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.submit(isAgent),
        isAgent = isAgent,
        form = ChooseSoleTraderAddressForm(),
        businessAddresses = businessAddresses,
        backUrl = backUrl(isAgent)
      ))
      )
    }else{
      val homeCall = if(isAgent) controllers.routes.HomeController.showAgent() else controllers.routes.HomeController.show()
      Future(Redirect(homeCall))
    }

  }

  def submit(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
    val businessAddresses = user.incomeSources.getAllUniqueBusinessAddressesWithIndex
    ChooseSoleTraderAddressForm().bindFromRequest().fold(
      formWithErrors => {
        Future {
          BadRequest(
            view(
              postAction = controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.submit(isAgent),
              isAgent = isAgent,
              form = formWithErrors,
              businessAddresses = businessAddresses,
              backUrl = backUrl(isAgent)
            )
          )
        }
      },
      validForm =>
        handleValidForm(isAgent, validForm)
    )
  }

  private def handleValidForm(
                                isAgent: Boolean,
                                validForm: ChooseSoleTraderAddressForm
                             )(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    val formResponse = validForm.response
    //TODO implement proper routes here in nav ticket
    formResponse match {
      case Some(ChooseSoleTraderAddressForm.existingAddress) =>
        //take the user to 'is this information correct' page
        Future.successful(Redirect(controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.show(isAgent)))
      case Some(ChooseSoleTraderAddressForm.newAddress) =>
        //take the user to 'is this address in the uk' page
        Future.successful(Redirect(controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.show(isAgent)))
      case _ =>
        Logger("application").error("[ChooseSoleTraderAddress] Invalid form response")
        Future(errorHandler(isAgent).showInternalServerError())
    }
  }
}
