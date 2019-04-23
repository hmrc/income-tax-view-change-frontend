/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.ReportDeadlinesService

import scala.concurrent.Future

@Singleton
class PreviousObligationsController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                              val authenticate: AuthenticationPredicate,
                                              val retrieveNino: NinoPredicate,
                                              val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                              val itvcErrorHandler: ItvcErrorHandler,
                                              val reportDeadlinesService: ReportDeadlinesService,
                                              implicit val config: FrontendAppConfig,
                                              implicit val messagesApi: MessagesApi
                                     ) extends BaseController {

  val getPreviousObligations: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>
      if(config.features.obligationsPageEnabled()) {
        reportDeadlinesService.previousObligationsWithIncomeType(user.incomeSources).map { previousObligations =>
          println(previousObligations)
          Ok(views.html.previousObligations(previousObligations))
        }
      } else {
        Future.successful(Redirect(controllers.routes.HomeController.home()))
      }
  }

}