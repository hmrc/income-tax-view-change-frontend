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
import common.models.core.Nino
import common.models.incomeSourceDetails.TaxYear
import common.services.DateService
import connectors.CalculationListConnector
import models.calculationList.{CalculationListErrorModel, CalculationListModel, CalculationListResponseModel}
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculationListService @Inject()(calculationListConnector: CalculationListConnector, dateService: DateService)
                                      (implicit ec: ExecutionContext) {

  def getCalculationList(nino: Nino, taxYearRange: String)
                        (implicit headerCarrier: HeaderCarrier, user: MtdItUser[_]): Future[CalculationListResponseModel] = {
    Logger("application").debug("" +
      s"Requesting calculation list (1896) data from the backend with nino / taxYearRange: ${nino.value} - $taxYearRange")
    calculationListConnector.getCalculationList(nino, taxYearRange, user.mtditid)
  }

  private def getLegacyCrystallisationResult(user: MtdItUser[_], taxYear: Int)(implicit hc: HeaderCarrier): Future[Option[Boolean]] = {
    calculationListConnector.getCalculationList(Nino(user.nino), taxYear.toString,user.mtditid).flatMap {
      case res: CalculationListModel => Future.successful(res.crystallised)
      case err: CalculationListErrorModel if err.code == 404 => Future.successful(Some(false))
      case err: CalculationListErrorModel => Future.failed(new InternalServerException(err.message))
    }
  }

  private def getTYSCrystallisationResult(user: MtdItUser[_], taxYear: Int)(implicit hc: HeaderCarrier): Future[Option[Boolean]] = {
    calculationListConnector.getCalculationList(Nino(user.nino), taxYear.toString, user.mtditid).flatMap {
      case res: CalculationListModel => Future.successful(res.crystallised)
      case err: CalculationListErrorModel if err.code == 404 => Future.successful(Some(false))
      case err: CalculationListErrorModel => Future.successful(Some(false))
    }
  }

  def isTaxYearCrystallised(taxYear: TaxYear)(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Boolean] = {
    determineTaxYearCrystallised(taxYear.endYear)
  }

  def determineTaxYearCrystallised(taxYear: Int)(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Boolean] = {

    val currentTaxYearEnd = dateService.getCurrentTaxYearEnd
    val futureTaxYear = taxYear >= currentTaxYearEnd
    val legacyTaxYear = taxYear <= 2023
    val isCrystallised = (futureTaxYear, legacyTaxYear) match {
      case (true, _) => Future.successful(Some(false)) /* tax year cannot be crystallised unless it is in the past */
      case (_, true) => getLegacyCrystallisationResult(user, taxYear)
      case (_, false) => getTYSCrystallisationResult(user, taxYear)
    }

    isCrystallised.map(_.contains(true))
  }
}
