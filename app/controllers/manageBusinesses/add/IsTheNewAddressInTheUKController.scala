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
import enums.BeforeSubmissionPage
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import forms.manageBusinesses.add.IsTheNewAddressInTheUKForm as form
import models.admin.OverseasBusinessAddress
import models.core.{Mode, NormalMode}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.errorPages.CustomNotFoundErrorView
import views.html.manageBusinesses.add.IsTheNewAddressInTheUKView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IsTheNewAddressInTheUKController @Inject()(val authActions: AuthActions,
                                                 val isTheNewAddressInTheUKView: IsTheNewAddressInTheUKView,
                                                 val sessionService: SessionService,
                                                 val customNotFoundErrorView: CustomNotFoundErrorView)
                                                (implicit val appConfig: FrontendAppConfig,
                                                 val itvcErrorHandler: ItvcErrorHandler,
                                                 val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                 val mcc: MessagesControllerComponents,
                                                 val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching with IncomeSourcesUtils with JourneyCheckerManageBusinesses {

  def show(mode: Mode, isTriggeredMigration: Boolean): Action[AnyContent] = authActions.asMTDIndividual(isTriggeredMigration).async {
    implicit user =>
      if isEnabled(OverseasBusinessAddress) then
        handleRequest(isAgent = false, mode, isTriggeredMigration)
      else
        Future.successful(Redirect(controllers.routes.HomeController.show().url))
  }

  def showAgent(mode: Mode, isTriggeredMigration: Boolean): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient(isTriggeredMigration).async {
    implicit user =>
      if isEnabled(OverseasBusinessAddress) then
        handleRequest(isAgent = true, mode, isTriggeredMigration)
      else
        Future.successful(Redirect(controllers.routes.HomeController.showAgent().url))
  }

  def handleRequest(isAgent: Boolean, mode: Mode, isTriggeredMigration: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, SelfEmployment), BeforeSubmissionPage) { sessionData =>

      val backURL = getBackURL(isAgent, mode)
      val postAction = getPostAction(isAgent, mode, isTriggeredMigration)

      Future.successful {
        Ok(isTheNewAddressInTheUKView(form.apply, isAgent, hasUKAddress(user), postAction, backURL))
      }
    }
  }.recover {
    case ex =>
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      errorHandler.showInternalServerError()
  }

  def submit(mode: Mode, isTriggeredMigration: Boolean): Action[AnyContent] = authActions.asMTDIndividual(isTriggeredMigration).async {
    implicit request =>
      handleSubmitRequest(isAgent = false, mode, isTriggeredMigration)(implicitly, itvcErrorHandler)
  }

  def submitAgent(mode: Mode, isTriggeredMigration: Boolean): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient(isTriggeredMigration).async {
    implicit request =>
      handleSubmitRequest(isAgent = true, mode, isTriggeredMigration)(implicitly, itvcErrorHandlerAgent)
  }
  
  def handleSubmitRequest(isAgent: Boolean, mode: Mode, isTriggeredMigration: Boolean)(implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, SelfEmployment), BeforeSubmissionPage) { sessionData =>
        form.apply.bindFromRequest().fold(
          formWithErrors =>
            Future.successful {
              BadRequest(
                isTheNewAddressInTheUKView(
                  form = formWithErrors,
                  postAction = getPostAction(isAgent, mode, isTriggeredMigration),
                  isAgent = isAgent,
                  hasUKAddress = hasUKAddress(user),
                  backUrl = getBackURL(isAgent, mode)
                )
              )
            },
          validForm =>
            handleValidForm(validForm, isAgent, isTriggeredMigration)
        )
    }
  }.recover {
    case ex =>
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      errorHandler.showInternalServerError()
  }

  private def handleValidForm(validForm: form,
                              isAgent: Boolean,
                              isTrigMig: Boolean = false)
                             (implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    //  TODO this should be implemented as a part of the https://jira.tools.tax.service.gov.uk/browse/MISUV-10722 Jira ticket
    val formResponse: Option[String] = validForm.toFormMap(form.response).headOption
    val ukPropertyUrl: String = controllers.manageBusinesses.add.routes.IsTheNewAddressInTheUKController.show(isTrigMig).url
    val foreignPropertyUrl: String = controllers.manageBusinesses.add.routes.IsTheNewAddressInTheUKController.show(isTrigMig).url
    
    formResponse match {
      case Some(form.responseUK) => Future.successful(Redirect(ukPropertyUrl))
      case Some(form.responseForeign) => Future.successful(Redirect(foreignPropertyUrl))
      case _ =>
        Logger("application").error(s"Unexpected response, isAgent = $isAgent")
        val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
        Future.successful(errorHandler.showInternalServerError())
    }
  }

  private def hasUKAddress(user: MtdItUser[?]): Boolean = {
    val ukCountryCodes = Seq("UK", "United Kingdom", "GB", "Great Britain")
    val validUKAddress = for {
      b <- user.incomeSources.businesses
      a <- b.address
      countryCode <- a.countryCode
      if ukCountryCodes.contains(countryCode) && a.addressLine1.isDefined && a.postCode.isDefined
    } yield a
    validUKAddress.nonEmpty
  }

  //  TODO this should be implemented as a part of the https://jira.tools.tax.service.gov.uk/browse/MISUV-10722 Jira ticket
  private def getBackURL(isAgent: Boolean, mode: Mode): String = {
    val notImplementedCall: Call = Call(method = "", url = "#NotImplemented")

    ((isAgent, mode) match {
      case (_, NormalMode) => notImplementedCall
      case (false, _) => notImplementedCall
      case (_, _) => notImplementedCall
    }).url
  }

  //  TODO this should be implemented as a part of the https://jira.tools.tax.service.gov.uk/browse/MISUV-10722 Jira ticket
  private def getPostAction(isAgent: Boolean, mode: Mode, isTriggeredMigration: Boolean): Call =
    Call(method = "", url = "#NotImplemented")
}
