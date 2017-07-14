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

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import play.api.Logger
import models._
import uk.gov.hmrc.play.http.HeaderCarrier

@Singleton
class BTAPartialService @Inject()(val obligationsService: ObligationsService, val financialDataService: FinancialDataService) {

  def getObligations(nino: String, businessIncomeSource: Option[BusinessIncomeModel])(implicit hc: HeaderCarrier) = {
    for{
      ob <- obligationsService.getBusinessObligations(nino, businessIncomeSource) match {
        case b: ObligationsModel => getMostRecentDueDate(b)
        case _ => _
      }
      prop <- obligationsService.getPropertyObligations(nino) match {
        case p: ObligationsModel => getMostRecentDueDate(p)
        case _ => _
      }
    } yield (ob,prop) match {
      case (b: LocalDate, p: LocalDate) => if(b.isBefore(p)) b else p
      case (b: LocalDate, _) => b
      case (_, p: LocalDate) => p
      case (_,_) =>
        Logger.warn("[BTAPartialService][getObligations] - No Obligations obtained")
        //TODO something better than this:
        LocalDate.parse("2100-01-01")
    }
  }

  def getEstimate(nino: String, year: Int) = {
    financialDataService.getLastEstimatedTaxCalculation(nino, year) match {
      case calc: LastTaxCalculation => calc.calcAmount
      case _ =>
        Logger.warn("[BTAPartialService][getObligations] - No LastCalc data retrieved")
        //TODO something better than this:
        -1
    }
  }

  private[BTAPartialService] def getMostRecentDueDate(model: ObligationsModel): LocalDate = {
    model.obligations.filter(_.met == false)
      .reduceLeft((x,y) => if(x.due isBefore y.due) x else y).due
  }

}
