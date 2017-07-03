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

import auth.MtdItUser
import config.AppConfig
import controllers.predicates.AuthenticationPredicate
import models.{BusinessDetailsErrorModel, BusinessDetailsModel, LastTaxCalculation, LastTaxCalculationError}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{BusinessDetailsService, FinancialDataService}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class EstimatedTaxLiabilityController @Inject()(implicit val config: AppConfig,
                                                implicit val messagesApi: MessagesApi,
                                                val authentication: AuthenticationPredicate,
                                                val estimatedTaxLiabilityService: FinancialDataService,
                                                val businessDetailsService: BusinessDetailsService
                                               ) extends BaseController {

  // TODO: Properties will always be 2017/18 for MVP. This needs to be enhanced post-MVP.
  val propertiesTaxYear = 2018

  val getEstimatedTaxLiability: Action[AnyContent] = authentication.async {
    implicit request => implicit user => {
      Logger.debug(s"[EstimatedTaxLiabilityController][getEstimatedTaxLiability] Calling Business Details Service with NINO: ${user.nino}")
      businessDetailsService.getBusinessDetails(user.nino) flatMap {
        case success: BusinessDetailsModel if success.business.nonEmpty => {
          Logger.debug("[EstimatedTaxLiabilityController][getEstimatedTaxLiability] Business Details found, using Tax Year.")
          getAndRenderEstimatedLiability(success.business.head.accountingPeriod.determineTaxYear)
        }
        case success: BusinessDetailsModel => {
          Logger.debug("[EstimatedTaxLiabilityController][getEstimatedTaxLiability] Business Details not found, assumed property - fixed Tax Year.")
          getAndRenderEstimatedLiability(propertiesTaxYear)
        }
        case error: BusinessDetailsErrorModel => {
          Logger.debug("[EstimatedTaxLiabilityController][getEstimatedTaxLiability] Business Details Error.")
          Future.successful(showInternalServerError)
        }
      }
    }
  }

  private def getAndRenderEstimatedLiability(taxYear: Int)(implicit hc: HeaderCarrier, request: Request[AnyContent], user: MtdItUser): Future[Result] = {
    Logger.debug(s"[EstimatedTaxLiabilityController][getEstimatedTaxLiability] Calling Estimated Tax Liability Service with NINO: ${user.nino}")
    estimatedTaxLiabilityService.getLastEstimatedTaxCalculation(user.nino, taxYear) map {
      case success: LastTaxCalculation =>
        Logger.debug(s"[EstimatedTaxLiabilityController][getEstimatedTaxLiability] Success Response: $success")
        Ok(views.html.estimatedTaxLiability(success.calcAmount, taxYear))
      case failure: LastTaxCalculationError =>
        Logger.warn(s"[EstimatedTaxLiabilityController][getEstimatedTaxLiability] Error Response: Status=${failure.status}, Message=${failure.message}")
        showInternalServerError
    }
  }
}
