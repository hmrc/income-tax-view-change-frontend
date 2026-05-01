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

package controllers

import enums.{MTDIndividual, MTDPrimaryAgent}
import mocks.auth.MockAuthActions
import play.api
import play.api.Application
import play.api.test.Helpers.*
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.noIncomeDetails

class NoIncomeSourcesControllerSpec extends MockAuthActions {

  override lazy val app: Application = applicationBuilderWithAuthBindings.build()

  lazy val controller: NoIncomeSourcesController = app.injector.instanceOf[NoIncomeSourcesController]

  "show()" when {

    "the user is an individual" should {
      "render the no income sources page" in {
        setupMockSuccess(MTDIndividual)
        mockItsaStatusRetrievalAction(noIncomeDetails)
        mockNoIncomeSources()

        val result = controller.show(isAgent = false)(fakeGetRequestBasedOnMTDUserType(MTDIndividual))

        status(result) shouldBe OK
        JsoupParse(result).toHtmlDocument.getElementById("no-income-sources-heading").text() shouldBe
          messages("noIncomeSources.error.title")
      }
    }

    "the user is an agent" should {
      "render the no income sources page" in {
        setupMockSuccess(MTDPrimaryAgent)
        mockItsaStatusRetrievalAction(noIncomeDetails)
        mockNoIncomeSources()

        val result = controller.show(isAgent = true)(fakeGetRequestBasedOnMTDUserType(MTDPrimaryAgent))

        status(result) shouldBe OK
        JsoupParse(result).toHtmlDocument.getElementById("no-income-sources-heading").text() shouldBe
          messages("noIncomeSources.error.title")
      }
    }
  }

  testMTDAuthFailuresForRole(
    controller.show(isAgent = false),
    MTDIndividual
  )(fakeGetRequestBasedOnMTDUserType(MTDIndividual))
}