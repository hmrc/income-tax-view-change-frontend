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

package authV2

import auth.MtdItUser
import auth.authV2.actions.RedirectIfNoIncomeSourcesAction
import authV2.AuthActionsTestData.*
import org.scalatest.Assertion
import play.api.mvc.{Result, Results}
import play.api.test.Helpers.*
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{businessesAndPropertyIncome, noIncomeDetails}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

import scala.concurrent.Future

class RedirectIfNoIncomeSourcesActionSpec extends AuthActionsSpecHelper {

  private val action = new RedirectIfNoIncomeSourcesAction()

  def defaultAsyncBody(
                        requestTestCase: MtdItUser[_] => Assertion
                      ): MtdItUser[_] => Future[Result] = testRequest => {
    requestTestCase(testRequest)
    Future.successful(Results.Ok("Successful"))
  }

  def defaultAsync: MtdItUser[_] => Future[Result] = _ => Future.successful(Results.Ok("Successful"))

  "refine" when {

    "the user has income sources" should {

      "allow an individual user through" in {
        val request = defaultMTDITUser(
          af = Some(Individual),
          incomeSources = businessesAndPropertyIncome,
          request = fakeRequestWithActiveSession
        )

        val result = action.invokeBlock(
          request,
          defaultAsyncBody(_.incomeSources shouldBe businessesAndPropertyIncome)
        )

        status(result) shouldBe OK
      }

      "allow an agent user through" in {
        val request = defaultMTDITUser(
          af = Some(Agent),
          incomeSources = businessesAndPropertyIncome,
          request = fakeRequestWithActiveSession
        )

        val result = action.invokeBlock(
          request,
          defaultAsyncBody(_.incomeSources shouldBe businessesAndPropertyIncome)
        )

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

        val result = action.invokeBlock(
          request,
          defaultAsync
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          controllers.routes.NoIncomeSourcesController.show(isAgent = false).url
        )
      }

      "redirect an agent user to the no income sources page" in {
        val request = defaultMTDITUser(
          af = Some(Agent),
          incomeSources = noIncomeDetails,
          request = fakeRequestWithActiveSession
        )

        val result = action.invokeBlock(
          request,
          defaultAsync
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          controllers.routes.NoIncomeSourcesController.show(isAgent = true).url
        )
      }
    }
  }
}