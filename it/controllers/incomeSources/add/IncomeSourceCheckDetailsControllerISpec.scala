package controllers.incomeSources.add

import enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment}
import enums.JourneyType.{Add, JourneyType}
import helpers.ComponentSpecBase
import models.incomeSourceDetails.{AddIncomeSourceData, Address, UIJourneySessionData}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testPropertyIncomeId, testSelfEmploymentId, testSessionId}

import java.time.LocalDate

class IncomeSourceCheckDetailsControllerISpec extends ComponentSpecBase {

  def checkBusinessDetailsShowUrl(incomeSourceType: IncomeSourceType): String = controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(incomeSourceType).url

  def checkBusinessDetailsSubmitUrl(incomeSourceType: IncomeSourceType): String = controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.submit(incomeSourceType).url

  val addBusinessReportingMethodUrl: String = controllers.incomeSources.add.routes.BusinessReportingMethodController.show(testSelfEmploymentId).url
  val addForeignPropReportingMethodUrl: String = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.show(testPropertyIncomeId).url
  val addUkPropReportingMethodUrl: String = controllers.incomeSources.add.routes.UKPropertyReportingMethodController.show(testPropertyIncomeId).url
  val errorPageUrl: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.show(SelfEmployment).url

  val testBusinessId: String = testSelfEmploymentId
  val testBusinessName: String = "Test Business"
  val testBusinessStartDate: LocalDate = LocalDate.of(2023, 1, 1)
  val testBusinessTrade: String = "Plumbing"
  val testBusinessAddressLine1: String = "Test Road"
  val testBusinessPostCode: String = "B32 1PQ"
  val testBusinessCountryCode: String = "United Kingdom"
  val testBusinessAccountingMethod: String = "cash"
  val testBusinessAccountingMethodView: String = "Cash basis accounting"
  val testAccountingPeriodEndDate: LocalDate = LocalDate.of(2023, 11, 11)
  val testCountryCode = "GB"
  val noAccountingMethod: String = ""
  val continueButtonText: String = messagesAPI("base.confirm-and-continue")
  val testBusinessAddress: Address = Address(lines = Seq(testBusinessAddressLine1), postcode = Some(testBusinessPostCode))

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  val testAddBusinessData = AddIncomeSourceData(
    businessName = Some(testBusinessName),
    businessTrade = Some(testBusinessTrade),
    dateStarted = Some(testBusinessStartDate),
    createdIncomeSourceId = Some(testBusinessId),
    address = Some(testBusinessAddress),
    countryCode = Some(testCountryCode),
    accountingPeriodEndDate = Some(testAccountingPeriodEndDate),
    incomeSourcesAccountingMethod = Some(testBusinessAccountingMethod)
  )

  val testAddPropertyData = AddIncomeSourceData(
    dateStarted = Some(testBusinessStartDate),
    incomeSourcesAccountingMethod = Some(testBusinessAccountingMethod)
  )

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = JourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(if (incomeSourceType == SelfEmployment) testAddBusinessData else testAddPropertyData))

  def testUIJourneySessionDataNoAccountingMethod(incomeSourceType: IncomeSourceType): UIJourneySessionData =
    testUIJourneySessionData(incomeSourceType).copy(
      addIncomeSourceData = Some({
        if (incomeSourceType == SelfEmployment) testAddBusinessData else testAddPropertyData
      }.copy(
        incomeSourcesAccountingMethod = None)))


}
