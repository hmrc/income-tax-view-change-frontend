/*
 * Copyright 2024 HM Revenue & Customs
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

package returns.services

import common.auth.MtdItUser
import returns.connectors.ChargeHistoryConnector
import returns.models.*
import returns.models.chargeHistory.*
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChargeHistoryService @Inject()(chargeHistoryConnector: ChargeHistoryConnector) {

  def chargeHistoryResponse(chargeReference: Option[String])
                           (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ChargesHistoryErrorModel, List[ChargeHistoryDetailsModel]]] = {
      chargeHistoryConnector.getChargeHistory(user.nino, chargeReference).map {
        case chargesHistory: ChargesHistoryModel => Right(chargesHistory.chargeHistoryDetails.getOrElse(Nil))
        case errorResponse: ChargesHistoryErrorModel => Left(errorResponse)
      }
  }
  
}
