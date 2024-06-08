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
import exceptions.MissingFieldException
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.optout.{MultiYearOptOutCheckpointViewModel, OneYearOptOutCheckpointViewModel, OptOutCheckpointViewModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.SessionService
import services.optout.{MultiYearOptOutProposition, OneYearOptOutProposition, OptOutService, OptOutTaxYear}
import utils.{AuthenticatorPredicate, OptOutJourney}
import views.html.optOut.{CheckOptOutAnswers, ConfirmOptOut}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmOptOutController @Inject()(view: ConfirmOptOut,
                                        checkOptOutAnswers: CheckOptOutAnswers,
                                        optOutService: OptOutService,
                                        auth: AuthenticatorPredicate,
                                        override val sessionService: SessionService)
                                       (implicit val appConfig: FrontendAppConfig,
                                        val ec: ExecutionContext,
                                        val authorisedFunctions: FrontendAuthorisedFunctions,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                        override val mcc: MessagesControllerComponents)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with OptOutJourney {

  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private def withOptOutQualifiedTaxYear(intent: Option[TaxYear], isAgent: Boolean)(oneYear: OneYearOptOutCheckpointViewModel => Result, multiYear: OptOutCheckpointViewModel => Result)
                                        (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    optOutService.optOutCheckPointPageViewModel(intent).map {
      case Some(optOutOneYearCheckpointViewModel: OneYearOptOutCheckpointViewModel) => oneYear(optOutOneYearCheckpointViewModel)
      case Some(multiYearCheckpointViewModel: MultiYearOptOutCheckpointViewModel) => multiYear(multiYearCheckpointViewModel)
      case _ =>
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

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    val postAction: Call = controllers.optOut.routes.ConfirmOptOutController.submit(isAgent)
    implicit user =>
      withRecover(isAgent) {
        withSessionData((sessionData: UIJourneySessionData) => {
          val intent = sessionData.optOutSessionData.flatMap(_.intent).getOrElse("Unable to find Opt Out Intent") //TODO Remove hard coded values
          val intentTaxYear = TaxYear.getTaxYearModel(intent)
          withOptOutQualifiedTaxYear(intentTaxYear, isAgent)(
            oneYearOptOutCheckpointViewModel => {
              Ok(view(oneYearOptOutCheckpointViewModel, isAgent = isAgent))
            },
            multiYearViewModel => {
              Ok(checkOptOutAnswers(multiYearViewModel, postAction ,isAgent))
            }
          )
        },
          ex => handleErrorCase(isAgent)(ex)
        )
      }
  }

  def handleErrorCase(isAgent: Boolean)(ex: Throwable)(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    Logger("application").error(s"Error retrieving Opt Out session data: ${ex.getMessage}", ex)
    Future.successful(errorHandler(isAgent).showInternalServerError())
  }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent = isAgent) {
    implicit user =>
      optOutService.makeOptOutUpdateRequest().map {
        case OptOutUpdateResponseSuccess(_, _) => Redirect(routes.ConfirmedOptOutController.show(isAgent))
        case _ => Redirect(routes.OptOutErrorController.show(isAgent))
      }
  }
}