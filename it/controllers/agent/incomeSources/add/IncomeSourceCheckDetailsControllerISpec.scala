package controllers.agent.incomeSources.add

import audit.models.CreateIncomeSourceAuditModel
import auth.MtdItUser
import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.createIncomeSource.{CreateIncomeSourceErrorResponse, CreateIncomeSourceResponse}
import models.incomeSourceDetails.viewmodels.CheckDetailsViewModel
import models.incomeSourceDetails.{AddIncomeSourceData, Address, UIJourneySessionData}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testNino, testSelfEmploymentId, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, noPropertyOrBusinessResponse}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate

class IncomeSourceCheckDetailsControllerISpec extends ComponentSpecBase {
  def checkBusinessDetailsShowUrl(incomeSourceType: IncomeSourceType): String = controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType).url

  def checkBusinessDetailsSubmitUrl(incomeSourceType: IncomeSourceType): String = controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.submitAgent(incomeSourceType).url

  val addBusinessReportingMethodUrl: String = controllers.incomeSources.add.routes.BusinessReportingMethodController.showAgent(testSelfEmploymentId).url
  val addForeignPropReportingMethodUrl: String = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.showAgent(testSelfEmploymentId).url
  val addUkPropReportingMethodUrl: String = controllers.incomeSources.add.routes.UKPropertyReportingMethodController.showAgent(testSelfEmploymentId).url

  def errorPageUrl(incomeSourceType: IncomeSourceType): String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType).url

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
  val testErrorReason: String = "Failed to create incomeSources: CreateIncomeSourceErrorResponse(500,Error creating incomeSource: [{\"status\":500,\"reason\":\"INTERNAL_SERVER_ERROR\"}])"


  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
    None, Some("1234567890"), None, Some(Agent), None
  )(FakeRequest())

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  val testSEViewModel: CheckDetailsViewModel = CheckDetailsViewModel(
    businessName = Some(testBusinessName),
    businessStartDate = Some(testBusinessStartDate),
    accountingPeriodEndDate = None,
    businessTrade = Some(testBusinessTrade),
    businessAddressLine1 = Some(testBusinessAddressLine1),
    businessAddressLine2 = None,
    businessAddressLine3 = None,
    businessAddressLine4 = None,
    businessPostalCode = Some(testBusinessPostCode),
    businessCountryCode = Some(testCountryCode),
    incomeSourcesAccountingMethod = Some(testBusinessAccountingMethod),
    cashOrAccrualsFlag = "CASH",
    incomeSourceType = SelfEmployment
  )

  val testUKPropertyViewModel = CheckDetailsViewModel(
    businessStartDate = Some(testBusinessStartDate),
    cashOrAccrualsFlag = "CASH",
    incomeSourceType = UkProperty
  )

  val testForeignPropertyViewModel = CheckDetailsViewModel(
    businessStartDate = Some(testBusinessStartDate),
    cashOrAccrualsFlag = "CASH",
    incomeSourceType = ForeignProperty
  )


  val testAddBusinessData: AddIncomeSourceData = AddIncomeSourceData(
    businessName = Some(testBusinessName),
    businessTrade = Some(testBusinessTrade),
    dateStarted = Some(testBusinessStartDate),
    createdIncomeSourceId = Some(testBusinessId),
    address = Some(testBusinessAddress),
    countryCode = Some(testCountryCode),
    accountingPeriodEndDate = Some(testAccountingPeriodEndDate),
    incomeSourcesAccountingMethod = Some(testBusinessAccountingMethod)
  )

  val testAddPropertyData: AddIncomeSourceData = AddIncomeSourceData(
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

  def uriSegment(incomeSourceType: IncomeSourceType): String = incomeSourceType match {
    case SelfEmployment => "business"
    case UkProperty => "uk-property"
    case ForeignProperty => "foreign-property"
  }

  def getRedirectUrl(incomeSourceType: IncomeSourceType): String = incomeSourceType match {
    case SelfEmployment => addBusinessReportingMethodUrl
    case UkProperty => addUkPropReportingMethodUrl
    case ForeignProperty => addForeignPropReportingMethodUrl
  }


  def runShowtest(incomeSourceType: IncomeSourceType): Unit = {
    s"calling GET ${checkBusinessDetailsShowUrl(incomeSourceType)}" should {
      "render the Check Business details page with accounting method" when {
        "User is authorised and has no existing businesses" in {
          Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
          enable(IncomeSources)
          stubAuthorisedAgentUser(authorised = true)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
          IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)

          await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

          val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/${uriSegment(incomeSourceType)}-check-details", clientDetailsWithConfirmation)

          incomeSourceType match {
            case SelfEmployment =>
              result should have(
                httpStatus(OK),
                pageTitleAgent("check-business-details.title"),
                elementTextByID("business-name-value")(testBusinessName),
                elementTextByID("start-date-value")("1 January 2023"),
                elementTextByID("business-trade-value")(testBusinessTrade),
                elementTextByID("business-address-value")(testBusinessAddressLine1 + " " + testBusinessPostCode + " " + testBusinessCountryCode),
                elementTextByID("business-accounting-value")(testBusinessAccountingMethodView),
                elementTextByID("confirm-button")(continueButtonText)
              )

            case UkProperty =>
              result should have(
                httpStatus(OK),
                pageTitleAgent("incomeSources.add.checkUKPropertyDetails.title"),
                elementTextByID("start-date-value")("1 January 2023"),
                elementTextByID("business-accounting-value")(testBusinessAccountingMethodView),
                elementTextByID("confirm-button")(continueButtonText)
              )

            case ForeignProperty =>
              result should have(
                httpStatus(OK),
                pageTitleAgent("incomeSources.add.foreign-property-check-details.title"),
                elementTextByID("start-date-value")("1 January 2023"),
                elementTextByID("business-accounting-value")(testBusinessAccountingMethodView),
                elementTextByID("confirm-button")(continueButtonText)
              )
          }
        }
      }
    }
  }

  "Calling the GET urls" should {
    runShowtest(SelfEmployment)
    runShowtest(UkProperty)
    runShowtest(ForeignProperty)
  }

  def runSubmitSuccessTest(incomeSourceType: IncomeSourceType): Unit = {
    s"calling POST ${checkBusinessDetailsSubmitUrl(incomeSourceType)}" should {
      "user selects 'confirm and continue'" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)
        await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))
        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/add/${uriSegment(incomeSourceType)}-check-details", clientDetailsWithConfirmation)(Map.empty)

        incomeSourceType match {
          case SelfEmployment => AuditStub.verifyAuditContainsDetail(
            CreateIncomeSourceAuditModel(SelfEmployment, testSEViewModel, None, None, Some(CreateIncomeSourceResponse(testSelfEmploymentId)))(testUser).detail)

          case UkProperty => AuditStub.verifyAuditContainsDetail(
            CreateIncomeSourceAuditModel(UkProperty, testUKPropertyViewModel, None, None, Some(CreateIncomeSourceResponse(testSelfEmploymentId)))(testUser).detail)

          case ForeignProperty => AuditStub.verifyAuditContainsDetail(
            CreateIncomeSourceAuditModel(ForeignProperty, testForeignPropertyViewModel, None, None, Some(CreateIncomeSourceResponse(testSelfEmploymentId)))(testUser).detail)
        }

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(getRedirectUrl(incomeSourceType))
        )
      }
    }
  }

  def businessNotAddedTest(incomeSourceType: IncomeSourceType): Unit = {
    s"calling POST ${checkBusinessDetailsSubmitUrl(incomeSourceType)}" should {
      "error in response from API" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        val response = List(CreateIncomeSourceErrorResponse(500, "INTERNAL_SERVER_ERROR"))
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsErrorResponseNew(testMtditid)(response)

        When(s"I call ${checkBusinessDetailsSubmitUrl(incomeSourceType)}")
        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/add/${uriSegment(incomeSourceType)}-check-details", clientDetailsWithConfirmation)(Map.empty)

        incomeSourceType match {
          case SelfEmployment => AuditStub.verifyAuditContainsDetail(
            CreateIncomeSourceAuditModel(SelfEmployment, testSEViewModel, Some("API_FAILURE"), Some(testErrorReason), None)(testUser).detail)

          case UkProperty => AuditStub.verifyAuditContainsDetail(
            CreateIncomeSourceAuditModel(UkProperty, testUKPropertyViewModel, Some("API_FAILURE"), Some(testErrorReason), None)(testUser).detail)

          case ForeignProperty => AuditStub.verifyAuditContainsDetail(
            CreateIncomeSourceAuditModel(ForeignProperty, testForeignPropertyViewModel, Some("API_FAILURE"), Some(testErrorReason), None)(testUser).detail)
        }

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(errorPageUrl(incomeSourceType))
        )
      }
    }
  }

  def noUserDetailsTest(incomeSourceType: IncomeSourceType): Unit = {
    s"calling POST ${checkBusinessDetailsSubmitUrl(incomeSourceType)}" should {
      "user session has no details" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/add/${uriSegment(incomeSourceType)}-check-details", clientDetailsWithConfirmation)(Map.empty)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(errorPageUrl(incomeSourceType))
        )
      }
    }
  }

  "Calling the POST urls" should {
    runSubmitSuccessTest(SelfEmployment)
    runSubmitSuccessTest(UkProperty)
    runSubmitSuccessTest(ForeignProperty)

    businessNotAddedTest(SelfEmployment)
    businessNotAddedTest(UkProperty)
    businessNotAddedTest(ForeignProperty)

    noUserDetailsTest(SelfEmployment)
    noUserDetailsTest(UkProperty)
    noUserDetailsTest(ForeignProperty)

  }

}
