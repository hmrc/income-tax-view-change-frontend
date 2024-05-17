package services

import models.claimToAdjustPoa.ClaimToAdjustPoaResponse.ClaimToAdjustPoaResponse
import models.claimToAdjustPoa.{ClaimToAdjustPoaRequest, SelectYourReason}
import models.incomeSourceDetails.TaxYear

import javax.inject.Inject
import scala.concurrent.Future

class ClaimToAdjustPoaCalculationService @Inject(){

  // To be repaced with actual connector
  private def connectorCall(request: ClaimToAdjustPoaRequest) : Future[ClaimToAdjustPoaResponse] = ???

//  def recalculate(nino: String, taxYear: TaxYear, amont: BigDecimal, poaAdjustmentReason: SelectYourReason): Future[Either[Throwable, Unit]] = {
//    // call connector
//    val request : ClaimToAdjustPoaRequest = ??? // prepare required request
//    connectorCallrequest() // Process response: log and transform to required type
//  }

}
