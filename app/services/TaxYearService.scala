package services

import auth.MtdItUser
import connectors.{CalculationListConnector, IncomeTaxCalculationConnector}
import models.calculationList.{CalculationListErrorModel, CalculationListModel, CalculationListResponseModel}
import models.core.Nino
import models.incomeSourceDetails.TaxYear
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponseModel}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{LocalDate, OffsetDateTime}
import javax.inject.Inject
import scala.concurrent.ExecutionContext


sealed trait TaxYearViewScenarios

case object LegacyAndCesa extends TaxYearViewScenarios

case object MtdSoftware extends TaxYearViewScenarios

case object IrsaAEnrolementHandedOff extends TaxYearViewScenarios

case object NoIrsaAEnrolement extends TaxYearViewScenarios

case object AgentBefore2023TaxYear extends TaxYearViewScenarios

case object Default extends TaxYearViewScenarios


class TaxYearService @Inject()(
                                calculationListConnector: CalculationListConnector,
                                incomeTaxCalculationConnector: IncomeTaxCalculationConnector,
                              ) {

  def jsonDateTimeToLocalDate(jsonDateTime: String): LocalDate = OffsetDateTime.parse(jsonDateTime).toLocalDate

  def aa(nino: String, taxYear: TaxYear)(implicit user: MtdItUser[_], headerCarrier: HeaderCarrier, ec: ExecutionContext) = {

    lazy val taxYear2023 = TaxYear(2023, 2024).toFinancialYearStart
    val irsaEnrolement = user.authUserDetails.saUtr

    for {
      calculationListResponse: CalculationListResponseModel <- calculationListConnector.getLegacyCalculationList(Nino(user.nino), taxYear.endYear.toString)
      getCalculationResponse: LiabilityCalculationResponseModel <- incomeTaxCalculationConnector.getCalculationResponse(user.mtditid, nino, taxYear.toString, None)
    } yield {
      (calculationListResponse, getCalculationResponse) match {
        case (response1: CalculationListErrorModel, response2: LiabilityCalculationError) =>
          LegacyAndCesa
        case SOMECONDITION =>
          cesaContent
        case SOMECONDITION =>
          MtdSoftware
        case _ if irsaEnrolement.isDefined => // Individual user with IRSA enrolment is handed to Classic SA / Liabilities & Payments =>
          IrsaAEnrolementHandedOff
        case _ if irsaEnrolement.isEmpty =>
          //          Individual user without IRSA enrolment sees guidance
          NoIrsaAEnrolement
        case (calcResponse: CalculationListModel, _) if user.isAgent() && jsonDateTimeToLocalDate(calcResponse.calculationTimestamp).isBefore(taxYear2023) =>
          //          Agent view a calculation for the  tax year before 2023–24
          AgentBefore2023TaxYear
        case _ =>
          Default
      }
    }
  }


  def determineUserTypeContent()(implicit user: MtdItUser[_], headerCarrier: HeaderCarrier, ec: ExecutionContext) = {
    ???
  }


}
