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

package controllers.incomeSources.add

import audit.AuditingService
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockSessionService
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.{AddIncomeSourceData, Address, UIJourneySessionData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.Assertion
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.CreateBusinessDetailsService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testSelfEmploymentId, testSessionId}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{emptyUIJourneySessionData, notCompletedUIJourneySessionData}
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.incomeSources.add.IncomeSourceCheckDetails

import java.time.LocalDate
import scala.concurrent.Future
import scala.util.Try

class IncomeSourceCheckDetailsControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate with MockSessionService with FeatureSwitching {

  val testBusinessId: String = testSelfEmploymentId
  val testBusinessName: String = "Test Business"
  val testBusinessStartDate: LocalDate = LocalDate.of(2023, 1, 2)
  val testBusinessTrade: String = "Plumbing"
  val testBusinessAddressLine1: String = "123 Main Street"
  val testBusinessPostCode: String = "AB123CD"
  val testBusinessAddress: Address = Address(lines = Seq(testBusinessAddressLine1), postcode = Some(testBusinessPostCode))
  val testBusinessAccountingMethod = "cash"
  val testAccountingPeriodEndDate: LocalDate = LocalDate.of(2023, 11, 11)
  val testCountryCode = "GB"
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockCheckBusinessDetails: IncomeSourceCheckDetails = app.injector.instanceOf[IncomeSourceCheckDetails]
  val mockBusinessDetailsService: CreateBusinessDetailsService = mock(classOf[CreateBusinessDetailsService])

  val testPropertyStartDate: LocalDate = LocalDate.of(2023, 1, 1)
  val testPropertyAccountingMethod: String = "CASH"
  val accruals: String = messages("incomeSources.add.accountingMethod.accruals")

  val testUIJourneySessionDataBusiness: UIJourneySessionData = UIJourneySessionData(
    sessionId = "some-session-id",
    journeyType = JourneyType(Add, SelfEmployment).toString,
    addIncomeSourceData = Some(AddIncomeSourceData(
      businessName = Some(testBusinessName),
      businessTrade = Some(testBusinessTrade),
      dateStarted = Some(testBusinessStartDate),
      address = Some(testBusinessAddress),
      countryCode = Some(testCountryCode),
      accountingPeriodEndDate = Some(testAccountingPeriodEndDate),
      incomeSourcesAccountingMethod = Some(testBusinessAccountingMethod)
    )))


  def testUIJourneySessionDataProperty(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = "some-session-id",
    journeyType = JourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(AddIncomeSourceData(
      dateStarted = Some(testBusinessStartDate),
      incomeSourcesAccountingMethod = Some(testBusinessAccountingMethod)
    )))

