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

package services

import auth.MtdItUser
import controllers.agent.utils
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import testUtils.TestSupport

class SessionServiceSpec extends TestSupport {

  object TestSessionService extends SessionService()

  "sessionService " when {
    "get method " should {
      "return the correct session value for given key" in {
        val user = getIndividualUser(fakeRequestConfirmedClientwithBusinessName())
        TestSessionService.get(utils.SessionKeys.businessName)(user, ec).futureValue shouldBe Right(Some("Test Name"))
      }
    }
    "set method" should {
      "set the correct session key and value" in {
        val requestHeader = fakeRequestConfirmedClientwithBusinessName()
        val result: Result = TestSessionService.set("key", "somevalue", Redirect("someurl"))(ec, requestHeader)
          .futureValue.toOption.get
        result.session.get("key") shouldBe Some("somevalue")
      }
    }
  }
}
