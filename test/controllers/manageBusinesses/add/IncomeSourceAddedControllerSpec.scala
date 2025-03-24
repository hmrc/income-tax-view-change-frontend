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
import models.admin.IncomeSourcesNewJourney
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services._
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

  ".show()" when {

    Seq(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSourceType =>

      s"the user is authenticated as a MTDIndividual - $incomeSourceType" should {

        "render the income source added page - OK (200)" when {

          "IncomeSources FeatureSwitch ENABLED with newly added IncomeSource and ObligationsViewModel without choosing reporting methods" in {

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
            enable(IncomeSourcesNewJourney)
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

          "IncomeSources FeatureSwitch ENABLED with newly added IncomeSource and ObligationsViewModel with an overall annual chosen reporting method" in {

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
            enable(IncomeSourcesNewJourney)
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
            enable(IncomeSourcesNewJourney)
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

          "IncomeSources FeatureSwitch ENABLED with newly added IncomeSource and ObligationsViewModel with an overall hybrid chosen reporting method" in {

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
            enable(IncomeSourcesNewJourney)
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

          "IncomeSources FeatureSwitch ENABLED, however IncomeSourceId == None" in {

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
            enable(IncomeSourcesNewJourney)
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

          "IncomeSources FeatureSwitch ENABLED, however Income Source was not retrieved - Income Source == None" in {

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
            enable(IncomeSourcesNewJourney)
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

          "IncomeSources FeatureSwitch ENABLED, however Date started == None" in {

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
            enable(IncomeSourcesNewJourney)
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

        "redirect to the home page - SEE_OTHER (303)" when {

          "IncomeSources FeatureSwitch DISABLED" in {

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

            status(result) shouldBe SEE_OTHER
            val redirectUrl = controllers.routes.HomeController.show().url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }
    }
  }

  def sessionDataCompletedJourney(journeyType: IncomeSourceJourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceCreatedJourneyComplete = Some(true))))

  incomeSourceTypes.foreach { incomeSourceType =>
    mtdAllRoles.foreach { mtdRole =>
      s"show${if (mtdRole != MTDIndividual) "Agent"}(incomeSourceType = ${incomeSourceType.key})" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        val action = if (mtdRole == MTDIndividual) testIncomeSourceAddedController.show(incomeSourceType) else testIncomeSourceAddedController.showAgent(incomeSourceType)
        s"the user is authenticated as a $mtdRole" should {
          "render the income source added page" when {
            "FS enabled with newly added income source and obligations view model without choosing reporting methods" in {
              disableAllSwitches()
              enable(IncomeSourcesNewJourney)
              setupMockSuccess(mtdRole)
              mockIncomeSource(incomeSourceType)
              mockISDS(incomeSourceType)

      s"the user is authenticated as a MTDPrimaryAgent - $incomeSourceType" should {

        "render the income source added page" when {

          "IncomeSources FeatureSwitch ENABLED with newly added IncomeSource and ObligationsViewModel without choosing reporting methods" in {

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

            "FS enabled with newly added income source and obligations view model with an overall annual chosen reporting method" in {
              disableAllSwitches()
              enable(IncomeSourcesNewJourney)
              setupMockSuccess(mtdRole)
              mockIncomeSource(incomeSourceType)
              mockISDS(incomeSourceType)

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

            "FS enabled with newly added income source and obligations view model with an overall quarterly chosen reporting method" in {
              disableAllSwitches()
              enable(IncomeSourcesNewJourney)
              setupMockSuccess(mtdRole)
              mockIncomeSource(incomeSourceType)
              mockISDS(incomeSourceType)

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

              val result = action(fakeRequest)
              status(result) shouldBe OK
            }

            "FS enabled with newly added income source and obligations view model with an overall hybrid chosen reporting method" in {
              disableAllSwitches()
              enable(IncomeSourcesNewJourney)
              setupMockSuccess(mtdRole)
              mockIncomeSource(incomeSourceType)
              mockISDS(incomeSourceType)

              when(mockDateService.getCurrentTaxYearStart).thenReturn(LocalDate.of(2023, 4, 6))
              when(mockDateService.getCurrentDate).thenReturn(LocalDate.of(2024, 2, 6))
              when(mockDateService.getAccountingPeriodEndDate(any())).thenReturn(LocalDate.of(2024, 4, 5))

              when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(
                Future(IncomeSourcesObligationsTestConstants.viewModel))

              when(mockNextUpdatesService.getOpenObligations()(any(), any())).
                thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))

              mockMongo(incomeSourceType, Some("A"), Some("Q"))

              val result = action(fakeRequest)
              status(result) shouldBe OK
            }
          }

          "return 303 SEE_OTHER" when {
            "Income Sources FS is disabled" in {
              disable(IncomeSourcesNewJourney)
              setupMockSuccess(mtdRole)
              mockIncomeSource(incomeSourceType)

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
            enable(IncomeSourcesNewJourney)
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
          "return 500 ISE" when {
            "Income source start date was not retrieved" in {
              enable(IncomeSourcesNewJourney)
              setupMockSuccess(mtdRole)
              mockIncomeSource(incomeSourceType)
              setupMockGetSessionKeyMongoTyped[String](Right(Some(testSelfEmploymentId)))
              mockFailure()
              mockMongo(incomeSourceType, None, None)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
            "Income source id is invalid" in {
              enable(IncomeSourcesNewJourney)
              setupMockSuccess(mtdRole)
              mockIncomeSource(incomeSourceType)
              setupMockGetSessionKeyMongoTyped[String](Right(Some(testSelfEmploymentId)))
              mockISDS(incomeSourceType)
              mockMongo(incomeSourceType, None, None)
              when(mockNextUpdatesService.getOpenObligations()(any(), any())).
                thenReturn(Future(testObligationsModel))

              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
            if (incomeSourceType == SelfEmployment) {
              "Supplied business has no name" in {
                disableAllSwitches()
                enable(IncomeSourcesNewJourney)
                setupMockSuccess(mtdRole)
                val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel(testNino, "", Some("2022"), List(BusinessDetailsModel(
                  testSelfEmploymentId,
                  incomeSource = Some(testIncomeSource),
                  None,
                  None,
                  None,
                  Some(LocalDate.of(2022, 1, 1)),
                  None,
                  cashOrAccruals = false
                )), List.empty)
                setupMockGetSessionKeyMongoTyped[String](Right(Some(testSelfEmploymentId)))
                setupMockGetIncomeSourceDetails()(sources)
                when(mockNextUpdatesService.getOpenObligations()(any(), any())).
                  thenReturn(Future(testObligationsModel))
                mockProperty()
                mockMongo(incomeSourceType, None, None)
                setupMockGetSessionKeyMongoTyped[String](key = AddIncomeSourceData.incomeSourceIdField, journeyType = IncomeSourceJourneyType(Add, incomeSourceType), result = Right(Some(testSelfEmploymentId)))
            val testLatencyDetails =
              LatencyDetails(
                latencyEndDate = LocalDate.of(year2019, 1, 1),
                taxYear1 = year2018.toString,
                latencyIndicator1 = "Q",
                taxYear2 = year2019.toString,
                latencyIndicator2 = "Q"
              )

            "FS enabled with newly added income source and obligations view model with an overall unknown chosen reporting method" in {
              disableAllSwitches()
              enable(IncomeSourcesNewJourney)
              setupMockSuccess(mtdRole)
              mockIncomeSource(incomeSourceType)
              mockISDS(incomeSourceType)

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

          "IncomeSources FeatureSwitch ENABLED with newly added IncomeSource and ObligationsViewModel with an overall hybrid chosen reporting method" in {

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
            enable(IncomeSourcesNewJourney)
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

          "IncomeSources FeatureSwitch ENABLED, however IncomeSourceId == None" in {

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
            enable(IncomeSourcesNewJourney)
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

          "IncomeSources FeatureSwitch ENABLED, however Income Source was not retrieved - Income Source == None" in {

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
            enable(IncomeSourcesNewJourney)
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

          "IncomeSources FeatureSwitch ENABLED, however Date started == None" in {

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
            enable(IncomeSourcesNewJourney)
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

        "redirect to the home page - SEE_OTHER (303)" when {

          "IncomeSources FeatureSwitch DISABLED" in {

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

            status(result) shouldBe SEE_OTHER
            val redirectUrl = controllers.routes.HomeController.showAgent().url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }

      s"the user is authenticated as a MTDSupportingAgent - $incomeSourceType" should {

        "render the income source added page" when {

          "IncomeSources FeatureSwitch ENABLED with newly added IncomeSource and ObligationsViewModel without choosing reporting methods" in {

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
            enable(IncomeSourcesNewJourney)
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

          "IncomeSources FeatureSwitch ENABLED with newly added IncomeSource and ObligationsViewModel with an overall annual chosen reporting method" in {

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
            enable(IncomeSourcesNewJourney)
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
            enable(IncomeSourcesNewJourney)
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

          "IncomeSources FeatureSwitch ENABLED with newly added IncomeSource and ObligationsViewModel with an overall hybrid chosen reporting method" in {

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
            enable(IncomeSourcesNewJourney)
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

          "IncomeSources FeatureSwitch ENABLED, however IncomeSourceId == None" in {

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
            enable(IncomeSourcesNewJourney)
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

          "IncomeSources FeatureSwitch ENABLED, however Income Source was not retrieved - Income Source == None" in {

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
            enable(IncomeSourcesNewJourney)
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

          "IncomeSources FeatureSwitch ENABLED, however Date started == None" in {

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
            enable(IncomeSourcesNewJourney)
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

        "redirect to the home page - SEE_OTHER (303)" when {

          "IncomeSources FeatureSwitch DISABLED" in {

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

            status(result) shouldBe SEE_OTHER
            val redirectUrl = controllers.routes.HomeController.showAgent().url
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }
    }
  }
}
