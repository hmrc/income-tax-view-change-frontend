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

package controllers

import config.FrontendAppConfig
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.{MockOptInService, MockOptOutService}
import models.ReportingFrequencyViewModel
import models.admin.{OptInOptOutContentUpdateR17, OptOutFs, ReportingFrequencyPage, SignUpFs}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.itsaStatus.ITSAStatus.{Mandated, Voluntary}
import models.optout.OptOutMultiYearViewModel
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import services.DateService
import services.optIn.OptInService
import services.optout.{OptOutProposition, OptOutService}
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.BusinessDetailsTestConstants.business1
import views.html.ReportingFrequencyView

import scala.concurrent.Future


class ReportingFrequencyPageControllerSpec extends MockAuthActions
  with MockOptOutService with MockOptInService with MockitoSugar {

  val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptOutService].toInstance(mockOptOutService),
      api.inject.bind[OptInService].toInstance(mockOptInService),
      api.inject.bind[DateService].toInstance(dateService)
    ).configure(Map("feature-switches.read-from-mongo" -> "false"))
    .build()

  lazy val testController: ReportingFrequencyPageController = app.injector.instanceOf[ReportingFrequencyPageController]
  lazy val reportingFrequencyView: ReportingFrequencyView = app.injector.instanceOf[ReportingFrequencyView]

  mtdAllRoles.foreach { mtdRole =>
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    val isAgent = mtdRole != MTDIndividual

    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      s"the user is authenticated as a $mtdRole" should {
        "render the reporting frequency page" when {
          "the reporting frequency feature switch is enabled" in {
            enable(ReportingFrequencyPage, SignUpFs, OptOutFs)
            setupMockSuccess(mtdRole)
            mockUpdateOptOutJourneyStatusInSessionData()
            mockFetchOptOutJourneyCompleteStatus()
            mockFetchSavedOptInSessionData()
            mockUpdateOptInJourneyStatusInSessionData()

            val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)
            val optOutProposition: OptOutProposition = OptOutProposition.createOptOutProposition(
              currentYear = TaxYear(2024, 2025),
              previousYearCrystallised = false,
              previousYearItsaStatus = Mandated,
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = Mandated
            )

            when(mockOptInService.availableOptInTaxYear()(any(), any(), any())).thenReturn(
              Future(Seq(TaxYear(2024, 2025)))
            )
            when(mockOptOutService.reportingFrequencyViewModels()(any(), any(), any())).thenReturn(
              Future((optOutProposition, Some(OptOutMultiYearViewModel())))
            )
            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncome))

            val result = action(fakeRequest)

            status(result) shouldBe Status.OK
            contentAsString(result) shouldBe
              reportingFrequencyView(
                ReportingFrequencyViewModel(
                  isAgent = isAgent,
                  optOutJourneyUrl = Some(controllers.optOut.oldJourney.routes.OptOutChooseTaxYearController.show(isAgent).url),
                  optInTaxYears = Seq(TaxYear(2024, 2025)),
                  itsaStatusTable =
                    Seq(
                      ("2023 to 2024", None, Some("Quarterly (mandatory)")),
                      ("2024 to 2025", None, Some("Quarterly")),
                      ("2025 to 2026", None, Some("Quarterly (mandatory)"))
                    ),
                  isAnyOfBusinessLatent = true,
                  displayCeasedBusinessWarning = false,
                  mtdThreshold = "£50,000",
                  proposition = optOutProposition,
                  isSignUpEnabled = true,
                  isOptOutEnabled = true
                ),
                optInOptOutContentUpdateR17IsEnabled = false,
                nextUpdatesLink = if(isAgent) controllers.routes.NextUpdatesController.showAgent().url else controllers.routes.NextUpdatesController.show().url
              ).toString
          }
          "the reporting frequency and the R17 content feature switches are enabled" in {
            enable(ReportingFrequencyPage, SignUpFs, OptOutFs, OptInOptOutContentUpdateR17)
            setupMockSuccess(mtdRole)
            mockUpdateOptOutJourneyStatusInSessionData()
            mockFetchOptOutJourneyCompleteStatus()
            mockFetchSavedOptInSessionData()
            mockUpdateOptInJourneyStatusInSessionData()

            val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)
            val optOutProposition: OptOutProposition = OptOutProposition.createOptOutProposition(
              currentYear = TaxYear(2024, 2025),
              previousYearCrystallised = false,
              previousYearItsaStatus = Mandated,
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = Mandated
            )

            when(mockOptInService.availableOptInTaxYear()(any(), any(), any())).thenReturn(
              Future(Seq(TaxYear(2024, 2025)))
            )
            when(mockOptOutService.reportingFrequencyViewModels()(any(), any(), any())).thenReturn(
              Future((optOutProposition, Some(OptOutMultiYearViewModel())))
            )
            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncome))

            val result = action(fakeRequest)

            status(result) shouldBe Status.OK
            contentAsString(result) shouldBe
              reportingFrequencyView(
                ReportingFrequencyViewModel(
                  isAgent = isAgent,
                  optOutJourneyUrl = Some(controllers.optOut.oldJourney.routes.OptOutChooseTaxYearController.show(isAgent).url),
                  optInTaxYears = Seq(TaxYear(2024, 2025)),
                  itsaStatusTable =
                    Seq(
                      ("2023 to 2024", Some("Yes"), Some("Required")),
                      ("2024 to 2025", Some("Yes"), Some("Voluntarily signed up")),
                      ("2025 to 2026", Some("Yes"), Some("Required"))
                    ),
                  isAnyOfBusinessLatent = true,
                  displayCeasedBusinessWarning = false,
                  mtdThreshold = "£50,000",
                  proposition = optOutProposition,
                  isSignUpEnabled = true,
                  isOptOutEnabled = true
                ),
                optInOptOutContentUpdateR17IsEnabled = true,
                nextUpdatesLink = if(isAgent) controllers.routes.NextUpdatesController.showAgent().url else controllers.routes.NextUpdatesController.show().url
              ).toString
          }
        }

        "render the error page" when {
          "the reporting frequency feature switch is disabled" in {
            disable(ReportingFrequencyPage)
            setupMockSuccess(mtdRole)
            mockUpdateOptOutJourneyStatusInSessionData()
            mockFetchOptOutJourneyCompleteStatus()
            mockFetchSavedOptInSessionData()
            mockUpdateOptInJourneyStatusInSessionData()

            val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)
            val optOutProposition: OptOutProposition = OptOutProposition.createOptOutProposition(
              currentYear = TaxYear(2024, 2025),
              previousYearCrystallised = false,
              previousYearItsaStatus = Mandated,
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = Mandated
            )

            when(mockOptInService.availableOptInTaxYear()(any(), any(), any())).thenReturn(
              Future(Seq(TaxYear(2024, 2025)))
            )
            when(mockOptOutService.reportingFrequencyViewModels()(any(), any(), any())).thenReturn(
              Future((optOutProposition, Some(OptOutMultiYearViewModel())))
            )
            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncome))

            val result = action(fakeRequest)

            status(result) shouldBe INTERNAL_SERVER_ERROR
            contentAsString(result).contains("Sorry, there is a problem with the service") shouldBe true
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
    }
  }
}
