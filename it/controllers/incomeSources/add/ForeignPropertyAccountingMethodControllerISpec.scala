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

import config.featureswitch.IncomeSources
import forms.utils.SessionKeys.{addForeignPropertyAccountingMethod}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

class ForeignPropertyAccountingMethodControllerISpec extends ComponentSpecBase {
  val foreignPropertyAccountingMethodShowUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.show().url
  val foreignPropertyAccountingMethodSubmitUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.submit().url

  val checkForeignPropertyDetailsShowUrl: String = controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.show().url

  val cashCookie: Map[String, String] = Map(addForeignPropertyAccountingMethod -> "cash")
  val accrualsCookie: Map[String, String] = Map(addForeignPropertyAccountingMethod -> "traditional")

  val continueButtonText: String = messagesAPI("base.continue")

  s"calling GET $foreignPropertyAccountingMethodShowUrl" should {
    "render the Foreign Property Accounting Method page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $foreignPropertyAccountingMethodShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-business-accounting-method")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.add.foreignPropertyAccountingMethod.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $foreignPropertyAccountingMethodSubmitUrl" should {
    s"redirect to $checkForeignPropertyDetailsShowUrl" when {
      "user selects 'cash'" in {
        val formData: Map[String, Seq[String]] = Map("incomeSources.add.foreignPropertyAccountingMethod" -> Seq("cash"))
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-business-accounting-method", cashCookie)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkForeignPropertyDetailsShowUrl)
        )
      }
      s"redirect to $checkForeignPropertyDetailsShowUrl" when {
        "user selects 'accruals'" in {
          val formData: Map[String, Seq[String]] = Map("incomeSources.add.foreignPropertyAccountingMethod" -> Seq("traditional"))
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-business-accounting-method", accrualsCookie)(formData)

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkForeignPropertyDetailsShowUrl)
          )
        }
      }
      s"return BAD_REQUEST $foreignPropertyAccountingMethodShowUrl" when {
        "user does not select anything" in {
          val formData: Map[String, Seq[String]] = Map("incomeSources.add.foreignPropertyAccountingMethod" -> Seq(""))
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-business-accounting-method")(formData)

          result should have(
            httpStatus(BAD_REQUEST),
            elementTextByID("error-summary-heading")(messagesAPI("base.error_summary.heading"))
          )
        }
      }
    }
  }
}