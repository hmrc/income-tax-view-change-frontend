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

import enums.IncomeSourceJourney._
import enums.JourneyType.{Add, IncomeSourceJourneyType, JourneyType}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.{MockNextUpdatesService, MockSessionService}
import models.admin.IncomeSourcesFs
import models.incomeSourceDetails._
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{DateService, NextUpdatesService, SessionService}
import testConstants.BaseTestConstants.{testNino, testSelfEmploymentId, testSessionId}
import testConstants.BusinessDetailsTestConstants.testIncomeSource
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessesAndPropertyIncome, notCompletedUIJourneySessionData}
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants

import java.time.LocalDate
import scala.concurrent.Future

class IncomeSourceAddedControllerSpec extends MockAuthActions
  with MockNextUpdatesService
  with MockSessionService {

  lazy val mockDateService: DateService = mock(classOf[DateService])

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[DateService].toInstance(mockDateService),
      api.inject.bind[NextUpdatesService].toInstance(mockNextUpdatesService)
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
    when(mockIncomeSourceDetailsService.getIncomeSource(any(), any(), any()))
      .thenReturn(
        Some(IncomeSourceFromUser(LocalDate.parse("2022-01-01"), Some("Business Name")))
      )
  }

  def mockProperty(): Unit = {
    when(mockIncomeSourceDetailsService.getIncomeSource(any(), any(), any()))
      .thenReturn(
        Some(IncomeSourceFromUser(LocalDate.parse("2022-01-01"), None))
      )
  }

  def mockISDS(incomeSourceType: IncomeSourceType): Unit = {
    if (incomeSourceType == SelfEmployment)
      when(mockIncomeSourceDetailsService.getIncomeSource(any(), any(), any()))
        .thenReturn(
          Some(IncomeSourceFromUser(LocalDate.parse("2022-01-01"), Some("Business Name")))
        )
    else
      when(mockIncomeSourceDetailsService.getIncomeSource(any(), any(), any()))
        .thenReturn(
          Some(IncomeSourceFromUser(LocalDate.parse("2022-01-01"), None))
        )
  }

  def mockFailure(): Unit = {
    when(mockIncomeSourceDetailsService.getIncomeSource(any(), any(), any()))
      .thenReturn(None)
  }

  def mockMongo(incomeSourceType: IncomeSourceType): Unit = {
    setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Add, incomeSourceType)))))
    when(mockSessionService.setMongoData(any())).thenReturn(Future(true))
  }

  val incomeSourceTypes: Seq[IncomeSourceType with Serializable] = List(SelfEmployment, UkProperty, ForeignProperty)

  def mockIncomeSource(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => mockBusinessIncomeSource()
      case UkProperty => mockUKPropertyIncomeSource()
      case ForeignProperty => mockForeignPropertyIncomeSource()
    }
  }

  def sessionDataCompletedJourney(journeyType: JourneyType): UIJourneySessionData = UIJourneySessionData(testSessionId, journeyType.toString, Some(AddIncomeSourceData(incomeSourceCreatedJourneyComplete = Some(true))))

  incomeSourceTypes.foreach { incomeSourceType =>
    mtdAllRoles.foreach { mtdRole =>
      s"show${if (mtdRole != MTDIndividual) "Agent"}(incomeSourceType = ${incomeSourceType.key})" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        val action = if (mtdRole == MTDIndividual) testIncomeSourceAddedController.show(incomeSourceType) else testIncomeSourceAddedController.showAgent(incomeSourceType)
        s"the user is authenticated as a $mtdRole" should {
          "render the income source added page" when {
            "FS enabled with newly added income source and obligations view model" in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              setupMockGetSessionKeyMongoTyped[String](Right(Some(testSelfEmploymentId)))
              mockIncomeSource(incomeSourceType)
              mockISDS(incomeSourceType)

              when(mockDateService.getCurrentTaxYearStart).thenReturn(LocalDate.of(2023, 4, 6))

              when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any())).thenReturn(
                Future(IncomeSourcesObligationsTestConstants.viewModel))

              when(mockNextUpdatesService.getOpenObligations()(any(), any())).
                thenReturn(Future(IncomeSourcesObligationsTestConstants.testObligationsModel))

              mockMongo(incomeSourceType)

              setupMockGetSessionKeyMongoTyped[String](key = AddIncomeSourceData.incomeSourceIdField, journeyType = IncomeSourceJourneyType(Add, incomeSourceType), result = Right(Some(testSelfEmploymentId)))

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
              val redirectUrl = if (mtdRole != MTDIndividual) controllers.routes.HomeController.showAgent().url else controllers.routes.HomeController.show().url
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
              mockMongo(incomeSourceType)
              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
            "Income source id is invalid" in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              mockIncomeSource(incomeSourceType)
              setupMockGetSessionKeyMongoTyped[String](Right(Some(testSelfEmploymentId)))
              mockISDS(incomeSourceType)
              mockMongo(incomeSourceType)
              when(mockNextUpdatesService.getOpenObligations()(any(), any())).
                thenReturn(Future(testObligationsModel))

              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
            if (incomeSourceType == SelfEmployment) {
              "Supplied business has no name" in {
                enable(IncomeSourcesFs)

                setupMockSuccess(mtdRole)
                val sources: IncomeSourceDetailsModel = IncomeSourceDetailsModel(testNino, "", Some("2022"), List(BusinessDetailsModel(
                  testSelfEmploymentId,
                  incomeSource = Some(testIncomeSource),
                  accountingPeriod = None,
                  tradingName = None,
                  firstAccountingPeriodEndDate = None,
                  tradingStartDate = Some(LocalDate.of(2022, 1, 1)),
                  contextualTaxYear = None,
                  cessation = None,
                  cashOrAccruals = false
                )), List.empty)
                setupMockGetSessionKeyMongoTyped[String](Right(Some(testSelfEmploymentId)))
                setupMockGetIncomeSourceDetails(sources)
                when(mockNextUpdatesService.getOpenObligations()(any(), any())).
                  thenReturn(Future(testObligationsModel))
                mockProperty()
                mockMongo(incomeSourceType)
                setupMockGetSessionKeyMongoTyped[String](key = AddIncomeSourceData.incomeSourceIdField, journeyType = IncomeSourceJourneyType(Add, incomeSourceType), result = Right(Some(testSelfEmploymentId)))

                val result = action(fakeRequest)
                status(result) shouldBe INTERNAL_SERVER_ERROR
              }
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }

      s"submit${if (mtdRole != MTDIndividual) "Agent"}(incomeSourceType = $incomeSourceType)" when {
        val action = if (mtdRole == MTDIndividual) testIncomeSourceAddedController.submit else testIncomeSourceAddedController.agentSubmit()
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")
        s"the user is authenticated as a $mtdRole" should {
          "redirect to add income sources" in {
            enable(IncomeSourcesFs)
            setupMockSuccess(mtdRole)

            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val result: Future[Result] = action(fakeRequest)
            status(result) shouldBe SEE_OTHER
            val expectedLocation = if (mtdRole == MTDIndividual) {
              controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
            } else {
              controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
            }
            redirectLocation(result) shouldBe Some(expectedLocation)
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}
