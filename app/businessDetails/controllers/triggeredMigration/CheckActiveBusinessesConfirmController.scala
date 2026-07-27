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

package businessDetails.controllers.triggeredMigration

import businessDetails.models.audit.TriggeredMigrationCompleteAuditModel
import businessDetails.forms.triggeredMigration.CheckActiveBusinessesConfirmForm
import businessDetails.services.triggeredMigration.TriggeredMigrationService
import businessDetails.utils.TriggeredMigrationUtils
import com.google.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import businessDetails.views.html.triggeredMigration.CheckActiveBusinessesConfirmView
import common.auth.AuthActions
import common.config.FrontendAppConfig
import common.services.{AuditingService, CustomerFactsUpdateService}
import common.utils.sessionUtils.SessionKeys

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckActiveBusinessesConfirmController @Inject()(
                                                        view: CheckActiveBusinessesConfirmView,
                                                        triggeredMigrationService: TriggeredMigrationService,
                                                        customerFactsUpdateService: CustomerFactsUpdateService,
                                                        auditingService: AuditingService,
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
    auth.asMTDIndividualOrAgentWithClient(isAgent, triggeredMigrationPage = true).async { implicit user =>
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
          form => form.response match {
            case Some(CheckActiveBusinessesConfirmForm.responseYes) =>
              val mtdId = user.mtditid
              customerFactsUpdateService.updateCustomerFacts(mtdId).flatMap { _ =>
                triggeredMigrationService.saveConfirmedData().flatMap {
                  _ => {
                    auditingService.extendedAudit(TriggeredMigrationCompleteAuditModel())
                    Future.successful(
                      // Stores the confirmation in the session cookie so IncomeSourceConnector can send the Gov-Test-Scenario header
                      // and retrieve post-migration stub data on subsequent requests in local and staging environments.
                      // This has no effect in production.
                      //
                      // To test the post-migration stub data in QA, add Gov-Test-Scenario to QA's headersAllowlist.
                      Redirect(routes.CheckCompleteController.show(isAgent))
                        .addingToSession(SessionKeys.triggeredMigrationConfirmed -> "true")
                    )
                  }
                }
              }
            case _ =>
              Future.successful(Redirect(routes.CheckHmrcRecordsController.show(isAgent)))
          }
        )
      }
    }
}
