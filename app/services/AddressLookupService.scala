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

import config.FrontendAppConfig
import connectors.AddressLookupConnector
import models.incomeSourceDetails.BusinessAddressModel
import models.incomeSourceDetails.viewmodels.httpparser.GetAddressLookupDetailsHttpParser.UnexpectedGetStatusFailure
import models.incomeSourceDetails.viewmodels.httpparser.PostAddressLookupHttpParser.{PostAddressLookupSuccessResponse, UnexpectedPostStatusFailure}
import play.api.Logger
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressLookupService @Inject()(val frontendAppConfig: FrontendAppConfig,
                                     addressLookupConnector: AddressLookupConnector)
                                    (implicit ec: ExecutionContext) {
  case class AddressError(status: String) extends RuntimeException

  def initialiseAddressJourney(isAgent: Boolean, isChange: Boolean)
                              (implicit hc: HeaderCarrier, request: RequestHeader): Future[Either[Throwable, Option[String]]] = {
    addressLookupConnector.initialiseAddressLookup(
      isAgent = isAgent,
      isChange = isChange
    ) map {
      case Left(UnexpectedPostStatusFailure(status)) =>
        Logger("application").info(s"[AddressLookupService][initialiseAddressJourney] - error during initialise $status")
        Left(AddressError("status: " + status))
      case Right(PostAddressLookupSuccessResponse(location: Option[String])) =>
        Logger("application").info(message = s"[AddressLookupService][initialiseAddressJourney] - success response: $location")
        Right(location)
    }
  }

  def fetchAddress(id: Option[String])(implicit hc: HeaderCarrier): Future[Either[Throwable, BusinessAddressModel]] = {
    id match {
      case Some(value) =>
        addressLookupConnector.getAddressDetails(value) map {
          case Left(UnexpectedGetStatusFailure(status)) =>
            Logger("application").error(s"[AddressLookupService][fetchAddress] - failed to get details for $id with status $status")
            Left(AddressError("status: " + status))
          case Right(None) =>
            Logger("application").info(s"[AddressLookupService][fetchAddress] - failed to get details for $id")
            Left(AddressError("Not found"))
          case Right(Some(model)) => Right(model)
      }
      case None =>
        Logger("application").error(s"[AddressLookupService][fetchAddress] - No id provided")
        Future(Left(AddressError("No id provided")))
    }
  }

}
