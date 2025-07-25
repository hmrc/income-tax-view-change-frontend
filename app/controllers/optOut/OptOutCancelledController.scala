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

import auth.authV2.AuthActions
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import models.incomeSourceDetails.TaxYear
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.optout.OptOutService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ReportingObligationsUtils
import views.html.errorPages.templates.ErrorTemplate
import views.html.optOut.OptOutCancelledView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class OptOutCancelledController @Inject()(val authActions: AuthActions,
                                          optOutService: OptOutService,
                                          view: OptOutCancelledView,
                                          errorTemplate: ErrorTemplate
                                         )(
                                           implicit val appConfig: FrontendAppConfig,
                                           mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext
                                         )

  extends FrontendController(mcc) with I18nSupport with FeatureSwitching with ReportingObligationsUtils {

  def show(): Action[AnyContent] =
    authActions.asMTDIndividual.async { implicit user =>
      withOptOutFS {
        optOutService.getTaxYearForOptOutCancelled()
          .map { taxYear: Option[TaxYear] =>
            Ok(view(
              isAgent = false,
              taxYear
            ))
          }.recover {
            case _ =>
              InternalServerError(
                errorTemplate(
                  pageTitle = "standardError.heading",
                  heading = "standardError.heading",
                  message = "standardError.message",
                  isAgent = false
                )
              )
          }
      }
    }

  def showAgent(): Action[AnyContent] =
    authActions.asMTDAgentWithConfirmedClient.async { implicit user =>
      withOptOutFS {
        optOutService.getTaxYearForOptOutCancelled()
          .map { taxYear: Option[TaxYear] =>
            Ok(view(
              isAgent = true,
              taxYear
            ))
          }.recover {
            case _ =>
              InternalServerError(
                errorTemplate(
                  pageTitle = "standardError.heading",
                  heading = "standardError.heading",
                  message = "standardError.message",
                  isAgent = true
                )
              )
          }
      }
    }
}