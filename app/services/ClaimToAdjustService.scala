package services

import connectors.FinancialDetailsConnector
import models.core.Nino
import models.incomeSourceDetails.TaxYear

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ClaimToAdjustService @Inject()(val financialDetailsConnector: FinancialDetailsConnector, implicit val ec: ExecutionContext){

  def canCustomerClaimToAdjust(nino: Nino, taxYear: TaxYear): Boolean = ???

}
