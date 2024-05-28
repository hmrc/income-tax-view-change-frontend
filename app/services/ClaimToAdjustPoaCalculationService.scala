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

import connectors.ClaimToAdjustPoaConnector
import models.claimToAdjustPoa.ClaimToAdjustPoaResponse.{ClaimToAdjustPoaError, ClaimToAdjustPoaFailure, ClaimToAdjustPoaInvalidJson, ClaimToAdjustPoaResponse, ClaimToAdjustPoaSuccess, UnexpectedError}
import models.claimToAdjustPoa.{ClaimToAdjustPoaRequest, SelectYourReason}
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClaimToAdjustPoaCalculationService @Inject()(
                                                    claimToAdjustPoaConnector: ClaimToAdjustPoaConnector)
                                                  (implicit ec: ExecutionContext) {


  def recalculate(nino: String, taxYear: TaxYear,
                  amount: BigDecimal, poaAdjustmentReason: SelectYourReason)
                 (implicit hc: HeaderCarrier): Future[Either[Throwable, Unit]] = {

    val request: ClaimToAdjustPoaRequest = ClaimToAdjustPoaRequest(
      nino = nino,
      taxYear = taxYear.endYear.toString,
      amount = amount,
      poaAdjustmentReason = poaAdjustmentReason
    )
      for {
        response <- claimToAdjustPoaConnector.postClaimToAdjustPoa(request)
      } yield response match {
        case Left(ClaimToAdjustPoaError(message)) =>
          Logger("application").error(s"POA recalculation failure: $message")
          Left(new Exception(message))
        case Left(ClaimToAdjustPoaInvalidJson) =>
          Logger("application").error(s"POA recalculation failure / json error: ${ClaimToAdjustPoaInvalidJson.message}")
          Left(new Exception(ClaimToAdjustPoaInvalidJson.message))
        case Left(UnexpectedError) =>
          Logger("application").error(s"POA recalculation failure / unexpected error: ${UnexpectedError.message}")
          Left(new Exception(UnexpectedError.message))
        case Right(ClaimToAdjustPoaSuccess(_)) =>
          Logger("application").info(s"POA recalculation success")
          Right(())
      }

  }


}
