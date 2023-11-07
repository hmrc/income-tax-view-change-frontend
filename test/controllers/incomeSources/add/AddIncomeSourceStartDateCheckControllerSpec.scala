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

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.AddIncomeSourceStartDateCheckForm
import forms.utils.SessionKeys
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockSessionService}
import models.incomeSourceDetails.AddIncomeSourceData.{accountingPeriodStartDateField, dateStartedField}
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, verify}
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.SessionService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.AddIncomeSourceStartDateCheck

import java.time.LocalDate
import scala.concurrent.Future


class AddIncomeSourceStartDateCheckControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with ImplicitDateFormatter
  with FeatureSwitching
  with MockSessionService {

  val testStartDate: LocalDate = LocalDate.of(2022, 11, 11)
  val testBusinessAccountingPeriodStartDate: LocalDate = LocalDate.of(2022, 11, 11)
  val testBusinessAccountingPeriodEndDate: LocalDate = LocalDate.of(2023, 4, 5)

  val responseNo: String = AddIncomeSourceStartDateCheckForm.responseNo
  val responseYes: String = AddIncomeSourceStartDateCheckForm.responseYes

  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val journeyTypeSE = JourneyType(Add, SelfEmployment)
  val journeyTypeUK = JourneyType(Add, UkProperty)
  val journeyTypeFP = JourneyType(Add, ForeignProperty)

  val addIncomeSourceDataEmpty = AddIncomeSourceData()
  val addIncomeSourceDataProperty = AddIncomeSourceData(dateStarted = Some(testStartDate))
  val addIncomeSourceDataSE = AddIncomeSourceData(dateStarted = Some(testStartDate), accountingPeriodStartDate = Some(testBusinessAccountingPeriodStartDate),
    accountingPeriodEndDate = Some(testBusinessAccountingPeriodEndDate))

  val uiJourneySessionDataSE: UIJourneySessionData = UIJourneySessionData("session-123456", "ADD-SE", Some(addIncomeSourceDataSE))
  val uiJourneySessionDataUK: UIJourneySessionData = UIJourneySessionData("session-123456", "ADD-UK", Some(addIncomeSourceDataProperty))
  val uiJourneySessionDataFP: UIJourneySessionData = UIJourneySessionData("session-123456", "ADD-FP", Some(addIncomeSourceDataProperty))

  object TestAddIncomeSourceStartDateCheckController
    extends AddIncomeSourceStartDateCheckController(authenticate = MockAuthenticationPredicate,
      authorisedFunctions = mockAuthService,
      checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
      retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
      retrieveBtaNavBar = MockNavBarPredicate,
      retrieveNino = app.injector.instanceOf[NinoPredicate],
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
      addIncomeSourceStartDateCheckView = app.injector.instanceOf[AddIncomeSourceStartDateCheck],
      languageUtils = languageUtils,
      sessionService = mockSessionService
    )(
      app.injector.instanceOf[FrontendAppConfig],
      dateService,
      app.injector.instanceOf[MessagesControllerComponents],
      ec,
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[AgentItvcErrorHandler]
    ) {

    val heading: String = messages("dateForm.check.heading")
    val titleAgent: String = s"${messages("htmlTitle.agent", heading)}"
    val title: String = s"${messages("htmlTitle", heading)}"
  }

  def verifyMongoDatesRemoved(): Unit = {
    val argument: ArgumentCaptor[UIJourneySessionData] = ArgumentCaptor.forClass(classOf[UIJourneySessionData])
    verify(mockSessionService).setMongoData(argument.capture())(any(), any())

    argument.getValue.addIncomeSourceData shouldBe Some(addIncomeSourceDataEmpty)
  }

  def verifySetMongoData(incomeSourceType: IncomeSourceType): Unit = {
    val argument: ArgumentCaptor[UIJourneySessionData] = ArgumentCaptor.forClass(classOf[UIJourneySessionData])
    verify(mockSessionService).setMongoData(argument.capture())(any(), any())

    if (incomeSourceType.equals(SelfEmployment)) {
      argument.getValue.addIncomeSourceData shouldBe Some(addIncomeSourceDataSE)
    } else {
      argument.getValue.addIncomeSourceData shouldBe Some(addIncomeSourceDataProperty)
    }
  }

  "Individual - AddIncomeSourceStartDateCheckController.show" should {
    s"return ${Status.OK}: render the custom not found error view" when {
      "Income Sources FS is disabled" in {
        disableAllSwitches()
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        val redirectUrl = controllers.routes.HomeController.show().url
        redirectLocation(result) shouldBe Some(redirectUrl)

      }
    }
    s"return ${Status.SEE_OTHER}: return to home page" when {
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = false, isChange = false)(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/sign-in")
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      s"calling Business Start Date Check Page but session does not contain key: ${SessionKeys.addBusinessStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(fakeRequestWithActiveSession)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      s"calling UK Property Start Date Check Page but session does not contain key: ${SessionKeys.addUkPropertyStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = false, isChange = false)(fakeRequestWithActiveSession)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      s"calling Foreign Property Start Date Check Page but session does not contain key: ${SessionKeys.foreignPropertyStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = false, isChange = false)(fakeRequestWithActiveSession)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.OK}" when {
      s"calling Business Start Date Check Page and session contains key: ${SessionKeys.addBusinessStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeSE, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(
          fakeRequestWithActiveSession
        )

        val document: Document = Jsoup.parse(contentAsString(result))

        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false).url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}" when {
      s"calling Foreign Property Start Date Check Page and session contains key: ${SessionKeys.foreignPropertyStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeFP, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = false)(
          fakeRequestWithActiveSession
        )

        val document: Document = Jsoup.parse(contentAsString(result))

        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = false).url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}" when {
      s"calling UK Property Start Date Check Page and session contains key: ${SessionKeys.addUkPropertyStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeUK, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = false, isChange = false)(
          fakeRequestWithActiveSession
        )

        val document: Document = Jsoup.parse(contentAsString(result))

        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = false, isChange = false).url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add Business Start Date Check Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeSE, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = true)(
          fakeRequestWithActiveSession
        )

        val document: Document = Jsoup.parse(contentAsString(result))

        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = true).url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add Foreign Property Start Date Check Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeFP, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = true)(
          fakeRequestWithActiveSession
        )

        val document: Document = Jsoup.parse(contentAsString(result))

        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = true).url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add UK Property Start Date Check Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeUK, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = false, isChange = true)(
          fakeRequestWithActiveSession
        )

        val document: Document = Jsoup.parse(contentAsString(result))

        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = false, isChange = true).url
        status(result) shouldBe OK
      }
    }
  }
  "Individual - AddIncomeSourceStartDateCheckController.submit" should {
    s"return ${Status.SEE_OTHER}: redirect to home page" when {
      "IncomeSources FS is disabled" in {
        disableAllSwitches()

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        val redirectUrl = controllers.routes.HomeController.show().url
        redirectLocation(result) shouldBe Some(redirectUrl)
      }
    }
  }
  "Individual - Sole Trader Business - AddIncomeSourceStartDateCheckController.submit" should {
    s"return ${Status.BAD_REQUEST} with an error summary" when {
      "form is submitted with neither radio option selected" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeSE, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession

            .withFormUrlEncodedBody())

        status(result) shouldBe BAD_REQUEST
      }
      "an invalid response is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeSE, Right(Some(testStartDate)))

        val invalidResponse: String = "£££"

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> invalidResponse
            ))

        status(result) shouldBe BAD_REQUEST
      }
    }
    s"return ${Status.SEE_OTHER}: redirect back to add business start date page with businessStartDate removed from session" when {
      "No is submitted with the form" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeSE, Right(Some(testStartDate)))
        setupMockGetMongo(Right(Some(uiJourneySessionDataSE)))
        setupMockSetMongoData(result = true)

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseNo
            ))

        status(result) shouldBe SEE_OTHER
        verifyMongoDatesRemoved()
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false).url)
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to add business trade page" when {
      "Yes is submitted with the form with a valid session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeSE, Right(Some(testStartDate)))
        setupMockGetMongo(Right(Some(uiJourneySessionDataSE)))
        setupMockSetMongoData(result = true)

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        verifySetMongoData(SelfEmployment)
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddBusinessTradeController.show(isAgent = false, isChange = false).url)
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to check business details page" when {
      "Yes is submitted with isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeSE, Right(Some(testStartDate)))
        setupMockGetMongo(Right(Some(uiJourneySessionDataSE)))
        setupMockSetMongoData(result = true)

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = true)(
          fakePostRequestWithActiveSession
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        verifySetMongoData(SelfEmployment)
        redirectLocation(result) shouldBe Some(routes.IncomeSourceCheckDetailsController.show(SelfEmployment).url)
      }
    }
  }
  "Individual - UK Property - AddIncomeSourceStartDateCheckController.submit" should {
    s"return ${Status.BAD_REQUEST} with an error summary" when {
      "form is submitted with neither radio option selected" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeUK, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession
            .withFormUrlEncodedBody())

        status(result) shouldBe BAD_REQUEST
      }
      "an invalid response is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeUK, Right(Some(testStartDate)))

        val invalidResponse: String = "£££"

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> invalidResponse
            ))

        status(result) shouldBe BAD_REQUEST
      }
    }
    s"return ${Status.SEE_OTHER}: redirect back to add UK Property start date page with ${SessionKeys.addUkPropertyStartDate} removed from session" when {
      "No is submitted with the form" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeUK, Right(Some(testStartDate)))
        setupMockGetMongo(Right(Some(uiJourneySessionDataUK)))
        setupMockSetMongoData(result = true)

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseNo
            ))

        status(result) shouldBe SEE_OTHER
        verifyMongoDatesRemoved()
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = false, isChange = false).url)
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to UK Property accounting method page" when {
      "Yes is submitted with the form with a valid session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeUK, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(UkProperty).url)
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to check uk property details page" when {
      "Yes is submitted with isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeUK, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = true)(
          fakePostRequestWithActiveSession

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.IncomeSourceCheckDetailsController.show(UkProperty).url)
      }
    }
  }
  "Individual - Foreign Property - AddIncomeSourceStartDateCheckController.submit" should {
    s"return ${Status.BAD_REQUEST} with an error summary" when {
      "form is submitted with neither radio option selected" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeFP, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = ForeignProperty, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession

            .withFormUrlEncodedBody())

        status(result) shouldBe BAD_REQUEST
      }
      "an invalid response is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeFP, Right(Some(testStartDate)))

        val invalidResponse: String = "£££"

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = ForeignProperty, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> invalidResponse
            ))

        status(result) shouldBe BAD_REQUEST
      }
    }
    s"return ${Status.SEE_OTHER}: redirect back to add Foreign Property start date page with ${SessionKeys.foreignPropertyStartDate} removed from session" when {
      "No is submitted with the form" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeFP, Right(Some(testStartDate)))
        setupMockGetMongo(Right(Some(uiJourneySessionDataFP)))
        setupMockSetMongoData(result = true)

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = ForeignProperty, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseNo
            ))

        status(result) shouldBe SEE_OTHER
        verifyMongoDatesRemoved()
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = false).url)
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to Foreign Property accounting method page" when {
      "Yes is submitted with the form with a valid session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeFP, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = ForeignProperty, isAgent = false, isChange = false)(
          fakePostRequestWithActiveSession

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(ForeignProperty).url)
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to check foreign property details page" when {
      "Yes is submitted with isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeFP, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = ForeignProperty, isAgent = false, isChange = true)(
          fakePostRequestWithActiveSession

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.IncomeSourceCheckDetailsController.show(ForeignProperty).url)
      }
    }
  }

  "Agent - AddIncomeSourceStartDateCheckController.show" should {
    s"return ${Status.OK}: render the custom not found error view" when {
      "Income Sources FS is disabled" in {
        disable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(fakeRequestConfirmedClient())

        status(result) shouldBe SEE_OTHER
        val redirectUrl = controllers.routes.HomeController.showAgent.url
        redirectLocation(result) shouldBe Some(redirectUrl)

      }
    }
    s"return ${Status.SEE_OTHER}: redirect to home page" when {
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = true, isChange = false)(fakeRequestConfirmedClient())

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/sign-in")
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      s"calling Business Start Date Check Page but session does not contain key: ${SessionKeys.addBusinessStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeSE, Right(None))

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      s"calling UK Property Start Date Check Page but session does not contain key: ${SessionKeys.addUkPropertyStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeUK, Right(None))

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = true, isChange = false)(fakeRequestConfirmedClient())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      s"calling Foreign Property Start Date Check Page and session does not contain key: ${SessionKeys.foreignPropertyStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeFP, Right(None))

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = false)(
          fakeRequestConfirmedClient()
        )

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    s"return ${Status.OK}" when {
      s"calling Business Start Date Check Page and session contains key: ${SessionKeys.addBusinessStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeSE, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(
          fakeRequestConfirmedClient()
        )

        val document: Document = Jsoup.parse(contentAsString(result))

        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false).url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}" when {
      s"calling UK Property Start Date Check Page and session contains key: ${SessionKeys.addUkPropertyStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeUK, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = true, isChange = false)(
          fakeRequestConfirmedClient())

        val document: Document = Jsoup.parse(contentAsString(result))

        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = true, isChange = false).url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}" when {
      s"calling Foreign Property Start Date Check Page and session contains key: ${SessionKeys.foreignPropertyStartDate}" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeFP, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = false)(
          fakeRequestConfirmedClient()
        )

        val document: Document = Jsoup.parse(contentAsString(result))

        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = false).url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add Business Start Date Check Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeSE, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = true)(
          fakeRequestConfirmedClient()
        )

        val document: Document = Jsoup.parse(contentAsString(result))

        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = true).url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add Foreign Property Start Date Check Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeFP, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = true)(
          fakeRequestConfirmedClient()
        )

        val document: Document = Jsoup.parse(contentAsString(result))

        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = true).url
        status(result) shouldBe OK
      }
    }
    s"return ${Status.OK}: render the Add UK Property Start Date Check Change page" when {
      "isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeUK, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = true, isChange = true)(
          fakeRequestConfirmedClient()
        )

        val document: Document = Jsoup.parse(contentAsString(result))

        document.getElementById("back").attr("href") shouldBe routes.AddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = true, isChange = true).url
        status(result) shouldBe OK
      }
    }
  }
  "Agent - AddIncomeSourceStartDateCheckController.submit" should {
    s"return ${Status.SEE_OTHER}: redirect to home page" when {
      "IncomeSources FS is disabled" in {
        disableAllSwitches()

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(
          fakeRequestConfirmedClient()
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        val redirectUrl = controllers.routes.HomeController.showAgent.url
        redirectLocation(result) shouldBe Some(redirectUrl)
      }
    }
  }
  "Agent - Sole Trader Business - AddIncomeSourceStartDateCheckController.submit" should {
    s"return ${Status.BAD_REQUEST} with an error summary" when {
      "form is submitted with neither radio option selected" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeSE, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(
          fakePostRequestConfirmedClient()

            .withFormUrlEncodedBody())

        status(result) shouldBe BAD_REQUEST
      }
      "an invalid response is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeSE, Right(Some(testStartDate)))

        val invalidResponse: String = "£££"

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(
          fakePostRequestConfirmedClient()

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> invalidResponse
            ))

        status(result) shouldBe BAD_REQUEST
      }
    }
    s"return ${Status.SEE_OTHER}: redirect back to add business start date page with businessStartDate removed from session" when {
      "No is submitted with the form" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeSE, Right(Some(testStartDate)))
        setupMockGetMongo(Right(Some(uiJourneySessionDataSE)))
        setupMockSetMongoData(result = true)

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(
          fakePostRequestConfirmedClient()
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseNo
            ))

        status(result) shouldBe SEE_OTHER
        verifyMongoDatesRemoved()
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false).url)
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to add business trade page" when {
      "Yes is submitted with the form with a valid session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeSE, Right(Some(testStartDate)))
        setupMockGetMongo(Right(Some(uiJourneySessionDataSE)))
        setupMockSetMongoData(result = true)

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = true, isChange = false)(
          fakePostRequestConfirmedClient()

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        verifySetMongoData(SelfEmployment)
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddBusinessTradeController.show(isAgent = true, isChange = false).url)
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to check business details page" when {
      "Yes is submitted with isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeSE, Right(Some(testStartDate)))
        setupMockGetMongo(Right(Some(uiJourneySessionDataSE)))
        setupMockSetMongoData(result = true)

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = true, isChange = true)(
          fakePostRequestConfirmedClient()
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        verifySetMongoData(SelfEmployment)
        redirectLocation(result) shouldBe Some(routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment).url)
      }
    }
  }
  "Agent - UK Property - AddIncomeSourceStartDateCheckController.submit" should {
    s"return ${Status.BAD_REQUEST} with an error summary" when {
      "form is submitted with neither radio option selected" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeUK, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = UkProperty, isAgent = true, isChange = false)(
          fakePostRequestConfirmedClient()

            .withFormUrlEncodedBody())

        status(result) shouldBe BAD_REQUEST
      }
      "an invalid response is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeUK, Right(Some(testStartDate)))

        val invalidResponse: String = "£££"

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = UkProperty, isAgent = true, isChange = false)(
          fakePostRequestConfirmedClient()

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> invalidResponse
            ))

        status(result) shouldBe BAD_REQUEST
      }
    }
    s"return ${Status.SEE_OTHER}: redirect back to add UK Property start date page with ${SessionKeys.addUkPropertyStartDate} removed from session" when {
      "No is submitted with the form" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeUK, Right(Some(testStartDate)))
        setupMockGetMongo(Right(Some(uiJourneySessionDataUK)))
        setupMockSetMongoData(result = true)

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = UkProperty, isAgent = true, isChange = false)(
          fakePostRequestConfirmedClient()
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseNo
            ))

        status(result) shouldBe SEE_OTHER
        verifyMongoDatesRemoved()
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = true, isChange = false).url)
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to UK Property accounting method page" when {
      "Yes is submitted with the form with a valid session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeUK, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = UkProperty, isAgent = true, isChange = false)(
          fakePostRequestConfirmedClient()

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(UkProperty).url)
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to check uk property details page" when {
      "Yes is submitted with isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeUK, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = UkProperty, isAgent = true, isChange = true)(
          fakePostRequestConfirmedClient()

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.IncomeSourceCheckDetailsController.showAgent(UkProperty).url)
      }
    }
  }
  "Agent - Foreign Property - AddIncomeSourceStartDateCheckController.submit" should {
    s"return ${Status.BAD_REQUEST} with an error summary" when {
      "form is submitted with neither radio option selected" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeFP, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = ForeignProperty, isAgent = true, isChange = false)(
          fakePostRequestConfirmedClient()

            .withFormUrlEncodedBody())

        status(result) shouldBe BAD_REQUEST
      }
      "an invalid response is submitted" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeFP, Right(Some(testStartDate)))
        setupMockGetMongo(Right(Some(uiJourneySessionDataFP)))

        val invalidResponse: String = "£££"

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = ForeignProperty, isAgent = true, isChange = false)(
          fakePostRequestConfirmedClient()

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> invalidResponse
            ))

        status(result) shouldBe BAD_REQUEST
      }
    }
    s"return ${Status.SEE_OTHER}: redirect back to add Foreign Property start date page with ${SessionKeys.foreignPropertyStartDate} removed from session" when {
      "No is submitted with the form" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeFP, Right(Some(testStartDate)))
        setupMockGetMongo(Right(Some(uiJourneySessionDataFP)))
        setupMockSetMongoData(result = true)

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = ForeignProperty, isAgent = true, isChange = false)(
          fakePostRequestConfirmedClient()
            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseNo
            ))

        status(result) shouldBe SEE_OTHER
        verifyMongoDatesRemoved()
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = false).url)
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to Foreign Property accounting method page" when {
      "Yes is submitted with the form with a valid session" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeFP, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = ForeignProperty, isAgent = true, isChange = false)(
          fakePostRequestConfirmedClient()

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(ForeignProperty).url)
      }
    }
    s"return ${Status.SEE_OTHER}: redirect to check foreign property details page" when {
      "Yes is submitted with isUpdate flag set to true" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockGetSessionKeyMongoTyped[LocalDate](dateStartedField, journeyTypeFP, Right(Some(testStartDate)))

        val result = TestAddIncomeSourceStartDateCheckController.submit(incomeSourceType = ForeignProperty, isAgent = true, isChange = true)(
          fakePostRequestConfirmedClient()

            .withFormUrlEncodedBody(
              AddIncomeSourceStartDateCheckForm.response -> responseYes
            ))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.IncomeSourceCheckDetailsController.showAgent(ForeignProperty).url)
      }
    }
  }
}

