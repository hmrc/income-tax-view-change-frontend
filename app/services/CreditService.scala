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
import config.FrontendAppConfig
import connectors.FinancialDetailsConnector
import models.core.ErrorModel
import models.creditsandrefunds.CreditsModel
import play.api.Logger
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreditService @Inject()(val financialDetailsConnector: FinancialDetailsConnector,
                              implicit val dateService: DateServiceInterface)
                             (implicit ec: ExecutionContext, implicit val appConfig: FrontendAppConfig) {
  def getAllCredits(implicit user: MtdItUser[_],
                    hc: HeaderCarrier): Future[CreditsModel] = {

    val mergeCreditAndRefundModels = (x: CreditsModel, y: CreditsModel) =>
      x.copy(transactions = x.transactions :++ y.transactions)

    Logger("application").debug(
      s"Requesting Financial Details for all periods for mtditid: ${user.mtditid}")

    Future.sequence(
        user.incomeSources.orderedTaxYearsByYearOfMigration.map { taxYear =>
          Logger("application").debug(s"Getting financial details for TaxYear: ${taxYear}")
          financialDetailsConnector.getCreditsAndRefund(taxYear, user.nino).map {
            case Right(financialDetails: CreditsModel) => Some(financialDetails)
            case Left(error: ErrorModel) if error.code != NOT_FOUND =>
              throw new Exception("Error response while getting Unpaid financial details")
            case _ => None
          }
      })
      .map(_.flatten)
      .map(_.reduce(mergeCreditAndRefundModels))
  }
}
