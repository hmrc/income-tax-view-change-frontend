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
import models.financialDetails.Repayment
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CreditService @Inject()(val financialDetailsConnector: FinancialDetailsConnector,
                              implicit val dateService: DateServiceInterface)
                             (implicit ec: ExecutionContext, implicit val appConfig: FrontendAppConfig) {

  def getAllCredits(implicit user: MtdItUser[_],
                    hc: HeaderCarrier): Future[CreditsModel] = {

    Logger("application").debug(
      s"Requesting Financial Details for all periods for mtditid: ${user.mtditid}")

    val from = Math.max(user.incomeSources.orderedTaxYearsByYearOfMigration.min, dateService.getCurrentTaxYearEnd - {appConfig.api1553MaxYears - 1})
    val to = user.incomeSources.orderedTaxYearsByYearOfMigration.max
    Logger("application").debug(s"Getting financial details for TaxYears: ${from} - ${to}")

    for {
      taxYearFrom <- Future.fromTry(Try(TaxYear.forYearEnd(from)))
      taxYearTo <- Future.fromTry(Try(TaxYear.forYearEnd(to)))
      response <- financialDetailsConnector.getCreditsAndRefund(taxYearFrom, taxYearTo, user.nino)
    } yield response match {
      case Right(financialDetails: CreditsModel) => financialDetails
      case Left(error: ErrorModel) if error.code != NOT_FOUND =>
        throw new Exception("Error response while getting Unpaid financial details")
      case _ => CreditsModel(0, 0, Nil)
    }
  }
}
