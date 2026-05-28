/*
 * Copyright 2023 HM Revenue & Customs
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

import common.auth.MtdItUser
import common.services.DateService
import connectors.CalculationListConnector
import models.calculationList.{CalculationListErrorModel, CalculationListModel, CalculationListResponseModel}
import models.core.Nino
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculationListService @Inject()(calculationListConnector: CalculationListConnector, dateService: DateService)
                                      (implicit ec: ExecutionContext) {

  def getCalculationList(nino: Nino, taxYear: Int)
                        (implicit headerCarrier: HeaderCarrier): Future[CalculationListResponseModel] = {
    Logger("application").debug("" +
      s"Requesting calculation list (1896) data from the backend with nino / taxYear: ${nino.value} - $taxYear")
    calculationListConnector.getCalculationList(nino, taxYear)
  }

  private def getCrystallisationResult(user: MtdItUser[_], taxYear: Int)(implicit hc: HeaderCarrier): Future[Option[Boolean]] = {
    calculationListConnector.getCalculationList(Nino(user.nino), taxYear).flatMap {
      case res: CalculationListModel => Future.successful(res.crystallised)
      case err: CalculationListErrorModel if err.code == 404 => Future.successful(Some(false))
      case err: CalculationListErrorModel =>
        Logger("application").error(s"CalculationListService#getLegacyCrystallisationResult: error received: ${err.code} - ${err.message}")
        Future.failed(new InternalServerException(err.message)) // treat as non-crystallised instead of throwing
    }
  }

  def isTaxYearCrystallised(taxYear: TaxYear)(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Boolean] = {
    determineTaxYearCrystallised(taxYear.endYear)
  }

  def determineTaxYearCrystallised(taxYear: Int)(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Boolean] = {

    val currentTaxYearEnd = dateService.getCurrentTaxYearEnd
    val futureTaxYear = taxYear >= currentTaxYearEnd
    val isCrystallised = if futureTaxYear 
      then Future.successful(Some(false)) /* tax year cannot be crystallised unless it is in the past */
      else getCrystallisationResult(user, currentTaxYearEnd)
    
    isCrystallised.map(_.contains(true))
  }
}
