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

package controllers

import audit.AuditingService
import audit.models.IvOutcomeSuccessAuditModel
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import controllers.predicates.{AuthenticationPredicate, NinoPredicate}
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import scala.concurrent.{ExecutionContext, Future}
import utils.Utilities.ToFutureSuccessful

class UpliftSuccessController @Inject()(implicit val appConfig: FrontendAppConfig,
                                        mcc: MessagesControllerComponents,
                                        val auditingService: AuditingService,
                                        implicit val executionContext: ExecutionContext,
                                        val authenticate: AuthenticationPredicate,
                                        val retrieveNino: NinoPredicate) extends BaseController()(mcc) with FeatureSwitching {

  def success(origin: String): Action[AnyContent] = (authenticate andThen retrieveNino).async {
    implicit user =>
      auditingService.audit(IvOutcomeSuccessAuditModel(user.nino))
      ( (Redirect(controllers.routes.HomeController.show().url).addingToSession("origin" -> origin)) ).asFuture 
  }
}
