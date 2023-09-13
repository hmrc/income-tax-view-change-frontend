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
import enums.IncomeSourceJourney.SelfEmployment
import forms.incomeSources.add.BusinessTradeForm
import forms.utils.SessionKeys
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.AddBusinessTrade

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AddBusinessTradeController @Inject()(authenticate: AuthenticationPredicate,
                                           val authorisedFunctions: AuthorisedFunctions,
                                           checkSessionTimeout: SessionTimeoutPredicate,
                                           retrieveNino: NinoPredicate,
                                           val addBusinessTradeView: AddBusinessTrade,
                                           val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                           val retrieveBtaNavBar: NavBarPredicate,
                                           val sessionService: SessionService,
                                           incomeSourceDetailsService: IncomeSourceDetailsService)
                                          (implicit val appConfig: FrontendAppConfig,
                                           implicit val itvcErrorHandler: ItvcErrorHandler,
                                           implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                           implicit override val mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils {

  lazy val checkBusinessStartDate: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.show(isAgent = false, isChange = false, SelfEmployment).url
  lazy val checkBusinessStartDateAgent: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.show(isAgent = true, isChange = false, SelfEmployment).url
  lazy val checkBusinessDetails: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url
  lazy val checkBusinessDetailsAgent: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url

  private def getBackURL(isAgent: Boolean, isChange: Boolean): String = {
    (isAgent, isChange) match {
      case (true, true) => checkBusinessDetailsAgent
      case (false, true) => checkBusinessDetails
      case (true, false) => checkBusinessStartDateAgent
      case (false, false) => checkBusinessStartDate
    }
  }

  private def getSuccessURL(isAgent: Boolean, isChange: Boolean): String = {
    lazy val addBusinessAddress: String = controllers.incomeSources.add.routes.AddBusinessAddressController.show(isChange = false).url
    lazy val addBusinessAddressAgent: String = controllers.incomeSources.add.routes.AddBusinessAddressController.showAgent(isChange = false).url

    (isAgent, isChange) match {
      case (true, true) => checkBusinessDetailsAgent
      case (false, true) => checkBusinessDetails
      case (true, false) => addBusinessAddressAgent
      case (false, false) => addBusinessAddress
    }
  }

  private def authenticatedAction(isAgent: Boolean)(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent) {
      Authenticated.async { implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
            authenticatedCodeBlock(mtdItUser)
          }
      }
    } else {
      (checkSessionTimeout andThen authenticate andThen retrieveNino
        andThen retrieveIncomeSources andThen retrieveBtaNavBar).async { implicit user =>
        authenticatedCodeBlock(user)
      }
    }
  }

  def show(isAgent: Boolean, isChange: Boolean): Action[AnyContent] = authenticatedAction(isAgent) {
    implicit user =>
      handleRequest(isAgent, isChange)
  }

  def handleRequest(isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFS {
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      sessionService.get(SessionKeys.businessTrade).map {
        case Right(tradeOpt) =>
          val filledForm = tradeOpt match {
            case Some(trade) => BusinessTradeForm.form.fill(BusinessTradeForm(trade))
            case None => BusinessTradeForm.form
          }
          val backURL = getBackURL(isAgent, isChange)
          val postAction = controllers.incomeSources.add.routes.AddBusinessTradeController.submit(isAgent, isChange)

          Ok(addBusinessTradeView(filledForm, postAction, isAgent, backURL))
        case Left(error) =>
          Logger("application").error(s"[AddBusinessTradeController][handleRequest] $error")
          errorHandler.showInternalServerError()
      }
    }
  }

  def submit(isAgent: Boolean, isChange: Boolean): Action[AnyContent] = authenticatedAction(isAgent) {
    implicit request =>
      handleSubmitRequest(isAgent, isChange)
  }

  def handleSubmitRequest(isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    withIncomeSourcesFS {
      sessionService.get(SessionKeys.businessName).flatMap {
        case Right(businessName) =>
          BusinessTradeForm.checkBusinessTradeWithBusinessName(BusinessTradeForm.form.bindFromRequest(), businessName).fold(
            formWithErrors => handleFormErrors(formWithErrors, isAgent, isChange),
            formData => handleSuccess(formData.trade, isAgent, isChange)
          )
        case Left(exception) => Future.failed(exception)
      }
    }.recover {
      case exception =>
        val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
        Logger("application").error(s"[AddBusinessTradeController][handleSubmitRequest] ${exception.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  def handleFormErrors(form: Form[BusinessTradeForm], isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val postAction = routes.AddBusinessTradeController.submit(isAgent, isChange)
    val backURL = getBackURL(isAgent, isChange)

    Future {
      Ok(addBusinessTradeView(form, postAction, isAgent = isAgent, backURL))
    }
  }

  def handleSuccess(businessTrade: String, isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val successURL = getSuccessURL(isAgent, isChange)

    sessionService.set(SessionKeys.businessTrade, businessTrade, Redirect(successURL)).flatMap {
      case Right(result) => Future.successful(result)
      case Left(exception) => Future.failed(exception)
    }.recover {
      case exception =>
        val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
        Logger("application").error(s"[AddBusinessTradeController][handleSuccess] ${exception.getMessage}")
        errorHandler.showInternalServerError()
    }
  }
}