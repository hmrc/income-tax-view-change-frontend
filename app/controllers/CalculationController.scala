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
import audit.models.EstimatesAuditing.EstimatesAuditModel
import auth.MtdItUser
import config.{FrontendAppConfig, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.AsyncActionPredicate
import models._
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.{CalculationService, ServiceInfoPartialService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class CalculationController @Inject()(implicit val config: FrontendAppConfig,
                                      implicit val messagesApi: MessagesApi,
                                      val actionPredicate: AsyncActionPredicate,
                                      val calculationService: CalculationService,
                                      val serviceInfoPartialService: ServiceInfoPartialService,
                                      val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                      val auditingService: AuditingService
                                       ) extends BaseController {
  import itvcHeaderCarrierForPartialsConverter.headerCarrierEncryptingSessionCookieFromRequest

  val redirectToEarliestEstimatedTaxLiability: Action[AnyContent] = actionPredicate.async {
    implicit request =>
      implicit user =>
        implicit sources =>
          serviceInfoPartialService.serviceInfoPartial().map { implicit serviceInfo =>
            Redirect(controllers.routes.CalculationController.getFinancialData(sources.earliestTaxYear.get))
          }
  }

  val getFinancialData: Int => Action[AnyContent] = taxYear => actionPredicate.async {
    implicit request =>
      implicit user =>
        implicit sources =>
          serviceInfoPartialService.serviceInfoPartial().flatMap { implicit serviceInfo =>
            calculationService.getFinancialData(user.nino, taxYear).map {
              case calcDisplayModel: CalcDisplayModel =>
                submitData(user, sources, calcDisplayModel.calcAmount.toString)
                if(calcDisplayModel.crystalisedFlag.equals("Crystalised")){
                  Ok(views.html.estimatedTaxLiability(calcDisplayModel, taxYear))
                } else {
                  Ok(views.html.estimatedTaxLiability(calcDisplayModel, taxYear))
                }
              case CalcDisplayNoDataFound =>
                Logger.debug(s"[FinancialDataController][getFinancialData[$taxYear]] No last tax calculation data could be retrieved. Not found")
                submitData(user, sources, "No data found")
                NotFound(views.html.noEstimatedTaxLiability(taxYear))
              case CalcDisplayError =>
                Logger.debug(s"[FinancialDataController][getFinancialData[$taxYear]] No last tax calculation data could be retrieved. Downstream error")
                Ok(views.html.estimatedTaxLiabilityError(taxYear))
            }
          }
  }

  private def submitData(user: MtdItUser, sources: IncomeSourcesModel, estimate: String)(implicit hc: HeaderCarrier): Unit =
    auditingService.audit(
      EstimatesAuditModel(user, sources, estimate),
      controllers.routes.CalculationController.getFinancialData(sources.earliestTaxYear.get).url
    )

}
