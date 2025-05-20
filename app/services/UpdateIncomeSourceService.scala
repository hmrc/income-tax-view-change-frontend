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

import connectors.UpdateIncomeSourceConnector
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponse, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateIncomeSourceService @Inject()(updateIncomeSourceConnector: UpdateIncomeSourceConnector) {

  def updateCessationDate(nino: String, incomeSourceId: String, cessationDate: LocalDate)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[UpdateIncomeSourceResponseError, UpdateIncomeSourceSuccess]] = {
    updateIncomeSourceConnector.updateCessationDate(
      nino = nino,
      incomeSourceId = incomeSourceId,
      cessationDate = Some(cessationDate)
    ) map {
      case _: UpdateIncomeSourceResponseModel => Right(UpdateIncomeSourceSuccess(incomeSourceId))
      case error: UpdateIncomeSourceResponseError => Left(error)
    }
  }

  def updateTaxYearSpecific(nino: String, incomeSourceId: String, taxYearSpecific: TaxYearSpecific)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpdateIncomeSourceResponse] = {
    updateIncomeSourceConnector.updateIncomeSourceTaxYearSpecific(nino = nino, incomeSourceId = incomeSourceId, taxYearSpecific).map {
      case res: UpdateIncomeSourceResponseModel =>
        Logger("application").info(s"Updated tax year specific reporting method : $res")
        res
      case err: UpdateIncomeSourceResponseError =>
        Logger("application").error(s"Failed to Updated tax year specific reporting method : $err")
        err
    }
  }

}

case class UpdateIncomeSourceSuccess(incomeSourceId: String)
