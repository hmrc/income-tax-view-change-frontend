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

package controllers.optIn

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import cats.data.OptionT
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.itsaStatus.ITSAStatus.Voluntary
import models.optin.OptInCompletedViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.optIn.OptInService
import utils.AuthenticatorPredicate
import views.html.optIn.OptInCompletedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OptInCompletedController @Inject()(val view: OptInCompletedView,
                                         val optInService: OptInService,
                                         val authorisedFunctions: FrontendAuthorisedFunctions,
                                         val auth: AuthenticatorPredicate)
                                        (implicit val appConfig: FrontendAppConfig,
                                         mcc: MessagesControllerComponents,
                                         val ec: ExecutionContext,
                                         val itvcErrorHandler: ItvcErrorHandler,
                                         val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception =>
        Logger("application").error(s"request failed :: $ex")
        errorHandler(isAgent).showInternalServerError()
    }
  }

  def show(isAgent: Boolean = false): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        withRecover(isAgent) {
          optinCompletedPageModel(isAgent)
            .map(model => Ok(view(model)))
            .getOrElse(errorHandler(isAgent).showInternalServerError())
        }
    }

  private def optinCompletedPageModel(isAgent: Boolean)(implicit user: MtdItUser[_]): OptionT[Future, OptInCompletedViewModel] = {
    OptionT(for {
      proposition <- optInService.fetchOptInProposition()
      intent <- optInService.fetchSavedChosenTaxYear()
    } yield {
      intent.map(optInTaxYear =>
        OptInCompletedViewModel(
          isAgent = isAgent,
          optInTaxYear = optInTaxYear,
          showAnnualReportingAdvice = proposition.showAnnualReportingAdvice(optInTaxYear),
          isCurrentYear = proposition.isCurrentTaxYear(optInTaxYear),
          optInIncludedNextYear = proposition.nextTaxYear.status == Voluntary
        ))
    })
  }
}