/*
 * Copyright 2026 HM Revenue & Customs
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

package hub.controllers

import common.enums.MTDIndividual
import common.mocks.auth.MockAuthActions
import common.connectors.ITSAStatusConnector
import common.services.DateServiceInterface
import common.testConstants.BaseTestConstants.businessesAndPropertyIncome
import hub.audit.models.ChooseTaxYearsRangeSubmittedAuditModel
import hub.forms.ChooseTaxYearsRangeForm
import play.api
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, NOT_IMPLEMENTED, OK, SEE_OTHER}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}

class ChooseTaxYearsRangeControllerSpec extends MockAuthActions {

  override lazy val app: Application =
    applicationBuilderWithAuthBindings
      .overrides(
        api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
        api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
      ).build()

  private lazy val controller = app.injector.instanceOf[ChooseTaxYearsRangeController]

  "show" should {
    "render the page when user has HMRC-MTD-IT and IR-SA enrolments" in {
      setupMockSuccess(MTDIndividual)
      mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
      setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

      val result = controller.show()(fakeGetRequestBasedOnMTDUserType(MTDIndividual))

      status(result) shouldBe OK
    }

    "redirect to home when user does not have IR-SA enrolment" in {
      setupMockUserAuthNoSAUtr
      mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
      setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

      val result = controller.show()(fakeGetRequestBasedOnMTDUserType(MTDIndividual))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(appConfig.homePageUrl(isAgent = false))
    }

    testMTDIndividualAuthFailures(controller.show())
  }

  "submit" should {
    "redirect to home when 2026 onwards option is selected" in {
      setupMockSuccess(MTDIndividual)
      mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
      setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

      val result = controller.submit()(
        fakePostRequestBasedOnMTDUserType(MTDIndividual)
          .withFormUrlEncodedBody(ChooseTaxYearsRangeForm.response -> ChooseTaxYearsRangeForm.mtdOption)
      )

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(appConfig.homePageUrl(isAgent = false))
      verifyExtendedAudit(ChooseTaxYearsRangeSubmittedAuditModel(
        taxYearsPresented = Seq("2018 to 2019 onwards", "2017 to 2018 and earlier"),
        taxYearRangeSelected = ChooseTaxYearsRangeForm.mtdOption
      ))
    }

    "return NotImplemented when 2025 to 2026 and earlier option is selected" in {
      setupMockSuccess(MTDIndividual)
      mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
      setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

      val result = controller.submit()(
        fakePostRequestBasedOnMTDUserType(MTDIndividual)
          .withFormUrlEncodedBody(ChooseTaxYearsRangeForm.response -> ChooseTaxYearsRangeForm.legacyOption)
      )

      status(result) shouldBe NOT_IMPLEMENTED
      contentAsString(result) should include("TODO")
    }

    "redirect to home when user does not have IR-SA enrolment" in {
      setupMockUserAuthNoSAUtr
      mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
      setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

      val result = controller.submit()(
        fakePostRequestBasedOnMTDUserType(MTDIndividual)
          .withFormUrlEncodedBody(ChooseTaxYearsRangeForm.response -> ChooseTaxYearsRangeForm.mtdOption)
      )

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(appConfig.homePageUrl(isAgent = false))
    }

    "return BadRequest when no option is selected" in {
      setupMockSuccess(MTDIndividual)
      mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
      setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

      val result = controller.submit()(fakePostRequestBasedOnMTDUserType(MTDIndividual))

      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include("Select an option")
    }

    testMTDIndividualAuthFailures(controller.submit())
  }
}
