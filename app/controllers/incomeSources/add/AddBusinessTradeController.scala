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
import enums.IncomeSourceJourney.{BeforeSubmissionPage, SelfEmployment}
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.BusinessTradeForm
import models.incomeSourceDetails.AddIncomeSourceData
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.add.AddBusinessTrade

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AddBusinessTradeController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                           val addBusinessTradeView: AddBusinessTrade,
                                           val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                           val sessionService: SessionService,
                                           auth: AuthenticatorPredicate)
                                          (implicit val appConfig: FrontendAppConfig,
                                           implicit val itvcErrorHandler: ItvcErrorHandler,
                                           implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                           implicit override val mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils with JourneyChecker {

  def show(isAgent: Boolean, isChange: Boolean): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        handleRequest(isAgent, isChange)
  }

  def submit(isAgent: Boolean, isChange: Boolean): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit request =>
        handleSubmitRequest(isAgent, isChange)
  }

  def handleRequest(isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withSessionData(JourneyType(Add, SelfEmployment), BeforeSubmissionPage) { sessionData =>
      val businessTradeOpt = sessionData.addIncomeSourceData.flatMap(_.businessTrade)
      val filledForm = businessTradeOpt.fold(BusinessTradeForm.form)(businessTrade =>
        BusinessTradeForm.form.fill(BusinessTradeForm(businessTrade)))

      Future.successful {
        Ok(addBusinessTradeView(filledForm, postAction(isAgent, isChange), isAgent, backUrl(isAgent, isChange)))
      }
    }
  }.recover {
    case ex =>
      Logger("application").error(s"[AddBusinessTradeController][handleRequest] - ${ex.getMessage} - ${ex.getCause}")
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      errorHandler.showInternalServerError()
  }

  def handleSubmitRequest(isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    withSessionData(JourneyType(Add, SelfEmployment), BeforeSubmissionPage) { sessionData =>
      val businessNameOpt = sessionData.addIncomeSourceData.flatMap(_.businessName)

      BusinessTradeForm.checkBusinessTradeWithBusinessName(BusinessTradeForm.form.bindFromRequest(), businessNameOpt).fold(
        formWithErrors => handleFormErrors(formWithErrors, isAgent, isChange),
        formData => handleSuccess(formData.trade, isAgent, isChange)
      )
    }
  }.recover {
    case ex =>
      Logger("application").error(s"[AddBusinessTradeController][handleSubmitRequest] - ${ex.getMessage} - ${ex.getCause}")
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      errorHandler.showInternalServerError()
  }

  def handleFormErrors(form: Form[BusinessTradeForm], isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val postAction = routes.AddBusinessTradeController.submit(isAgent, isChange)
    val backURL = backUrl(isAgent, isChange)

    Future.successful {
      BadRequest(addBusinessTradeView(form, postAction, isAgent, backURL))
    }
  }

  def handleSuccess(businessTrade: String, isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val successURL = Redirect(redirectUrl(isAgent, isChange))
    val journeyType = JourneyType(Add, SelfEmployment)

    sessionService.setMongoKey(AddIncomeSourceData.businessTradeField, businessTrade, journeyType).flatMap {
      case Right(result) if result => Future.successful(successURL)
      case Right(_) => Future.failed(new Exception("Mongo update call was not acknowledged"))
      case Left(exception) => Future.failed(exception)
    }
  }

  private lazy val backUrl: (Boolean, Boolean) => String = (isAgent, isChange) => {
    if (isChange) routes.IncomeSourceCheckDetailsController.show(isAgent, SelfEmployment)
    else          routes.AddIncomeSourceStartDateCheckController.show(isAgent, isChange, SelfEmployment)
  }.url

  private lazy val redirectUrl: (Boolean, Boolean) => String = (isAgent, isChange) => {
    if (isChange) routes.IncomeSourceCheckDetailsController.show(isAgent, SelfEmployment)
    else          routes.AddBusinessAddressController.show(isAgent, isChange)
  }.url

  private lazy val postAction: (Boolean, Boolean) => Call = (isAgent, isChange) =>
    controllers.incomeSources.add.routes.AddBusinessTradeController.submit(isAgent, isChange)
}
