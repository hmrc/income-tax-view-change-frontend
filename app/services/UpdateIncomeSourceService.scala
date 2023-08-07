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
import exceptions.MissingSessionKey
import forms.utils.SessionKeys.ceaseUKPropertyEndDate
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponse, UpdateIncomeSourceResponseModel}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateIncomeSourceService @Inject()(connector: IncomeTaxViewChangeConnector) {

  //TODO: We should use updateCessationDatev2 method
  def updateCessationDate(implicit request: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Exception, UpdateIncomeSourceResponse]] = {
    val nino: String = request.nino
    val incomeSourceId: Option[String] = request.incomeSources.properties.filter(_.isUkProperty).map(_.incomeSourceId).headOption
    request.session.get(ceaseUKPropertyEndDate) match {
      case Some(date) =>
        connector.updateCessationDate(
          nino = nino,
          incomeSourceId = incomeSourceId.get,
          cessationDate = Some(LocalDate.parse(date))).map(Right(_))
      case _ => Future.successful(Left(MissingSessionKey(ceaseUKPropertyEndDate)))
    }

  }

  def updateCessationDatev2(nino: String, incomeSourceId: String, cessationDate: String)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[UpdateIncomeSourceError, UpdateIncomeSourceSuccess]] = {
    connector.updateCessationDate(
      nino = nino,
      incomeSourceId = incomeSourceId,
      cessationDate = Some(LocalDate.parse(cessationDate))
    ) map {
      case _: UpdateIncomeSourceResponseModel => Right(UpdateIncomeSourceSuccess(incomeSourceId))
      case _ => Left(UpdateIncomeSourceError("Failed to update cessationDate"))
    }
  }

  def updateTaxYearSpecific(nino: String, incomeSourceId: String, taxYearSpecific: List[TaxYearSpecific])
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpdateIncomeSourceResponse] = {
    connector.updateIncomeSourceTaxYearSpecific(nino = nino, incomeSourceId = incomeSourceId, taxYearSpecific)
  }

}

case class UpdateIncomeSourceError(reason: String)

case class UpdateIncomeSourceSuccess(incomeSourceId: String)