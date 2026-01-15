/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.triggeredMigration

import auth.authV2.AuthActions
import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import forms.triggeredMigration.CheckActiveBusinessesConfirmForm
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.TriggeredMigrationUtils
import views.html.triggeredMigration.CheckActiveBusinessesConfirmView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckActiveBusinessesConfirmController @Inject()(
                                                        view: CheckActiveBusinessesConfirmView,
                                                        val auth: AuthActions
                                                      )(
                                                        mcc: MessagesControllerComponents,
                                                        implicit val appConfig: FrontendAppConfig,
                                                        implicit val ec: ExecutionContext
                                                      ) extends FrontendController(mcc) with I18nSupport with TriggeredMigrationUtils {


  def show(isAgent: Boolean): Action[AnyContent] =
    auth.asMTDIndividualOrAgentWithClient(isAgent, triggeredMigrationPage = true).async { implicit user =>
      withTriggeredMigrationFS {
        val form = CheckActiveBusinessesConfirmForm()
        Future.successful(
          Ok(
            view(
              form = form,
              postAction = routes.CheckActiveBusinessesConfirmController.submit(isAgent),
              backUrl = routes.CheckHmrcRecordsController.show(isAgent).url,
              isAgent = isAgent
            )
          )
        )
      }
    }

  def submit(isAgent: Boolean): Action[AnyContent] =
    auth.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
      withTriggeredMigrationFS {
        CheckActiveBusinessesConfirmForm().bindFromRequest().fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  form = formWithErrors,
                  postAction = routes.CheckActiveBusinessesConfirmController.submit(isAgent),
                  backUrl = routes.CheckHmrcRecordsController.show(isAgent).url,
                  isAgent = isAgent
                )
              )
            ),
          _ =>
            Future.successful(Redirect(routes.CheckActiveBusinessesConfirmController.show(isAgent)))
        )
      }
    }
}
