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

import common.controllers.ControllerISpecHelper
import common.enums.MTDIndividual
import common.helpers.GetInsourceDetailsStub
import common.helpers.servicemocks.AuditStub
import common.testConstants.BaseIntegrationTestConstants.{testMtditid, testSaUtr}
import common.testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncome
import hub.forms.ChooseTaxYearsRangeForm
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json

class ChooseTaxYearsRangeControllerISpec extends ControllerISpecHelper {

  private val path = "/manage-self-assessment/choose-tax-years-range"
  private def stubIncomeSourceDetails(): Unit =
    GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncome)

  s"GET $path" should {
    "render the page for an authorised individual" in {
      stubAuthorised(MTDIndividual)
      stubIncomeSourceDetails()

      val result = buildGETMTDClient(path).futureValue

      result should have(
        httpStatus(OK),
        pageTitle(MTDIndividual, "Which tax years do you want to view and manage?")
      )
    }

    testAuthFailures(path, MTDIndividual)
  }

  s"POST $path" should {
    "redirect to your-tasks when MTD option is selected" in {
      stubAuthorised(MTDIndividual)
      stubIncomeSourceDetails()

      val result = buildPOSTMTDPostClient(
        path = path,
        body = Map(ChooseTaxYearsRangeForm.response -> Seq(ChooseTaxYearsRangeForm.mtdOption))
      ).futureValue

      result should have(
        httpStatus(SEE_OTHER),
        redirectURI(hub.controllers.newHomePage.routes.HandleYourTasksController.show().url)
      )

      AuditStub.verifyAuditContainsDetail(
        Json.obj(
          "taxYearsPresented" -> Seq("2017 to 2018 onwards", "2016 to 2017 and earlier"),
          "taxYearRangeSelected" -> ChooseTaxYearsRangeForm.mtdOption
        )
      )
    }

    "redirect to classic SA when legacy option is selected" in {
      stubAuthorised(MTDIndividual)
      stubIncomeSourceDetails()

      val result = buildPOSTMTDPostClient(
        path = path,
        body = Map(ChooseTaxYearsRangeForm.response -> Seq(ChooseTaxYearsRangeForm.legacyOption))
      ).futureValue

      result should have(
        httpStatus(SEE_OTHER),
        redirectURI(appConfig.saViewLandPService(testSaUtr))
      )
    }

    "return BadRequest when no option is selected" in {
      stubAuthorised(MTDIndividual)
      stubIncomeSourceDetails()

      val result = buildPOSTMTDPostClient(path = path, body = Map.empty).futureValue

      result should have(
        httpStatus(BAD_REQUEST),
        elementTextByClass("govuk-error-summary__title")("There is a problem"),
        elementTextByClass("govuk-error-summary__list")("Select which tax years you want to view and manage")
      )
    }

    testAuthFailures(path, MTDIndividual, optBody = Some(Map(ChooseTaxYearsRangeForm.response -> Seq(ChooseTaxYearsRangeForm.mtdOption))))
  }
}
