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

package services

import connectors.RepaymentConnector
import models.core.RepaymentJourneyResponseModel
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Singleton
import javax.inject.Inject
import scala.concurrent.Future

@Singleton
class RepaymentService @Inject()(val repaymentConnector: RepaymentConnector){

  def start(nino: String, fullAmount: BigDecimal)
           (implicit headerCarrier: HeaderCarrier): Future[RepaymentJourneyResponseModel] = {
    Logger("application").debug(s"Repayment journey start with nino: $nino and fullAmount: $fullAmount ")
    repaymentConnector.start(nino, fullAmount)
  }

  def view(nino: String)
           (implicit headerCarrier: HeaderCarrier): Future[RepaymentJourneyResponseModel] = {
    Logger("application").debug(s"Repayment journey view with nino: $nino")
    repaymentConnector.view(nino)
  }

}
