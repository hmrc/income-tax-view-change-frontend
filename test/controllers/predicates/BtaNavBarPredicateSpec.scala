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

import auth.{MtdItUser, MtdItUserWithNino}
import config.ItvcErrorHandler
import config.featureswitch.{BtaNavBar, FeatureSwitching}
import controllers.bta.BtaNavBarController
import mocks.services.{MockAsyncCacheApi, MockBtaNavBarService, MockIncomeSourceDetailsService}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Results.InternalServerError
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import play.twirl.api.Html
import testConstants.BaseTestConstants.{testListLink, testMtditid, testNino, testRetrievedUserName}
import testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncome
import testUtils.TestSupport
import views.html.bta.BtaNavBar

import scala.concurrent.Future

class BtaNavBarPredicateSpec extends TestSupport with MockAsyncCacheApi with FeatureSwitching {

  val mockBtaNavBarController = mock[BtaNavBarController]
  val mockItvcErrorHandler = mock[ItvcErrorHandler]


  object BtaNavBarPredicate extends BtaNavBarPredicate(mockBtaNavBarController, mockItvcErrorHandler)(appConfig, ec)

  val testView: BtaNavBar = app.injector.instanceOf[BtaNavBar]
  lazy val userWithNino: MtdItUserWithNino[Any] = MtdItUserWithNino(testMtditid, testNino, Some(testRetrievedUserName),
    None, Some("testUtr"), Some("testCredId"), Some("Individual"), None)
  lazy val successResponse: MtdItUser[Any] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), singleBusinessIncome,
    Some(testView.apply(testListLink)), Some("testUtr"), Some("testCredId"), Some("Individual"), None)

  "The BtaNavBarPredicate" when {

    "A valid response is received from the Bta Nav Bar Service" should {

      "return the expected MtdItUser" in {
        enable(BtaNavBar)
        when(mockBtaNavBarController.btaNavBarPartial(any())(any(), any())).thenReturn(Future.successful(Some(testView.apply(testListLink))))
        val result = BtaNavBarPredicate.refine(successResponse)
        result.futureValue.right.get shouldBe successResponse
      }

    }

    "An invalid response is received from the Income Source Details Service" should {

      "Return Status of 500 (ISE)" in {
        enable(BtaNavBar)
        when(mockBtaNavBarController.btaNavBarPartial(any())(any(), any())).thenReturn(Future.successful(None))
        when(mockItvcErrorHandler.showInternalServerError()(any())).thenReturn(InternalServerError(""))

        val result = BtaNavBarPredicate.refine(successResponse)
        status(Future.successful(result.futureValue.left.get)) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

  }
}
