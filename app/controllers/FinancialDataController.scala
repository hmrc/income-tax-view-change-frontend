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

import config.AppConfig
import controllers.predicates.AsyncActionPredicate
import models._
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import services.FinancialDataService

import scala.concurrent.Future

@Singleton
class FinancialDataController @Inject()(implicit val config: AppConfig,
                                                implicit val messagesApi: MessagesApi,
                                                val actionPredicate: AsyncActionPredicate,
                                                val financialDataService: FinancialDataService
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
          Logger.debug("[FinancialDataController][redirectToEarliestEstimatedTaxLiability] No Income Sources.")
          Future.successful(showInternalServerError)
      }
  }

//  val getFinancialData: Int => Action[AnyContent] = taxYear => actionPredicate.async {
//    implicit request => implicit user => implicit sources =>
//      Logger.debug(s"[FinancialDataController][getFinancialData] Calling Financial Data Service with NINO: ${user.nino}")
//      for{
//        eTL <- financialDataService.getLastEstimatedTaxCalculation(user.nino, taxYear)
//        calc <- {
//          eTL match {
//            case eTLSuccess: LastTaxCalculation =>
//              Logger.debug(s"[FinancialDataController][getFinancialData] eTLSuccess Response: $eTLSuccess")
//              val a: Future[CalculationDataResponseModel] = financialDataService.getCalculationData(user.nino, eTLSuccess.calcID)
//            case eTLFailure: LastTaxCalculationError =>
//              Logger.warn(s"[FinancialDataController][getEstimatedTaxLiability] Error Response: Status=${eTLFailure.status}, Message=${eTLFailure.message}")
//              Future.successful(CalculationDataErrorModel())
//          }
//        }
//      } yield (eTL, calc) match {
//        case (eTLSuccess: LastTaxCalculation, calcSuccess: CalculationDataModel) =>
//          Logger.debug("[EstimatedTaxLiabilityController][getFinancialData] Successfully retrieved CalcDataModel & LastTaxCalc model - serving Html page")
//          Ok(views.html.estimatedTaxLiability(eTLSuccess.calcAmount, calcSuccess, taxYear))
//        case (eTLSuccess: LastTaxCalculation, calcFailure: CalculationDataErrorModel) =>
//          Logger.warn(s"[FinancialDataController][getFinancialData] Error Response: Status=${calcFailure.code}, Message=${calcFailure.message}")
//          showInternalServerError
//        case (eTLFailure: LastTaxCalculationError, _) =>
//          Logger.warn(s"[FinancialDataController][getFinancialData] Error Response: Status=${eTLFailure.status}, Message=${eTLFailure.message}")
//          showInternalServerError
//        case _ =>
//          Logger.warn("[FinancialDataController][getFinancialData] Unexpected Error!!")
//          showInternalServerError
//      }
//  }

  val getFinancialData: Int => Action[AnyContent] = taxYear => actionPredicate.async {
    implicit request => implicit user => implicit sources =>
      financialDataService.getFinancialData(user.nino, taxYear) map { (a,b) =>
        case (model: Some())  =>
      }
  }

  private[FinancialDataController] def redirectToYear(year: Int): Future[Result] =
    Future.successful(Redirect(controllers.routes.FinancialDataController.getFinancialData(year)))
}
