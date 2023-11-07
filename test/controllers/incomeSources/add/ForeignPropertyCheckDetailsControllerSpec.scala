///*
// * Copyright 2023 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package controllers.incomeSources.add
//
//import config.featureswitch.{FeatureSwitching, IncomeSources}
//import config.{AgentItvcErrorHandler, ItvcErrorHandler}
//import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
//import enums.IncomeSourceJourney.ForeignProperty
//import enums.JourneyType.{Add, JourneyType}
//import forms.utils.SessionKeys
//import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
//import mocks.services.MockSessionService
//import models.createIncomeSource.CreateIncomeSourceResponse
//import models.incomeSourceDetails.AddIncomeSourceData.{dateStartedField, incomeSourcesAccountingMethodField}
//import org.jsoup.Jsoup
//import org.jsoup.nodes.Document
//import org.mockito.ArgumentMatchers.any
//import org.mockito.Mockito.{mock, when}
//import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
//import play.api.mvc.{MessagesControllerComponents, Result}
//import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
//import services.{CreateBusinessDetailsService, SessionService}
//import testConstants.BaseTestConstants
//import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
//import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessesAndPropertyIncome, noIncomeDetails}
//import testUtils.TestSupport
//import uk.gov.hmrc.http.HttpClient
//import views.html.incomeSources.add.ForeignPropertyCheckDetails
//
//import java.time.LocalDate
//import scala.concurrent.Future
//
//class ForeignPropertyCheckDetailsControllerSpec extends TestSupport with MockAuthenticationPredicate
//with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate with FeatureSwitching with MockSessionService{
//
//  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
//  val mockBusinessDetailsService: CreateBusinessDetailsService = mock(classOf[CreateBusinessDetailsService])
//
//  val testPropertyStartDate: LocalDate = LocalDate.of(2023, 1, 1)
//  val testPropertyAccountingMethod: String = "CASH"
//  val accruals: String = messages("incomeSources.add.accountingMethod.accruals")
//  val journeyType: JourneyType = JourneyType(Add, ForeignProperty)
//  val testJourneyTypeString: String = JourneyType(Add, ForeignProperty).toString
//
//
//  object TestForeignPropertyCheckDetailsController extends ForeignPropertyCheckDetailsController(
//    app.injector.instanceOf[ForeignPropertyCheckDetails],
//    app.injector.instanceOf[SessionTimeoutPredicate],
//    MockAuthenticationPredicate,
//    mockAuthService,
//    app.injector.instanceOf[NinoPredicate],
//    MockIncomeSourceDetailsPredicate,
//    mockIncomeSourceDetailsService,
//    app.injector.instanceOf[NavBarPredicate],
//    businessDetailsService = mockBusinessDetailsService,
//    sessionService = mockSessionService,
//  )(
//    ec,
//    mcc = app.injector.instanceOf[MessagesControllerComponents],
//    appConfig,
//    app.injector.instanceOf[ItvcErrorHandler],
//    app.injector.instanceOf[AgentItvcErrorHandler]) {
//
//    val htmlTitle: String = s"${messages("htmlTitle", messages("incomeSources.add.foreign-property-check-details.title"))}"
//    val htmlTitleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.add.foreign-property-check-details.title"))}"
//    val title: String = messages("incomeSources.add.foreign-property-check-details.title")
//    val titleAgent: String = s"${messages("incomeSources.add.foreign-property-check-details.title")}"
//    val heading: String = messages("incomeSources.add.foreign-property-check-details.heading")
//    val link: String = s"${messages("incomeSources.add.foreign-property-check-details.change")}"
//  }
//
//  lazy val errorUrl: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.show(incomeSourceType = ForeignProperty).url
//  lazy val agentErrorUrl: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType = ForeignProperty).url
//
//  "ForeignPropertyCheckDetailsController" should {
//
//    "redirect a user back to the custom error page" when {
//      "the user is not authenticated" should {
//        "redirect them to sign in" in {
//          setupMockAuthorisationException()
//          val result = TestForeignPropertyCheckDetailsController.show()(fakeRequestWithActiveSession)
//          status(result) shouldBe SEE_OTHER
//          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
//        }
//      }
//      "agent is not authenticated" should {
//        "redirect them to sign in" in {
//          setupMockAgentAuthorisationException()
//          val result = TestForeignPropertyCheckDetailsController.showAgent()(fakeRequestConfirmedClient())
//          status(result) shouldBe SEE_OTHER
//          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
//        }
//      }
//    }
//    "the user has timed out" should {
//      "redirect to the session timeout page" in {
//        setupMockAuthorisationException()
//
//        val result = TestForeignPropertyCheckDetailsController.submit()(fakeRequestWithTimeoutSession)
//
//        status(result) shouldBe SEE_OTHER
//        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
//      }
//    }
//
//    "feature switch is disabled" should {
//      "redirect to home page (individual)" in {
//        disableAllSwitches()
//
//        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
//        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
//
//        val result: Future[Result] = TestForeignPropertyCheckDetailsController.show()(fakeRequestWithActiveSession)
//        status(result) shouldBe SEE_OTHER
//        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
//      }
//      "redirect to home page (agent)" in {
//        disableAllSwitches()
//
//        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
//        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
//
//        val result: Future[Result] = TestForeignPropertyCheckDetailsController.showAgent()(fakeRequestConfirmedClient())
//        status(result) shouldBe SEE_OTHER
//        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
//      }
//    }
//
//    "return 200 OK" when {
//      "the session contains full business details and FS enabled (individual)" in {
//        disableAllSwitches()
//        enable(IncomeSources)
//
//        mockNoIncomeSources()
//        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
//
//        setupMockCreateSession(true)
//        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType, Right(Some(testPropertyStartDate)))
//        setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, journeyType, Right(Some(accruals)))
//
//        val result = TestForeignPropertyCheckDetailsController.show()(fakeRequestWithActiveSession)
//
//        val document: Document = Jsoup.parse(contentAsString(result))
//        val changeDetailsLinks = document.select(".govuk-summary-list__actions .govuk-link")
//
//        status(result) shouldBe OK
//        document.title shouldBe TestForeignPropertyCheckDetailsController.htmlTitle
//        document.select("h1:nth-child(1)").text shouldBe TestForeignPropertyCheckDetailsController.title
//        changeDetailsLinks.first().text shouldBe TestForeignPropertyCheckDetailsController.link
//
//      }
//      "the session contains full business details and FS enabled (agent)" in {
//        disableAllSwitches()
//        enable(IncomeSources)
//
//        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
//        setupMockGetIncomeSourceDetails()(noIncomeDetails)
//
//        setupMockCreateSession(true)
//        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType, Right(Some(testPropertyStartDate)))
//        setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, journeyType, Right(Some(accruals)))
//
//        val result = TestForeignPropertyCheckDetailsController.showAgent()(fakeRequestConfirmedClient())
//
//        val document: Document = Jsoup.parse(contentAsString(result))
//        val changeDetailsLinks = document.select(".govuk-summary-list__actions .govuk-link")
//
//        status(result) shouldBe OK
//        document.title shouldBe TestForeignPropertyCheckDetailsController.htmlTitleAgent
//        document.select("h1:nth-child(1)").text shouldBe TestForeignPropertyCheckDetailsController.titleAgent
//        changeDetailsLinks.first().text shouldBe TestForeignPropertyCheckDetailsController.link
//      }
//    }
//  }
//
//  ".show" should {
//    "return an error" when {
//      "session data is missing a start date (individual)" in {
//        disableAllSwitches()
//        enable(IncomeSources)
//
//        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
//        setupMockGetIncomeSourceDetails()(noIncomeDetails)
//
//        setupMockCreateSession(true)
//        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType, Right(None))
//        setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, journeyType, Right(Some(accruals)))
//
//        val result = TestForeignPropertyCheckDetailsController.show()(fakeRequestWithActiveSession)
//
//        val document: Document = Jsoup.parse(contentAsString(result))
//
//        status(result) shouldBe INTERNAL_SERVER_ERROR
//        document.title shouldBe messages("standardError.heading") + " - GOV.UK"
//      }
//      "session data is missing a start date (agent)" in {
//        disableAllSwitches()
//        enable(IncomeSources)
//
//        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
//        setupMockGetIncomeSourceDetails()(noIncomeDetails)
//
//        setupMockCreateSession(true)
//        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType, Right(None))
//        setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, journeyType, Right(Some(accruals)))
//
//        val result = TestForeignPropertyCheckDetailsController.showAgent()(fakeRequestConfirmedClient())
//
//        val document: Document = Jsoup.parse(contentAsString(result))
//
//        status(result) shouldBe INTERNAL_SERVER_ERROR
//        document.title shouldBe messages("standardError.heading") + " - GOV.UK"
//      }
//
//      "session data is missing an accounting method (individual)" in {
//        disableAllSwitches()
//        enable(IncomeSources)
//
//        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
//        setupMockGetIncomeSourceDetails()(noIncomeDetails)
//
//        setupMockCreateSession(true)
//        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType, Right(Some(testPropertyStartDate)))
//        setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, journeyType, Right(None))
//        val result = TestForeignPropertyCheckDetailsController.show()(fakeRequestWithActiveSession)
//
//        val document: Document = Jsoup.parse(contentAsString(result))
//
//        status(result) shouldBe INTERNAL_SERVER_ERROR
//        document.title shouldBe messages("standardError.heading") + " - GOV.UK"
//      }
//      "session data is missing an accounting method (agent)" in {
//        disableAllSwitches()
//        enable(IncomeSources)
//
//        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
//        setupMockGetIncomeSourceDetails()(noIncomeDetails)
//
//        setupMockCreateSession(true)
//        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType, Right(Some(testPropertyStartDate)))
//        setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, journeyType, Right(None))
//        val result = TestForeignPropertyCheckDetailsController.showAgent()(fakeRequestConfirmedClient())
//
//        val document: Document = Jsoup.parse(contentAsString(result))
//
//        status(result) shouldBe INTERNAL_SERVER_ERROR
//        document.title shouldBe messages("standardError.heading") + " - GOV.UK"
//      }
//    }
//  }
//
//  ".submit" should {
//    "redirect to the reporting method page" when {
//      "foreign property model successfully created and user clicks continue button (individual)" in {
//        disableAllSwitches()
//        enable(IncomeSources)
//
//        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
//        setupMockGetIncomeSourceDetails()(noIncomeDetails)
//
//        setupMockCreateSession(true)
//        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType, Right(Some(testPropertyStartDate)))
//        setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, journeyType, Right(Some(testPropertyAccountingMethod)))
//
//        when(mockBusinessDetailsService.createForeignProperty(any())(any(), any(), any())).
//          thenReturn(Future(Right(CreateIncomeSourceResponse("123"))))
//        val result = TestForeignPropertyCheckDetailsController.submit()(fakeRequestWithActiveSession)
//
//        status(result) shouldBe SEE_OTHER
//        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.show("123").url)
//      }
//      "foreign property model successfully created and user clicks continue button (agent)" in {
//        disableAllSwitches()
//        enable(IncomeSources)
//
//        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
//        setupMockGetIncomeSourceDetails()(noIncomeDetails)
//
//        setupMockCreateSession(true)
//        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType, Right(Some(testPropertyStartDate)))
//        setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, journeyType, Right(Some(testPropertyAccountingMethod)))
//
//        when(mockBusinessDetailsService.createForeignProperty(any())(any(), any(), any())).
//          thenReturn(Future(Right(CreateIncomeSourceResponse("123"))))
//
//        val result = TestForeignPropertyCheckDetailsController.submitAgent(fakeRequestConfirmedClient())
//
//        status(result) shouldBe SEE_OTHER
//        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.showAgent("123").url)
//      }
//    }
//    "redirect to custom error page" when {
//      "individual is missing session storage" in {
//        disableAllSwitches()
//        enable(IncomeSources)
//
//        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
//        setupMockGetIncomeSourceDetails()(noIncomeDetails)
//        setupMockCreateSession(true)
//        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType, Right(None))
//        setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, journeyType, Right(Some(accruals)))
//
//        val result = TestForeignPropertyCheckDetailsController.submit()(fakeRequestWithActiveSession)
//
//        status(result) shouldBe SEE_OTHER
//        redirectLocation(result) shouldBe Some(errorUrl)
//      }
//      "agent is missing session storage" in {
//        disableAllSwitches()
//        enable(IncomeSources)
//
//        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
//        setupMockGetIncomeSourceDetails()(noIncomeDetails)
//
//        setupMockCreateSession(true)
//        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType, Right(None))
//        setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, journeyType, Right(Some(accruals)))
//
//        val result = TestForeignPropertyCheckDetailsController.submitAgent(fakeRequestConfirmedClient())
//
//        status(result) shouldBe SEE_OTHER
//        redirectLocation(result) shouldBe Some(agentErrorUrl)
//      }
//
//      "foreign property model can't be created (individual)" in {
//        disableAllSwitches()
//        enable(IncomeSources)
//
//        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
//        setupMockGetIncomeSourceDetails()(noIncomeDetails)
//
//        setupMockCreateSession(true)
//        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType, Right(Some(testPropertyStartDate)))
//        setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, journeyType, Right(Some(testPropertyAccountingMethod)))
//
//        when(mockBusinessDetailsService.createForeignProperty(any())(any(), any(), any())).
//          thenReturn(Future(Left(new Error(s"Failed to created incomeSources"))))
//        val result = TestForeignPropertyCheckDetailsController.submit()(fakeRequestWithActiveSession)
//
//        status(result) shouldBe SEE_OTHER
//        redirectLocation(result) shouldBe Some(errorUrl)
//      }
//      "foreign property model cannot be created (agent)" in {
//        disableAllSwitches()
//        enable(IncomeSources)
//
//        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
//        setupMockGetIncomeSourceDetails()(noIncomeDetails)
//
//        setupMockCreateSession(true)
//        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyType, Right(Some(testPropertyStartDate)))
//        setupMockGetSessionKeyMongoTyped[String](incomeSourcesAccountingMethodField, journeyType, Right(Some(testPropertyAccountingMethod)))
//
//        when(mockBusinessDetailsService.createForeignProperty(any())(any(), any(), any())).
//          thenReturn(Future(Left(new Error(s"Failed to created incomeSources"))))
//
//        val result = TestForeignPropertyCheckDetailsController.submitAgent(fakeRequestConfirmedClient())
//
//        status(result) shouldBe SEE_OTHER
//        redirectLocation(result) shouldBe Some(agentErrorUrl)
//      }
//    }
//  }
//}