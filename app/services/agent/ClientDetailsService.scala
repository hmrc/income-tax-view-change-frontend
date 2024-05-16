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

package services.agent

import connectors.BusinessDetailsConnector
import connectors.agent.CitizenDetailsConnector
import models.citizenDetails.{CitizenDetailsErrorModel, CitizenDetailsModel}
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel}
import play.api.Logger
import play.api.http.Status.NOT_FOUND
import services.agent.ClientDetailsService._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import utils.Utilities.ToFutureSuccessful

@Singleton
class ClientDetailsService @Inject()(citizenDetailsConnector: CitizenDetailsConnector,
                                     businessDetailsConnector: BusinessDetailsConnector)
                                    (implicit ec: ExecutionContext) {

  def checkClientDetails(utr: String)(implicit hc: HeaderCarrier): Future[Either[ClientDetailsFailure, ClientDetails]] =
    citizenDetailsConnector.getCitizenDetailsBySaUtr(utr) flatMap {
      case CitizenDetailsModel(optionalFirstName, optionalLastName, Some(nino)) =>
        businessDetailsConnector.getBusinessDetails(nino) flatMap {
          case IncomeSourceDetailsModel(_, mtdbsa, _, _, _) =>
            ( (Right(ClientDetailsService.ClientDetails(optionalFirstName, optionalLastName, nino, mtdbsa))) ).asFuture 
          case IncomeSourceDetailsError(NOT_FOUND, _) => ( (Left(BusinessDetailsNotFound)) ).asFuture 
          case _ =>
            Logger("application").error(s"error response from Income Source Details")
            ( (Left(APIError)) ).asFuture 
        }
      case CitizenDetailsModel(_, _, None) => ( (Left(CitizenDetailsNotFound)) ).asFuture 
      case CitizenDetailsErrorModel(NOT_FOUND, _) => ( (Left(CitizenDetailsNotFound)) ).asFuture 
      case _=>
        Logger("application").error("error response from Citizen Details")
        ( (Left(APIError)) ).asFuture 
    }
}

object ClientDetailsService {

  sealed trait ClientDetailsFailure

  final case object BusinessDetailsNotFound extends ClientDetailsFailure

  final case object CitizenDetailsNotFound extends ClientDetailsFailure

  final case object APIError extends ClientDetailsFailure

  final case class ClientDetails(firstName: Option[String], lastName: Option[String], nino: String, mtdItId: String)

}
