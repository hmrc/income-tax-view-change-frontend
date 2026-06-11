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

package obligations.controllers.reportingObligations

import common.config.FrontendAppConfig
import common.connectors.ITSAStatusConnector
import common.enums.MTDIndividual
import common.mocks.auth.MockAuthActions
import common.mocks.services.MockDateService
import common.models.admin.{OptOutFs, SignUpFs}
import common.services.{DateService, DateServiceInterface}
import common.models.itsaStatus.ITSAStatus.{Mandated, Voluntary}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import obligations.mocks.services.{MockOptOutService, MockSignUpService}
import obligations.models.reportingObligations.ReportingFrequencyViewModel
import obligations.services.reportingObligations.optOut.{OptOutProposition, OptOutService}
import obligations.services.reportingObligations.signUp.SignUpService
import obligations.services.reportingObligations.signUp.core.SignUpProposition
import obligations.views.html.reportingObligations.ReportingFrequencyView
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, mock as mMock}
import org.scalatestplus.mockito.MockitoSugar
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import common.testConstants.BaseTestConstants.{testMtditid, testNino}
import obligations.testConstants.BusinessDetailsTestConstants.business1

import java.time.LocalDate
import scala.concurrent.Future


class ReportingFrequencyPageControllerSpec extends MockAuthActions
  with MockOptOutService with MockSignUpService with MockDateService with MockitoSugar {

  val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  lazy val mockDateServiceInjected: DateService = mMock(classOfDateService)

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptOutService].toInstance(mockOptOutService),
      api.inject.bind[SignUpService].toInstance(mockSignUpService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInjected)
    ).configure(Map("feature-switches.read-from-mongo" -> "true"))
    .build()

  lazy val testController: ReportingFrequencyPageController = app.injector.instanceOf[ReportingFrequencyPageController]
  lazy val reportingFrequencyView: ReportingFrequencyView = app.injector.instanceOf[ReportingFrequencyView]

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockDateServiceInterface.getCurrentDate).thenReturn(LocalDate.of(2023, 1, 1))
    when(mockDateServiceInterface.getCurrentTaxYearEnd).thenReturn(2024)
  }

  mtdAllRoles.foreach { mtdRole =>

    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    val isAgent = mtdRole != MTDIndividual

    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      s"the user is authenticated as a $mtdRole" should {
        "render the reporting frequency page" when {
          "the reporting frequency feature switches are enabled" in {

            val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)

            when(mockDateServiceInjected.getCurrentDate).thenReturn(LocalDate.of(2023, 1, 1))
            when(mockDateServiceInjected.getCurrentTaxYear).thenReturn(TaxYear(2023, 2024))
            setupMockSuccess(mtdRole, false, List(SignUpFs, OptOutFs))
            mockItsaStatusRetrievalAction(singleBusinessIncome, TaxYear(2023, 2024))
            mockUpdateOptOutJourneyStatusInSessionData()
            mockFetchOptOutJourneyCompleteStatus()
            mockUpdateOptInJourneyStatusInSessionData()

            val optOutProposition: OptOutProposition =
              OptOutProposition.createOptOutProposition(
                currentYear = TaxYear(2024, 2025),
                previousYearCrystallised = false,
                previousYearItsaStatus = Mandated,
                currentYearItsaStatus = Voluntary,
                nextYearItsaStatus = Mandated
              )

            val optInProposition: SignUpProposition =
              SignUpProposition.createSignUpProposition(
                currentYear = TaxYear(2024, 2025),
                currentYearItsaStatus = Voluntary,
                nextYearItsaStatus = Mandated
              )

            when(mockSignUpService.fetchSignUpProposition()(any(), any(), any()))
              .thenReturn(Future(optInProposition))

            when(mockSignUpService.availableSignUpTaxYear()(any(), any(), any()))
              .thenReturn(Future(Seq(TaxYear(2024, 2025))))

            when(mockOptOutService.initialiseJourneyWithProposition()(any(), any(), any()))
              .thenReturn(Future(optOutProposition))

            when(mockIncomeSourceConnector.getIncomeSources()(any(), any()))
              .thenReturn(Future(singleBusinessIncome))

            val result = action(fakeRequest)

            status(result) shouldBe Status.OK

            contentAsString(result) shouldBe
              reportingFrequencyView(
                ReportingFrequencyViewModel(
                  isAgent = isAgent,
                  signUpTaxYears = Seq(TaxYear(2024, 2025)),
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
                nextUpdatesLink =
                  if (isAgent) obligations.controllers.routes.NextUpdatesController.showAgent().url
                  else obligations.controllers.routes.NextUpdatesController.show().url
              ).toString
          }
          "the reporting frequency and the R17 content feature switches are enabled" in {

            val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)

            setupMockSuccess(mtdRole, false, List(SignUpFs, OptOutFs))
            mockItsaStatusRetrievalAction(singleBusinessIncome, TaxYear(2023, 2024))
            mockUpdateOptOutJourneyStatusInSessionData()
            mockFetchOptOutJourneyCompleteStatus()
            mockFetchSavedSignUpSessionData()
            mockUpdateOptInJourneyStatusInSessionData()

            val optOutProposition: OptOutProposition =
              OptOutProposition.createOptOutProposition(
                currentYear = TaxYear(2024, 2025),
                previousYearCrystallised = false,
                previousYearItsaStatus = Mandated,
                currentYearItsaStatus = Voluntary,
                nextYearItsaStatus = Mandated
              )

            val optInProposition: SignUpProposition =
              SignUpProposition.createSignUpProposition(
                currentYear = TaxYear(2024, 2025),
                currentYearItsaStatus = Voluntary,
                nextYearItsaStatus = Mandated
              )

            when(mockSignUpService.fetchSignUpProposition()(any(), any(), any()))
              .thenReturn(Future(optInProposition))

            when(mockSignUpService.availableSignUpTaxYear()(any(), any(), any())).thenReturn(
              Future(Seq(TaxYear(2024, 2025)))
            )
            when(mockOptOutService.initialiseJourneyWithProposition()(any(), any(), any())).thenReturn(
              Future(optOutProposition)
            )
            when(
              mockIncomeSourceConnector.getIncomeSources()(any(), any())
            ).thenReturn(Future(singleBusinessIncome))

            val result = action(fakeRequest)

            status(result) shouldBe Status.OK
            contentAsString(result) shouldBe
              reportingFrequencyView(
                ReportingFrequencyViewModel(
                  isAgent = isAgent,
                  signUpTaxYears = Seq(TaxYear(2024, 2025)),
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
                nextUpdatesLink = if (isAgent) obligations.controllers.routes.NextUpdatesController.showAgent().url else obligations.controllers.routes.NextUpdatesController.show().url
              ).toString
          }
        }

      }
      testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
    }
  }
}
