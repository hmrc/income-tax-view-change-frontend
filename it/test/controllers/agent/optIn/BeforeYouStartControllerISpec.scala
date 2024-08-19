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

package controllers.agent.optIn

import controllers.optIn.BeforeYouStartControllerISpec._
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, clientDetailsWithoutConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class BeforeYouStartControllerISpec extends ComponentSpecBase {
  val isAgent: Boolean = true
  val beforeYouStartControllerPageUrl: String = controllers.optIn.routes.BeforeYouStartController.show(isAgent).url

  s"calling GET $beforeYouStartControllerPageUrl" should {
    s"render before you start page $beforeYouStartControllerPageUrl" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.getBeforeYouStart(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("optIn.beforeYouStart.heading"),
          elementTextByID("heading")(headingText),
          elementTextByID("desc1")(desc1),
          elementTextByID("desc2")(desc2),
          elementTextByID("reportQuarterly")(reportQuarterlyText),
          elementTextByID("voluntaryStatus")(voluntaryStatus),
          elementTextByID("voluntaryStatus-text")(voluntaryStatusText)
        )
      }
    }
  }

  s"calling GET $beforeYouStartControllerPageUrl" when {
    "the user is unauthorised" in {
      stubAuthorisedAgentUser(authorised = false)

      val result = IncomeTaxViewChangeFrontend.getBeforeYouStart(clientDetailsWithoutConfirmation)

      Then(s"The user is redirected to ${controllers.routes.SignInController.signIn.url}")
      result should have(
        httpStatus(SEE_OTHER)
      )
    }
  }

}

object BeforeYouStartControllerISpec {
  val headingText = "Before you start"
  val desc1 = "Reporting quarterly allows HMRC to give you a more precise forecast of how much tax you owe to help you budget more accurately."
  val desc2 = "To report quarterly you will need compatible software. There are both paid and free options for you or your agent to choose from."
  val reportQuarterlyText = "Reporting quarterly"
  val voluntaryStatus = "Your voluntary status"
  val voluntaryStatusText = "As you would be voluntarily opting in to reporting quarterly, you can decide to opt out and return to reporting annually at any time."
}