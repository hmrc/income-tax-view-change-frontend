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
import play.api.Logger
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

  private def getBackURL(isAgent: Boolean, isChange: Boolean): String = {
    ((isAgent, isChange) match {
      case (_, false) => routes.AddIncomeSourceStartDateCheckController.show(isAgent, isChange = false, SelfEmployment)
      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }).url
  }

  private def getSuccessURL(isAgent: Boolean, isChange: Boolean): String = {
    ((isAgent, isChange) match {
      case (false, false) => routes.AddBusinessAddressController.show(isChange)
      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (_, false) => routes.AddBusinessAddressController.showAgent(isChange)
      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }).url
  }

  def show(isAgent: Boolean, isChange: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      handleRequest(isAgent, isChange)
  }

  def handleRequest(isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withSessionData(JourneyType(Add, SelfEmployment), BeforeSubmissionPage) { sessionData =>
      val businessTradeOpt = sessionData.addIncomeSourceData.flatMap(_.businessTrade)
      val filledForm = businessTradeOpt.fold(BusinessTradeForm.form)(businessTrade =>
        BusinessTradeForm.form.fill(BusinessTradeForm(businessTrade)))
      val backURL = getBackURL(isAgent, isChange)
      val postAction = controllers.incomeSources.add.routes.AddBusinessTradeController.submit(isAgent, isChange)

      Future.successful {
        Ok(addBusinessTradeView(filledForm, postAction, isAgent, backURL))
      }
    }
  }.recover {
    case ex =>
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      errorHandler.showInternalServerError()
  }

  def submit(isAgent: Boolean, isChange: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit request =>
      handleSubmitRequest(isAgent, isChange)
  }

  def handleSubmitRequest(isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    withSessionData(JourneyType(Add, SelfEmployment), BeforeSubmissionPage) { sessionData =>
      val businessNameOpt = sessionData.addIncomeSourceData.flatMap(_.businessName)

      BusinessTradeForm
        .checkBusinessTradeWithBusinessName(BusinessTradeForm.form.bindFromRequest(), businessNameOpt).fold(
          formWithErrors =>
            Future.successful {
              BadRequest(
                addBusinessTradeView(
                  businessTradeForm = formWithErrors,
                  postAction = routes.AddBusinessTradeController.submit(isAgent, isChange),
                  isAgent = isAgent,
                  backURL = getBackURL(isAgent, isChange)
                )
              )
            },
          validForm =>
            sessionService.setMongoData(
              sessionData.copy(
                addIncomeSourceData =
                  sessionData.addIncomeSourceData.map(
                    _.copy(
                      businessTrade = Some(validForm.trade)
                    )
                  )
              )
            ) flatMap {
              case true  => Future.successful(Redirect(getSuccessURL(isAgent, isChange)))
              case false => Future.failed(new Exception("Mongo update call was not acknowledged"))
            }
        )
    }
  }.recover {
    case ex =>
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      errorHandler.showInternalServerError()
  }
}