  def sessionDataCompletedJourney(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(journeyIsComplete = Some(true))))

  def sessionDataISAdded(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceAdded = Some(true))))

  def sessionDataPartial(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceId = Some("1234"), dateStarted = Some(LocalDate.parse("2022-01-01")))))

  def sessionData(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, None)

  object TestCheckDetailsController extends IncomeSourceCheckDetailsController(
    checkDetailsView = app.injector.instanceOf[IncomeSourceCheckDetails],
    authorisedFunctions = mockAuthService,
    businessDetailsService = mockBusinessDetailsService,
    auditingService = app.injector.instanceOf[AuditingService],
    testAuthenticator
  )(ec, mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    sessionService = mockSessionService,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler]
  )

  "IncomeSourceCheckDetailsController" should {
    ".show" should {
      "return 200 OK" when {
        "the session contains full business details and FS enabled" when {
          def runSuccessTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
            disableAllSwitches()
            enable(IncomeSources)
            mockNoIncomeSources()

            if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
            else         setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

            setupMockCreateSession(true)
            setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Add, incomeSourceType)))))

            val result =
              if (isAgent) TestCheckDetailsController.show(isAgent, incomeSourceType)(fakeRequestConfirmedClient())
              else         TestCheckDetailsController.show(isAgent, incomeSourceType)(fakeRequestWithActiveSession)

            val document: Document = Jsoup.parse(contentAsString(result))
            val changeDetailsLinks = document.select(".govuk-summary-list__actions .govuk-link")

            status(result) shouldBe OK
            document.title shouldBe getTitle(incomeSourceType, isAgent)
            document.select("h1:nth-child(1)").text shouldBe getHeading(incomeSourceType)
            Try(changeDetailsLinks.first().text).toOption shouldBe Some(getLink(incomeSourceType))
          }

          "individual" when {
            "Self Employment"  in {runSuccessTest(isAgent = false, SelfEmployment)}
            "Uk Property"      in {runSuccessTest(isAgent = false, UkProperty)}
            "Foreign Property" in {runSuccessTest(isAgent = false, ForeignProperty)}
          }

          "agent" when {
            "Self Employment"  in {runSuccessTest(isAgent = true, SelfEmployment)}
            "Uk Property"      in {runSuccessTest(isAgent = true, UkProperty)}
            "Foreign Property" in {runSuccessTest(isAgent = true, ForeignProperty)}
          }
        }
      }

      "return 303 and redirect an individual back to the home page" when {
        "the IncomeSources FS is disabled" when {
          def runFSDisabledTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {

            disable(IncomeSources)
            setupMockAuthorisationSuccess(isAgent)
            mockSingleBusinessIncomeSource()
            setupMockCreateSession(true)
            setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Add, incomeSourceType)))))

            val result =
              TestCheckDetailsController.show(isAgent, incomeSourceType)(
                if (isAgent) fakeRequestConfirmedClient()
                else         fakeRequestWithActiveSession
              )

            val redirectUrl =
              if (isAgent) controllers.routes.HomeController.showAgent.url
              else         controllers.routes.HomeController.show().url

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }

          "individual" when {
            "Self Employment"   in { runFSDisabledTest(isAgent = false, SelfEmployment)}
            "Uk Property"       in { runFSDisabledTest(isAgent = false, UkProperty)}
            "Foreign Property"  in { runFSDisabledTest(isAgent = false, ForeignProperty)}
          }

          "agent" when {
            "Self Employment"   in {runFSDisabledTest(isAgent = true, SelfEmployment)}
            "Uk Property"       in {runFSDisabledTest(isAgent = true, UkProperty)}
            "Foreign Property"  in {runFSDisabledTest(isAgent = true, ForeignProperty)}
          }
        }

        "called with an unauthenticated user" when {
          def runUnauthorisedTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
            if (isAgent) setupMockAgentAuthorisationException()
            else         setupMockAuthorisationException()

            val result =
              TestCheckDetailsController.show(isAgent, incomeSourceType)(
                if (isAgent) fakeRequestConfirmedClient()
                else         fakeRequestWithActiveSession
              )

            status(result) shouldBe SEE_OTHER
          }

          "individual" when {
            "Self Employment"  in {runUnauthorisedTest(isAgent = false, SelfEmployment)}
            "Uk Property"      in {runUnauthorisedTest(isAgent = false, UkProperty)}
            "Foreign Property" in {runUnauthorisedTest(isAgent = false, ForeignProperty)}
          }
          "agent" when {
            "Self Employment"   in {runUnauthorisedTest(isAgent = true, SelfEmployment)}
            "Uk Property"       in {runUnauthorisedTest(isAgent = true, UkProperty)}
            "Foreign Property"  in {runUnauthorisedTest(isAgent = true, ForeignProperty)}
          }
        }
      }

      s"return ${Status.SEE_OTHER}: redirect to the relevant You Cannot Go Back page" when {
        s"user has already completed the journey" when {
          def journeyCompletedTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
            disableAllSwitches()
            enable(IncomeSources)

            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)
            setupMockGetMongo(Right(Some(sessionDataCompletedJourney(JourneyType(Add, incomeSourceType)))))

            val result: Future[Result] =
              TestCheckDetailsController.show(isAgent, incomeSourceType)(
                if (isAgent) fakeRequestConfirmedClient()
                else         fakeRequestWithActiveSession
              )

            status(result) shouldBe SEE_OTHER
            val redirectUrl = controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.show(isAgent, incomeSourceType).url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }

          "individual" when {
            "Self Employment"  in {journeyCompletedTest(isAgent = false, SelfEmployment)}
            "Uk Property"      in {journeyCompletedTest(isAgent = false, UkProperty)}
            "Foreign Property" in {journeyCompletedTest(isAgent = false, ForeignProperty)}
          }
          "agent" when {
            "Self Employment"   in { journeyCompletedTest(isAgent = true, SelfEmployment)}
            "Uk Property"       in { journeyCompletedTest(isAgent = true, UkProperty)}
            "Foreign Property"  in { journeyCompletedTest(isAgent = true, ForeignProperty)}
          }
        }
        s"user has already added their income source" when {
          def incomeSourceAddedTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {

            disableAllSwitches()
            enable(IncomeSources)
            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)
            setupMockGetMongo(Right(Some(sessionDataISAdded(JourneyType(Add, incomeSourceType)))))

            val result: Future[Result] =
              TestCheckDetailsController.show(isAgent, incomeSourceType)(
                if (isAgent) fakeRequestConfirmedClient()
                else         fakeRequestWithActiveSession
              )

            status(result) shouldBe SEE_OTHER
            val redirectUrl = controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.show(isAgent, incomeSourceType).url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }

          "individual" when {
            "Self Employment"   in {incomeSourceAddedTest(isAgent = false, SelfEmployment)}
            "Uk Property"       in {incomeSourceAddedTest(isAgent = false, UkProperty)}
            "Foreign Property"  in {incomeSourceAddedTest(isAgent = false, ForeignProperty)}
          }
          "agent" when {
            "Self Employment"   in {incomeSourceAddedTest(isAgent = true, SelfEmployment)}
            "Uk Property"       in {incomeSourceAddedTest(isAgent = true, UkProperty)}
            "Foreign Property"  in {incomeSourceAddedTest(isAgent = true, ForeignProperty)}
          }
        }
      }

      "return 500 INTERNAL_SERVER_ERROR" when {
        "there is session data missing" when {
          def missingSessionDataTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {

            disableAllSwitches()
            enable(IncomeSources)
            mockNoIncomeSources()
            setupMockAuthorisationSuccess(isAgent)
            setupMockCreateSession(true)
            setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Add, incomeSourceType)))))

            val result = TestCheckDetailsController.show(isAgent, incomeSourceType)(
              if (isAgent) fakeRequestConfirmedClient()
              else fakeRequestWithActiveSession
            )
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }

          "individual" when {
            "Self Employment"  in {missingSessionDataTest(isAgent = false, SelfEmployment)}
            "Uk Property"      in {missingSessionDataTest(isAgent = false, UkProperty)}
            "Foreign Property" in {missingSessionDataTest(isAgent = false, ForeignProperty)}
          }
          "agent" when {
            "Self Employment"   in {missingSessionDataTest(isAgent = true, SelfEmployment)}
            "Uk Property"       in {missingSessionDataTest(isAgent = true, UkProperty)}
            "Foreign Property"  in {missingSessionDataTest(isAgent = true, ForeignProperty)
            }
          }
        }
      }
    }

    ".submit" should {
      "return 303" when {
        "data is correct and redirect next page" when {
          def successFullRedirectTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {

            disableAllSwitches()
            enable(IncomeSources)
            mockNoIncomeSources()

            if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
            else         setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

            when(mockBusinessDetailsService.createRequest(any())(any(), any(), any()))
              .thenReturn(Future {
                Right(CreateIncomeSourceResponse(testBusinessId))
              })

            setupMockCreateSession(true)
            setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Add, incomeSourceType)))))
            setupMockSetMongoData(true)

            val result =
              TestCheckDetailsController.submit(isAgent, incomeSourceType)(
                if (isAgent) fakeRequestConfirmedClient()
                else         fakeRequestWithActiveSession
              )

            val redirectUrl: (Boolean, IncomeSourceType, String) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType, id: String) =>
              routes.IncomeSourceReportingMethodController.show(isAgent, incomeSourceType).url

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl(isAgent, incomeSourceType, testSelfEmploymentId))
          }

          "individual" when {
            "Self Employment"   in {successFullRedirectTest(isAgent = false, SelfEmployment)}
            "Uk Property"       in {successFullRedirectTest(isAgent = false, UkProperty)}
            "Foreign Property"  in {successFullRedirectTest(isAgent = false, ForeignProperty)}
          }
          "agent" when {
            "Self Employment"  in {successFullRedirectTest(isAgent = true, SelfEmployment)}
            "Uk Property"      in {successFullRedirectTest(isAgent = true, UkProperty)}
            "Foreign Property" in {successFullRedirectTest(isAgent = true, ForeignProperty)
            }
          }
        }
        "redirect to custom error page when unable to create business" when {
          def businessCreateFailTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
            disableAllSwitches()
            enable(IncomeSources)
            mockNoIncomeSources()

            if (isAgent) setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
            else         setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

            when(mockBusinessDetailsService.createRequest(any())(any(), any(), any()))
              .thenReturn(Future {
                Left(new Error("Test Error"))
              })

            setupMockCreateSession(true)
            setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Add, incomeSourceType)))))
            when(mockSessionService.deleteMongoData(any())(any())).thenReturn(Future(true))

            val result =
              TestCheckDetailsController.submit(isAgent, incomeSourceType)(
                if (isAgent) fakeRequestConfirmedClient()
                else         fakeRequestWithActiveSession
              )

            val redirectUrl = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.show(isAgent, incomeSourceType).url

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }

          "individual" when {
            "Self Employment"  in {businessCreateFailTest(isAgent = false, SelfEmployment)}
            "Uk Property"      in {businessCreateFailTest(isAgent = false, UkProperty)}
            "Foreign Property" in {businessCreateFailTest(isAgent = false, ForeignProperty)}
          }
          "agent" when {
            "Self Employment"  in {businessCreateFailTest(isAgent = true, SelfEmployment)}
            "Uk Property"      in {businessCreateFailTest(isAgent = true, UkProperty)}
            "Foreign Property" in {businessCreateFailTest(isAgent = true, ForeignProperty)}
          }
        }
      }
    }
  }

  def getHeading(sourceType: IncomeSourceType): String = {
    sourceType match {
      case SelfEmployment => messages("check-business-details.title")
      case UkProperty => messages("incomeSources.add.checkUKPropertyDetails.title")
      case ForeignProperty => messages("incomeSources.add.foreign-property-check-details.title")
    }
  }

  def getTitle(sourceType: IncomeSourceType, isAgent: Boolean): String = {
    val prefix: String = if (isAgent) "htmlTitle.agent" else "htmlTitle"
    sourceType match {
      case SelfEmployment => s"${messages(prefix, messages("check-business-details.title"))}"
      case UkProperty => messages(prefix, messages("incomeSources.add.checkUKPropertyDetails.title"))
      case ForeignProperty => messages(prefix, messages("incomeSources.add.foreign-property-check-details.title"))
    }
  }

  def getLink(sourceType: IncomeSourceType): String = {
    sourceType match {
      case SelfEmployment => s"${messages("check-business-details.change")}"
      case UkProperty => s"${messages("check-business-details.change")}"
      case ForeignProperty => s"${messages("incomeSources.add.foreign-property-check-details.change")}"
    }
  }
}
