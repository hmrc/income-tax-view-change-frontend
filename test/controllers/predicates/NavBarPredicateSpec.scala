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

import auth.MtdItUser
import config.ItvcErrorHandler
import config.featureswitch.{FeatureSwitching, NavBarFs}
import controllers.bta.BtaNavBarController
import mocks.services.MockAsyncCacheApi
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.Results.InternalServerError
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.Html
import testConstants.BaseTestConstants.{testListLink, testMtditid, testNino, testRetrievedUserName}
import testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncome
import testUtils.TestSupport
import views.html.navBar.{BtaNavBar, PtaPartial}

import scala.concurrent.Future

class NavBarPredicateSpec extends TestSupport with MockAsyncCacheApi with FeatureSwitching {

  val mockBtaNavBarController = mock[BtaNavBarController]
  val mockItvcErrorHandler = mock[ItvcErrorHandler]
  val mockPtaPartial = mock[PtaPartial]

  object NavBarPredicate extends NavBarPredicate(mockBtaNavBarController, mockPtaPartial, mockItvcErrorHandler)(appConfig, ec, messagesApi)

  val testView: BtaNavBar = app.injector.instanceOf[BtaNavBar]

  lazy val successResponseWithoutOrigin: MtdItUser[Any] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), singleBusinessIncome,
    None, Some("testUtr"), Some("testCredId"), Some("Individual"), None)

  lazy val successResponseWithBtaOrigin: MtdItUser[Any] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), singleBusinessIncome,
    Some(testView.apply(testListLink)), Some("testUtr"), Some("testCredId"), Some("Individual"), None)(fakeRequestWithNinoAndOrigin("BTA"))

  lazy val successResponseWithPtaOrigin: MtdItUser[Any] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), singleBusinessIncome,
    Some(Html("")), Some("testUtr"), Some("testCredId"), Some("Individual"), None)(fakeRequestWithNinoAndOrigin("PTA"))

  lazy val noAddedPartial: MtdItUser[Any] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), singleBusinessIncome,
    None, Some("testUtr"), Some("testCredId"), Some("Individual"), None)

  "The NavBarPredicate" when {

    "The Nav Bar is enabled" should {
      "Return a valid response from the Bta Nav Bar Controller which" should {
        "return the expected MtdItUser with a batPartial" in {
          enable(NavBarFs)
          when(mockBtaNavBarController.btaNavBarPartial(any())(any(), any())).thenReturn(Future.successful(Some(testView.apply(testListLink))))

          val result = NavBarPredicate.refine(successResponseWithBtaOrigin)
          result.futureValue.right.get shouldBe successResponseWithBtaOrigin
        }
        "Return a valid response when origin pta is present in session" should {
          "return the expected MtdItUserWithNino with a ptaPartial" in {
            enable(NavBarFs)
            when(mockPtaPartial.apply()(any(), any(), any())).thenReturn(Html(""))

            val result = NavBarPredicate.refine(successResponseWithPtaOrigin)
            result.futureValue.right.get shouldBe successResponseWithPtaOrigin
          }
        }
        "Return a redirect to TaxAccountRouter when origin is not present in session which" should {
          "return the expected Redirect with a tax account router" in {
            enable(NavBarFs)
            val result = NavBarPredicate.refine(successResponseWithoutOrigin)
            status(Future.successful(result.futureValue.left.get)) shouldBe Status.SEE_OTHER
            redirectLocation(Future.successful(result.futureValue.left.get)).get shouldBe "http://localhost:9280/account"
          }
        }

        "return an invalid response from the Bta Nav Bar Controller which" should {
          "Return Status of 500 (ISE)" in {
            enable(NavBarFs)
            when(mockBtaNavBarController.btaNavBarPartial(any())(any(), any())).thenReturn(Future.successful(None))
            when(mockItvcErrorHandler.showInternalServerError()(any())).thenReturn(InternalServerError(""))

            val result = NavBarPredicate.refine(successResponseWithBtaOrigin)
            status(Future.successful(result.futureValue.left.get)) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
      }
    }

    "The Bta Nav Bar is disabled" should {
      "Always return to origin call without nav bar partial content" in {
        disable(NavBarFs)

        val result = NavBarPredicate.refine(successResponseWithoutOrigin)
        result.futureValue.right.get shouldBe successResponseWithoutOrigin
      }
    }
  }
}
