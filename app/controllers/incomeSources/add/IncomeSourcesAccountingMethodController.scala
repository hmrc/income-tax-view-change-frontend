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

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import forms.incomeSources.add.IncomeSourcesAccountingMethodForm
import forms.utils.SessionKeys.addIncomeSourcesAccountingMethod
import models.incomeSourceDetails.BusinessDetailsModel
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.IncomeSourcesAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourcesAccountingMethodController @Inject()(val authenticate: AuthenticationPredicate,
                                                        val authorisedFunctions: FrontendAuthorisedFunctions,
                                                        val checkSessionTimeout: SessionTimeoutPredicate,
                                                        val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                        val retrieveBtaNavBar: NavBarPredicate,
                                                        val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                        val retrieveNino: NinoPredicate,
                                                        val view: IncomeSourcesAccountingMethod,
                                                        val customNotFoundErrorView: CustomNotFoundError)
                                                       (implicit val appConfig: FrontendAppConfig,
                                                   mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext,
                                                   val itvcErrorHandler: ItvcErrorHandler,
                                                   val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def handleUserActiveBusinessesCashOrAccruals(isAgent: Boolean, errorHandler: ShowInternalServerError, incomeSourceType: String)
                                              (implicit user: MtdItUser[_], backUrl: String, postAction: Call, messages: Messages): Future[Result] = {
    val userActiveBusinesses: List[BusinessDetailsModel] = user.incomeSources.businesses.filterNot(_.isCeased)

    if (userActiveBusinesses.flatMap(_.cashOrAccruals).distinct.size > 1) {
      Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"Error getting business cashOrAccruals Field")
    }

    userActiveBusinesses.map(_.cashOrAccruals).headOption match {
      case Some(cashOrAccrualsFieldMaybe) if (cashOrAccrualsFieldMaybe.isDefined) =>
        val accountingMethodIsAccruals: String = if (cashOrAccrualsFieldMaybe.get) {
          "accruals"
        } else {
          "cash"
        }
        if (isAgent) {
          Future.successful(Redirect(controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent())
            .addingToSession(addIncomeSourcesAccountingMethod -> accountingMethodIsAccruals))
        } else {
          Future.successful(Redirect(controllers.incomeSources.add.routes.CheckBusinessDetailsController.show())
            .addingToSession(addIncomeSourcesAccountingMethod -> accountingMethodIsAccruals))
        }
      case Some(cashOrAccrualsFieldMaybe) if cashOrAccrualsFieldMaybe.isEmpty =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting business cashOrAccruals field")
        Future.successful(errorHandler.showInternalServerError())
      case None =>
        Future.successful(Ok(view(
          incomeSourcesType = incomeSourceType,
          form = IncomeSourcesAccountingMethodForm(incomeSourceType),
          postAction = postAction,
          isAgent = isAgent,
          backUrl = backUrl,
          btaNavPartial = user.btaNavPartial
        )(user, messages)))
    }
  }

  private def loadIncomeSourceAccountingMethod(isAgent: Boolean, errorHandler: ShowInternalServerError, incomeSourceType: String)
                                              (implicit user: MtdItUser[_], backUrl: String, postAction: Call, messages: Messages): Future[Result] = {
    Future.successful(Ok(view(
      incomeSourcesType = incomeSourceType,
      form = IncomeSourcesAccountingMethodForm(incomeSourceType),
      postAction = postAction,
      isAgent = isAgent,
      backUrl = backUrl,
      btaNavPartial = user.btaNavPartial
    )(user, messages)))
  }

  def handleRequest(isAgent: Boolean, incomeSourceType: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val backUrl = incomeSourceType match {
      case SelfEmployment.key =>
        if (isAgent)
          routes.AddBusinessAddressController.showAgent().url
        else
          routes.AddBusinessAddressController.show().url
      case UkProperty.key =>
          routes.AddIncomeSourceStartDateCheckController.showUKProperty(isAgent, isUpdate = false).url
      case ForeignProperty.key =>
          routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent, isUpdate = false).url
    }
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.submitAgent(incomeSourceType) else
      controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.submit(incomeSourceType)

    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    if (incomeSourcesEnabled) {
      if(incomeSourceType == SelfEmployment.key) {
        handleUserActiveBusinessesCashOrAccruals(isAgent = isAgent, errorHandler = errorHandler, incomeSourceType)(
          user = user, backUrl = backUrl, postAction = postAction, messages = messages)
      } else {
        loadIncomeSourceAccountingMethod(isAgent = isAgent, errorHandler = errorHandler, incomeSourceType)(
          user = user, backUrl = backUrl, postAction = postAction, messages = messages)
      }
    } else {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting BusinessEndDate page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

   private def actionIndividualSubmitRequest(incomeSourceType: String): (Call, String, Call) = {
    incomeSourceType match {
      case SelfEmployment.key =>
          (routes.IncomeSourcesAccountingMethodController.submit(SelfEmployment.key),
            routes.AddBusinessAddressController.show().url,
            routes.CheckBusinessDetailsController.show())
      case UkProperty.key =>
          (routes.IncomeSourcesAccountingMethodController.submit(UkProperty.key),
            routes.AddIncomeSourceStartDateCheckController.showUKProperty(isAgent = false, isUpdate = false).url,
            routes.CheckUKPropertyDetailsController.show())
      case ForeignProperty.key =>
          (routes.IncomeSourcesAccountingMethodController.submit(ForeignProperty.key),
            routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = false, isUpdate = false).url,
            routes.ForeignPropertyCheckDetailsController.show())
    }
  }

  private def actionAgentSubmitRequest(incomeSourceType: String): (Call, String, Call) = {
    incomeSourceType match {
      case SelfEmployment.key =>
          (routes.IncomeSourcesAccountingMethodController.submitAgent(SelfEmployment.key),
            routes.AddBusinessAddressController.showAgent().url,
            routes.CheckBusinessDetailsController.showAgent())
      case UkProperty.key =>
          (routes.IncomeSourcesAccountingMethodController.submitAgent(UkProperty.key),
            routes.AddIncomeSourceStartDateCheckController.showUKProperty(isAgent = true, isUpdate = false).url,
            routes.CheckUKPropertyDetailsController.showAgent())
      case ForeignProperty.key =>
          (routes.IncomeSourcesAccountingMethodController.submitAgent(ForeignProperty.key),
            routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = true, isUpdate = false).url,
            routes.ForeignPropertyCheckDetailsController.showAgent())
    }
  }

  def handleSubmitRequest(isAgent: Boolean, incomeSourceType: String)(implicit user: MtdItUser[_]): Future[Result] = {
    val (postAction, backUrl, redirect) = if (isAgent)
      actionAgentSubmitRequest(incomeSourceType)
    else
      actionIndividualSubmitRequest(incomeSourceType)

    IncomeSourcesAccountingMethodForm(incomeSourceType).bindFromRequest().fold(
      hasErrors => Future.successful(BadRequest(view(
        incomeSourcesType = incomeSourceType,
        form = hasErrors,
        postAction = postAction,
        backUrl = backUrl,
        isAgent = isAgent
      ))),
      validatedInput => {
        if (validatedInput.equals(Some("cash"))) {
          Future.successful(Redirect(redirect)
            .addingToSession(addIncomeSourcesAccountingMethod -> "cash"))
        } else {
          Future.successful(Redirect(redirect)
            .addingToSession(addIncomeSourcesAccountingMethod -> "accruals"))
        }
      }
    )
  }

  def show(incomeSourceType: String): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          incomeSourceType
        )
    }

  def showAgent(incomeSourceType: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              incomeSourceType
            )
        }
  }

  def submit(incomeSourceType: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmitRequest(isAgent = false, incomeSourceType)
  }

  def submitAgent(incomeSourceType: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true, incomeSourceType)
        }
  }

  def changeIncomeSourcesAccountingMethod(incomeSourceType: String): Action[AnyContent] = Action {
    Ok("Change Business Accounting Method  WIP")
  }

  def changeIncomeSourcesAccountingMethodAgent(incomeSourceType: String): Action[AnyContent] = Action {
    Ok("Agent Change Business Accounting Method  WIP")
  }
}
