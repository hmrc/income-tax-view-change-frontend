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

    val mergeCreditAndRefundModels = (x: CreditsModel, y: CreditsModel) =>
      x.copy(transactions = x.transactions :++ y.transactions.filterNot(item => item.transactionType == Repayment || x.transactions.map(_.transactionId).contains(item.transactionId)))

    Logger("application").debug(
      s"Requesting Financial Details for all periods for mtditid: ${user.mtditid}")

    Future.sequence(
        user.incomeSources.orderedTaxYearsByYearOfMigration.map { taxYearInt =>
          Logger("application").debug(s"Getting financial details for TaxYear: ${taxYearInt}")
          for {
            taxYear <- Future.fromTry(Try(TaxYear.forYearEnd(taxYearInt)))
            response <- financialDetailsConnector.getCreditsAndRefund(taxYear, user.nino)
          } yield response match {
            case Right(financialDetails: CreditsModel) => Some(financialDetails)
            case Left(error: ErrorModel) if error.code != NOT_FOUND =>
              throw new Exception("Error response while getting Unpaid financial details")
            case _ => None
          }
        })
      .map(_.flatten)
      .map(_.reduceOption(mergeCreditAndRefundModels).getOrElse(CreditsModel(0, 0, 0, 0, Nil)))
  }

  def getAllCreditsV2(implicit user: MtdItUser[_],
                      hc: HeaderCarrier): Future[CreditsModel] = {

    Logger("application").debug(
      s"Requesting Financial Details for all periods for mtditid: ${user.mtditid}")

    val (from, to) = (user.incomeSources.orderedTaxYearsByYearOfMigration.min, user.incomeSources.orderedTaxYearsByYearOfMigration.max)
    Logger("application").debug(s"Getting financial details for TaxYears: ${from} - ${to}")

    for {
      taxYearFrom <- Future.fromTry(Try(TaxYear.forYearEnd(from)))
      taxYearTo <- Future.fromTry(Try(TaxYear.forYearEnd(to)))
      response <- financialDetailsConnector.getCreditsAndRefund(taxYearFrom, taxYearTo, user.nino)
    } yield response match {
      case Right(financialDetails: CreditsModel) => financialDetails
      case Left(error: ErrorModel) if error.code != NOT_FOUND =>
        throw new Exception("Error response while getting Unpaid financial details")
      case _ => CreditsModel(0, 0, 0, 0, Nil)
    }
  }
}
