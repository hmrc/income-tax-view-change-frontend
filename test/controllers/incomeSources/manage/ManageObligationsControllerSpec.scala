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

package controllers.incomeSources.manage

import enums.IncomeSourceJourney._
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.{MockClientDetailsService, MockNextUpdatesService, MockSessionService}
import models.admin.IncomeSourcesFs
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{NextUpdatesService, SessionService}
import testConstants.BaseTestConstants.{testNino, testPropertyIncomeId}
import testConstants.BusinessDetailsTestConstants.testIncomeSource
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessesAndPropertyIncome, foreignPropertyIncomeWithCeasedForiegnPropertyIncome, ukPropertyIncomeWithCeasedUkPropertyIncome}
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants.quarterlyObligationDatesSimple
import utils.IncomeSourcesUtils

import java.time.LocalDate
import scala.concurrent.Future

class ManageObligationsControllerSpec extends MockAuthActions
  with MockClientDetailsService
  with MockNextUpdatesService
  with MockSessionService {

  lazy val mockIncomeSourcesUtils: IncomeSourcesUtils = mock(classOf[IncomeSourcesUtils])


  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[NextUpdatesService].toInstance(mockNextUpdatesService),
      api.inject.bind[IncomeSourcesUtils].toInstance(mockIncomeSourcesUtils)
    ).build()

  lazy val testController = app.injector.instanceOf[ManageObligationsController]

  val testTaxYear = "2023-2024"
  val changeToA = "annual"
  val changeToQ = "quarterly"
  val testId = "XAIS00000000001"

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel("123", List(SingleObligationModel(
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

  private val propertyDetailsModelUK = PropertyDetailsModel(
    incomeSourceId = testPropertyIncomeId,
    accountingPeriod = None,
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some("uk-property"),
    tradingStartDate = None,
    contextualTaxYear = None,
    cessation = None,
    cashOrAccruals = Some(false),
    latencyDetails = None
  )
  private val propertyDetailsModelForeign = propertyDetailsModelUK.copy(incomeSourceType = Some("foreign-property"))

  def getIncomeSourcesResponse(incomeSourceType: IncomeSourceType): IncomeSourceDetailsModel = {
    incomeSourceType match {
      case UkProperty => ukPropertyIncomeWithCeasedUkPropertyIncome
      case ForeignProperty => foreignPropertyIncomeWithCeasedForiegnPropertyIncome
      case _ => IncomeSourceDetailsModel(
        testNino, "",
        Some("2022"),
        List(BusinessDetailsModel(
          testId,
          incomeSource = Some(testIncomeSource),
          None,
          Some("Test name"),
          None,
          Some(LocalDate.of(2022, 1, 1)),
          contextualTaxYear = None,
          None,
          cashOrAccruals = Some(false)
        )), List.empty)
    }
  }

  lazy val obligationsViewModel = {
    val day = LocalDate.of(2023, 1, 1)
    val dates: Seq[DatesModel] = Seq(
      DatesModel(day, day, day, "Quarterly", isFinalDec = false, obligationType = "Quarterly")
    )
    ObligationsViewModel(
      quarterlyObligationDatesSimple,
      dates,
      2023,
      showPrevTaxYears = true
    )
  }

  val incomeSourceTypes = List(SelfEmployment, UkProperty, ForeignProperty)

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    incomeSourceTypes.foreach { incomeSourceType =>
      s"show${if (isAgent) "Agent"}($incomeSourceType)" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        def action(changeTo: String = changeToA, taxYear: String = testTaxYear) = if (mtdRole == MTDIndividual) {
          testController.show(changeTo, taxYear, incomeSourceType)
        } else {
          testController.showAgent(changeTo, taxYear, incomeSourceType)
        }
        s"the user is authenticated as a $mtdRole" should {
          "render the reporting method change error page" when {
            "IncomeSources FS is enabled" in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(getIncomeSourcesResponse(incomeSourceType))
              incomeSourceType match {
                case SelfEmployment => when(mockSessionService.getMongoKey(any(), any())(any(), any())).thenReturn(Future(Right(Some(testId))))
                case UkProperty => when(mockIncomeSourcesUtils.getActiveProperty(any())(any()))
                  .thenReturn(Some(propertyDetailsModelUK))
                case _ => when(mockIncomeSourcesUtils.getActiveProperty(any())(any()))
                  .thenReturn(Some(propertyDetailsModelForeign))
              }

              when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
                .thenReturn(Future(obligationsViewModel))
              when(mockNextUpdatesService.getOpenObligations()(any(), any())).
                thenReturn(Future(testObligationsModel))

              val result = action()(fakeRequest)
              status(result) shouldBe OK
            }

            if (incomeSourceType == SelfEmployment) {
              "incomeSourceFs enabled and business has no name" in {
                enable(IncomeSourcesFs)
                setupMockSuccess(mtdRole)
                val source = IncomeSourceDetailsModel(testNino, "", Some("2022"), List(
                  BusinessDetailsModel(
                    testId,
                    incomeSource = Some(testIncomeSource),
                    None,
                    None,
                    None,
                    Some(LocalDate.of(2022, 1, 1)),
                    contextualTaxYear = None,
                    None,
                    cashOrAccruals = Some(false)
                  )), List.empty)
                setupMockGetIncomeSourceDetails(source)
                when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
                  .thenReturn(Future(obligationsViewModel))
                when(mockNextUpdatesService.getOpenObligations()(any(), any())).
                  thenReturn(Future(testObligationsModel))
                when(mockSessionService.getMongoKey(any(), any())(any(), any())).thenReturn(Future(Right(Some(testId))))

                val result = action()(fakeRequest)
                status(result) shouldBe OK
                contentAsString(result) should include("Sole trader business")
              }
            }
          }

          "redirect to the home page" when {
            "feature switch is disabled" in {
              disable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(getIncomeSourcesResponse(incomeSourceType))

              val result = action()(fakeRequest)
              status(result) shouldBe SEE_OTHER
              val homeUrl = if (isAgent) {
                controllers.routes.HomeController.showAgent().url
              } else {
                controllers.routes.HomeController.show().url
              }
              redirectLocation(result) shouldBe Some(homeUrl)
            }
          }

          "render the error page" when {
            "invalid taxYear in url" in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(getIncomeSourcesResponse(incomeSourceType))
              incomeSourceType match {
                case SelfEmployment => when(mockSessionService.getMongoKey(any(), any())(any(), any())).thenReturn(Future(Right(Some(testId))))
                case UkProperty => when(mockIncomeSourcesUtils.getActiveProperty(any())(any()))
                  .thenReturn(Some(propertyDetailsModelUK))
                case _ => when(mockIncomeSourcesUtils.getActiveProperty(any())(any()))
                  .thenReturn(Some(propertyDetailsModelForeign))
              }

              when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
                .thenReturn(Future(obligationsViewModel))
              when(mockNextUpdatesService.getOpenObligations()(any(), any())).
                thenReturn(Future(testObligationsModel))

              val invalidTaxYear = "2345"
              val result = action(taxYear = invalidTaxYear)(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
            "invalid changeTo in url" in {
              enable(IncomeSourcesFs)
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(getIncomeSourcesResponse(incomeSourceType))
              incomeSourceType match {
                case SelfEmployment => when(mockSessionService.getMongoKey(any(), any())(any(), any())).thenReturn(Future(Right(Some(testId))))
                case UkProperty => when(mockIncomeSourcesUtils.getActiveProperty(any())(any()))
                  .thenReturn(Some(propertyDetailsModelUK))
                case _ => when(mockIncomeSourcesUtils.getActiveProperty(any())(any()))
                  .thenReturn(Some(propertyDetailsModelForeign))
              }

              when(mockNextUpdatesService.getObligationsViewModel(any(), any())(any(), any(), any()))
                .thenReturn(Future(obligationsViewModel))
              when(mockNextUpdatesService.getOpenObligations()(any(), any())).
                thenReturn(Future(testObligationsModel))
              val invalidChangeTo = "2345"

              val result = action(changeTo = invalidChangeTo)(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
            if (incomeSourceType != SelfEmployment) {
              s"user has no active ${incomeSourceType.messagesCamel}" in {
                enable(IncomeSourcesFs)
                setupMockSuccess(mtdRole)
                mockNoIncomeSources()

                val result = action()(fakeRequest)
                status(result) shouldBe INTERNAL_SERVER_ERROR
              }

              s"user has more than one active ${incomeSourceType.messagesCamel}" in {
                enable(IncomeSourcesFs)
                setupMockSuccess(mtdRole)
                if (incomeSourceType == UkProperty) {
                  mockTwoActiveUkPropertyIncomeSourcesErrorScenario()
                } else {
                  mockTwoActiveForeignPropertyIncomeSourcesErrorScenario()
                }

                val result = action()(fakeRequest)
                status(result) shouldBe INTERNAL_SERVER_ERROR
              }
            }
          }
          testMTDAuthFailuresForRole(action(), mtdRole)(fakeRequest)
        }
      }
    }

    s"submit" when {
      val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)
      val action = if (mtdRole == MTDIndividual) {
        testController.submit()
      } else {
        testController.agentSubmit()
      }
      s"the user is authenticated as a $mtdRole" should {
        "redirect to ManageIncomeSources controller" in {

          setupMockSuccess(mtdRole)
          enable(IncomeSourcesFs)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          val result = action(fakeRequest)
          redirectLocation(result) shouldBe Some(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(isAgent).url)
        }
      }
      testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
    }
  }
}
