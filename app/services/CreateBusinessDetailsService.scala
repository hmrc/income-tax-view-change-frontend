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
import connectors.CreateIncomeSourceConnector
import models.addIncomeSource._
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckForeignPropertyViewModel}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


class CreateBusinessDetailsService @Inject()(val createIncomeSourceConnector: CreateIncomeSourceConnector) {


  /**
   * @param viewModel - provided by the caller/controller
   * @return
   */
  def convertToCreateBusinessIncomeSourceRequest(viewModel: CheckBusinessDetailsViewModel): Either[Throwable, CreateBusinessIncomeSourceRequest] = {
    Try {
      CreateBusinessIncomeSourceRequest(
        List(
          BusinessDetails(
            accountingPeriodStartDate = viewModel.businessStartDate.toString,
            accountingPeriodEndDate = viewModel.accountingPeriodEndDate.toString,
            tradingName = viewModel.businessName.get, // raise error if not provided
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
        )
      )
    }.toEither
  }

  def createBusinessDetails(viewModel: CheckBusinessDetailsViewModel)
                           (implicit ec: ExecutionContext, user: MtdItUser[_], hc: HeaderCarrier): Future[Either[Throwable, AddIncomeSourceResponse]] = {
    // actual mapping from view model to request object
    convertToCreateBusinessIncomeSourceRequest(viewModel) match {
      case Right(requestObject) =>
        createIncomeSourceConnector.createBusiness(user.mtditid, requestObject).flatMap(response =>
          response match {
            case Right(List(incomeSourceId)) =>
              Logger("application").info("[CreateBusinessDetailsService][createBusinessDetails] - income source created: $incomeSourceId ")
              Future.successful(Right(incomeSourceId))
            case Left(ex) =>
              Logger("application").error("[CreateBusinessDetailsService][createBusinessDetails] - failed to created")
              Future.successful {
                Left(new Error(s"Failed to created incomeSources: $ex"))
              }
          }
        )
      case Left(ex) =>
        Logger("application").error("[CreateBusinessDetailsService][createBusinessDetails] - unable to create request object")
        Future.successful(Left(ex))
    }
  }

  /**
   * @param viewModel - view model provided by the caller/controller
   * @return
   */
  def createForeignPropertyIncomeSourceRequest(viewModel: CheckForeignPropertyViewModel) : Either[Throwable, CreateForeignPropertyIncomeSource] = {
    Try(CreateForeignPropertyIncomeSource(tradingStartDate = viewModel.tradingStartDate.toString,
      cashOrAccrualsFlag = viewModel.cashOrAccrualsFlag,
      startDate = viewModel.tradingStartDate.toString)).toEither
  }

  def createForeignProperty(viewModel: CheckForeignPropertyViewModel)
                           (implicit ec: ExecutionContext, user: MtdItUser[_], hc: HeaderCarrier): Future[Either[Throwable, AddIncomeSourceResponse]] = {
    // map view model to request object
    createForeignPropertyIncomeSourceRequest(viewModel) match {
      case Right(requestObject) =>
        for {
          res <- createIncomeSourceConnector.createForeignProperty(user.mtditid, requestObject)
        } yield res match {
          case Right(List(incomeSourceId)) =>
            Logger("application").info("[CreateBusinessDetailsService][createBusinessDetails] - New income source created successfully: $incomeSourceId ")
            Right(incomeSourceId)
          case Left(ex) =>
            Logger("application").error("[CreateBusinessDetailsService][createBusinessDetails] - failed to created ")
            Left(new Error(s"Failed to created incomeSources: $ex"))
        }
      case Left(ex) =>
        Logger("application").error("[CreateBusinessDetailsService][createBusinessDetails] - unable to create request object ")
        Future.successful(Left(new Error(s"Failed to created incomeSources: $ex")))
    }
  }
}