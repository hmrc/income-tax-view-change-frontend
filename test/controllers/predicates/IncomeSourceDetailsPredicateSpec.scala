/*
 * Copyright 2022 HM Revenue & Customs
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

import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.IncomeSourceDetailsTestConstants._
import auth.{MtdItUser, MtdItUserWithNino}
import config.ItvcErrorHandler
import mocks.services.{MockAsyncCacheApi, MockIncomeSourceDetailsService}
import play.api.http.Status
import play.api.test.Helpers._
import play.api.mvc.MessagesControllerComponents
import testUtils.TestSupport

import scala.concurrent.Future


class IncomeSourceDetailsPredicateSpec extends TestSupport with MockIncomeSourceDetailsService with MockAsyncCacheApi {

  object IncomeSourceDetailsPredicate extends IncomeSourceDetailsPredicate(mockIncomeSourceDetailsService,
    app.injector.instanceOf[ItvcErrorHandler])(
    ec, app.injector.instanceOf[MessagesControllerComponents]
  )

  lazy val userWithNino: MtdItUserWithNino[Any] = MtdItUserWithNino(testMtditid, testNino, Some(testRetrievedUserName),
    None, Some("testUtr"), Some("testCredId"), Some("Individual"), None)
  lazy val successResponse: MtdItUser[Any] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), singleBusinessIncome,
    None, Some("testUtr"), Some("testCredId"), Some("Individual"), None)

  "The IncomeSourceDetailsPredicate" when {

    "A valid response is received from the Income Source Details Service" should {

      "return the expected MtdItUser" in {
        mockSingleBusinessIncomeSource()
        val result = IncomeSourceDetailsPredicate.refine(userWithNino)
        result.futureValue.right.get shouldBe successResponse
      }

    }

    "An invalid response is received from the Income Source Details Service" should {

      "Return Status of 500 (ISE)" in {
        mockErrorIncomeSource()
        val result = IncomeSourceDetailsPredicate.refine(userWithNino)
        status(Future.successful(result.futureValue.left.get)) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

  }

}
