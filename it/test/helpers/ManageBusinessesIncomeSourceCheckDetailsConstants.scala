package helpers

import auth.MtdItUser
import enums.IncomeSourceJourney.{ForeignProperty, UkProperty}
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckDetailsViewModel, CheckPropertyViewModel}
import models.incomeSourceDetails.{AddIncomeSourceData, Address, IncomeSourceDetailsModel}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testSelfEmploymentId}
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndPropertyResponse
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate

object ManageBusinessesIncomeSourceCheckDetailsConstants {

  val testBusinessId: String = testSelfEmploymentId
  val testBusinessName: String = "Test Business"
  val testBusinessStartDate: LocalDate = LocalDate.of(2023, 1, 1)
  val testBusinessTrade: String = "Plumbing"
  val testBusinessAddressLine1: String = "Test Road"
  val testBusinessPostCode: String = "B32 1PQ"
  val testBusinessCountryCode: String = "United Kingdom"
  val testBusinessAccountingMethod: String = "CASH"
  val testBusinessAccountingMethodView: String = "Cash basis accounting"
  val testAccountingPeriodEndDate: LocalDate = LocalDate.of(2023, 11, 11)
  val testCountryCode = "GB"
  val noAccountingMethod: String = ""
  val testBusinessAddress: Address = Address(lines = Seq(testBusinessAddressLine1), postcode = Some(testBusinessPostCode))
  val testErrorReason: String = "Failed to create incomeSources: CreateIncomeSourceErrorResponse(500,Error creating incomeSource: [{\"status\":500,\"reason\":\"INTERNAL_SERVER_ERROR\"}])"


  val testSEViewModel: CheckDetailsViewModel = CheckBusinessDetailsViewModel(
    businessName = Some(testBusinessName),
    businessStartDate = Some(testBusinessStartDate),
    accountingPeriodEndDate = (testAccountingPeriodEndDate),
    businessTrade = testBusinessTrade,
    businessAddressLine1 = testBusinessAddressLine1,
    businessAddressLine2 = None,
    businessAddressLine3 = None,
    businessAddressLine4 = None,
    businessPostalCode = Some(testBusinessPostCode),
    businessCountryCode = Some(testCountryCode),
    incomeSourcesAccountingMethod = Some(testBusinessAccountingMethod),
    cashOrAccrualsFlag = "CASH",
    showedAccountingMethod = false
  )

  val testUKPropertyViewModel = CheckPropertyViewModel(
    tradingStartDate = testBusinessStartDate,
    cashOrAccrualsFlag = "CASH",
    incomeSourceType = UkProperty
  )

  val testForeignPropertyViewModel = CheckPropertyViewModel(
    tradingStartDate = testBusinessStartDate,
    cashOrAccrualsFlag = "CASH",
    incomeSourceType = ForeignProperty
  )


  val testAddBusinessData: AddIncomeSourceData = AddIncomeSourceData(
    businessName = Some(testBusinessName),
    businessTrade = Some(testBusinessTrade),
    dateStarted = Some(testBusinessStartDate),
    incomeSourceId = Some(testBusinessId),
    address = Some(testBusinessAddress),
    countryCode = Some(testCountryCode),
    accountingPeriodEndDate = Some(testAccountingPeriodEndDate),
    incomeSourcesAccountingMethod = Some(testBusinessAccountingMethod)
  )

  val testAddBusinessDataError: AddIncomeSourceData = AddIncomeSourceData(
    businessName = Some(testBusinessName),
    businessTrade = Some(testBusinessTrade),
    dateStarted = None,
    incomeSourceId = Some(testBusinessId),
    address = Some(testBusinessAddress),
    countryCode = Some(testCountryCode),
    accountingPeriodEndDate = Some(testAccountingPeriodEndDate),
    incomeSourcesAccountingMethod = Some(testBusinessAccountingMethod)
  )

  val testAddPropertyData: AddIncomeSourceData = AddIncomeSourceData(
    dateStarted = Some(testBusinessStartDate),
    incomeSourcesAccountingMethod = Some(testBusinessAccountingMethod)
  )
}
