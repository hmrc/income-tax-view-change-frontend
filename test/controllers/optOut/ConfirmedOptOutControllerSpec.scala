/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.optOut

import enums.{CurrentTaxYear, MTDIndividual, NextTaxYear, NoChosenTaxYear, PreviousTaxYear}
import mocks.auth.MockAuthActions
import mocks.services.MockOptOutService
import models.admin.OptOutFs
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Mandated, Voluntary}
import models.optout._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.optout._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class ConfirmedOptOutControllerSpec extends MockAuthActions with MockOptOutService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptOutService].toInstance(mockOptOutService)
    ).build()

  lazy val testController = app.injector.instanceOf[ConfirmedOptOutController]

  val taxYear = TaxYear.forYearEnd(2024)
  val optOutYear: OptOutTaxYear = CurrentOptOutTaxYear(ITSAStatus.Voluntary, taxYear)
  val eligibleTaxYearResponse = Future.successful(Some(ConfirmedOptOutViewModel(optOutYear.taxYear, Some(OneYearOptOutFollowedByMandated))))
  val noEligibleTaxYearResponse = Future.successful(None)
  val failedResponse = Future.failed(new Exception("some error"))

  mtdAllRoles.foreach { mtdRole =>
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    val isAgent = mtdRole != MTDIndividual

    s"show(isAgent = $isAgent)" when {

      val action = testController.show(isAgent)

      s"the user is authenticated as a $mtdRole" should {

        "render the Confirmed page" in {

          enable(OptOutFs)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockOptOutConfirmedPageViewModel(eligibleTaxYearResponse)

          when(mockOptOutService.getQuarterlyUpdatesCount(any())(any(), any(), any()))
            .thenReturn(Future.successful(3))

          when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
            Future(
              OptOutProposition.createOptOutProposition(
                currentYear = TaxYear(2024, 2025),
                previousYearCrystallised = false,
                previousYearItsaStatus = Mandated,
                currentYearItsaStatus = Voluntary,
                nextYearItsaStatus = Voluntary
              )
            )
          )

          when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
            .thenReturn(
              Future(CurrentTaxYear)
            )

          mockUpdateOptOutJourneyStatusInSessionData()
          mockFetchOptOutJourneyCompleteStatus()

          val result = action(fakeRequest)

          status(result) shouldBe Status.OK
        }

        s"return result with $INTERNAL_SERVER_ERROR status" when {
          "there is no tax year eligible for opt out" in {
            enable(OptOutFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockOptOutConfirmedPageViewModel(noEligibleTaxYearResponse)

            when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
              Future(
                OptOutProposition.createOptOutProposition(
                  currentYear = TaxYear(2024, 2025),
                  previousYearCrystallised = false,
                  previousYearItsaStatus = Mandated,
                  currentYearItsaStatus = Voluntary,
                  nextYearItsaStatus = Voluntary
                )
              )
            )

            when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
              .thenReturn(
                Future(CurrentTaxYear)
              )

            val result = action(fakeRequest)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }

          "opt out service fails" in {
            enable(OptOutFs)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockOptOutConfirmedPageViewModel(failedResponse)

            when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
              Future(throw new Exception("some error"))
            )

            when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
              .thenReturn(
                Future(CurrentTaxYear)
              )

            val result = action(fakeRequest)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }

        "redirect to the home page" when {
          "the opt out feature switch is disabled" in {
            disable(OptOutFs)

            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val result = action(fakeRequest)

            val redirectUrl = if (isAgent) {
              "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
            } else {
              "/report-quarterly/income-and-expenses/view"
            }

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
    }
  }

  ".viewScenarioHandler()" when {

    "CurrentYearNYQuarterlyOrAnnualScenario" when {

      "CurrentTaxYear && MultiYearOptOutProposition && proposition.isCurrentYearQuarterly && proposition.isNextYearQuarterly" should {

        "return the correct enum" in {

          val quarterlyUpdatesCount = 0

          when(mockOptOutService.getQuarterlyUpdatesCount(any())(any(), any(), any()))
            .thenReturn(Future.successful(quarterlyUpdatesCount))

          when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
            Future(
              OptOutProposition.createOptOutProposition(
                currentYear = TaxYear(2024, 2025),
                previousYearCrystallised = false,
                previousYearItsaStatus = Mandated,
                currentYearItsaStatus = Voluntary,
                nextYearItsaStatus = Voluntary
              )
            )
          )

          when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
            .thenReturn(
              Future(CurrentTaxYear)
            )

          whenReady(testController.viewScenarioHandler()) { result =>
            result shouldBe Right(CurrentYearNYQuarterlyOrAnnualScenario)
          }
        }
      }

      "CurrentTaxYear && SingleYearOptOutProposition && proposition.isCurrentYearQuarterly && proposition.isNextYearQuarterly" should {

        "return the correct enum" in {

          val quarterlyUpdatesCount = 0

          when(mockOptOutService.getQuarterlyUpdatesCount(any())(any(), any(), any()))
            .thenReturn(Future.successful(quarterlyUpdatesCount))

          when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
            Future(
              OptOutProposition.createOptOutProposition(
                currentYear = TaxYear(2024, 2025),
                previousYearCrystallised = false,
                previousYearItsaStatus = ITSAStatus.Annual,
                currentYearItsaStatus = Voluntary,
                nextYearItsaStatus = ITSAStatus.Annual
              )
            )
          )

          when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
            .thenReturn(
              Future(CurrentTaxYear)
            )

          whenReady(testController.viewScenarioHandler()) { result =>
            result shouldBe Right(CurrentYearNYQuarterlyOrAnnualScenario)
          }
        }
      }

      "CurrentTaxYear && SingleYearOptOutProposition && proposition.isCurrentYearQuarterly && proposition.isNextYearQuarterly && quarterlyUpdatesCount > 0" should {

        "return the correct enum" in {

          val quarterlyUpdatesCount = 3

          when(mockOptOutService.getQuarterlyUpdatesCount(any())(any(), any(), any()))
            .thenReturn(Future.successful(quarterlyUpdatesCount))

          when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
            Future(
              OptOutProposition.createOptOutProposition(
                currentYear = TaxYear(2024, 2025),
                previousYearCrystallised = false,
                previousYearItsaStatus = ITSAStatus.Annual,
                currentYearItsaStatus = Voluntary,
                nextYearItsaStatus = ITSAStatus.Annual
              )
            )
          )

          when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
            .thenReturn(
              Future(CurrentTaxYear)
            )

          whenReady(testController.viewScenarioHandler()) { result =>
            result shouldBe Right(CurrentYearNYQuarterlyOrAnnualScenario)
          }
        }
      }
    }

    "CurrentYearNYMandatedScenario" when {

      "CurrentTaxYear && proposition.isCurrentYearQuarterly && proposition.isNextYear == Mandated && quarterlyUpdatesCount == 0" should {

        "return the correct enum" in {

          val quarterlyUpdatesCount = 0

          when(mockOptOutService.getQuarterlyUpdatesCount(any())(any(), any(), any()))
            .thenReturn(Future.successful(quarterlyUpdatesCount))

          when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
            Future(
              OptOutProposition.createOptOutProposition(
                currentYear = TaxYear(2024, 2025),
                previousYearCrystallised = false,
                previousYearItsaStatus = ITSAStatus.Annual,
                currentYearItsaStatus = Voluntary,
                nextYearItsaStatus = Mandated
              )
            )
          )

          when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
            .thenReturn(
              Future(CurrentTaxYear)
            )

          whenReady(testController.viewScenarioHandler()) { result =>
            result shouldBe Right(CurrentYearNYMandatedScenario)
          }
        }
      }

      "CurrentTaxYear && proposition.isCurrentYearQuarterly && proposition.isNextYearQuarterly && quarterlyUpdatesCount > 0" should {

        "return the correct enum" in {

          val quarterlyUpdatesCount = 3

          when(mockOptOutService.getQuarterlyUpdatesCount(any())(any(), any(), any()))
            .thenReturn(Future.successful(quarterlyUpdatesCount))

          when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
            Future(
              OptOutProposition.createOptOutProposition(
                currentYear = TaxYear(2024, 2025),
                previousYearCrystallised = false,
                previousYearItsaStatus = ITSAStatus.Annual,
                currentYearItsaStatus = Voluntary,
                nextYearItsaStatus = Mandated
              )
            )
          )

          when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
            .thenReturn(
              Future(CurrentTaxYear)
            )

          whenReady(testController.viewScenarioHandler()) { result =>
            result shouldBe Right(CurrentYearNYMandatedScenario)
          }
        }
      }
    }

    "NextYearCYMandatedOrQuarterlyScenario" when {

      "NextTaxYear && proposition.isCurrentYearQuarterly && proposition.isNextYear == Mandated && quarterlyUpdatesCount == 0" should {

        "return the correct enum" in {

          val quarterlyUpdatesCount = 0

          when(mockOptOutService.getQuarterlyUpdatesCount(any())(any(), any(), any()))
            .thenReturn(Future.successful(quarterlyUpdatesCount))

          when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
            Future(
              OptOutProposition.createOptOutProposition(
                currentYear = TaxYear(2024, 2025),
                previousYearCrystallised = false,
                previousYearItsaStatus = ITSAStatus.Annual,
                currentYearItsaStatus = Voluntary,
                nextYearItsaStatus = Mandated
              )
            )
          )

          when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
            .thenReturn(Future(NextTaxYear))

          whenReady(testController.viewScenarioHandler()) { result =>
            result shouldBe Right(NextYearCYMandatedOrQuarterlyScenario)
          }
        }
      }

      "NextTaxYear && proposition.isCurrentYearQuarterly && proposition.isNextYear == Mandated && quarterlyUpdatesCount > 0" should {

        "return the correct enum" in {

          val quarterlyUpdatesCount = 3

          when(mockOptOutService.getQuarterlyUpdatesCount(any())(any(), any(), any()))
            .thenReturn(Future.successful(quarterlyUpdatesCount))

          when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
            Future(
              OptOutProposition.createOptOutProposition(
                currentYear = TaxYear(2024, 2025),
                previousYearCrystallised = false,
                previousYearItsaStatus = ITSAStatus.Annual,
                currentYearItsaStatus = Voluntary,
                nextYearItsaStatus = Mandated
              )
            )
          )

          when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
            .thenReturn(Future(NextTaxYear))

          whenReady(testController.viewScenarioHandler()) { result =>
            result shouldBe Right(NextYearCYMandatedOrQuarterlyScenario)
          }
        }
      }
    }

    "NextYearCYAnnualScenario" when {

      "NextTaxYear && proposition.isCurrentYearAnnual && proposition.isNextYearQuarterly && quarterlyUpdatesCount == 0" should {

        "return the correct enum" in {

          val quarterlyUpdatesCount = 0

          when(mockOptOutService.getQuarterlyUpdatesCount(any())(any(), any(), any()))
            .thenReturn(Future.successful(quarterlyUpdatesCount))

          when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
            Future(
              OptOutProposition.createOptOutProposition(
                currentYear = TaxYear(2024, 2025),
                previousYearCrystallised = false,
                previousYearItsaStatus = ITSAStatus.Annual,
                currentYearItsaStatus = ITSAStatus.Annual,
                nextYearItsaStatus = ITSAStatus.Voluntary
              )
            )
          )

          when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
            .thenReturn(Future(NextTaxYear))

          whenReady(testController.viewScenarioHandler()) { result =>
            result shouldBe Right(NextYearCYAnnualScenario)
          }
        }
      }
    }

    "PreviousAndNoStatusValidScenario" when {

      "NoChosenTaxYear && proposition.isCurrentYearAnnual && proposition.isNextYearAnnual" should {

        "return the correct enum" in {

          val quarterlyUpdatesCount = 0

          when(mockOptOutService.getQuarterlyUpdatesCount(any())(any(), any(), any()))
            .thenReturn(Future.successful(quarterlyUpdatesCount))

          when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
            Future(
              OptOutProposition.createOptOutProposition(
                currentYear = TaxYear(2024, 2025),
                previousYearCrystallised = false,
                previousYearItsaStatus = ITSAStatus.Annual,
                currentYearItsaStatus = ITSAStatus.Annual,
                nextYearItsaStatus = ITSAStatus.Annual
              )
            )
          )

          when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
            .thenReturn(Future(NoChosenTaxYear))

          whenReady(testController.viewScenarioHandler()) { result =>
            result shouldBe Right(PreviousAndNoStatusValidScenario)
          }
        }
      }

      "PreviousTaxYear && proposition.isCurrentYearQuarterly && proposition.isNextYearAnnual" should {

        "return the correct enum" in {

          val quarterlyUpdatesCount = 0

          when(mockOptOutService.getQuarterlyUpdatesCount(any())(any(), any(), any()))
            .thenReturn(Future.successful(quarterlyUpdatesCount))

          when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
            Future(
              OptOutProposition.createOptOutProposition(
                currentYear = TaxYear(2024, 2025),
                previousYearCrystallised = false,
                previousYearItsaStatus = ITSAStatus.Annual,
                currentYearItsaStatus = ITSAStatus.Voluntary,
                nextYearItsaStatus = ITSAStatus.Annual
              )
            )
          )

          when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
            .thenReturn(Future(PreviousTaxYear))

          whenReady(testController.viewScenarioHandler()) { result =>
            result shouldBe Right(PreviousAndNoStatusValidScenario)
          }
        }
      }

      "PreviousTaxYear && proposition.isCurrentYearAnnual && proposition.isNextYearQuarterly" should {

        "return the correct enum" in {

          val quarterlyUpdatesCount = 0

          when(mockOptOutService.getQuarterlyUpdatesCount(any())(any(), any(), any()))
            .thenReturn(Future.successful(quarterlyUpdatesCount))

          when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
            Future(
              OptOutProposition.createOptOutProposition(
                currentYear = TaxYear(2024, 2025),
                previousYearCrystallised = false,
                previousYearItsaStatus = ITSAStatus.Annual,
                currentYearItsaStatus = ITSAStatus.Annual,
                nextYearItsaStatus = ITSAStatus.Voluntary
              )
            )
          )

          when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
            .thenReturn(Future(PreviousTaxYear))

          whenReady(testController.viewScenarioHandler()) { result =>
            result shouldBe Right(PreviousAndNoStatusValidScenario)
          }
        }
      }

      "PreviousTaxYear && proposition.isCurrentYearQuarterly && proposition.isNextYearQuarterly" should {

        "return the correct enum" in {

          val quarterlyUpdatesCount = 0

          when(mockOptOutService.getQuarterlyUpdatesCount(any())(any(), any(), any()))
            .thenReturn(Future.successful(quarterlyUpdatesCount))

          when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
            Future(
              OptOutProposition.createOptOutProposition(
                currentYear = TaxYear(2024, 2025),
                previousYearCrystallised = false,
                previousYearItsaStatus = ITSAStatus.Annual,
                currentYearItsaStatus = ITSAStatus.Voluntary,
                nextYearItsaStatus = ITSAStatus.Voluntary
              )
            )
          )

          when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
            .thenReturn(Future(PreviousTaxYear))

          whenReady(testController.viewScenarioHandler()) { result =>
            result shouldBe Right(PreviousAndNoStatusValidScenario)
          }
        }
      }
    }

    "PreviousYearDefaultScenario" when {
      val listOfPreviousDefaultLineups = Seq(
        (ITSAStatus.Exempt, ITSAStatus.Voluntary),
        (ITSAStatus.Exempt, ITSAStatus.Annual),
        (ITSAStatus.Exempt, ITSAStatus.Mandated),
        (ITSAStatus.DigitallyExempt, ITSAStatus.Voluntary),
        (ITSAStatus.DigitallyExempt, ITSAStatus.Annual),
        (ITSAStatus.DigitallyExempt, ITSAStatus.Mandated),
        (ITSAStatus.Dormant, ITSAStatus.Voluntary),
        (ITSAStatus.Dormant, ITSAStatus.Annual),
        (ITSAStatus.Dormant, ITSAStatus.Mandated),
        (ITSAStatus.Exempt, ITSAStatus.Exempt),
        (ITSAStatus.DigitallyExempt, ITSAStatus.DigitallyExempt),
        (ITSAStatus.Dormant, ITSAStatus.Dormant),
        (ITSAStatus.Voluntary, ITSAStatus.Exempt),
        (ITSAStatus.Annual, ITSAStatus.Exempt),
        (ITSAStatus.Mandated, ITSAStatus.Exempt),
        (ITSAStatus.Voluntary, ITSAStatus.DigitallyExempt),
        (ITSAStatus.Annual, ITSAStatus.DigitallyExempt),
        (ITSAStatus.Mandated, ITSAStatus.DigitallyExempt),
        (ITSAStatus.Voluntary, ITSAStatus.Dormant),
        (ITSAStatus.Annual, ITSAStatus.Dormant),
        (ITSAStatus.Mandated, ITSAStatus.Dormant)
      )
      listOfPreviousDefaultLineups.foreach { statuses =>
        s"PreviousTaxYear && CY = ${statuses._1} && CY+1 = ${statuses._2}" should {

          "return the correct enum" in {

            val quarterlyUpdatesCount = 0

            when(mockOptOutService.getQuarterlyUpdatesCount(any())(any(), any(), any()))
              .thenReturn(Future.successful(quarterlyUpdatesCount))

            when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
              Future(
                OptOutProposition.createOptOutProposition(
                  currentYear = TaxYear(2024, 2025),
                  previousYearCrystallised = false,
                  previousYearItsaStatus = ITSAStatus.Voluntary,
                  currentYearItsaStatus = statuses._1,
                  nextYearItsaStatus = statuses._2
                )
              )
            )

            when(mockOptOutService.determineOptOutIntentYear()(any(), any()))
              .thenReturn(Future(PreviousTaxYear))

            whenReady(testController.viewScenarioHandler()) { result =>
              result shouldBe Right(DefaultValidScenario)
            }
          }
        }
      }

    }
  }
}
