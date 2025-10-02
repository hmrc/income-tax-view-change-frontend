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

package controllers.manageBusinesses.add

import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney._
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent}
import mocks.auth.MockAuthActions
import mocks.services.{MockITSAStatusService, MockNextUpdatesService, MockSessionService}
import models.UIJourneySessionData
import models.incomeSourceDetails._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{await, defaultAwaitTimeout, status}
import services._
import testConstants.BaseTestConstants.{testSelfEmploymentId, testSessionId}
import testConstants.BusinessDetailsTestConstants.{year2018, year2019}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessIncome, notCompletedUIJourneySessionData}
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants._
import views.html.manageBusinesses.add.IncomeSourceAddedObligationsView

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceAddedControllerSpec extends MockAuthActions with MockNextUpdatesService with MockSessionService with MockITSAStatusService {

  override lazy val app: Application = applicationBuilderWithAuthBindings.build()

  val authActions: AuthActions = app.injector.instanceOf(classOf[AuthActions])
  val incomeSourceAddedObligationsView: IncomeSourceAddedObligationsView = app.injector.instanceOf(classOf[IncomeSourceAddedObligationsView])

  val itvcErrorHandler: ItvcErrorHandler = app.injector.instanceOf(classOf[ItvcErrorHandler])
  val agentItvcErrorHandler: AgentItvcErrorHandler = app.injector.instanceOf(classOf[AgentItvcErrorHandler])

  val mockDateService: DateService = mock(classOf[DateService])

  override lazy val mockIncomeSourceDetailsService: IncomeSourceDetailsService = mock(classOf[IncomeSourceDetailsService])
  override lazy val mockNextUpdatesService: NextUpdatesService = mock(classOf[NextUpdatesService])
  override lazy val mockSessionService: SessionService = mock(classOf[SessionService])

  val sessionService: SessionService = app.injector.instanceOf(classOf[SessionService])

  val frontendAppConfig: FrontendAppConfig = app.injector.instanceOf(classOf[FrontendAppConfig])
  val messagesControllerComponents: MessagesControllerComponents = app.injector.instanceOf(classOf[MessagesControllerComponents])

  val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  lazy val testIncomeSourceAddedController: IncomeSourceAddedController =
    new IncomeSourceAddedController(
      authActions = authActions,
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
      view = incomeSourceAddedObligationsView,
      nextUpdatesService = mockNextUpdatesService,
      itvcErrorHandler = itvcErrorHandler,
      itvcErrorHandlerAgent = agentItvcErrorHandler,
      dateService = mockDateService,
      sessionService = mockSessionService
    )(frontendAppConfig, messagesControllerComponents, executionContext)

  ".getNextUpdatesUrl" should {

    "return the correct agent url" in {
      testIncomeSourceAddedController.getNextUpdatesUrl(true) shouldBe "/report-quarterly/income-and-expenses/view/agents/next-updates"
    }

    "return the correct non-agent url" in {
      testIncomeSourceAddedController.getNextUpdatesUrl(false) shouldBe "/report-quarterly/income-and-expenses/view/next-updates"
    }
  }

  ".getReportingFrequencyUrl" should {

    "return the correct agent url" in {
      testIncomeSourceAddedController.getReportingFrequencyUrl(true) shouldBe "/report-quarterly/income-and-expenses/view/agents/reporting-frequency"
    }

    "return the correct non-agent url" in {
      testIncomeSourceAddedController.getReportingFrequencyUrl(false) shouldBe "/report-quarterly/income-and-expenses/view/reporting-frequency"
    }
  }

  ".getManageBusinessUrl" should {

    "return the correct agent url" in {
      testIncomeSourceAddedController.getManageBusinessUrl(true) shouldBe "/report-quarterly/income-and-expenses/view/agents/manage-your-businesses"
    }

    "return the correct non-agent url" in {
      testIncomeSourceAddedController.getManageBusinessUrl(false) shouldBe "/report-quarterly/income-and-expenses/view/manage-your-businesses"
    }
  }

  Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>

    s".contentLogicHelper - $incomeSourceType" when {

      "user selects 'Yes' on ChangeReportingFrequency page" when {

        "isReportingQuarterlyForNextYear == true" should {

          "return the correct scenario enum - SignUpNextYearOnly" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)

            val mongoUserAnswers =
              UIJourneySessionData(
                sessionId = testSessionId,
                journeyType = journeyType.toString,
                addIncomeSourceData =
                  Some(
                    AddIncomeSourceData(
                      businessName = Some("A Business Name"),
                      businessTrade = Some("A Trade"),
                      dateStarted = Some(LocalDate.of(2022, 1, 1)),
                      accountingPeriodStartDate = Some(LocalDate.of(2022, 4, 5)),
                      accountingPeriodEndDate = Some(LocalDate.of(2023, 4, 6)),
                      incomeSourceId = Some(testSelfEmploymentId),
                      address = Some(Address(Seq("line1", "line2"), Some("N1 1EE"))),
                      countryCode = Some("A Country"),
                      incomeSourcesAccountingMethod = Some("cash"),
                      reportingMethodTaxYear1 = None,
                      reportingMethodTaxYear2 = None,
                      incomeSourceCreatedJourneyComplete = None,
                      changeReportingFrequency = Some(true)
                    )
                  ),
                incomeSourceReportingFrequencyData = Some(
                  IncomeSourceReportingFrequencySourceData(
                    displayOptionToChangeForCurrentTaxYear = true,
                    displayOptionToChangeForNextTaxYear = true,
                    isReportingQuarterlyCurrentYear = false,
                    isReportingQuarterlyForNextYear = true
                  )

                )
              )

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(Future(Right(Some(mongoUserAnswers))))

            val result = testIncomeSourceAddedController.contentLogicHelper(incomeSourceType)
            await(result) shouldBe SignUpNextYearOnly
          }
        }

        "isReportingQuarterlyCurrentYear == true" should {

          "return the correct scenario enum - SignUpCurrentYearOnly" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)

            val mongoUserAnswers =
              UIJourneySessionData(
                sessionId = testSessionId,
                journeyType = journeyType.toString,
                addIncomeSourceData =
                  Some(
                    AddIncomeSourceData(
                      businessName = Some("A Business Name"),
                      businessTrade = Some("A Trade"),
                      dateStarted = Some(LocalDate.of(2022, 1, 1)),
                      accountingPeriodStartDate = Some(LocalDate.of(2022, 4, 5)),
                      accountingPeriodEndDate = Some(LocalDate.of(2023, 4, 6)),
                      incomeSourceId = Some(testSelfEmploymentId),
                      address = Some(Address(Seq("line1", "line2"), Some("N1 1EE"))),
                      countryCode = Some("A Country"),
                      incomeSourcesAccountingMethod = Some("cash"),
                      reportingMethodTaxYear1 = None,
                      reportingMethodTaxYear2 = None,
                      incomeSourceCreatedJourneyComplete = None,
                      changeReportingFrequency = Some(true)
                    )
                  ),
                incomeSourceReportingFrequencyData = Some(
                  IncomeSourceReportingFrequencySourceData(
                    displayOptionToChangeForCurrentTaxYear = true,
                    displayOptionToChangeForNextTaxYear = true,
                    isReportingQuarterlyCurrentYear = true,
                    isReportingQuarterlyForNextYear = false
                  )
                )
              )

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(Future(Right(Some(mongoUserAnswers))))

            val result = testIncomeSourceAddedController.contentLogicHelper(incomeSourceType)
            await(result) shouldBe SignUpCurrentYearOnly
          }
        }

        "(isReportingQuarterlyCurrentYear == true && isReportingQuarterlyForNextYear == true)" should {

          "return the correct scenario enum - SignUpBothYears" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)

            val mongoUserAnswers =
              UIJourneySessionData(
                sessionId = testSessionId,
                journeyType = journeyType.toString,
                addIncomeSourceData =
                  Some(
                    AddIncomeSourceData(
                      businessName = Some("A Business Name"),
                      businessTrade = Some("A Trade"),
                      dateStarted = Some(LocalDate.of(2022, 1, 1)),
                      accountingPeriodStartDate = Some(LocalDate.of(2022, 4, 5)),
                      accountingPeriodEndDate = Some(LocalDate.of(2023, 4, 6)),
                      incomeSourceId = Some(testSelfEmploymentId),
                      address = Some(Address(Seq("line1", "line2"), Some("N1 1EE"))),
                      countryCode = Some("A Country"),
                      incomeSourcesAccountingMethod = Some("cash"),
                      reportingMethodTaxYear1 = None,
                      reportingMethodTaxYear2 = None,
                      incomeSourceCreatedJourneyComplete = None,
                      changeReportingFrequency = Some(true)
                    )
                  ),
                incomeSourceReportingFrequencyData = Some(
                  IncomeSourceReportingFrequencySourceData(
                    displayOptionToChangeForCurrentTaxYear = true,
                    displayOptionToChangeForNextTaxYear = true,
                    isReportingQuarterlyCurrentYear = true,
                    isReportingQuarterlyForNextYear = true
                  )
                )
              )

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(Future(Right(Some(mongoUserAnswers))))

            val result = testIncomeSourceAddedController.contentLogicHelper(incomeSourceType)
            await(result) shouldBe SignUpBothYears
          }
        }

        "user is unable to select any years to opt in / unable to signup to quarterly reporting for MTD" should {

          "return the correct scenario enum - OptedOut" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)

            val mongoUserAnswers =
              UIJourneySessionData(
                sessionId = testSessionId,
                journeyType = journeyType.toString,
                addIncomeSourceData =
                  Some(
                    AddIncomeSourceData(
                      businessName = Some("A Business Name"),
                      businessTrade = Some("A Trade"),
                      dateStarted = Some(LocalDate.of(2022, 1, 1)),
                      accountingPeriodStartDate = Some(LocalDate.of(2022, 4, 5)),
                      accountingPeriodEndDate = Some(LocalDate.of(2023, 4, 6)),
                      incomeSourceId = Some(testSelfEmploymentId),
                      address = Some(Address(Seq("line1", "line2"), Some("N1 1EE"))),
                      countryCode = Some("A Country"),
                      incomeSourcesAccountingMethod = Some("cash"),
                      reportingMethodTaxYear1 = None,
                      reportingMethodTaxYear2 = None,
                      incomeSourceCreatedJourneyComplete = None,
                      changeReportingFrequency = None
                    )
                  ),
                incomeSourceReportingFrequencyData = None
              )

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(Future(Right(Some(mongoUserAnswers))))

            val result = testIncomeSourceAddedController.contentLogicHelper(incomeSourceType)
            await(result) shouldBe OptedOut
          }
        }
      }

      "user selects 'No' in ChangeReportingFrequency page" should {

        "return the correct scenario enum - NotSigningUp" in {

          val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)

          val mongoUserAnswers =
            UIJourneySessionData(
              sessionId = testSessionId,
              journeyType = journeyType.toString,
              addIncomeSourceData =
                Some(
                  AddIncomeSourceData(
                    businessName = Some("A Business Name"),
                    businessTrade = Some("A Trade"),
                    dateStarted = Some(LocalDate.of(2022, 1, 1)),
                    accountingPeriodStartDate = Some(LocalDate.of(2022, 4, 5)),
                    accountingPeriodEndDate = Some(LocalDate.of(2023, 4, 6)),
                    incomeSourceId = Some(testSelfEmploymentId),
                    address = Some(Address(Seq("line1", "line2"), Some("N1 1EE"))),
                    countryCode = Some("A Country"),
                    incomeSourcesAccountingMethod = Some("cash"),
                    reportingMethodTaxYear1 = None,
                    reportingMethodTaxYear2 = None,
                    incomeSourceCreatedJourneyComplete = None,
                    changeReportingFrequency = Some(false)
                  )
                ),
              incomeSourceReportingFrequencyData = Some(
                IncomeSourceReportingFrequencySourceData(
                  displayOptionToChangeForCurrentTaxYear = true,
                  displayOptionToChangeForNextTaxYear = true,
                  isReportingQuarterlyCurrentYear = true,
                  isReportingQuarterlyForNextYear = true
                )

              )
            )

          when(mockSessionService.getMongo(any())(any(), any()))
            .thenReturn(Future(Right(Some(mongoUserAnswers))))

          val result = testIncomeSourceAddedController.contentLogicHelper(incomeSourceType)
          await(result) shouldBe NotSigningUp
        }
      }

      "user unable to answer ChangeReportingFrequency page" when {

        "user has a single business and opts in/signs up to quarterly reporting for MTD" should {

          "return the correct scenario enum - OnlyOneBusinessInLatency" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)

            val mongoUserAnswers =
              UIJourneySessionData(
                sessionId = testSessionId,
                journeyType = journeyType.toString,
                addIncomeSourceData =
                  Some(
                    AddIncomeSourceData(
                      businessName = Some("A Business Name"),
                      businessTrade = Some("A Trade"),
                      dateStarted = Some(LocalDate.of(2022, 1, 1)),
                      accountingPeriodStartDate = Some(LocalDate.of(2022, 4, 5)),
                      accountingPeriodEndDate = Some(LocalDate.of(2023, 4, 6)),
                      incomeSourceId = Some(testSelfEmploymentId),
                      address = Some(Address(Seq("line1", "line2"), Some("N1 1EE"))),
                      countryCode = Some("A Country"),
                      incomeSourcesAccountingMethod = Some("cash"),
                      reportingMethodTaxYear1 = None,
                      reportingMethodTaxYear2 = None,
                      incomeSourceCreatedJourneyComplete = None,
                      changeReportingFrequency = None
                    )
                  ),
                incomeSourceReportingFrequencyData = Some(
                  IncomeSourceReportingFrequencySourceData(
                    displayOptionToChangeForCurrentTaxYear = true,
                    displayOptionToChangeForNextTaxYear = true,
                    isReportingQuarterlyCurrentYear = true,
                    isReportingQuarterlyForNextYear = false
                  )
                )
              )

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(Future(Right(Some(mongoUserAnswers))))

            val result = testIncomeSourceAddedController.contentLogicHelper(incomeSourceType)
            await(result) shouldBe OnlyOneYearAvailableToSignUp
          }
        }

        "user is unable to select any years to opt in / unable to signup to quarterly reporting for MTD" should {

          "return the correct scenario enum - OptedOut" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)

            val mongoUserAnswers =
              UIJourneySessionData(
                sessionId = testSessionId,
                journeyType = journeyType.toString,
                addIncomeSourceData =
                  Some(
                    AddIncomeSourceData(
                      businessName = Some("A Business Name"),
                      businessTrade = Some("A Trade"),
                      dateStarted = Some(LocalDate.of(2022, 1, 1)),
                      accountingPeriodStartDate = Some(LocalDate.of(2022, 4, 5)),
                      accountingPeriodEndDate = Some(LocalDate.of(2023, 4, 6)),
                      incomeSourceId = Some(testSelfEmploymentId),
                      address = Some(Address(Seq("line1", "line2"), Some("N1 1EE"))),
                      countryCode = Some("A Country"),
                      incomeSourcesAccountingMethod = Some("cash"),
                      reportingMethodTaxYear1 = None,
                      reportingMethodTaxYear2 = None,
                      incomeSourceCreatedJourneyComplete = None,
                      changeReportingFrequency = None
                    )
                  ),
                incomeSourceReportingFrequencyData = None
              )

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(Future(Right(Some(mongoUserAnswers))))

            val result = testIncomeSourceAddedController.contentLogicHelper(incomeSourceType)
            await(result) shouldBe OptedOut
          }
        }
      }
    }
  }


  ".show()" when {

    Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>

      s"the user is authenticated as a MTDIndividual - $incomeSourceType" should {

        "render the income source added page - OK (200)" when {

          "newly added IncomeSource and ObligationsViewModel without choosing reporting methods" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDIndividual)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data => data.copy(reportingMethodTaxYear1 = None, reportingMethodTaxYear2 = None)
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDIndividual)
            val result = testIncomeSourceAddedController.show(incomeSourceType)(fakeRequest)
            status(result) shouldBe OK
          }

          "newly added IncomeSource and ObligationsViewModel with an overall annual chosen reporting method" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "A"
              )

            disableAllSwitches()
            setupMockSuccess(MTDIndividual)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data => data.copy(reportingMethodTaxYear1 = Some("A"), reportingMethodTaxYear2 = Some("A"))
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDIndividual)
            val result = testIncomeSourceAddedController.show(incomeSourceType)(fakeRequest)
            status(result) shouldBe OK
          }

          "IncomeSources FeatureSwitch ENABLED with newly added IncomeSource and ObligationsViewModel with an overall quarterly chosen reporting method" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "Q",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDIndividual)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data => data.copy(reportingMethodTaxYear1 = Some("Q"), reportingMethodTaxYear2 = Some("Q"))
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDIndividual)
            val result = testIncomeSourceAddedController.show(incomeSourceType)(fakeRequest)
            status(result) shouldBe OK
          }

          "newly added IncomeSource and ObligationsViewModel with an overall hybrid chosen reporting method" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDIndividual)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data => data.copy(reportingMethodTaxYear1 = Some("A"), reportingMethodTaxYear2 = Some("Q"))
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDIndividual)
            val result = testIncomeSourceAddedController.show(incomeSourceType)(fakeRequest)
            status(result) shouldBe OK
          }
        }

        "render the error page - INTERNAL_SERVER_ERROR (500)" when {

          "IncomeSourceId == None" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDIndividual)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data =>
                            data.copy(incomeSourceId = None, reportingMethodTaxYear1 = None, reportingMethodTaxYear2 = None)
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDIndividual)
            val result = testIncomeSourceAddedController.show(incomeSourceType)(fakeRequest)
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }

          "Income Source was not retrieved - Income Source == None" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDIndividual)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(None)

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data => data.copy(reportingMethodTaxYear1 = None, reportingMethodTaxYear2 = None)
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDIndividual)
            val result = testIncomeSourceAddedController.show(incomeSourceType)(fakeRequest)
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }

          "however Date started == None" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDIndividual)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data =>
                            data.copy(dateStarted = None, reportingMethodTaxYear1 = None, reportingMethodTaxYear2 = None)
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDIndividual)
            val result = testIncomeSourceAddedController.show(incomeSourceType)(fakeRequest)
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }

        }
      }
    }
  }

  ".showAgent()" when {

    Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>

      s"the user is authenticated as a MTDPrimaryAgent - $incomeSourceType" should {

        "render the income source added page" when {

          "newly added IncomeSource and ObligationsViewModel without choosing reporting methods" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDPrimaryAgent)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data => data.copy(reportingMethodTaxYear1 = None, reportingMethodTaxYear2 = None)
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDPrimaryAgent)
            val result = testIncomeSourceAddedController.showAgent(incomeSourceType)(fakeRequest)
            status(result) shouldBe OK
          }

          "newly added IncomeSource and ObligationsViewModel with an overall annual chosen reporting method" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "A"
              )

            disableAllSwitches()
            setupMockSuccess(MTDPrimaryAgent)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data => data.copy(reportingMethodTaxYear1 = Some("A"), reportingMethodTaxYear2 = Some("A"))
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDPrimaryAgent)
            val result = testIncomeSourceAddedController.showAgent(incomeSourceType)(fakeRequest)
            status(result) shouldBe OK
          }

          "newly added IncomeSource and ObligationsViewModel with an overall quarterly chosen reporting method" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "Q",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDPrimaryAgent)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data => data.copy(reportingMethodTaxYear1 = Some("Q"), reportingMethodTaxYear2 = Some("Q"))
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDPrimaryAgent)
            val result = testIncomeSourceAddedController.showAgent(incomeSourceType)(fakeRequest)
            status(result) shouldBe OK
          }

          "newly added IncomeSource and ObligationsViewModel with an overall hybrid chosen reporting method" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDPrimaryAgent)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data => data.copy(reportingMethodTaxYear1 = Some("A"), reportingMethodTaxYear2 = Some("Q"))
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDPrimaryAgent)
            val result = testIncomeSourceAddedController.showAgent(incomeSourceType)(fakeRequest)
            status(result) shouldBe OK
          }
        }

        "render the error page - INTERNAL_SERVER_ERROR (500)" when {

          "IncomeSourceId == None" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDPrimaryAgent)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data =>
                            data.copy(incomeSourceId = None, reportingMethodTaxYear1 = None, reportingMethodTaxYear2 = None)
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDPrimaryAgent)
            val result = testIncomeSourceAddedController.showAgent(incomeSourceType)(fakeRequest)
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }

          "Income Source was not retrieved - Income Source == None" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDPrimaryAgent)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(None)

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))
            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data => data.copy(reportingMethodTaxYear1 = None, reportingMethodTaxYear2 = None)
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDPrimaryAgent)
            val result = testIncomeSourceAddedController.showAgent(incomeSourceType)(fakeRequest)
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }

          "Date started == None" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDPrimaryAgent)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data =>
                            data.copy(dateStarted = None, reportingMethodTaxYear1 = None, reportingMethodTaxYear2 = None)
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDPrimaryAgent)
            val result = testIncomeSourceAddedController.showAgent(incomeSourceType)(fakeRequest)
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }
        }
      }

      s"the user is authenticated as a MTDSupportingAgent - $incomeSourceType" should {

        "render the income source added page" when {

          "newly added IncomeSource and ObligationsViewModel without choosing reporting methods" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDSupportingAgent)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data => data.copy(reportingMethodTaxYear1 = None, reportingMethodTaxYear2 = None)
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDSupportingAgent)
            val result = testIncomeSourceAddedController.showAgent(incomeSourceType)(fakeRequest)
            status(result) shouldBe OK
          }

          "newly added IncomeSource and ObligationsViewModel with an overall annual chosen reporting method" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "A"
              )

            disableAllSwitches()
            setupMockSuccess(MTDSupportingAgent)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data => data.copy(reportingMethodTaxYear1 = Some("A"), reportingMethodTaxYear2 = Some("A"))
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDSupportingAgent)
            val result = testIncomeSourceAddedController.showAgent(incomeSourceType)(fakeRequest)
            status(result) shouldBe OK
          }

          "newly added IncomeSource and ObligationsViewModel with an overall quarterly chosen reporting method" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "Q",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDSupportingAgent)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data => data.copy(reportingMethodTaxYear1 = Some("Q"), reportingMethodTaxYear2 = Some("Q"))
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDSupportingAgent)
            val result = testIncomeSourceAddedController.showAgent(incomeSourceType)(fakeRequest)
            status(result) shouldBe OK
          }

          "newly added IncomeSource and ObligationsViewModel with an overall hybrid chosen reporting method" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDSupportingAgent)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data => data.copy(reportingMethodTaxYear1 = Some("A"), reportingMethodTaxYear2 = Some("Q"))
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDSupportingAgent)
            val result = testIncomeSourceAddedController.showAgent(incomeSourceType)(fakeRequest)
            status(result) shouldBe OK
          }
        }

        "render the error page - INTERNAL_SERVER_ERROR (500)" when {

          "however IncomeSourceId == None" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDSupportingAgent)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data =>
                            data.copy(incomeSourceId = None, reportingMethodTaxYear1 = None, reportingMethodTaxYear2 = None)
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDSupportingAgent)
            val result = testIncomeSourceAddedController.showAgent(incomeSourceType)(fakeRequest)
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }

          "however Income Source was not retrieved - Income Source == None" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDSupportingAgent)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(None)

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data => data.copy(reportingMethodTaxYear1 = None, reportingMethodTaxYear2 = None)
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDSupportingAgent)
            val result = testIncomeSourceAddedController.showAgent(incomeSourceType)(fakeRequest)
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }

          "however Date started == None" in {

            val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
            val taxYearStartDate = LocalDate.of(2023, 4, 6)

            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "A",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            disableAllSwitches()
            setupMockSuccess(MTDSupportingAgent)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
              .thenReturn(Future.successful(businessIncome))

            when(mockIncomeSourceDetailsService.getIncomeSource(incomeSourceType = any(), incomeSourceId = any(), incomeSourceDetailsModel = any()))
              .thenReturn(
                Some(IncomeSourceFromUser(startDate = LocalDate.parse("2022-01-01"), businessName = Some("Business Name")))
              )

            when(mockIncomeSourceDetailsService.getLatencyDetailsFromUser(incomeSourceType = any(), incomeSourceDetailsModel = any()))
              .thenReturn(Some(testLatencyDetails))

            when(mockDateService.getCurrentTaxYearStart)
              .thenReturn(taxYearStartDate)

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2024, 2, 6))

            when(mockDateService.getAccountingPeriodEndDate(any()))
              .thenReturn(LocalDate.of(2024, 4, 5))

            when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
              .thenReturn(Future(viewModel))

            when(mockNextUpdatesService.getOpenObligations()(any(), any())).
              thenReturn(Future(testObligationsModel))

            when(mockSessionService.setMongoData(any()))
              .thenReturn(Future(true))

            when(mockSessionService.getMongo(any())(any(), any()))
              .thenReturn(
                Future(
                  Right(Some(
                    notCompletedUIJourneySessionData(journeyType = journeyType)
                      .copy(addIncomeSourceData =
                        notCompletedUIJourneySessionData(journeyType = journeyType)
                          .addIncomeSourceData.map(data =>
                            data.copy(dateStarted = None, reportingMethodTaxYear1 = None, reportingMethodTaxYear2 = None)
                          )
                      )
                  ))
                )
              )

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDSupportingAgent)
            val result = testIncomeSourceAddedController.showAgent(incomeSourceType)(fakeRequest)
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }
        }
      }
    }
  }
}