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

import auth.MtdItUser
import connectors.IncomeTaxViewChangeConnector
import models.calculationList.{CalculationListErrorModel, CalculationListModel, CalculationListResponseModel}
import models.core.Nino
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculationListService @Inject()(incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector)
                                      (implicit ec: ExecutionContext)  {

  def getLegacyCalculationList(nino: Nino, taxYearEnd: String)
                              (implicit headerCarrier: HeaderCarrier): Future[CalculationListResponseModel] = {
    Logger("application").debug(s"[CalculationService][getLatestCalculation] - " +
      s"Requesting legacy calculation list (1404) data from the backend with nino / taxYearEnd: ${nino.value} - $taxYearEnd")
    incomeTaxViewChangeConnector.getLegacyCalculationList(nino, taxYearEnd)
  }

  def getCalculationList(nino: Nino, taxYearRange: String)
                        (implicit headerCarrier: HeaderCarrier): Future[CalculationListResponseModel] = {
    Logger("application").debug(s"[CalculationService][getLatestCalculation] - " +
      s"Requesting calculation list (1896) data from the backend with nino / taxYearRange: ${nino.value} - $taxYearRange")
    incomeTaxViewChangeConnector.getCalculationList(nino, taxYearRange)
  }

  def isTaxYearCrystallised(taxYear: Int)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Boolean]] = {
    if (taxYear <= 2023) {
      incomeTaxViewChangeConnector.getLegacyCalculationList(Nino(user.nino), taxYear.toString).flatMap {
        case res: CalculationListModel => Future.successful(res.crystallised)
        case err: CalculationListErrorModel => Future.failed(new InternalServerException(err.message))
      }
    } else {
      val taxYearRange = s"${(taxYear - 1).toString.substring(2)}-${taxYear.toString.substring(2)}"
      incomeTaxViewChangeConnector.getCalculationList(Nino(user.nino), taxYearRange).flatMap {
        case res: CalculationListModel => Future.successful(res.crystallised)
        case err: CalculationListErrorModel => Future.failed(new InternalServerException(err.message))
      }
    }

  }
}
