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

package v2.controllers

import connectors.ITSAStatusConnector
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.MockYearOfMigrationService
import models.itsaStatus.ITSAStatusYearOfMigrationModel
import services.{DateServiceInterface, YearOfMigrationService}
import testConstants.BaseTestConstants.{businessesAndPropertyIncome, testNino, testSaUtr}
import audit.models.ChooseTaxYearsRangeSubmittedAuditModel
import forms.ChooseTaxYearsRangeForm
import play.api
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}

import scala.concurrent.Future

class ChooseTaxYearsRangeControllerSpec extends MockAuthActions with MockYearOfMigrationService {

  override lazy val app: Application =
    applicationBuilderWithAuthBindings
      .overrides(
        api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
        api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface),
        api.inject.bind[YearOfMigrationService].toInstance(mockYearOfMigrationService)
      ).build()

  private lazy val controller = app.injector.instanceOf[ChooseTaxYearsRangeController]

  "show" should {
    "render the page" in {
      setupMockSuccess(MTDIndividual)
      mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
      setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
      setupMockYearOfMigration(testNino)(Future.successful(ITSAStatusYearOfMigrationModel(Some("2020"))))

      val result = controller.show()(fakeGetRequestBasedOnMTDUserType(MTDIndividual))

      status(result) shouldBe OK
    }
  }

  "submit" should {
    "validate the landing page form without an SA enrolment instead of redirecting home" in {
      setupMockSuccess(MTDIndividual)
      setupMockUserAuthNoSAUtr
      mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
      setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
      setupMockYearOfMigration(testNino)(Future.successful(ITSAStatusYearOfMigrationModel(Some("2020"))))

      val result = controller.submit()(fakePostRequestBasedOnMTDUserType(MTDIndividual))

      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include("Select which tax years you want to view and manage")
    }

    "redirect to your-tasks when MTD onwards option is selected" in {
      setupMockSuccess(MTDIndividual)
      mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
      setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
      setupMockYearOfMigration(testNino)(Future.successful(ITSAStatusYearOfMigrationModel(Some("2018"))))

      val result = controller.submit()(
        fakePostRequestBasedOnMTDUserType(MTDIndividual)
          .withFormUrlEncodedBody(ChooseTaxYearsRangeForm.response -> ChooseTaxYearsRangeForm.mtdOption)
      )

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(v2.controllers.newHomePage.routes.HandleYourTasksController.show().url)
      verifyExtendedAudit(ChooseTaxYearsRangeSubmittedAuditModel(
        taxYearsPresented = Seq("2018 to 2019 onwards", "2017 to 2018 and earlier"),
        taxYearRangeSelected = ChooseTaxYearsRangeForm.mtdOption
      ))
    }

    "redirect to classic SA when legacy option is selected" in {
      setupMockSuccess(MTDIndividual)
      mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
      setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
      setupMockYearOfMigration(testNino)(Future.successful(ITSAStatusYearOfMigrationModel(Some("2020"))))
      val result = controller.submit()(
        fakePostRequestBasedOnMTDUserType(MTDIndividual)
          .withFormUrlEncodedBody(ChooseTaxYearsRangeForm.response -> ChooseTaxYearsRangeForm.legacyOption)
      )

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(appConfig.saViewLandPService(testSaUtr))
    }

    "return BadRequest when no option is selected" in {
      setupMockSuccess(MTDIndividual)
      mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
      setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
      setupMockYearOfMigration(testNino)(Future.successful(ITSAStatusYearOfMigrationModel(Some("2020"))))

      val result = controller.submit()(fakePostRequestBasedOnMTDUserType(MTDIndividual))

      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include("Select which tax years you want to view and manage")
    }

    testMTDIndividualAuthFailures(controller.submit())

  }

  "showPtaMultipleEnrolment" should {
    "render the page" in {
      setupMockSuccess(MTDIndividual)
      mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
      setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
      setupMockYearOfMigration(testNino)(Future.successful(ITSAStatusYearOfMigrationModel(Some("2020"))))

      val result = controller.showPtaMultipleEnrolment()(fakeGetRequestBasedOnMTDUserType(MTDIndividual))

      status(result) shouldBe OK
    }
  }

  "submitPtaMultipleEnrolment" should {
    "redirect to your-tasks when MTD onwards option is selected" in {
      setupMockSuccess(MTDIndividual)
      mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
      setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
      setupMockYearOfMigration(testNino)(Future.successful(ITSAStatusYearOfMigrationModel(Some("2018"))))

      val result = controller.submitPtaMultipleEnrolment()(
        fakePostRequestBasedOnMTDUserType(MTDIndividual)
          .withFormUrlEncodedBody(ChooseTaxYearsRangeForm.response -> ChooseTaxYearsRangeForm.mtdOption)
      )

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(v2.controllers.newHomePage.routes.HandleYourTasksController.show().url)
      verifyExtendedAudit(ChooseTaxYearsRangeSubmittedAuditModel(
        taxYearsPresented = Seq("2018 to 2019 onwards", "2017 to 2018 and earlier"),
        taxYearRangeSelected = ChooseTaxYearsRangeForm.mtdOption
      ))
    }

    "redirect to PTA Wrong Account Page when legacy option is selected" in {
      setupMockSuccess(MTDIndividual)
      mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
      setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
      setupMockYearOfMigration(testNino)(Future.successful(ITSAStatusYearOfMigrationModel(Some("2020"))))
      val result = controller.submitPtaMultipleEnrolment()(
        fakePostRequestBasedOnMTDUserType(MTDIndividual)
          .withFormUrlEncodedBody(ChooseTaxYearsRangeForm.response -> ChooseTaxYearsRangeForm.legacyOption)
      )

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(appConfig.personalTaxAccountWrongAccountUrl)
    }

    "return BadRequest when no option is selected" in {
      setupMockSuccess(MTDIndividual)
      mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
      setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
      setupMockYearOfMigration(testNino)(Future.successful(ITSAStatusYearOfMigrationModel(Some("2020"))))

      val result = controller.submitPtaMultipleEnrolment()(fakePostRequestBasedOnMTDUserType(MTDIndividual))

      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include("Select which tax years you want to view and manage")
    }

    testMTDIndividualAuthFailures(controller.submitPtaMultipleEnrolment())
  }
}
