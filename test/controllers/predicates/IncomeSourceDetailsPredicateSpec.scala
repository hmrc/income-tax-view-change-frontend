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

package controllers.predicates

import auth.{MtdItUser, MtdItUserOptionNino}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import mocks.services.{MockAsyncCacheApi, MockIncomeSourceDetailsService}
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import scala.concurrent.Future


class IncomeSourceDetailsPredicateSpec extends TestSupport with MockIncomeSourceDetailsService with MockAsyncCacheApi {

  object IncomeSourceDetailsPredicate extends IncomeSourceDetailsPredicate(mockIncomeSourceDetailsService)(
    ec,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    app.injector.instanceOf[MessagesControllerComponents]
  )

  lazy val userOptionNino: MtdItUserOptionNino[Any] = MtdItUserOptionNino(testMtditid, Some(testNino), Some(testRetrievedUserName),
    None, Some("testUtr"), Some("testCredId"), Some(Individual), None)
  lazy val successResponse: MtdItUser[Any] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), singleBusinessIncome,
    None, Some("testUtr"), Some("testCredId"), Some(Individual), None)

  "The IncomeSourceDetailsPredicate" when {

    "A valid response is received from the Income Source Details Service" should {

      "return the expected MtdItUser" in {
        mockSingleBusinessIncomeSource()
        val result = IncomeSourceDetailsPredicate.refine(userOptionNino)
        result.futureValue.toOption.get shouldBe successResponse
      }

    }

    "An invalid response is received from the Income Source Details Service" should {

      "Return Status of 500 (ISE)" in {
        mockErrorIncomeSource()
        val result = IncomeSourceDetailsPredicate.refine(userOptionNino)
        status(Future.successful(result.futureValue.swap.toOption.value)) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

  }

}
