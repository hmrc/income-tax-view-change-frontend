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

package controllers.optOut

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.optout.OptOutUpdateRequestModel.OptOutUpdateResponseSuccess
import controllers.agent.predicates.ClientConfirmedController
import models.incomeSourceDetails.TaxYear
import models.optout.{OptOutCheckpointViewModel, OptOutMultiYearCheckpointViewModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.UIJourneySessionDataRepository
import services.optout.OptOutService
import utils.{AuthenticatorPredicate, OptOutJourney}
import views.html.optOut.{ConfirmOptOut, ConfirmOptOutMultiYear}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmOptOutController @Inject()(view: ConfirmOptOut,
                                        multiyearCheckpointView: ConfirmOptOutMultiYear,
                                        optOutService: OptOutService,
                                        auth: AuthenticatorPredicate,
                                        repository: UIJourneySessionDataRepository)
                                       (implicit val appConfig: FrontendAppConfig,
                                        val ec: ExecutionContext,
                                        val authorisedFunctions: FrontendAuthorisedFunctions,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                        override val mcc: MessagesControllerComponents)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      withRecover(isAgent) {
        withOptOutQualifiedTaxYear(isAgent)(
          viewModel => Ok(view(viewModel, isAgent = isAgent))
        )
      }
  }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent = isAgent) {
    implicit user =>
      optOutService.makeOptOutUpdateRequest().map {
        case OptOutUpdateResponseSuccess(_, _) => Redirect(routes.ConfirmedOptOutController.show(isAgent))
        case _ => itvcErrorHandler.showInternalServerError()
      }
  }

  def showMultiYearConfirm(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>

      withRecover(isAgent) {

        repository.get(hc.sessionId.get.value, OptOutJourney.Name) map { sessionData =>
          val taxYear = for {
            data <- sessionData
            optOutData <- data.optOutSessionData
            selected <- optOutData.selectedOptOutYear
            parsed <- TaxYear.getTaxYearModel(selected)
          } yield parsed

          taxYear match {
            case Some(ty) => Ok(multiyearCheckpointView(OptOutMultiYearCheckpointViewModel(ty), isAgent))
            case _ => itvcErrorHandler.showInternalServerError()
          }
        }
      }
  }

  def submitMultiYearConfirm(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      withRecover(isAgent) {
        Future.successful(itvcErrorHandler.showInternalServerError())
      }
  }

  private def withOptOutQualifiedTaxYear(isAgent: Boolean)(function: OptOutCheckpointViewModel => Result)
                                        (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    optOutService.optOutCheckPointPageViewModel().map {
      case Some(optOutOneYearCheckpointViewModel) => function(optOutOneYearCheckpointViewModel)
      case None =>
        Logger("application").error("No qualified tax year available for opt out")
        errorHandler(isAgent).showInternalServerError()
    }

  }

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception =>
        Logger("application").error(s"request failed :: $ex")
        errorHandler(isAgent).showInternalServerError()
    }
  }

}