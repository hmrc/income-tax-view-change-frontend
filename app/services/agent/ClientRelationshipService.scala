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

package services.agent

import connectors.IncomeTaxViewChangeConnector
import connectors.agent.{AgentClientRelationshipsConnector, CitizenDetailsConnector}
import javax.inject.{Inject, Singleton}
import models.citizenDetails.{CitizenDetailsErrorModel, CitizenDetailsModel}
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel}
import services.agent.ClientRelationshipService._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientRelationshipService @Inject()(agentClientRelationshipsConnector: AgentClientRelationshipsConnector,
                                          citizenDetailsConnector: CitizenDetailsConnector,
                                          incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector)
                                         (implicit ec: ExecutionContext) {

  def checkAgentClientRelationship(utr: String, arn: String)(implicit hc: HeaderCarrier): Future[Either[ClientRelationshipFailure, ClientDetails]] =
    citizenDetailsConnector.getCitizenDetailsBySaUtr(utr) flatMap {
      case CitizenDetailsModel(optionalFirstName, optionalLastName, Some(nino)) =>
        incomeTaxViewChangeConnector.getBusinessDetails(nino) flatMap {
          case IncomeSourceDetailsModel(mtdbsa, _, _, _) =>
            agentClientRelationshipsConnector.agentClientRelationship(arn, mtdbsa) map {
              case true => Right(ClientRelationshipService.ClientDetails(optionalFirstName, optionalLastName, nino, mtdbsa))
              case false => Left(NoAgentClientRelationship)
            }
          case IncomeSourceDetailsError(code, _) if code == 404 => Future.successful(Left(BusinessDetailsNotFound))
          case _ => Future.successful(Left(UnexpectedResponse))
        }
      case CitizenDetailsErrorModel(code, _) if code == 404 => Future.successful(Left(CitizenDetailsNotFound))
      case _ => Future.successful(Left(UnexpectedResponse))
    }

}

object ClientRelationshipService {

  sealed trait ClientRelationshipFailure

  case object BusinessDetailsNotFound extends ClientRelationshipFailure

  case object CitizenDetailsNotFound extends ClientRelationshipFailure

  case object UnexpectedResponse extends ClientRelationshipFailure

  case object NoAgentClientRelationship extends ClientRelationshipFailure

  case class ClientDetails(firstName: Option[String], lastName: Option[String], nino: String, mtdItId: String)

}
