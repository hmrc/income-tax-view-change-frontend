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

import enums.IncomeSourceJourney._
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.{MockITSAStatusService, MockNextUpdatesService, MockSessionService}
import models.admin.IncomeSourcesFs
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails._
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{DateService, ITSAStatusService, NextUpdatesService, SessionService}
import testConstants.BaseTestConstants.{testNino, testSelfEmploymentId, testSessionId}
import testConstants.BusinessDetailsTestConstants.testIncomeSource
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.notCompletedUIJourneySessionData
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants

import java.time.LocalDate
import scala.concurrent.Future

class IncomeSourceAddedControllerSpec extends MockAuthActions
  with MockNextUpdatesService
  with MockSessionService
  with MockITSAStatusService {

  lazy val mockDateService: DateService = mock(classOf[DateService])

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[DateService].toInstance(mockDateService),
      api.inject.bind[NextUpdatesService].toInstance(mockNextUpdatesService),
      api.inject.bind[ITSAStatusService].toInstance(mockITSAStatusService)
    ).build()

  lazy val testIncomeSourceAddedController = app.injector.instanceOf[IncomeSourceAddedController]

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(testSelfEmploymentId, List(SingleObligationModel(
      LocalDate.of(2022, 7, 1),
      LocalDate.of(2022, 7, 2),
      LocalDate.of(2022, 8, 2),
      "Quarterly",
      None,
      "#001",
      StatusFulfilled
    ),
      SingleObligationModel(
        LocalDate.of(2022, 7, 1),
        LocalDate.of(2022, 7, 2),
        LocalDate.of(2022, 8, 2),
        "Quarterly",
        None,
        "#002",
        StatusFulfilled
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

  def mockMongo(incomeSourceType: IncomeSourceType, reportingMethodTaxYear1: Option[String], reportingMethodTaxYear2: Option[String]): Unit = {
    setupMockGetMongo(Right(Some(
      notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType))
        .copy(addIncomeSourceData = notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType)).addIncomeSourceData.map(data =>
          data.copy(reportingMethodTaxYear1 = reportingMethodTaxYear1, reportingMethodTaxYear2 = reportingMethodTaxYear2)
        ))
    )))
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

  def sessionDataCompletedJourney(journeyType: IncomeSourceJourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(journeyIsComplete = Some(true))))

  incomeSourceTypes.foreach { incomeSourceType =>
    mtdAllRoles.foreach { mtdRole =>
      s"show${if (mtdRole != MTDIndividual) "Agent"}(incomeSourceType = ${incomeSourceType.key})" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        val action = if (mtdRole == MTDIndividual) testIncomeSourceAddedController.show(incomeSourceType) else testIncomeSourceAddedController.showAgent(incomeSourceType)
        s"the user is authenticated as a $mtdRole" should {
          "render the income source added page" when {
            "FS enabled with newly added income source and obligations view model without choosing reporting methods" in {
              disableAllSwitches()
              enable(IncomeSourcesFs)
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

              mockMongo(incomeSourceType, None, None)

              val result = action(fakeRequest)
              status(result) shouldBe OK
            }

            "FS enabled with newly added income source and obligations view model with an overall annual chosen reporting method" in {
              disableAllSwitches()
              enable(IncomeSourcesFs)
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

              mockMongo(incomeSourceType, Some("A"), Some("A"))

              val result = action(fakeRequest)
              status(result) shouldBe OK
            }

            "FS enabled with newly added income source and obligations view model with an overall quarterly chosen reporting method" in {
              disableAllSwitches()
              enable(IncomeSourcesFs)
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

              mockMongo(incomeSourceType, Some("Q"), Some("Q"))

              val result = action(fakeRequest)
              status(result) shouldBe OK
            }

            "FS enabled with newly added income source and obligations view model with an overall hybrid chosen reporting method" in {
              disableAllSwitches()
              enable(IncomeSourcesFs)
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
              disable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              mockIncomeSource(incomeSourceType)

              val result = action(fakeRequest)
              status(result) shouldBe SEE_OTHER
              val redirectUrl = if (mtdRole != MTDIndividual) controllers.routes.HomeController.showAgent.url else controllers.routes.HomeController.show().url
              redirectLocation(result) shouldBe Some(redirectUrl)
            }
          }
          "return 500 ISE" when {
            "Income source start date was not retrieved" in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              mockIncomeSource(incomeSourceType)
              setupMockGetSessionKeyMongoTyped[String](Right(Some(testSelfEmploymentId)))
              mockFailure()
              mockMongo(incomeSourceType, None, None)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
            "Income source id is invalid" in {
              enable(IncomeSourcesFs)
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
                enable(IncomeSourcesFs)

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

                val result = action(fakeRequest)
                status(result) shouldBe INTERNAL_SERVER_ERROR
              }
            }

            "FS enabled with newly added income source and obligations view model with an overall unknown chosen reporting method" in {
              disableAllSwitches()
              enable(IncomeSourcesFs)
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

              mockMongo(incomeSourceType, Some("A"), None)

              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}
