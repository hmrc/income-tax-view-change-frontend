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

import models.claimToAdjustPoa.ClaimToAdjustPoaResponse.{ClaimToAdjustPoaFailure, ClaimToAdjustPoaResponse, ClaimToAdjustPoaSuccess}
import models.claimToAdjustPoa.{ClaimToAdjustPoaRequest, SelectYourReason}
import models.incomeSourceDetails.TaxYear
import play.api.Logger

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClaimToAdjustPoaCalculationService @Inject()(implicit ec: ExecutionContext) {

  // To be replaced with actual connector
  private def connectorCall(request: ClaimToAdjustPoaRequest, success: Boolean): Future[ClaimToAdjustPoaResponse] =
    Future.successful(Right(ClaimToAdjustPoaSuccess("time")))

  def recalculate(nino: String, taxYear: TaxYear, amount: BigDecimal, poaAdjustmentReason: SelectYourReason): Future[Either[Throwable, Unit]] = {

    val request: ClaimToAdjustPoaRequest = ClaimToAdjustPoaRequest(
      nino = nino,
      taxYear = taxYear.endYear.toString,
      amount = amount,
      poaAdjustmentReason = poaAdjustmentReason
    )
    connectorCall(request, success = true) flatMap {
      case Left(failure: ClaimToAdjustPoaFailure) =>
        Logger("application").error(s"failed to get details for ")
        Future.successful(Left(new Exception(failure.message)))
      case Right(_: ClaimToAdjustPoaSuccess) => Future.successful(Right((): Unit))
    }
  }


}
