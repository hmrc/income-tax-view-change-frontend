/*
 * Copyright 2017 HM Revenue & Customs
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

import assets.TestConstants._
import auth.{MtdItUser, MtdItUserWithNino}
import config.ItvcErrorHandler
import mocks.services.MockIncomeSourceDetailsService
import play.api.http.Status
import play.api.i18n.MessagesApi
import utils.TestSupport


class IncomeSourceDetailsPredicateSpec extends TestSupport with MockIncomeSourceDetailsService {

  object IncomeSourceDetailsPredicate extends IncomeSourceDetailsPredicate()(
    app.injector.instanceOf[MessagesApi],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[ItvcErrorHandler]
  )

  lazy val userWithNino = MtdItUserWithNino(testMtditid, testNino, Some(testUserDetails))
  lazy val successResponse = MtdItUser(testMtditid, testNino, Some(testUserDetails), IncomeSourceDetails.businessIncomeSourceSuccess)

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
