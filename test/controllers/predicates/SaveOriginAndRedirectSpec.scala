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

import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import mocks.services.MockAsyncCacheApi
import org.mockito.Mockito.mock
import play.api.http.HeaderNames
import play.api.http.Status.SEE_OTHER
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import play.twirl.api.Html
import testConstants.BaseTestConstants.{testListLink, testMtditid, testNino, testRetrievedUserName}
import testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncome
import testUtils.TestSupport
import uk.gov.hmrc.http.SessionKeys
import views.html.navBar.BtaNavBar

import scala.concurrent.Future

class SaveOriginAndRedirectSpec extends TestSupport with MockAsyncCacheApi with FeatureSwitching {

  lazy val successResponseWithoutOrigin: MtdItUser[Any] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), singleBusinessIncome,
    None, Some("testUtr"), Some("testCredId"), Some("Individual"), None)

  lazy val successResponseWithBtaOriginAndWithoutSession: MtdItUser[Any] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), singleBusinessIncome,
    Some(testView.apply(testListLink)), Some("testUtr"), Some("testCredId"), Some("Individual"), None)(fakeRequestQueryStringAndOriginWithoutSession("BTA"))

  lazy val successResponseWithSessionOriginPTA: MtdItUser[Any] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), singleBusinessIncome,
    Some(Html("")), Some("testUtr"), Some("testCredId"), Some("Individual"), None)(fakeRequestQueryStringAndOrigin("BTA", "PTA"))
  val testView: BtaNavBar = app.injector.instanceOf[BtaNavBar]

  lazy val successResponseWithInvalidQueryString: MtdItUser[Any] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), singleBusinessIncome,
      None, Some("testUtr"), Some("testCredId"), Some("Individual"), None)(fakeRequestQueryStringAndOriginWithoutSession("INVALID"))

  val obj: SaveOriginAndRedirect = new SaveOriginAndRedirect() {
    override def messagesApi: MessagesApi = mock(classOf[MessagesApi])

    override val appConfig: FrontendAppConfig = mock(classOf[FrontendAppConfig])
  }

  def fakeRequestQueryStringAndOrigin(origin: String, session: String): FakeRequest[AnyContentAsEmpty.type] = fakeRequestWithQueryString(origin).withSession(
    "origin" -> session)

  def fakeRequestQueryStringAndOriginWithoutSession(origin: String): FakeRequest[AnyContentAsEmpty.type] = fakeRequestWithQueryString(origin)

  def fakeRequestWithQueryString(origin: String): FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, s"test/url?origin=$origin")

  "The SaveOriginAndRedirect" when {

    "Valid request is passed" should {
      "redirect to call with session origin changes to BTA" in {
        val result: Future[Result] = obj.saveOriginAndReturnToHomeWithoutQueryParams(successResponseWithSessionOriginPTA, false)
        result.futureValue.header.status shouldBe SEE_OTHER
        result.futureValue.session.get("origin") shouldBe Some("BTA")
      }
      "redirect to call with adding origin BTA to session" in {
        val result: Future[Result] = obj.saveOriginAndReturnToHomeWithoutQueryParams(successResponseWithBtaOriginAndWithoutSession, false)
        result.futureValue.header.status shouldBe SEE_OTHER
        result.futureValue.session.get("origin") shouldBe Some("BTA")
      }
      "return to original call when invalid queryString is passed" in {
        val result: Future[Result] = obj.saveOriginAndReturnToHomeWithoutQueryParams(successResponseWithInvalidQueryString, false)
        result.futureValue.header.status shouldBe SEE_OTHER
        result.futureValue.session.get("origin") shouldBe None
      }
      "return to original call when query string is not passed" in {
        val result: Future[Result] = obj.saveOriginAndReturnToHomeWithoutQueryParams(successResponseWithoutOrigin, false)
        result.futureValue.header.status shouldBe SEE_OTHER
        result.futureValue.session.get("origin") shouldBe None
      }
      "return to original call when navBarFs is disabled" in {
        val result: Future[Result] = obj.saveOriginAndReturnToHomeWithoutQueryParams(successResponseWithoutOrigin)
        result.futureValue.header.status shouldBe SEE_OTHER
        result.futureValue.session.get("origin") shouldBe None
      }
    }
  }

}
