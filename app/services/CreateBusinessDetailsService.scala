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


import connectors.IncomeSourceConnector
import models.addIncomeSource.{AddIncomeSourceResponse, AddressDetails, BusinessDetails, IncomeSourceResponse}
import models.incomeSourceDetails.viewmodels.CheckBusinessDetailsViewModel
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


class CreateBusinessDetailsService @Inject()(val incomeSourceConnector: IncomeSourceConnector) {

def createBusinessDetails(mtditid: String, viewModel: CheckBusinessDetailsViewModel)
                         (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[Throwable, List[IncomeSourceResponse]]] = {
  val businessDetails =
    BusinessDetails(
      accountingPeriodStartDate = "",
      accountingPeriodEndDate = "",
      tradingName = "",
      addressDetails = AddressDetails(
        addressLine1 = "",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        countryCode = "GB",
        postalCode = "SE13 4ER"
      ),
      typeOfBusiness = None,
      tradingStartDate = "",
      cashOrAccrualsFlag = "Cash",
      cessationDate = None,
      cessationReason = None
    )
  for {
    res <- incomeSourceConnector.create(mtditid, businessDetails)
  } yield res match {
    case Right(success) =>
      Right(success)
    case Left(ex) =>
      Left(new Error(("Failed to created incomeSources: $ex")))
  }

}

}