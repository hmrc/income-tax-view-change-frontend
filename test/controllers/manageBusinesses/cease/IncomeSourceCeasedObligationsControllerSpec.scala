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

package controllers.manageBusinesses.cease

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney._
import enums.JourneyType.{Cease, JourneyType}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockNextUpdatesService, MockSessionService}
import models.incomeSourceDetails._
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, NextUpdatesResponseModel, ObligationsModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.mockito.stubbing.OngoingStubbing
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import testConstants.BaseTestConstants.{testNino, testPropertyIncomeId, testSelfEmploymentId, testSessionId}
import testConstants.BusinessDetailsTestConstants.testIncomeSource
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{foreignPropertyIncomeWithCeasedForiegnPropertyIncome, ukPropertyIncomeWithCeasedUkPropertyIncome}
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants.quarterlyObligationDatesSimple
import testUtils.TestSupport
import utils.IncomeSourcesUtils
import views.html.manageBusinesses.cease.IncomeSourceCeasedObligations

import java.time.LocalDate
import scala.concurrent.Future

class IncomeSourceCeasedObligationsControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with MockNextUpdatesService
  with FeatureSwitching
  with MockSessionService {

  val mockIncomeSourcesUtils: IncomeSourcesUtils = mock(classOf[IncomeSourcesUtils])

  object TestIncomeSourceCeasedObligationsController extends IncomeSourceCeasedObligationsController(
    authorisedFunctions = mockAuthService,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    obligationsView = app.injector.instanceOf[IncomeSourceCeasedObligations],
    sessionService = mockSessionService,
    nextUpdatesService = mockNextUpdatesService,
    auth = testAuthenticator
  )(
    ec = ec,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    dateService = dateService
  )

  private def setMongoSessionData(incomeSourceType: IncomeSourceType, incomeSourceId: Option[String] = Some(testSelfEmploymentId),
                                  ceaseDate: Option[LocalDate] = Some(LocalDate.of(2022, 10, 10))): Unit = {
    setupMockCreateSession(true)
    val sessionData = UIJourneySessionData(
      sessionId = testSessionId,
      journeyType = JourneyType(Cease, incomeSourceType).toString,
      ceaseIncomeSourceData = Some(CeaseIncomeSourceData(
        incomeSourceId = incomeSourceId,
        endDate = ceaseDate,
        ceaseIncomeSourceDeclare = Some("true"),
        journeyIsComplete = None
      ))
    )
    setupMockGetMongo(Right(Some(sessionData)))
  }


  val testId = "XAIS00000000001"

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(
    NextUpdatesModel("123", List(NextUpdateModel(
      LocalDate.of(2022, 7, 1),
      LocalDate.of(2022, 7, 2),
      LocalDate.of(2022, 8, 2),
      "Quarterly",
      None,
      "#001"
    ),
      NextUpdateModel(
        LocalDate.of(2022, 7, 1),
        LocalDate.of(2022, 7, 2),
        LocalDate.of(2022, 8, 2),
        "Quarterly",
        None,
        "#002"
      )
    ))
  ))
  private val propertyDetailsModelUK = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some("uk-property"),
    tradingStartDate = None,
    cessation = None,
    cashOrAccruals = false,
    latencyDetails = None
  )
  private val propertyDetailsModelForeign = propertyDetailsModelUK.copy(incomeSourceType = Some("foreign-property"))

  def setUpBusiness(isAgent: Boolean): OngoingStubbing[Future[NextUpdatesResponseModel]] = {
    setupMockAuthorisationSuccess(isAgent)

    val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel(testNino, "", Some("2022"), List(BusinessDetailsModel(
      testId,
      incomeSource = Some(testIncomeSource),
      None,
      Some("Test name"),
      None,
      Some(LocalDate.of(2022, 1, 1)),
      None,
      cashOrAccruals = false
    )), List.empty)
    setupMockGetIncomeSourceDetails()(sources)

    val day = LocalDate.of(2023, 1, 1)
    val dates: Seq[DatesModel] = Seq(
      DatesModel(day, day, day, "EOPS", isFinalDec = false, obligationType = "EOPS")
    )
    when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(Future(ObligationsViewModel(
      quarterlyObligationDatesSimple,
      dates,
      dates,
      2023,
      showPrevTaxYears = true
    )))
    when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
      thenReturn(Future(testObligationsModel))
  }

  def setUpProperty(isAgent: Boolean, isUkProperty: Boolean): OngoingStubbing[Future[NextUpdatesResponseModel]] = {
    setupMockAuthorisationSuccess(isAgent)

    if (isUkProperty) {
      setupMockGetIncomeSourceDetails()(ukPropertyIncomeWithCeasedUkPropertyIncome)
      when(mockIncomeSourcesUtils.getActiveProperty(any())(any()))
        .thenReturn(Some(propertyDetailsModelUK))
    }
    else {
      setupMockGetIncomeSourceDetails()(foreignPropertyIncomeWithCeasedForiegnPropertyIncome)
      when(mockIncomeSourcesUtils.getActiveProperty(any())(any()))
        .thenReturn(Some(propertyDetailsModelForeign))
    }
    val day = LocalDate.of(2023, 1, 1)
    val dates: Seq[DatesModel] = Seq(
      DatesModel(day, day, day, "EOPS", isFinalDec = false, obligationType = "EOPS")
    )
    when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(Future(ObligationsViewModel(
      quarterlyObligationDatesSimple,
      dates,
      dates,
      2023,
      showPrevTaxYears = true
    )))
    when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
      thenReturn(Future(testObligationsModel))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIncomeSourceDetailsService)
    disableAllSwitches()
    enable(IncomeSources)
  }

  "IncomeSourceCeasedObligationsController" should {
    s"return $OK showing ceased business obligation page - Individual" when {
      val isAgent = false
      s"business type is $SelfEmployment and all required data is available" in {
        val incomeSourceType = SelfEmployment
        setupMockAuthorisationSuccess(isAgent)
        setUpBusiness(isAgent = false)
        setMongoSessionData(incomeSourceType)
        val result: Future[Result] = TestIncomeSourceCeasedObligationsController.show(incomeSourceType)(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
      s"business type is $UkProperty and all required data is available" in {
        val incomeSourceType = UkProperty
        setupMockAuthorisationSuccess(isAgent)
        setUpProperty(isAgent = isAgent, isUkProperty = true)
        setMongoSessionData(incomeSourceType)
        val result: Future[Result] = TestIncomeSourceCeasedObligationsController.show(incomeSourceType)(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
      s"business type is $ForeignProperty and all required data is available" in {
        val incomeSourceType = ForeignProperty
        setupMockAuthorisationSuccess(isAgent)
        setUpProperty(isAgent = isAgent, isUkProperty = false)
        setMongoSessionData(incomeSourceType)
        val result: Future[Result] = TestIncomeSourceCeasedObligationsController.show(incomeSourceType)(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
    }
    s"return $OK showing ceased business obligation page - Agent" when {
      val isAgent = true
      s"business type is $SelfEmployment and all required data is available" in {
        val incomeSourceType = SelfEmployment
        setupMockAuthorisationSuccess(isAgent)
        setUpBusiness(isAgent = isAgent)
        setMongoSessionData(incomeSourceType)
        val result: Future[Result] = TestIncomeSourceCeasedObligationsController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
      s"business type is $UkProperty and all required data is available" in {
        val incomeSourceType = UkProperty
        setupMockAuthorisationSuccess(isAgent)
        setUpProperty(isAgent = isAgent, isUkProperty = true)
        setMongoSessionData(incomeSourceType)
        val result: Future[Result] = TestIncomeSourceCeasedObligationsController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
      s"business type is $ForeignProperty and all required data is available" in {
        val incomeSourceType = ForeignProperty
        setupMockAuthorisationSuccess(isAgent)
        setUpProperty(isAgent = isAgent, isUkProperty = false)
        setMongoSessionData(incomeSourceType)
        val result: Future[Result] = TestIncomeSourceCeasedObligationsController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
        status(result) shouldBe OK
      }
    }
    s"return $INTERNAL_SERVER_ERROR showing error page" when {
      val isAgent = false
      s"income source ID is missing in session for $SelfEmployment business" in {
        val incomeSourceType = SelfEmployment
        setMongoSessionData(incomeSourceId = None, incomeSourceType = incomeSourceType)
        setUpBusiness(isAgent = isAgent)
        setupMockAuthorisationSuccess(isAgent)

        val result: Future[Result] = TestIncomeSourceCeasedObligationsController.show(incomeSourceType)(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      s"income source ID doesn't match any business for $UkProperty business" in {
        val incomeSourceType = UkProperty
        setMongoSessionData(incomeSourceId = None, incomeSourceType = incomeSourceType)
        setUpBusiness(isAgent = isAgent)
        setupMockAuthorisationSuccess(isAgent)
        val result: Future[Result] = TestIncomeSourceCeasedObligationsController.show(incomeSourceType)(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR

      }
      s"income source ID doesn't match any business for $ForeignProperty business" in {
        val incomeSourceType = ForeignProperty
        setMongoSessionData(incomeSourceId = None, incomeSourceType = incomeSourceType)
        setUpBusiness(isAgent = isAgent)
        setupMockAuthorisationSuccess(isAgent)
        val result: Future[Result] = TestIncomeSourceCeasedObligationsController.show(incomeSourceType)(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      s"business end date is missing in session" in {
        val incomeSourceType = SelfEmployment
        setMongoSessionData(incomeSourceId = Some(testId), ceaseDate = None, incomeSourceType = incomeSourceType)
        setUpBusiness(isAgent = isAgent)
        setupMockAuthorisationSuccess(isAgent)
        val result: Future[Result] = TestIncomeSourceCeasedObligationsController.show(incomeSourceType)(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      s"business end date and income source ID is missing in session" in {
        val incomeSourceType = UkProperty
        setMongoSessionData(incomeSourceId = None, ceaseDate = None, incomeSourceType = incomeSourceType)
        setUpProperty(isAgent = isAgent, isUkProperty = false)
        setupMockAuthorisationSuccess(isAgent)
        val result: Future[Result] = TestIncomeSourceCeasedObligationsController.show(incomeSourceType)(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
