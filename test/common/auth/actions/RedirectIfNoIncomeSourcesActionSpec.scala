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

package common.auth.actions

import common.auth.MtdItUser
import common.auth.actions.AuthActionsTestData.*
import common.controllers.routes as appRoutes
import common.models.admin.{FeatureSwitchName, NoIncomeSourcesRedirect}
import common.testConstants.BaseTestConstants.*
import common.testConstants.IncomeSourceDetailsTestConstants.noIncomeDetails
import org.scalatest.Assertion
import play.api.mvc.{Result, Results}
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

import scala.concurrent.Future

class RedirectIfNoIncomeSourcesActionSpec extends AuthActionsSpecHelper {

  def actionWithSwitch(enabledSwitches: Set[FeatureSwitchName]) =
    new RedirectIfNoIncomeSourcesAction(mockAppConfig) {
      override def isEnabled(featureSwitch: FeatureSwitchName)(implicit user: MtdItUser[_]): Boolean =
        enabledSwitches.contains(featureSwitch)
    }

  val actionEnabled  = actionWithSwitch(Set(NoIncomeSourcesRedirect))
  val actionDisabled = actionWithSwitch(Set.empty)

  def defaultAsyncBody(
                        requestTestCase: MtdItUser[_] => Assertion
                      ): MtdItUser[_] => Future[Result] = testRequest => {
    requestTestCase(testRequest)
    Future.successful(Results.Ok("Successful"))
  }

  def defaultAsync: MtdItUser[_] => Future[Result] = _ => Future.successful(Results.Ok("Successful"))

  "refine" when {

    "the NoIncomeSourcesRedirect feature switch is enabled" when {

      "the user has income sources" should {

        "allow an individual user through" in {
          val request = defaultMTDITUser(
            af = Some(Individual),
            incomeSources = businessesAndPropertyIncome,
            request = fakeRequestWithActiveSession
          )

          val result = actionEnabled.invokeBlock(request, defaultAsyncBody(_.incomeSources shouldBe businessesAndPropertyIncome))

          status(result) shouldBe OK
        }

        "allow an agent user through" in {
          val request = defaultMTDITUser(
            af = Some(Agent),
            incomeSources = businessesAndPropertyIncome,
            request = fakeRequestWithActiveSession
          )

          val result = actionEnabled.invokeBlock(request, defaultAsyncBody(_.incomeSources shouldBe businessesAndPropertyIncome))

          status(result) shouldBe OK
        }
      }

      "the user has no income sources" should {

        "redirect an individual user to the no income sources page" in {
          val request = defaultMTDITUser(
            af = Some(Individual),
            incomeSources = noIncomeDetails,
            request = fakeRequestWithActiveSession
          )

          val result = actionEnabled.invokeBlock(request, defaultAsync)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(appRoutes.NoIncomeSourcesController.show(isAgent = false).url)
        }

        "redirect an agent user to the no income sources page" in {
          val request = defaultMTDITUser(
            af = Some(Agent),
            incomeSources = noIncomeDetails,
            request = fakeRequestWithActiveSession
          )

          val result = actionEnabled.invokeBlock(request, defaultAsync)

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(appRoutes.NoIncomeSourcesController.show(isAgent = true).url)
        }
      }
    }

    "the NoIncomeSourcesRedirect feature switch is disabled" should {

      "allow an individual user with no income sources through" in {
        val request = defaultMTDITUser(
          af = Some(Individual),
          incomeSources = noIncomeDetails,
          request = fakeRequestWithActiveSession
        )

        val result = actionDisabled.invokeBlock(request, defaultAsync)

        status(result) shouldBe OK
      }

      "allow an agent user with no income sources through" in {
        val request = defaultMTDITUser(
          af = Some(Agent),
          incomeSources = noIncomeDetails,
          request = fakeRequestWithActiveSession
        )

        val result = actionDisabled.invokeBlock(request, defaultAsync)

        status(result) shouldBe OK
      }
    }
  }
}