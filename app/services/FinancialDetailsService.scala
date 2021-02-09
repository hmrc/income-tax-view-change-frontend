/*
 * Copyright 2021 HM Revenue & Customs
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

import auth.{MtdItUser, MtdItUserWithNino}
import config.FrontendAppConfig
import config.featureswitch.{API5, FeatureSwitching}
import connectors.IncomeTaxViewChangeConnector
import models.financialDetails.{FinancialDetailsErrorModel, FinancialDetailsModel, FinancialDetailsResponseModel}
import models.incomeSourceDetails.IncomeSourceDetailsResponse
import play.api.Logger
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialDetailsService @Inject()(val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector)
                                       (implicit val appConfig: FrontendAppConfig) extends FeatureSwitching {

  def getFinancialDetails(taxYear: Int)(implicit hc: HeaderCarrier, mtdUser: MtdItUser[_]): Future[FinancialDetailsResponseModel] = {
    incomeTaxViewChangeConnector.getFinancialDetails(taxYear)
  }

  def getAllFinancialDetails(implicit user: MtdItUser[_],
                             hc: HeaderCarrier, ec: ExecutionContext): Future[List[(Int, FinancialDetailsResponseModel)]] = {
    Logger.debug(
      s"[IncomeSourceDetailsService][getAllFinancialDetails] - Requesting Financial Details for all periods for mtditid: ${user.mtditid}")

    Future.sequence(user.incomeSources.orderedTaxYears(isEnabled(API5)).map {
      taxYear =>
        incomeTaxViewChangeConnector.getFinancialDetails(taxYear).map {
          case financialDetails: FinancialDetailsModel => Some((taxYear, financialDetails))
          case error: FinancialDetailsErrorModel if error.code != 404 => Some((taxYear, error))
          case _ => None
        }
    }
    ).map(_.flatten)
  }

  def getAllUnpaidFinancialDetails(implicit user: MtdItUser[_],
                                   hc: HeaderCarrier, ec: ExecutionContext): Future[List[FinancialDetailsResponseModel]] = {
    getAllFinancialDetails.map { chargesWithYears =>
      chargesWithYears.collect {
        case (_, errorModel: FinancialDetailsErrorModel) => errorModel
        case (_, financialDetails: FinancialDetailsModel) if !financialDetails.isAllPaid =>
          financialDetails.copy(
            financialDetails = financialDetails.financialDetails.filterNot(
              charge => charge.originalAmount.exists(_ <= 0) || charge.outstandingAmount.isEmpty)
          )
      }
    }
  }
}
