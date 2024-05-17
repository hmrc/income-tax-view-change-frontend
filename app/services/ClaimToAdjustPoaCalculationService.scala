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

package services

import models.claimToAdjustPoa.ClaimToAdjustPoaResponse.ClaimToAdjustPoaResponse
import models.claimToAdjustPoa.{ClaimToAdjustPoaRequest, SelectYourReason}
import models.incomeSourceDetails.TaxYear

import javax.inject.Inject
import scala.concurrent.Future

class ClaimToAdjustPoaCalculationService @Inject(){

  // To be repaced with actual connector
//  private def connectorCall(request: ClaimToAdjustPoaRequest) : Future[ClaimToAdjustPoaResponse] = ???

//  def recalculate(nino: String, taxYear: TaxYear, amount: BigDecimal, poaAdjustmentReason: SelectYourReason): Future[Either[Throwable, Unit]] = {
//    // call connector
//    val request : ClaimToAdjustPoaRequest = ??? // prepare required request
//    connectorCallrequest() // Process response: log and transform to required type
//  }

}
