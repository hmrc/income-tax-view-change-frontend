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

import audit.AuditingService
import audit.models.ReportDeadlinesAuditing.ReportDeadlinesAuditModel
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, ObligationsPage, ReportDeadlines}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import javax.inject.{Inject, Singleton}
import models.incomeSourcesWithDeadlines.IncomeSourcesWithDeadlinesModel
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import services.ReportDeadlinesService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportDeadlinesController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                          val authenticate: AuthenticationPredicate,
                                          val retrieveNino: NinoPredicate,
                                          val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                          val auditingService: AuditingService,
                                          val reportDeadlinesService: ReportDeadlinesService,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          implicit val appConfig: FrontendAppConfig,
                                          implicit val messagesApi: MessagesApi,
                                          implicit val ec: ExecutionContext
                                     ) extends BaseController with FeatureSwitching{

  val getReportDeadlines: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>
      if (isEnabled(ReportDeadlines)) renderView else fRedirectToHome
  }

  private def renderView[A](implicit user: MtdItUser[A]): Future[Result] = {
    if(user.incomeSources.hasBusinessIncome || user.incomeSources.hasPropertyIncome) {
      auditReportDeadlines(user)
      reportDeadlinesService.createIncomeSourcesWithDeadlinesModel(user.incomeSources).map {
        case withDeadlines: IncomeSourcesWithDeadlinesModel if !withDeadlines.hasAnyServerErrors =>
          Ok(views.html.obligations(withDeadlines))
        case _=>
          Logger.error(s"[ReportDeadlinesController][renderView] error occurred while trying to render report Deadlines page")
          itvcErrorHandler.showInternalServerError
      }
    } else {
      Future.successful(Ok(views.html.noReportDeadlines()))
    }
  }

  private def auditReportDeadlines[A](user: MtdItUser[A])(implicit hc: HeaderCarrier): Unit =
    auditingService.audit(ReportDeadlinesAuditModel(user), Some(controllers.routes.ReportDeadlinesController.getReportDeadlines().url))

}
