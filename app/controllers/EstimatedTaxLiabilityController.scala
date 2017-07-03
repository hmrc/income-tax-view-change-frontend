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
import controllers.predicates.AsyncActionPredicate
import models.{BusinessIncomeModel, LastTaxCalculation, LastTaxCalculationError}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.EstimatedTaxLiabilityService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class EstimatedTaxLiabilityController @Inject()(implicit val config: AppConfig,
                                                implicit val messagesApi: MessagesApi,
                                                val actionPredicate: AsyncActionPredicate,
                                                val estimatedTaxLiabilityService: EstimatedTaxLiabilityService
                                               ) extends BaseController {

  val redirectToEarliestEstimatedTaxLiability: Action[AnyContent] = actionPredicate.async {
    implicit request => implicit user => implicit sources =>
      (sources.businessDetails, sources.propertyDetails) match {
        case (Some(business), Some(property)) =>
          if (property.accountingPeriod.determineTaxYear < business.accountingPeriod.determineTaxYear) {
            redirectToYear(property.accountingPeriod.determineTaxYear)
          } else {
            redirectToYear(business.accountingPeriod.determineTaxYear)
          }
        case (Some(business), None) => redirectToYear(business.accountingPeriod.determineTaxYear)
        case (None, Some(property)) => redirectToYear(property.accountingPeriod.determineTaxYear)
        case (_, _) =>
          Logger.debug("[EstimatedTaxLiabilityController][redirectToEarliestEstimatedTaxLiability] No Income Sources.")
          Future.successful(showInternalServerError)
      }
  }

  val getEstimatedTaxLiability: Int => Action[AnyContent] = taxYear => actionPredicate.async {
    implicit request => implicit user => implicit sources =>
      getAndRenderEstimatedLiability(taxYear)
  }

  private[EstimatedTaxLiabilityController] def redirectToYear(year: Int): Future[Result] =
    Future.successful(Redirect(controllers.routes.EstimatedTaxLiabilityController.getEstimatedTaxLiability(year)))

  private[EstimatedTaxLiabilityController] def getAndRenderEstimatedLiability(taxYear: Int)(implicit hc: HeaderCarrier,
                                                                                            request: Request[AnyContent],
                                                                                            user: MtdItUser): Future[Result] = {
    Logger.debug(s"[EstimatedTaxLiabilityController][redirectToEarliestEstimatedTaxLiability] Calling Estimated Tax Liability Service with NINO: ${user.nino}")
    estimatedTaxLiabilityService.getLastEstimatedTaxCalculation(user.nino, taxYear) map {
      case success: LastTaxCalculation =>
        Logger.debug(s"[EstimatedTaxLiabilityController][redirectToEarliestEstimatedTaxLiability] Success Response: $success")
        Ok(views.html.estimatedTaxLiability(success.calcAmount, taxYear))
      case failure: LastTaxCalculationError =>
        Logger.warn(s"[EstimatedTaxLiabilityController][redirectToEarliestEstimatedTaxLiability] " +
          s"Error Response: Status=${failure.status}, Message=${failure.message}")
        showInternalServerError
    }
  }
}
