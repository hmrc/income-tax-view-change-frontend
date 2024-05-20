package controllers.optOut

import connectors.ITSAStatusUpdateConnector
import controllers.optOut.ConfirmedOptOutControllerSpec.emptyBodyString
import helpers.{ComponentSpecBase, ITSAStatusUpdateConnectorStub}
import helpers.servicemocks.ITSAStatusDetailsStub.ITSAYearStatus
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.itsaStatus.ITSAStatus
import models.optOut.OptOutUpdateRequestModel.OptOutUpdateResponseFailure
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.mvc.Http.Status
import play.mvc.Http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

object ConfirmedOptOutControllerSpec {
  val emptyBodyString = ""
}
class ConfirmedOptOutControllerSpec extends ComponentSpecBase {

  val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  val previousYear = currentTaxYear.addYears(-1)
  val taxableEntityId = "123"

  s"user confirms opt-out for one-year scenario" should {
    "show opt-out complete page" in {

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus)
      ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
      CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
      ITSAStatusUpdateConnectorStub.stubPUTItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
        Status.NO_CONTENT, emptyBodyString,
        Map(ITSAStatusUpdateConnector.CorrelationIdHeader -> "123")
      )

      val result = IncomeTaxViewChangeFrontend.confirmOneYearOptOut()

      result should have(
        httpStatus(OK),
      )

    }
  }

  s"user confirms opt-out for one-year scenario and missing header" should {
    "show opt-out complete page" in {

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus)
      ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
      CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
      ITSAStatusUpdateConnectorStub.stubPUTItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
        Status.NO_CONTENT, emptyBodyString,
        Map("missing-header-name" -> "missing-header-value")
      )

      val result = IncomeTaxViewChangeFrontend.confirmOneYearOptOut()

      result should have(
        httpStatus(OK),
      )

    }
  }

  s"user confirms opt-out for one-year scenario and update fails" should {
    "show error page" in {

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus)
      ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
      CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
      ITSAStatusUpdateConnectorStub.stubPUTItsaStatusUpdate(propertyOnlyResponse.asInstanceOf[IncomeSourceDetailsModel].nino,
        BAD_REQUEST, Json.toJson(OptOutUpdateResponseFailure.defaultFailure()).toString(),
        Map(ITSAStatusUpdateConnector.CorrelationIdHeader -> "123")
      )

      val result = IncomeTaxViewChangeFrontend.confirmOneYearOptOut()

      result should have(
        httpStatus(INTERNAL_SERVER_ERROR),
      )

    }
  }

}
