/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.predicates

import assets.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import assets.IncomeSourceDetailsTestConstants._
import auth.{MtdItUser, MtdItUserWithNino}
import config.ItvcErrorHandler
import mocks.services.MockIncomeSourceDetailsService
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import testUtils.TestSupport


class IncomeSourceDetailsPredicateSpec extends TestSupport with MockIncomeSourceDetailsService {

  object IncomeSourceDetailsPredicate extends IncomeSourceDetailsPredicate(mockIncomeSourceDetailsService,
    app.injector.instanceOf[ItvcErrorHandler])(
    ec, app.injector.instanceOf[MessagesControllerComponents]
  )

  lazy val userWithNino = MtdItUserWithNino(testMtditid, testNino, Some(testRetrievedUserName),
    Some("testUtr"), Some("testCredId"), Some("Individual"), None)
  lazy val successResponse = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), singleBusinessIncome,
    Some("testUtr"), Some("testCredId"), Some("Individual"), None)

  "The IncomeSourceDetailsPredicate" when {

    "A valid response is received from the Income Source Details Service" should {

      "return the expected MtdItUser" in {
        mockSingleBusinessIncomeSource()
        val result = IncomeSourceDetailsPredicate.refine(userWithNino)
        result.right.get shouldBe successResponse
      }

    }

    "An invalid response is received from the Income Source Details Service" should {

      "Return Status of 500 (ISE)" in {
        mockErrorIncomeSource()
        val result = IncomeSourceDetailsPredicate.refine(userWithNino)
        status(result.left.get) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

  }

}
