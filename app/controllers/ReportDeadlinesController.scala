/*
 * Copyright 2017 HM Revenue & Customs
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
import controllers.predicates.AsyncActionPredicate
import models.IncomeSourcesModel
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.ServiceInfoPartialService
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class ReportDeadlinesController @Inject()(implicit val config: FrontendAppConfig,
                                          implicit val messagesApi: MessagesApi,
                                          val actionPredicate: AsyncActionPredicate,
                                          val serviceInfoPartialService: ServiceInfoPartialService,
                                          val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                          val auditingService: AuditingService
                                     ) extends BaseController {
  import itvcHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest

  val getReportDeadlines: Action[AnyContent] = actionPredicate.async {
    implicit request =>
      implicit user =>
        implicit sources =>
          submitData(user, sources)
          serviceInfoPartialService.serviceInfoPartial.map { implicit serviceInfo =>
            Ok(views.html.report_deadlines(sources))
          }
  }

  private def submitData(user: MtdItUser, sources: IncomeSourcesModel)(implicit hc: HeaderCarrier): Unit =
    auditingService.audit(ReportDeadlinesAuditModel(user, sources), controllers.routes.ReportDeadlinesController.getReportDeadlines().url)

}
