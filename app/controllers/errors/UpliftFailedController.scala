/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.errors

import audit.AuditingService
import audit.models.IvOutcomeFailureAuditModel
import config.FrontendAppConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.errorPages.UpliftFailed

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpliftFailedController @Inject()(upliftFailedView: UpliftFailed,
                                       mcc: MessagesControllerComponents,
                                       auditingService: AuditingService)
                                      (implicit ec: ExecutionContext,
                                       implicit val config: FrontendAppConfig) extends FrontendController(mcc) {
  def show(): Action[AnyContent] = Action.async { implicit request =>
    val journeyId = request.getQueryString("journeyId")
    if (journeyId.isDefined) {
      auditingService.audit(IvOutcomeFailureAuditModel(journeyId.get))
    }
    Future.successful(Forbidden(upliftFailedView()))
  }
}
