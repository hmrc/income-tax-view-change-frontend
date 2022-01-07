/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.IncomeTaxViewChangeConnector
import connectors.agent.CitizenDetailsConnector
import javax.inject.{Inject, Singleton}
import models.citizenDetails.{CitizenDetailsErrorModel, CitizenDetailsModel}
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel}
import services.agent.ClientDetailsService._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientDetailsService @Inject()(citizenDetailsConnector: CitizenDetailsConnector,
																		 incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector)
																		(implicit ec: ExecutionContext) {

  def checkClientDetails(utr: String)(implicit hc: HeaderCarrier): Future[Either[ClientDetailsFailure, ClientDetails]] =
    citizenDetailsConnector.getCitizenDetailsBySaUtr(utr) flatMap {
      case CitizenDetailsModel(optionalFirstName, optionalLastName, Some(nino)) =>
        incomeTaxViewChangeConnector.getBusinessDetails(nino) flatMap {
          case IncomeSourceDetailsModel(mtdbsa, _, _, _) =>
						Future.successful(Right(ClientDetailsService.ClientDetails(optionalFirstName, optionalLastName, nino, mtdbsa)))
          case IncomeSourceDetailsError(code, _) if code == 404 => Future.successful(Left(BusinessDetailsNotFound))
          case _ => Future.successful(Left(UnexpectedResponse))
        }
      case CitizenDetailsErrorModel(code, _) if code == 404 => Future.successful(Left(CitizenDetailsNotFound))
      case _ => Future.successful(Left(UnexpectedResponse))
    }

}

object ClientDetailsService {

  sealed trait ClientDetailsFailure

  case object BusinessDetailsNotFound extends ClientDetailsFailure

  case object CitizenDetailsNotFound extends ClientDetailsFailure

  case object UnexpectedResponse extends ClientDetailsFailure

  case class ClientDetails(firstName: Option[String], lastName: Option[String], nino: String, mtdItId: String)

}
