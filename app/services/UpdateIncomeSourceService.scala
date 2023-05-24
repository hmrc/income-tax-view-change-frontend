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
import models.updateIncomeSource.UpdateIncomeSourceResponse
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateIncomeSourceService @Inject()(connector: IncomeTaxViewChangeConnector) {

  def updateCessationDate(implicit request: MtdItUser[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Exception,UpdateIncomeSourceResponse]] = {
    val nino: String = request.nino
    val incomeSourceId: Option[String] = request.incomeSources.properties.filter(_.isUkProperty).flatMap(_.incomeSourceId).headOption
    val ceaseUKPropertyEndDateSessionKey = "ceaseUKPropertyEndDate"
    request.session.get(ceaseUKPropertyEndDateSessionKey) match {
      case Some(date) =>
        connector.updateCessationDate(
          nino = nino,
          incomeSourceId = incomeSourceId.get,
          cessationDate = Some(LocalDate.parse(date))).map(Right(_))
      case _ => Future.successful(Left(new Exception(s"Missing session field - $ceaseUKPropertyEndDateSessionKey")))
    }

  }

}
