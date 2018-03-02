/*
 * Copyright 2018 HM Revenue & Customs
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

import audit.AuditingService
import audit.models.ReportDeadlinesAuditing.ReportDeadlinesAuditModel
import auth.MtdItUser
import config.{FrontendAppConfig, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class ReportDeadlinesController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                          val authenticate: AuthenticationPredicate,
                                          val retrieveNino: NinoPredicate,
                                          val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                          val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                          val auditingService: AuditingService,
                                          implicit val config: FrontendAppConfig,
                                          implicit val messagesApi: MessagesApi
                                     ) extends BaseController {

  val getReportDeadlines: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>
      if (config.features.reportDeadlinesEnabled()) renderView else redirectToHome
  }

  private def redirectToHome: Future[Result] = Future.successful(Redirect(controllers.routes.HomeController.home().url))

  private def renderView[A](implicit user: MtdItUser[A]): Future[Result] = {
    auditReportDeadlines(user)
    Future.successful(Ok(views.html.report_deadlines(user.incomeSources)))
  }

  private def auditReportDeadlines[A](user: MtdItUser[A])(implicit hc: HeaderCarrier): Unit =
    auditingService.audit(ReportDeadlinesAuditModel(user), controllers.routes.ReportDeadlinesController.getReportDeadlines().url)

}
