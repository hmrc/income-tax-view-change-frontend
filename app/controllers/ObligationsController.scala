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

import config.{AppConfig, ItvcHeaderCarrierForPartialsConverter}
import audit.AuditingService
import audit.models.ObligationsAuditing.ObligationsAuditModel
import auth.MtdItUser
import config.AppConfig
import controllers.predicates.AsyncActionPredicate
import models.{IncomeSourcesModel, ObligationsModel}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.ObligationsService
import uk.gov.hmrc.play.http.HeaderCarrier
import services.{ObligationsService, ServiceInfoPartialService}

@Singleton
class ObligationsController @Inject()(implicit val config: AppConfig,
                                      implicit val messagesApi: MessagesApi,
                                      val actionPredicate: AsyncActionPredicate,
                                      val obligationsService: ObligationsService,
                                      val serviceInfoPartialService: ServiceInfoPartialService,
                                      val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                      val auditingService: AuditingService
                                     ) extends BaseController {
  import itvcHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest

  val getObligations: Action[AnyContent] = actionPredicate.async {
    implicit request => implicit user => implicit sources =>
      submitData(user, sources)
      serviceInfoPartialService.serviceInfoPartial.flatMap { implicit serviceInfo =>
        for {
          business <- obligationsService.getBusinessObligations(user.nino, sources.businessDetails)
          property <- obligationsService.getPropertyObligations(user.nino)
        } yield (business, property) match {
          case (businessSuccess: ObligationsModel, propertySuccess: ObligationsModel) =>
            Logger.debug("[ObligationsController][getObligations] Business & Property Obligations retrieved. Serving HTML page")
            Ok(views.html.obligations(Some(businessSuccess), Some(propertySuccess)))
          case (businessSuccess: ObligationsModel, _) =>
            Logger.debug("[ObligationsController][getObligations] Business Obligations retrieved. Serving HTML page")
            Ok(views.html.obligations(Some(businessSuccess), None))
          case (_, propertySuccess: ObligationsModel) =>
            Logger.debug("[ObligationsController][getObligations] Property Obligations retrieved. Serving HTML page")
            Ok(views.html.obligations(None, Some(propertySuccess)))
          case (_, _) =>
            Logger.debug("[ObligationsController][getObligations] No obligations retrieved. Throwing ISE")
            showInternalServerError
      }
  }
}

  private def submitData(user: MtdItUser, sources: IncomeSourcesModel)(implicit hc: HeaderCarrier): Unit =
    auditingService.audit(ObligationsAuditModel(user, sources), controllers.routes.ObligationsController.getObligations().url)

}
