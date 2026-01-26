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
import forms.triggeredMigration.CheckCompleteForm
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.triggeredMigration.CheckCompleteView
import utils.TriggeredMigrationUtils

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckCompleteController @Inject()(view: CheckCompleteView,
                                        val auth: AuthActions)
                                       (mcc: MessagesControllerComponents,
                                        implicit val appConfig: FrontendAppConfig,
                                        implicit val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with TriggeredMigrationUtils {

  private def nextUpdatesLink(isAgent: Boolean): String =
    if (isAgent) controllers.routes.NextUpdatesController.showAgent().url
    else controllers.routes.NextUpdatesController.show().url

  private def homepageLink(isAgent: Boolean) =
    if(isAgent) controllers.routes.HomeController.showAgent()
    else controllers.routes.HomeController.show()

  def show(isAgent: Boolean): Action[AnyContent] = auth.asMTDIndividualOrAgentWithClient(isAgent, triggeredMigrationPage = true).async { implicit user =>
    withTriggeredMigrationFS {
      val compatibleSoftwareLink: String = appConfig.compatibleSoftwareLink
      val form = CheckCompleteForm()

      Future.successful(Ok(
        view(
          isAgent,
          compatibleSoftwareLink,
          nextUpdatesLink(isAgent),
          homepageLink(isAgent),
          form,
          postAction = controllers.triggeredMigration.routes.CheckCompleteController.submit(isAgent)
        ))
      )
    }
  }

  def submit(isAgent: Boolean): Action[AnyContent] =
    auth.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
      val compatibleSoftwareLink: String = appConfig.compatibleSoftwareLink
      withTriggeredMigrationFS {
        CheckCompleteForm().bindFromRequest().fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  isAgent,
                  compatibleSoftwareLink,
                  nextUpdatesLink(isAgent),
                  homepageLink(isAgent),
                  form = formWithErrors,
                  postAction = controllers.triggeredMigration.routes.CheckCompleteController.submit(isAgent)
                )
              )
            ),

          value =>
            (isAgent, value.response) match {
              case (false, Some(CheckCompleteForm.responseContinue)) => Future.successful(Redirect(controllers.routes.HomeController.show()))
              case (true, Some(CheckCompleteForm.responseContinue)) => Future.successful(Redirect(controllers.routes.HomeController.showAgent()))
              case (_, _) => Future.successful(Redirect(routes.CheckHmrcRecordsController.show(isAgent)))
            }
        )
      }
    }
}
