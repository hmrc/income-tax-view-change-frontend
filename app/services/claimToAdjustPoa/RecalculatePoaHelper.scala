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

package services.claimToAdjustPoa

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import controllers.routes.HomeController
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.{PaymentOnAccountViewModel, PoAAmendmentData}
import models.core.Nino
import play.api.Logger
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.ErrorRecovery

import scala.concurrent.{ExecutionContext, Future}

trait RecalculatePoaHelper extends FeatureSwitching with ErrorRecovery {

  private def dataFromSession(poaSessionService: PaymentOnAccountSessionService)(implicit hc: HeaderCarrier, ec: ExecutionContext)
  : Future[PoAAmendmentData] = {
    poaSessionService.getMongo(hc, ec).flatMap {
      case Right(Some(newPoaData: PoAAmendmentData)) =>
        Future.successful(newPoaData)
      case _ =>
        Future.failed(new Exception(s"Failed to retrieve session data"))
    }
  }

  private def handlePoaAndOtherData(poa: PaymentOnAccountViewModel,
                                      otherData: PoAAmendmentData, nino: Nino, ctaCalculationService: ClaimToAdjustPoaCalculationService)
                                     (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    otherData match {
      case PoAAmendmentData(Some(poaAdjustmentReason), Some(amount), _) =>
        ctaCalculationService.recalculate(nino, poa.taxYear, amount, poaAdjustmentReason) map {
          case Left(ex) =>
            Logger("application").error(s"POA recalculation request failed: ${ex.getMessage}")
            Redirect(controllers.claimToAdjustPoa.routes.ApiFailureSubmittingPoaController.show(user.isAgent()))
          case Right(_) =>
            Redirect(controllers.claimToAdjustPoa.routes.PoaAdjustedController.show(user.isAgent()))
        }
      case PoAAmendmentData(_, _, _) =>
        Future.successful(logAndRedirect("Missing poaAdjustmentReason and/or amount"))
    }
  }

  protected def handleSubmitPoaData(claimToAdjustService: ClaimToAdjustService, ctaCalculationService: ClaimToAdjustPoaCalculationService,
                                    poaSessionService: PaymentOnAccountSessionService)
                                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    if (isEnabled(AdjustPaymentsOnAccount)) {
      {
        for {
          poaMaybe <- claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(user.nino))
        } yield poaMaybe match {
          case Right(Some(poa)) =>
            dataFromSession(poaSessionService).flatMap(otherData =>
              handlePoaAndOtherData(poa, otherData, Nino(user.nino), ctaCalculationService)
            )
          case Right(None) =>
            Future.successful(logAndRedirect("Failed to create PaymentOnAccount model"))
          case Left(ex) =>
            Future.successful(logAndRedirect(s"Exception: ${ex.getMessage} - ${ex.getCause}."))
        }
      }.flatten
    } else {
      Future.successful(
        Redirect(if (user.isAgent()) HomeController.showAgent else HomeController.show())
      )
    }
  }
}
