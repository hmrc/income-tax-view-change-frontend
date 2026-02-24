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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import forms.manageBusinesses.add.ChooseSoleTraderAddressForm
import jakarta.inject.Singleton
import models.UIJourneySessionData
import models.admin.OverseasBusinessAddress
import models.incomeSourceDetails.{AddIncomeSourceData, ChooseSoleTraderAddressUserAnswer}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.add.ChooseSoleTraderAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChooseSoleTraderAddressController @Inject()(
                                                   authActions: AuthActions,
                                                   itvcErrorHandler: ItvcErrorHandler,
                                                   itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                   val sessionService: SessionService,
                                                   view: ChooseSoleTraderAddressView,
                                                 )(implicit val appConfig: FrontendAppConfig,
                                                   val mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext
                                                 ) extends FrontendController(mcc) with FeatureSwitching with IncomeSourcesUtils with I18nSupport with JourneyCheckerManageBusinesses {

  lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) =>
    if (isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler

  private def backUrl(isAgent: Boolean): String =
    if (isAgent) controllers.routes.HomeController.showAgent().url
    else controllers.routes.HomeController.show().url //TODO: Change in nav ticket

  private def buildAddressOptions(user: MtdItUser[_]): Seq[(String, Int)] = {
    user.incomeSources.getAllUniqueBusinessAddressesWithIndex.map {
      case (answer, index) =>
        val id = index
        val display =
          s"${answer.addressLine1.getOrElse("")}, ${answer.postcode.getOrElse("")}"
        (display, id)
    }
  }


  private def handleValidForm(
                               isAgent: Boolean,
                               validForm: ChooseSoleTraderAddressForm,
                             )(implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    val formResponse = validForm.response
    lazy val isNewAddress = ChooseSoleTraderAddressUserAnswer(None, None, None, true)
    lazy val IsAddressInTheUk = Redirect(controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.show(isAgent)) // TODO: Add logic to navigate to the 'Is this address in the uk' page
    lazy val showGenericErrorPage = errorHandler(isAgent).showInternalServerError()

    sessionService.getMongo(IncomeSourceJourneyType(Add, SelfEmployment)).flatMap {
      case Left(error) =>
        Future(errorHandler(isAgent).showInternalServerError())
      case Right(uiSessionDataOpt) =>
        uiSessionDataOpt match {
          case Some(uiSessionData) =>
            formResponse match {
              case "new-address" =>
                val updatedData: UIJourneySessionData = uiSessionData.copy(addIncomeSourceData = uiSessionData.addIncomeSourceData.map(_.copy(chooseSoleTraderAddress = Some(isNewAddress))))
                sessionService.setMongoData(updatedData).map { data => IsAddressInTheUk }
              case previousBusinessAddressIndex =>
                val previousBusinessAddressDetails: ChooseSoleTraderAddressUserAnswer = mtdItUser.incomeSources.getAllUniqueBusinessAddresses(previousBusinessAddressIndex.toInt)
                val redirect: Result = Redirect(controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.show(isAgent)) // TODO: Update with route
                val updatedData: UIJourneySessionData = uiSessionData.copy(addIncomeSourceData = uiSessionData.addIncomeSourceData.map(_.copy(chooseSoleTraderAddress = Some(previousBusinessAddressDetails))))
                sessionService.setMongoData(updatedData).map { data => redirect }
              case _ =>
                uiSessionData.addIncomeSourceData.map(_.copy(chooseSoleTraderAddress = None))
                val updatedData: UIJourneySessionData = uiSessionData.copy(addIncomeSourceData = uiSessionData.addIncomeSourceData.map(_.copy(chooseSoleTraderAddress = None)))
                Logger("application").error("[ChooseSoleTraderAddress][handleValidForm] Pre-existing ui session data but invalid form response")
                sessionService.setMongoData(updatedData).map { data => showGenericErrorPage }
            }
          case None =>
            formResponse match {
              case "new-address" =>
                UIJourneySessionData(
                  sessionId = hc.sessionId.get.value,
                  journeyType = "ADD-SE",
                  addIncomeSourceData = Some(AddIncomeSourceData(chooseSoleTraderAddress = Some(isNewAddress)))
                )
                val redirect = Redirect(controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.show(isAgent)) // TODO: Add logic to navigate to the 'Is this address in the uk' page
                Future(redirect)
              case previousBusinessAddressIndex =>
                val previousBusinessAddressDetails: ChooseSoleTraderAddressUserAnswer = mtdItUser.incomeSources.getAllUniqueBusinessAddresses(previousBusinessAddressIndex.toInt)
                val uiSessionData =
                  UIJourneySessionData(
                    sessionId = hc.sessionId.get.value,
                    journeyType = "ADD-SE",
                    addIncomeSourceData = Some(AddIncomeSourceData(chooseSoleTraderAddress = Some(previousBusinessAddressDetails)))
                  )
                val redirect = Redirect(controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.show(isAgent)) // TODO: Update with route
                sessionService.setMongoData(uiSessionData).map { data => redirect }
              case _ =>
                Logger("application").error("[ChooseSoleTraderAddress][handleValidForm] No existing ui session data and invalid form response")
                Future(showGenericErrorPage)
            }
        }
    }
  }

  def show(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent = isAgent).async { implicit user =>

    /*TODO currently if there are no business addresses then this will redirect the user to the home page.
    * in the nav ticket we will want to likely redirect to the enter business address page instead
    */

    if (isEnabled(OverseasBusinessAddress)) {
      val chooseSoleTraderAddressRadioOptionsWithIndex: Seq[(String, Int)] = buildAddressOptions(user)

      // radios to be labelled 0, 1, 2, 3, 4, 5 etc. for each user business address or
      // if user selects 'None of these, I want to add a new address' then the radio id will be set to 'new-address'
      val radioButtonValues =
        chooseSoleTraderAddressRadioOptionsWithIndex.map { case (_, id) => id.toString } :+ "new-address"

      val form = ChooseSoleTraderAddressForm.form(radioButtonValues)

      Future(
        Ok(
          view(
            postAction = controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.submit(isAgent),
            isAgent = isAgent,
            form = form,
            chooseSoleTraderAddressRadioOptionsWithIndex = chooseSoleTraderAddressRadioOptionsWithIndex,
            backUrl = backUrl(isAgent)
          )
        )
      )
    } else {
      val homeCall = if (isAgent) controllers.routes.HomeController.showAgent() else controllers.routes.HomeController.show()
      Future(Redirect(homeCall))
    }
  }

  def submit(isAgent: Boolean): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
      // TODO: we need to save the user address to the ui session data
      val addressOptions: Seq[(String, Int)] = buildAddressOptions(user)

      // radios to be labelled 0, 1, 2, 3, 4, 5 etc. for each user business address or
      // if user selects 'None of these, I want to add a new address' then the radio id will be set to 'new-address'
      val radioButtonValues =
        addressOptions.map { case (_, id) => id.toString } :+ "new-address"

      ChooseSoleTraderAddressForm.form(radioButtonValues)
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Future {
              BadRequest(
                view(
                  postAction = controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.submit(isAgent),
                  isAgent = isAgent,
                  form = formWithErrors,
                  chooseSoleTraderAddressRadioAnswersWithIndex = addressOptions,
                  backUrl = backUrl(isAgent)
                )
              )
            }
          },
          validForm => handleValidForm(isAgent, validForm)
        )
    }
}
