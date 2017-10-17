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

import models._
import play.api.Logger
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.ImplicitListMethods

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@Singleton
class BTAPartialService @Inject()(val obligationsService: ObligationsService, val financialDataService: FinancialDataService) extends ImplicitListMethods {

  def getNextObligation(nino: String, incomeSources: IncomeSourcesModel)(implicit hc: HeaderCarrier): Future[ObligationsResponseModel] = {
    for{
      biz <- incomeSources.businessSources
      prop <- incomeSources.propertySource
    } yield (biz,prop) match {
      case (b: ObligationsModel, p: ObligationsModel) => getMostRecentDueDate(ObligationsModel(b.obligations ++ p.obligations))
      case (b: ObligationsModel, _) => getMostRecentDueDate(ObligationsModel(b.obligations))
      case (_, p: ObligationsModel) => getMostRecentDueDate(ObligationsModel(p.obligations))
      case (_,_) =>
        Logger.warn("[BTAPartialService][getNextObligation] - No Obligations obtained")
        ObligationsErrorModel(500, "Could not retrieve obligations")
    }
  }

  def getEstimate(nino: String, year: Int)(implicit headerCarrier: HeaderCarrier): Future[LastTaxCalculationResponseModel] = {
    financialDataService.getLastEstimatedTaxCalculation(nino, year) map {
      case calc: LastTaxCalculation => calc
      case NoLastTaxCalculation => NoLastTaxCalculation
      case error: LastTaxCalculationError =>
        Logger.warn("[BTAPartialService][getNextObligation] - No LastCalc data retrieved")
        error
    }
  }

  private[BTAPartialService] def getMostRecentDueDate(model: ObligationsModel): ObligationModel = {
    if(!model.obligations.exists(!_.met)){
      model.obligations.reduceLeft((x,y) => if(x.due isAfter y.due) x else y)
    } else {
      model.obligations.filter(!_.met).reduceLeft((x,y) => if(x.due isBefore y.due) x else y)
    }
  }

}
