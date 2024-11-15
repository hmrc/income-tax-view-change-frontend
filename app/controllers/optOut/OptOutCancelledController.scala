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

import auth.FrontendAuthorisedFunctions
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.optout.OptOutService
import views.html.errorPages.templates.ErrorTemplate
import views.html.optOut.OptOutCancelledView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class OptOutCancelledController @Inject()(
                                           val authorisedFunctions: FrontendAuthorisedFunctions,
                                           val auth: AuthActions,
                                           optOutService: OptOutService,
                                           view: OptOutCancelledView,
                                           errorTemplate: ErrorTemplate
                                         )(
                                           implicit val appConfig: FrontendAppConfig,
                                           mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext,
                                           val itvcErrorHandler: ItvcErrorHandler,
                                           val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                         )

  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(): Action[AnyContent] =
    auth.individualOrAgentWithClient.async { implicit user =>
      for {
        proposition <- optOutService.fetchOptOutProposition()
        availableOptOutYears = proposition.availableOptOutYears
        isOneYearOptOut = proposition.isOneYearOptOut
      } yield {
        if (isOneYearOptOut) {
          availableOptOutYears.headOption.map(_.taxYear) match {
            case Some(taxYear) =>
              Ok(view(user.isAgent(), taxYear.startYear.toString, taxYear.endYear.toString))
            case _ =>
              InternalServerError(
                errorTemplate(
                  pageTitle = "standardError.heading",
                  heading = "standardError.heading",
                  message = "standardError.message",
                  isAgent = user.isAgent()
                )
              )
          }
        } else {
          InternalServerError(
            errorTemplate(
              pageTitle = "standardError.heading",
              heading = "standardError.heading",
              message = "standardError.message",
              isAgent = user.isAgent()
            )
          )
        }
      }
    }
}