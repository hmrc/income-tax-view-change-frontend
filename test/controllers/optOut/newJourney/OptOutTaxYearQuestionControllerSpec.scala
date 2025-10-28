/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.optOut.newJourney

import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.MockOptOutService
import models.admin.{OptInOptOutContentUpdateR17, OptOutFs, ReportingFrequencyPage}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.optout.newJourney.OptOutTaxYearQuestionViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.optout._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class OptOutTaxYearQuestionControllerSpec extends MockAuthActions with MockOptOutService {

  lazy val mockOptOutSubmissionService: OptOutSubmissionService = mock(classOf[OptOutSubmissionService])

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptOutService].toInstance(mockOptOutService),
      api.inject.bind[OptOutSubmissionService].toInstance(mockOptOutSubmissionService)
    ).build()

  lazy val testController = app.injector.instanceOf[OptOutTaxYearQuestionController]

  val currentYear = Some("2025")
  val nextYear = Some("2026")


  val optOutProposition: OptOutProposition = OptOutTestSupport.buildThreeYearOptOutProposition()
  val yearEnd = optOutProposition.availableTaxYearsForOptOut(1).endYear
  val currentTaxYear: TaxYear = TaxYear.forYearEnd(yearEnd)
  val optOutTaxYear: CurrentOptOutTaxYear = CurrentOptOutTaxYear(ITSAStatus.Voluntary, currentTaxYear)
  val optOutState = Some(MultiYearOptOutDefault)
  val viewModel = OptOutTaxYearQuestionViewModel(
    taxYear = optOutTaxYear,
    optOutState = optOutState,
    numberOfQuarterlyUpdates = 0,
    currentYearStatus = ITSAStatus.Voluntary,
    nextYearStatus = ITSAStatus.Voluntary
  )

  private def reportingObligationsLink(isAgent: Boolean): Option[String] = {
    if (isAgent) {
      Some("/report-quarterly/income-and-expenses/view/agents/reporting-frequency")
    } else {
      Some("/report-quarterly/income-and-expenses/view/reporting-frequency")
    }
  }

  private def homeLink(isAgent: Boolean): Option[String] = {
    if (isAgent) {
      Some("/report-quarterly/income-and-expenses/view/agents/client-income-tax")
    } else {
      Some("/report-quarterly/income-and-expenses/view")
    }
  }

  private def taxYearQuestionLink(isAgent: Boolean): Option[String] = {
    if (isAgent) {
      Some("/report-quarterly/income-and-expenses/view/agents/optout?taxYear=2025")
    } else {
      Some("/report-quarterly/income-and-expenses/view/optout?taxYear=2025")
    }
  }

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual

    s"show(isAgent = $isAgent)" when {
      s"the user is authenticated as a $mtdRole" should {
        s"render the Opt Out Tax Year Question page when returning a valid view model" in {
          val action = testController.show(isAgent, currentYear)
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)

          enable(OptOutFs, ReportingFrequencyPage, OptInOptOutContentUpdateR17)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockIsOptOutTaxYearValid(Future.successful(Some(viewModel)))
          mockSaveIntent(Future.successful(true))

          val result = action(fakeRequest)

          status(result) shouldBe OK
        }

        "redirect the user to the reporting obligations page when no view model is returned" in {
          val action = testController.show(isAgent, currentYear)
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)

          enable(OptOutFs, ReportingFrequencyPage, OptInOptOutContentUpdateR17)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockIsOptOutTaxYearValid(Future.successful(None))

          val result = action(fakeRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe reportingObligationsLink(isAgent)
        }
        "redirect the user to the home page when all the feature switches are disabled" in {
          disable(OptOutFs)
          disable(ReportingFrequencyPage)
          disable(OptInOptOutContentUpdateR17)

          val action = testController.show(isAgent, currentYear)
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          val result = action(fakeRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe homeLink(isAgent)
        }

        "redirect the user to the reporting obligations page when only the OptOutOptInContentR17 feature switch is disabled" in {
          enable(OptOutFs, ReportingFrequencyPage)
          disable(OptInOptOutContentUpdateR17)

          val action = testController.show(isAgent, currentYear)
          val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

          val result = action(fakeRequest)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe reportingObligationsLink(isAgent)
        }
      }
    }
    s"submit(isAgent = $isAgent)" when {
      s"the user is authenticated as a $mtdRole" should {
        "redirect the user when they select 'Yes' - to confirm page" in {
          val action = testController.submit(isAgent, currentYear)
          val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

          enable(OptOutFs, ReportingFrequencyPage, OptInOptOutContentUpdateR17)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockIsOptOutTaxYearValid(Future.successful(Some(viewModel)))

          when(mockOptOutSubmissionService.updateTaxYearsITSAStatusRequest()(any(), any(), any()))
            .thenReturn(Future(Right(List(ITSAStatusUpdateResponseSuccess()))))

          val formData = Map("opt-out-tax-year-question" -> "Yes")

          val result = action(fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.optOut.routes.ConfirmedOptOutController.show(isAgent).url)
        }

        "redirect the user when they select 'Yes' - opt out single year followed by mandated with updates" in {
          val action = testController.submit(isAgent, currentYear)
          val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

          enable(OptOutFs, ReportingFrequencyPage, OptInOptOutContentUpdateR17)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockIsOptOutTaxYearValid(Future.successful(Some(viewModel.copy(numberOfQuarterlyUpdates = 2, optOutState = Some(OneYearOptOutFollowedByMandated)))))
          mockMakeOptOutUpdateRequest(Future.successful(ITSAStatusUpdateResponseSuccess()))

          val formData = Map(
            "opt-out-tax-year-question" -> "Yes",
          )

          val result = action(fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.optOut.newJourney.routes.ConfirmOptOutUpdateController.show(isAgent, currentYear.getOrElse("")).url)
        }

        "redirect the user to the opt out error page when they select 'Yes' and the submit fails " in {
          val action = testController.submit(isAgent, currentYear)
          val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

          enable(OptOutFs, ReportingFrequencyPage, OptInOptOutContentUpdateR17)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockIsOptOutTaxYearValid(Future.successful(Some(viewModel)))

          when(mockOptOutSubmissionService.updateTaxYearsITSAStatusRequest()(any(), any(), any()))
            .thenReturn(Future(Right(List(ITSAStatusUpdateResponseFailure.defaultFailure()))))

          val formData = Map(
            "opt-out-tax-year-question" -> "Yes",
          )

          val result = action(fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.optOut.oldJourney.routes.OptOutErrorController.show(isAgent).url)
        }

        "redirect to the reporting obligations page when the user selects 'No'" in {
          val action = testController.submit(isAgent, currentYear)
          val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

          enable(OptOutFs, ReportingFrequencyPage, OptInOptOutContentUpdateR17)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockIsOptOutTaxYearValid(Future.successful(Some(viewModel)))

          val formData = Map(
            "opt-out-tax-year-question" -> "No"
          )

          val result = action(fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*))

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe reportingObligationsLink(isAgent)
        }

        "return an error when an invalid form response is submitted" in {
          val action = testController.submit(isAgent, currentYear)
          val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

          enable(OptOutFs, ReportingFrequencyPage, OptInOptOutContentUpdateR17)

          setupMockSuccess(mtdRole)
          setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
          mockIsOptOutTaxYearValid(Future.successful(Some(viewModel)))

          val formData = Map(
            "opt-out-tax-year-question" -> ""
          )

          val result = action(fakeRequest.withFormUrlEncodedBody(formData.toSeq: _*))

          status(result) shouldBe BAD_REQUEST
        }
      }
    }
  }
}
