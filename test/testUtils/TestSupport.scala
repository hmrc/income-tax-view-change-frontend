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

package testUtils

import assets.BaseTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import auth.MtdItUser
import config.{FrontendAppConfig, ItvcHeaderCarrierForPartialsConverter}
import controllers.agent.utils
import implicits.ImplicitDateFormatterImpl
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HeaderNames
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.{Configuration, Environment}
import play.twirl.api.Html
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.play.partials.HeaderCarrierForPartials
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

trait TestSupport extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach with MaterializerSupport {
  this: Suite =>

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]

  implicit val mockImplicitDateFormatter: ImplicitDateFormatterImpl = new ImplicitDateFormatterImpl(languageUtils)

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  implicit val mockItvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter = mock[ItvcHeaderCarrierForPartialsConverter]

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders(HeaderNames.REFERER -> testReferrerUrl)
  implicit val hcwc: HeaderCarrierForPartials = HeaderCarrierForPartials(headerCarrier, "")

  implicit val conf: Configuration = app.configuration
  implicit val environment: Environment = app.injector.instanceOf[Environment]
  implicit val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  implicit val user: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = businessAndPropertyAligned,
    saUtr = Some("saUtr"),
    credId = Some("credId"),
    userType = Some("Individual"),
    None
  )(FakeRequest())

  implicit val serviceInfo: Html = Html("")

  implicit class JsoupParse(x: Future[Result]) {
    def toHtmlDocument: Document = Jsoup.parse(bodyOf(x))
  }

  lazy val fakeRequestWithActiveSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    SessionKeys.lastRequestTimestamp -> "1498236506662",
    SessionKeys.authToken -> "Bearer Token"
  ).withHeaders(
    HeaderNames.REFERER -> "/test/url"
  )

  lazy val fakeRequestWithTimeoutSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    SessionKeys.lastRequestTimestamp -> "1498236506662"
  )

  lazy val fakeRequestWithClientUTR: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    utils.SessionKeys.clientUTR -> "1234567890"
  )

  lazy val fakeRequestWithClientDetails: FakeRequest[AnyContentAsEmpty.type] = fakeRequestWithActiveSession.withSession(
    utils.SessionKeys.clientFirstName -> "Test",
    utils.SessionKeys.clientLastName -> "User",
    utils.SessionKeys.clientUTR -> "1234567890"
  )

  def fakeRequestConfirmedClient(clientNino: String = "AA111111A"): FakeRequest[AnyContentAsEmpty.type] =
    fakeRequestWithActiveSession.withSession(
      utils.SessionKeys.clientFirstName -> "Test",
      utils.SessionKeys.clientLastName -> "User",
      utils.SessionKeys.clientUTR -> "1234567890",
      utils.SessionKeys.clientMTDID -> "XAIT00000000015",
      utils.SessionKeys.clientNino -> clientNino,
      utils.SessionKeys.confirmedClient -> "true"
    )

  lazy val fakeRequestWithNino: FakeRequest[AnyContentAsEmpty.type] = fakeRequestWithActiveSession.withSession("nino" -> testNino)

  lazy val fakeRequestWithNinoAndCalc: FakeRequest[AnyContentAsEmpty.type] = fakeRequestWithActiveSession.withSession(
    forms.utils.SessionKeys.calculationId -> "1234567890",
    "nino" -> testNino
  )

  lazy val fakeRequestNoSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  implicit class FakeRequestUtil[C](fakeRequest: FakeRequest[C]) {
    def addingToSession(newSessions: (String, String)*): FakeRequest[C] = {
      fakeRequest.withSession(fakeRequest.session.data ++: newSessions: _*)
    }
  }

}
