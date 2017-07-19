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

package services

import javax.inject.{Inject, Singleton}
import play.api.Logger
import models._
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@Singleton
class BTAPartialService @Inject()(val obligationsService: ObligationsService, val financialDataService: FinancialDataService) {

  def getObligations(nino: String, businessIncomeSource: Option[BusinessIncomeModel])(implicit hc: HeaderCarrier): Future[ObligationsResponseModel] = {
    for{
      biz <- obligationsService.getBusinessObligations(nino, businessIncomeSource) map {
        case b: ObligationsModel => getMostRecentDueDate(b)
        case _ => ObligationsErrorModel(500, "model... bad")
      }
      prop <- obligationsService.getPropertyObligations(nino) map {
        case p: ObligationsModel => getMostRecentDueDate(p)
        case _ => ObligationsErrorModel(500, "model... bad")
      }
    } yield (biz,prop) match {
      case (b: ObligationModel, p: ObligationModel) => if(b.due.isBefore(p.due)) b else p
      case (b: ObligationModel, _) => b
      case (_, p: ObligationModel) => p
      case (_,_) =>
        Logger.warn("[BTAPartialService][getObligations] - No Obligations obtained")
        ObligationsErrorModel(500, "Could not retrieve obligations")
    }
  }

  def getEstimate(nino: String, year: Int)(implicit headerCarrier: HeaderCarrier): Future[LastTaxCalculationResponseModel] = {
    financialDataService.getLastEstimatedTaxCalculation(nino, year) map {
      case calc: LastTaxCalculation => calc
      case NoLastTaxCalculation => NoLastTaxCalculation
      case error: LastTaxCalculationError =>
        Logger.warn("[BTAPartialService][getObligations] - No LastCalc data retrieved")
        error
    }
  }

  private[BTAPartialService] def getMostRecentDueDate(model: ObligationsModel): ObligationModel = {
    model.obligations.filter(_.met == false)
      .reduceLeft((x,y) => if(x.due isBefore y.due) x else y)
  }

}
