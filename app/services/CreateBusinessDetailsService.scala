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
import connectors.IncomeSourceConnector
import models.addIncomeSource.{AddBusinessIncomeSourcesRequest, AddressDetails, BusinessDetails, IncomeSourceResponse}
import models.incomeSourceDetails.viewmodels.CheckBusinessDetailsViewModel
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class CreateBusinessDetailsService @Inject()(val incomeSourceConnector: IncomeSourceConnector) {


  def createBusinessDetails(viewModel: CheckBusinessDetailsViewModel)
                           (implicit ec: ExecutionContext, user: MtdItUser[_], hc: HeaderCarrier): Future[Either[Throwable, IncomeSourceResponse]] = {
    val businessDetails =
      BusinessDetails(
        accountingPeriodStartDate = viewModel.businessStartDate.toString, // TODO: verify date format required
        accountingPeriodEndDate = viewModel.accountingPeriodEndDate.toString,
        tradingName = viewModel.businessName.getOrElse(""),
        addressDetails = AddressDetails(
          addressLine1 = viewModel.businessAddressLine1,
          addressLine2 = viewModel.businessAddressLine2,
          addressLine3 = viewModel.businessAddressLine3,
          addressLine4 = viewModel.businessAddressLine4,
          countryCode = viewModel.businessCountryCode,
          postalCode = viewModel.businessPostalCode
        ),
        typeOfBusiness = Some(viewModel.businessTrade),
        tradingStartDate = viewModel.businessStartDate.toString,
        cashOrAccrualsFlag = viewModel.cashOrAccrualsFlag,
        cessationDate = None,
        cessationReason = None
      )
    val requestObject = AddBusinessIncomeSourcesRequest(businessDetails =
      List(businessDetails)
    )
    for {
      res <- incomeSourceConnector.create(user.mtditid, requestObject)
    } yield res match {
      case Right(List(incomeSourceId)) =>
        Logger("application").info("[PaymentAllocationsService][getPaymentAllocation] - New income source created successfully: $incomeSourceId ")
        Right(incomeSourceId)
      case Left(ex) =>
        Logger("application").error("[CreateBusinessDetailsService][createBusinessDetails] - failed to created ")
        Left(new Error(s"Failed to created incomeSources: $ex"))
    }

  }

}