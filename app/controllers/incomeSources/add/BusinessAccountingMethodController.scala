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
import exceptions.MissingFieldException
import forms.incomeSources.add.BusinessAccountingMethodForm
import forms.utils.SessionKeys.addBusinessAccountingMethod
import models.incomeSourceDetails.BusinessDetailsModel
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.BusinessAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessAccountingMethodController @Inject()(val authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: FrontendAuthorisedFunctions,
                                                   val checkSessionTimeout: SessionTimeoutPredicate,
                                                   val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                   val retrieveBtaNavBar: NavBarPredicate,
                                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                   val retrieveNino: NinoPredicate,
                                                   val view: BusinessAccountingMethod,
                                                   val customNotFoundErrorView: CustomNotFoundError)
                                                  (implicit val appConfig: FrontendAppConfig,
                                                   mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext,
                                                   val itvcErrorHandler: ItvcErrorHandler,
                                                   val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def handleRequest(isAgent: Boolean, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val backUrl: String = if (isAgent) controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url else
      controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url //checkurl
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.BusinessAccountingMethodController.submitAgent() else //placeholder controller
      controllers.incomeSources.add.routes.BusinessAccountingMethodController.submit() //checkurl

    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    if (incomeSourcesEnabled) {
      Future.successful(Ok(view(
        form = BusinessAccountingMethodForm.form,
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl,
        btaNavPartial = user.btaNavPartial
      )(user, messages)))
    } else {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting BusinessEndDate page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  def currentAccountingMethod(business: BusinessDetailsModel): String =
    if (business.cashOrAccrualsFlag.getOrElse(throw new MissingFieldException("cashOrAccrualsFlag"))) {
      "accruals"
    } else {
      "cash"
    }

  def show(): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        if (user.incomeSources.businesses.nonEmpty) {
          val currentAccountingMethodValue: String = currentAccountingMethod(user.incomeSources.businesses.head)
          Future.successful(Redirect(controllers.incomeSources.add.routes.CheckBusinessDetailsController.show())
            .addingToSession(addBusinessAccountingMethod -> currentAccountingMethodValue))
        } else {
          handleRequest(
            isAgent = false
          )
        }
    }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            if (mtdItUser.incomeSources.businesses.nonEmpty) {
              val currentAccountingMethodValue: String = currentAccountingMethod(mtdItUser.incomeSources.businesses.head)
              Future.successful(Redirect(controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent())
                .addingToSession(addBusinessAccountingMethod -> currentAccountingMethodValue))
            } else {
              handleRequest(
                isAgent = false
              )
            }
        }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      BusinessAccountingMethodForm.form.bindFromRequest().fold(
        hasErrors => Future.successful(BadRequest(view(
          form = hasErrors,
          postAction = controllers.incomeSources.add.routes.BusinessAccountingMethodController.submit(),
          backUrl = controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url,
          isAgent = false
        ))),
        validatedInput => {
          if (validatedInput.equals(Some("cash"))) {
            Future.successful(Redirect(controllers.incomeSources.add.routes.CheckBusinessDetailsController.show())
              .addingToSession(addBusinessAccountingMethod -> "cash"))
          } else {
            Future.successful(Redirect(controllers.incomeSources.add.routes.CheckBusinessDetailsController.show())
              .addingToSession(addBusinessAccountingMethod -> "accruals"))
          }
        }
      )
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            BusinessAccountingMethodForm.form.bindFromRequest().fold(
              hasErrors => Future.successful(BadRequest(view(
                form = hasErrors,
                postAction = controllers.incomeSources.add.routes.BusinessAccountingMethodController.submit(),
                backUrl = controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url,
                isAgent = true,
              ))),
              validatedInput => {
                if (validatedInput.equals("cash")) {
                  Future.successful(Redirect(controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent())
                    .addingToSession(addBusinessAccountingMethod -> "cash"))
                } else {
                  Future.successful(Redirect(controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent())
                    .addingToSession(addBusinessAccountingMethod -> "accruals"))
                }
              }
            )
        }
  }
}
