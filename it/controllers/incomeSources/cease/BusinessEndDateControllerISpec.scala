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

package controllers.incomeSources.cease

import config.featureswitch.IncomeSources
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testPropertyIncomeSourceId, testSelfEmploymentId}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, ukPropertyOnlyResponse}

class BusinessEndDateControllerISpec extends ComponentSpecBase {
  val dateBusinessShowUrl: String = controllers.incomeSources.cease.routes.BusinessEndDateController.show(testPropertyIncomeSourceId).url
  val dateBusinessSubmitUrl: String = controllers.incomeSources.cease.routes.BusinessEndDateController.submit(testPropertyIncomeSourceId).url
  val checkCeaseBusinessDetailsShowUrl: String = controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.show().url
  val hintText: String = messagesAPI("dateForm.hint")
  val continueButtonText: String = messagesAPI("base.continue")

  s"calling GET $dateBusinessShowUrl" should {
    "render the Date Business Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call GET $dateBusinessShowUrl")
        val result = IncomeTaxViewChangeFrontend.getBusinessEndDate
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.cease.BusinessEndDate.heading"),
          elementTextByID("business-end-date-hint")(hintText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateBusinessSubmitUrl" should {
    "redirect to showBusinessEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("business-end-date.day" -> Seq("27"), "business-end-date.month" -> Seq("8"), "business-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/cease/business-end-date?id=$testSelfEmploymentId")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkCeaseBusinessDetailsShowUrl)
        )
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("business-end-date.day" -> Seq("aa"), "business-end-date.month" -> Seq("5"), "business-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/cease/business-end-date?id=$testSelfEmploymentId")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("business-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.BusinessEndDate.error.invalid"))
        )
      }
    }
  }
}

