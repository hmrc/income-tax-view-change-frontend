/*
 * Copyright 2020 HM Revenue & Customs
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

import config.featureswitch.{FeatureSwitching, ObligationsPage}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import javax.inject.{Inject, Singleton}
import models.reportDeadlines.ObligationsModel
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.ReportDeadlinesService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreviousObligationsController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                              val authenticate: AuthenticationPredicate,
                                              val retrieveNino: NinoPredicate,
                                              val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                              val itvcErrorHandler: ItvcErrorHandler,
                                              val reportDeadlinesService: ReportDeadlinesService,
                                              implicit val appConfig: FrontendAppConfig,
                                              implicit val ec: ExecutionContext,
                                              implicit val messagesApi: MessagesApi
                                     ) extends BaseController with FeatureSwitching {

  val getPreviousObligations: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>
      if(isEnabled(ObligationsPage)) {
        reportDeadlinesService.getReportDeadlines(previous = true).map {
          case previousObligations: ObligationsModel => Ok(views.html.previousObligations(previousObligations))
          case _ => Ok(views.html.previousObligations(ObligationsModel(List())))
        }
      } else {
        Future.successful(Redirect(controllers.routes.HomeController.home()))
      }
  }

}