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
import enums.IncomeSourceJourney._
import enums.JourneyType.{Add, JourneyType}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockNextUpdatesService, MockSessionService}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails._
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.DateService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testNino, testSelfEmploymentId, testSessionId}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessesAndPropertyIncome, notCompletedUIJourneySessionData}
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import views.html.incomeSources.add.IncomeSourceAddedObligations

import java.time.LocalDate
import scala.concurrent.Future

class IncomeSourceAddedControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with MockNextUpdatesService
  with MockSessionService
  with FeatureSwitching {

  val mockDateService: DateService = mock(classOf[DateService])

  object TestIncomeSourceAddedController extends IncomeSourceAddedController(
    authorisedFunctions = mockAuthService,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    obligationsView = app.injector.instanceOf[IncomeSourceAddedObligations],
    mockNextUpdatesService,
    testAuthenticator
  )(
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    mockSessionService,
    ec = ec,
    mockDateService
  )

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(
    NextUpdatesModel(testSelfEmploymentId, List(NextUpdateModel(
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

  def mockSelfEmployment(): Unit = {
    when(mockIncomeSourceDetailsService.getIncomeSourceFromUser(any(), mkIncomeSourceId(any()))(any())).thenReturn(
      Some((LocalDate.parse("2022-01-01"), Some("Business Name")))
    )
  }

  def mockProperty(): Unit = {
    when(mockIncomeSourceDetailsService.getIncomeSourceFromUser(any(), mkIncomeSourceId(any()))(any())).thenReturn(
      Some((LocalDate.parse("2022-01-01"), None))
    )
  }

  def mockISDS(incomeSourceType: IncomeSourceType): Unit = {
    if (incomeSourceType == SelfEmployment)
      when(mockIncomeSourceDetailsService.getIncomeSourceFromUser(any(), mkIncomeSourceId(any()))(any())).thenReturn(
        Some((LocalDate.parse("2022-01-01"), Some("Business Name")))
      )
    else
      when(mockIncomeSourceDetailsService.getIncomeSourceFromUser(any(), mkIncomeSourceId(any()))(any())).thenReturn(
        Some((LocalDate.parse("2022-01-01"), None))
      )
  }

  def mockFailure(): Unit = {
    when(mockIncomeSourceDetailsService.getIncomeSourceFromUser(any(), mkIncomeSourceId(any()))(any())).thenReturn(
      None
    )
  }

  def mockMongo(incomeSourceType: IncomeSourceType): Unit = {
    setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Add, incomeSourceType)))))
    when(mockSessionService.setMongoData(any())(any(), any())).thenReturn(Future(true))
  }

  val incomeSourceTypes: Seq[IncomeSourceType with Serializable] = List(SelfEmployment, UkProperty, ForeignProperty)

  def mockIncomeSource(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => mockBusinessIncomeSource()
      case UkProperty => mockUKPropertyIncomeSource()
      case ForeignProperty => mockForeignPropertyIncomeSource()
    }
  }

  def sessionDataCompletedJourney(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(journeyIsComplete = Some(true))))

  "IncomeSourceAddedController" should {
    "redirect a user back to the custom error page" when {
      "the user is not authenticated" should {
        "redirect them to sign in" in {
          setupMockAuthorisationException()
          val result = TestIncomeSourceAddedController.show(isAgent = false, SelfEmployment)(fakeRequestWithActiveSession)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }
      }
    }

    for (incomeSourceType <- incomeSourceTypes) yield {
      for (isAgent <- Seq(true, false)) yield {

        s"IncomeSourceAddedController.show (${incomeSourceType.key}, ${if (isAgent) "Agent" else "Individual"})" should {
          "return 200 OK" when {
            "FS enabled with newly added income source and obligations view model" in {
              disableAllSwitches()
              enable(IncomeSources)
              setupMockAuthorisationSuccess(isAgent)
              setupMockGetSessionKeyMongoTyped[String](Right(Some(testSelfEmploymentId)))
              mockIncomeSource(incomeSourceType)
              mockISDS(incomeSourceType)

              when(mockDateService.getCurrentTaxYearStart(any())).thenReturn(LocalDate.of(2023, 4, 6))

              when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(
                Future(IncomeSourcesObligationsTestConstants.viewModel))

              when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
                thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))

              mockMongo(incomeSourceType)

              setupMockGetSessionKeyMongoTyped[String](key = AddIncomeSourceData.incomeSourceIdField, journeyType = JourneyType(Add, incomeSourceType), result = Right(Some(testSelfEmploymentId)))

              val result = if (isAgent) TestIncomeSourceAddedController.show(isAgent, incomeSourceType)(fakeRequestConfirmedClient())
              else TestIncomeSourceAddedController.show(isAgent, incomeSourceType)(fakeRequestWithActiveSession)
              status(result) shouldBe OK
            }
          }
          "return 303 SEE_OTHER" when {
            "Income Sources FS is disabled" in {
              disable(IncomeSources)
              setupMockAuthorisationSuccess(isAgent)
              mockIncomeSource(incomeSourceType)

              val result = if (isAgent) TestIncomeSourceAddedController.show(isAgent, incomeSourceType)(fakeRequestConfirmedClient())
              else TestIncomeSourceAddedController.show(isAgent, incomeSourceType)(fakeRequestWithActiveSession)
              status(result) shouldBe SEE_OTHER
              val redirectUrl = if (isAgent) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
              redirectLocation(result) shouldBe Some(redirectUrl)
            }
            "redirect to the session timeout page" when {
              "the user has timed out" in {
                if (isAgent) setupMockAgentAuthorisationException(exception = BearerTokenExpired()) else setupMockAuthorisationException()
                val result = if (isAgent) TestIncomeSourceAddedController.show(isAgent, incomeSourceType)(fakeRequestConfirmedClient())
                else TestIncomeSourceAddedController.show(isAgent, incomeSourceType)(fakeRequestWithTimeoutSession)
                status(result) shouldBe SEE_OTHER
                redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout.url)
              }
            }
            "redirect a user back to the custom error page" when {
              "the user is not authenticated" should {
                "redirect them to sign in" in {
                  if (isAgent) setupMockAgentAuthorisationException() else setupMockAuthorisationException()
                  val result = if (isAgent) TestIncomeSourceAddedController.show(isAgent, incomeSourceType)(fakeRequestConfirmedClient())
                  else TestIncomeSourceAddedController.show(isAgent, incomeSourceType)(fakeRequestWithActiveSession)
                  status(result) shouldBe SEE_OTHER
                  redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
                }
              }
            }
          }
          "return 500 ISE" when {
            "Income source start date was not retrieved" in {
              enable(IncomeSources)
              setupMockAuthorisationSuccess(isAgent)
              mockIncomeSource(incomeSourceType)
              setupMockGetSessionKeyMongoTyped[String](Right(Some(testSelfEmploymentId)))
              mockFailure()
              mockMongo(incomeSourceType)
              val result = if (isAgent) TestIncomeSourceAddedController.show(isAgent, incomeSourceType)(fakeRequestConfirmedClient())
              else TestIncomeSourceAddedController.show(isAgent, incomeSourceType)(fakeRequestWithActiveSession)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
            "Income source id is invalid" in {
              enable(IncomeSources)
              setupMockAuthorisationSuccess(isAgent)
              mockIncomeSource(incomeSourceType)
              setupMockGetSessionKeyMongoTyped[String](Right(Some(testSelfEmploymentId)))
              mockISDS(incomeSourceType)
              mockMongo(incomeSourceType)
              when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
                thenReturn(Future(testObligationsModel))

              val result = if (isAgent) TestIncomeSourceAddedController.show(isAgent, incomeSourceType)(fakeRequestConfirmedClient())
              else TestIncomeSourceAddedController.show(isAgent, incomeSourceType)(fakeRequestWithActiveSession)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
            if (incomeSourceType == SelfEmployment) {
              "Supplied business has no name" in {
                disableAllSwitches()
                enable(IncomeSources)

                setupMockAuthorisationSuccess(isAgent)
                val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel(testNino, "", Some("2022"), List(BusinessDetailsModel(
                  testSelfEmploymentId,
                  None,
                  None,
                  None,
                  Some(LocalDate.of(2022, 1, 1)),
                  None,
                  cashOrAccruals = false
                )), List.empty)
                setupMockGetSessionKeyMongoTyped[String](Right(Some(testSelfEmploymentId)))
                setupMockGetIncomeSourceDetails()(sources)
                when(mockNextUpdatesService.getNextUpdates(any())(any(), any())).
                  thenReturn(Future(testObligationsModel))
                mockProperty()
                mockMongo(incomeSourceType)
                setupMockGetSessionKeyMongoTyped[String](key = AddIncomeSourceData.incomeSourceIdField, journeyType = JourneyType(Add, incomeSourceType), result = Right(Some(testSelfEmploymentId)))

                val result: Future[Result] = if (isAgent) TestIncomeSourceAddedController.show(isAgent, incomeSourceType)(fakeRequestConfirmedClient())
                else TestIncomeSourceAddedController.show(isAgent, incomeSourceType)(fakeRequestWithActiveSession)
                status(result) shouldBe INTERNAL_SERVER_ERROR
              }
            }
          }
        }
      }
    }

    ".submit" should {
      "take the individual back to add income sources" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceAddedController.submit(isAgent = false)(fakeRequestWithActiveSession)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceController.show(isAgent = false).url)
      }
      "take the agent back to add income sources" in {
        disableAllSwitches()
        enable(IncomeSources)

        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
        setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

        val result: Future[Result] = TestIncomeSourceAddedController.submit(isAgent = false)(fakeRequestConfirmedClient())
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceController.show(isAgent = true).url)
      }
    }
  }
}
