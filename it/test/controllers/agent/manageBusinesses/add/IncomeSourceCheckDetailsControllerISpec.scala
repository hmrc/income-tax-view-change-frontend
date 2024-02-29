/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.agent.manageBusinesses.add

import audit.models.CreateIncomeSourceAuditModel
import auth.MtdItUser
import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.createIncomeSource.{CreateIncomeSourceErrorResponse, CreateIncomeSourceResponse}
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckDetailsViewModel, CheckPropertyViewModel}
import models.incomeSourceDetails.{AddIncomeSourceData, Address, UIJourneySessionData}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.{emptyUIJourneySessionData, multipleBusinessesAndPropertyResponse, noPropertyOrBusinessResponse}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate

class IncomeSourceCheckDetailsControllerISpec extends ComponentSpecBase {
  def checkBusinessDetailsShowUrl(incomeSourceType: IncomeSourceType): String = controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType).url

  def checkBusinessDetailsSubmitUrl(incomeSourceType: IncomeSourceType): String = controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.submitAgent(incomeSourceType).url

  val addBusinessReportingMethodUrl: String = {
    controllers.manageBusinesses.add.routes.IncomeSourceReportingMethodController.show(isAgent = true, SelfEmployment).url
  }

  val addForeignPropReportingMethodUrl: String = {
    controllers.manageBusinesses.add.routes.IncomeSourceReportingMethodController.show(isAgent = true, ForeignProperty).url
  }

  val addUkPropReportingMethodUrl: String = {
    controllers.manageBusinesses.add.routes.IncomeSourceReportingMethodController.show(isAgent = true, UkProperty).url
  }

  def errorPageUrl(incomeSourceType: IncomeSourceType): String = controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType).url

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
    case SelfEmployment => "sole-trader"
    case UkProperty => "uk-property"
    case ForeignProperty => "foreign-property"
  }

  def uriDetailsSegment(incomeSourceType: IncomeSourceType): String = incomeSourceType match {
    case SelfEmployment => "business-"
    case UkProperty => ""
    case ForeignProperty => ""
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

          val result = IncomeTaxViewChangeFrontend.get(s"/manage-your-businesses/add-${uriSegment(incomeSourceType)}/${uriDetailsSegment(incomeSourceType)}check-answers", clientDetailsWithConfirmation)

          incomeSourceType match {
            case SelfEmployment =>
              result should have(
                httpStatus(OK),
                pageTitleAgent("check-details.title"),
                elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testBusinessName),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")("1 January 2023"),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(3) dd:nth-of-type(1)")(testBusinessTrade),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(4) dd:nth-of-type(1)")(testBusinessAddressLine1 + " " + testBusinessPostCode + " " + testBusinessCountryCode),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(5) dd:nth-of-type(1)")(testBusinessAccountingMethodView),
                elementTextByID("confirm-button")(continueButtonText)
              )

            case UkProperty =>
              result should have(
                httpStatus(OK),
                pageTitleAgent("check-details-uk.title"),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(1) dd:nth-of-type(1)")("1 January 2023"),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(testBusinessAccountingMethodView),
                elementTextByID("confirm-button")(continueButtonText)
              )

            case ForeignProperty =>
              result should have(
                httpStatus(OK),
                pageTitleAgent("check-details-fp.title"),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(1) dd:nth-of-type(1)")("1 January 2023"),
                elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(testBusinessAccountingMethodView),
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
        val result = IncomeTaxViewChangeFrontend.post(s"/manage-your-businesses/add-${uriSegment(incomeSourceType)}/${uriDetailsSegment(incomeSourceType)}check-answers", clientDetailsWithConfirmation)(Map.empty)

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
        await(sessionService.setMongoData(testUIJourneySessionData(incomeSourceType)))

        When(s"I call ${checkBusinessDetailsSubmitUrl(incomeSourceType)}")
        val result = IncomeTaxViewChangeFrontend.post(s"/manage-your-businesses/add-${uriSegment(incomeSourceType)}/${uriDetailsSegment(incomeSourceType)}check-answers", clientDetailsWithConfirmation)(Map.empty)

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

        await(sessionService.setMongoData(emptyUIJourneySessionData(JourneyType(Add, incomeSourceType))))

        val result = IncomeTaxViewChangeFrontend.post(s"/manage-your-businesses/add-${uriSegment(incomeSourceType)}/${uriDetailsSegment(incomeSourceType)}check-answers", clientDetailsWithConfirmation)(Map.empty)

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
