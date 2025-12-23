package services

import auth.MtdItUser
import connectors.{CalculationListConnector, IncomeTaxCalculationConnector}
import models.calculationList.{CalculationListErrorModel, CalculationListResponseModel}
import models.core.Nino
import models.incomeSourceDetails.TaxYear
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponseModel}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TaxYearService @Inject()(
                                calculationListConnector: CalculationListConnector,
                                incomeTaxCalculationConnector: IncomeTaxCalculationConnector,
                              ) {


  def aa(nino: String, taxYear: TaxYear)(implicit user: MtdItUser[_], headerCarrier: HeaderCarrier, ec: ExecutionContext) = {
    for {
      calculationListResponse: CalculationListResponseModel <- calculationListConnector.getLegacyCalculationList(Nino(user.nino), taxYear.endYear.toString)
      getCalculationResponse: LiabilityCalculationResponseModel <- incomeTaxCalculationConnector.getCalculationResponse(user.mtditid, nino, taxYear.toString, None)
    } yield {
      (calculationListResponse, getCalculationResponse) match {
        case (response1: CalculationListErrorModel, response2: LiabilityCalculationError) | CESAContent =>
          LegacyAndCesaContent
        case SOMECONDITION =>
          cesaContent
        case SOMECONDITION =>
          mtdSoftwareContent
        case Individual user with IRSA enrolment is handed to Classic SA / Liabilities & Payments =>
          mtdSoftwareContent
        case Individual user without IRSA enrolment sees guidance =>
          newContent
        case Agent view a calculation for the  tax year before 2023–24  =>
          newContent
        case _ =>
          newContent
      }
    }
  }


  def determineUserTypeContent()(implicit user: MtdItUser[_], headerCarrier: HeaderCarrier, ec: ExecutionContext) = {
    ???
  }


}
