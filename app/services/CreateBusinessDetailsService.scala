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
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import models.createIncomeSource._
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.viewmodels._
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CreateBusinessDetailsService @Inject()(val createIncomeSourceConnector: CreateIncomeSourceConnector) {

  private def createBusiness(requestObject: CreateBusinessIncomeSourceRequest)
                            (implicit ec: ExecutionContext, user: MtdItUser[_], hc: HeaderCarrier): Future[Either[Throwable, CreateIncomeSourceResponse]] = {
    createIncomeSourceConnector.createBusiness(user.mtditid, requestObject).flatMap(response => handleResponse(response))
  }

  private def createUKProperty(requestObject: CreateUKPropertyIncomeSourceRequest)
                              (implicit ec: ExecutionContext, user: MtdItUser[_], hc: HeaderCarrier): Future[Either[Throwable, CreateIncomeSourceResponse]] = {
    createIncomeSourceConnector.createUKProperty(user.mtditid, requestObject).flatMap(response => handleResponse(response))
  }

  private def createForeignProperty(requestObject: CreateForeignPropertyIncomeSourceRequest)
                                   (implicit ec: ExecutionContext, user: MtdItUser[_], hc: HeaderCarrier): Future[Either[Throwable, CreateIncomeSourceResponse]] = {
    createIncomeSourceConnector.createForeignProperty(user.mtditid, requestObject).flatMap(response => handleResponse(response))
  }

  def handleResponse(response: Either[CreateIncomeSourceErrorResponse, List[CreateIncomeSourceResponse]]): Future[Either[Error, CreateIncomeSourceResponse]] = {
    response match {
      case Right(List(incomeSourceId)) =>
        Logger("application").info(s"[CreateBusinessDetailsService][handleResponse] - New income source created successfully: $incomeSourceId")
        Future.successful(Right(incomeSourceId))
      case Right(_) =>
        Logger("application").error("[CreateBusinessDetailsService][handleResponse] - failed to create, unexpected response")
        Future.successful(Left(new Error("Failed to create incomeSources")))
      case Left(incomeSourceError) =>
        Logger("application").error("[CreateBusinessDetailsService][handleResponse] - failed to create")
        Future.successful(Left(new Error(s"Failed to create incomeSources: ${incomeSourceError}")))}
  }

  private def removeEmptyStrings(strOpt: Option[String]): Option[String] = {
    strOpt match {
      case Some(str) => if (str == "") None else Some(str)
      case None => None
    }
  }

  def createRequest(viewModel: CheckDetailsViewModel)(implicit
                                                      ec: ExecutionContext,
                                                      user: MtdItUser[_],
                                                      hc: HeaderCarrier): Future[Either[Throwable, CreateIncomeSourceResponse]] = {
    viewModel.incomeSourceType match {
      case SelfEmployment => createBusiness(viewModel)
      case UkProperty => createUKProperty(viewModel)
      case ForeignProperty => createForeignProperty(viewModel)
    }
  }

  def convertToCreateBusinessIncomeSourceRequest(viewModel: CheckDetailsViewModel): Either[Throwable, CreateBusinessIncomeSourceRequest] = {
    Try {
      CreateBusinessIncomeSourceRequest(
        List(
          BusinessDetails(
            accountingPeriodStartDate = viewModel.businessStartDate.get.format(DateTimeFormatter.ISO_LOCAL_DATE),
            accountingPeriodEndDate = viewModel.accountingPeriodEndDate.get.format(DateTimeFormatter.ISO_LOCAL_DATE),
            tradingName = viewModel.businessName.get,
            addressDetails = AddressDetails(
              addressLine1 = viewModel.businessAddressLine1.get,
              addressLine2 = removeEmptyStrings(viewModel.businessAddressLine2),
              addressLine3 = removeEmptyStrings(viewModel.businessAddressLine3),
              addressLine4 = removeEmptyStrings(viewModel.businessAddressLine4),
              countryCode = Some("GB"), // required to be GB by API when postcode present
              postalCode = viewModel.businessPostalCode
            ),
            typeOfBusiness = viewModel.businessTrade,
            tradingStartDate = viewModel.businessStartDate.get.format(DateTimeFormatter.ISO_LOCAL_DATE),
            cashOrAccrualsFlag = viewModel.cashOrAccrualsFlag.toUpperCase,
            cessationDate = None,
            cessationReason = None
          )
        )
      )
    }.toEither
  }

  def createBusiness(viewModel: CheckDetailsViewModel)(implicit
                                                       ec: ExecutionContext,
                                                       user: MtdItUser[_],
                                                       hc: HeaderCarrier): Future[Either[Throwable, CreateIncomeSourceResponse]] = {
    convertToCreateBusinessIncomeSourceRequest(viewModel) match {
      case Right(requestObject) =>
        createBusiness(requestObject)
      case Left(ex) =>
        Logger("application").error("[CreateBusinessDetailsService][createBusiness] - unable to create request object")
        Future.successful(Left(ex))
    }
  }

  private def createForeignPropertyIncomeSourceRequest(viewModel: CheckDetailsViewModel): Either[Throwable, CreateForeignPropertyIncomeSourceRequest] = {
    Try(
      CreateForeignPropertyIncomeSourceRequest(
        PropertyDetails(
          tradingStartDate = viewModel.businessStartDate.toString,
          cashOrAccrualsFlag = viewModel.cashOrAccrualsFlag.toUpperCase,
          startDate = viewModel.businessStartDate.toString
        )
      )
    ).toEither
  }

  def createForeignProperty(viewModel: CheckDetailsViewModel)(implicit
                                                              ec: ExecutionContext,
                                                              user: MtdItUser[_],
                                                              hc: HeaderCarrier): Future[Either[Throwable, CreateIncomeSourceResponse]] = {
    createForeignPropertyIncomeSourceRequest(viewModel) match {
      case Right(requestObject) =>
        createForeignProperty(requestObject)
      case Left(ex) =>
        Logger("application").error("[CreateBusinessDetailsService][createForeignProperty] - unable to create request object")
        Future.successful(Left(ex))
    }
  }

  private def createUKPropertyIncomeSourceRequest(viewModel: CheckDetailsViewModel): Either[Throwable, CreateUKPropertyIncomeSourceRequest] = {
    Try(
      CreateUKPropertyIncomeSourceRequest(
        PropertyDetails(
          tradingStartDate = viewModel.businessStartDate.toString,
          cashOrAccrualsFlag = viewModel.cashOrAccrualsFlag.toUpperCase,
          startDate = viewModel.businessStartDate.toString
        )
      )
    ).toEither
  }

  def createUKProperty(viewModel: CheckDetailsViewModel)
                      (implicit
                       ec: ExecutionContext,
                       user: MtdItUser[_],
                       hc: HeaderCarrier): Future[Either[Throwable, CreateIncomeSourceResponse]] = {
    createUKPropertyIncomeSourceRequest(viewModel) match {
      case Right(requestObject) =>
        createUKProperty(requestObject)
      case Left(ex) =>
        Logger("application").error("[CreateBusinessDetailsService][createUKProperty] - unable to create request object")
        Future.successful(Left(ex))
    }
  }
}
