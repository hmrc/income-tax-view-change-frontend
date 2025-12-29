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

package controllers.bta

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.DateServiceInterface
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessIncome

class BTAPartialControllerSpec extends MockAuthActions {

  override lazy val app: Application =
    applicationBuilderWithAuthBindings
      .overrides(
        api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
        api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
        api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
      ).build()

  lazy val testController = app.injector.instanceOf[BTAPartialController]
  val action = testController.setupPartial

  ".setupPartial()" when {

    "the user is authorised" should {

      "render the BTA partial page" in {

        setupMockSuccess(MTDIndividual)
        mockItsaStatusRetrievalAction(businessIncome)
        mockBusinessIncomeSource()

        val result = action(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        status(result) shouldBe Status.OK
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
        document.getElementById("it-quarterly-reporting-heading").text() shouldBe messages("bta_partial.heading")
      }
    }
    testMTDAuthFailuresForRole(action, MTDIndividual)(fakeRequestWithActiveSession)
  }
}
