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
import models.financialDetails.{BalanceDetails, FinancialDetailsErrorModel, FinancialDetailsModel, FinancialDetailsResponseModel}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreditService @Inject()(val financialDetailsService: FinancialDetailsService)
                             (implicit ec: ExecutionContext, implicit val appConfig: FrontendAppConfig) {

  def getCreditCharges()(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[List[FinancialDetailsModel]] = {
    financialDetailsService.getAllCreditChargesandPaymentsFinancialDetails.map {
      case financialDetails if financialDetails.exists(_.isInstanceOf[FinancialDetailsErrorModel]) =>
        throw new Exception("Error response while getting Unpaid financial details")
      case financialDetails: List[FinancialDetailsResponseModel] => financialDetails.asInstanceOf[List[FinancialDetailsModel]]
    }
  }
}
object CreditService {
  def maybeBalanceDetails(financialDetailsModels: List[FinancialDetailsModel]): Option[BalanceDetails] =
    financialDetailsModels match {
      case financialDetailsModel: List[FinancialDetailsModel] if financialDetailsModels.nonEmpty =>
        financialDetailsModel.headOption.map(balance => balance.balanceDetails)
      case _ => None
    }
}
