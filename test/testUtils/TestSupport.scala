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

package testUtils

import auth.MtdItUser
import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.FeatureSwitching
import config.{FrontendAppConfig, ItvcHeaderCarrierForPartialsConverter}
import controllers.agent.utils
import implicits.ImplicitDateFormatterImpl
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.mock
import org.scalactic.Equality
import org.scalatest._
import play.api.http.HeaderNames
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import play.api.{Configuration, Environment}
import services.DateService
import testConstants.BaseTestConstants._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants._
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, SessionKeys}
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.play.partials.HeaderCarrierForPartials
import org.scalatestplus.play.guice._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait TestSupport extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterEach with Injecting with FeatureSwitching {
  this: Suite =>

  import play.twirl.api.Html

  implicit val htmlEq =
    new Equality[Html] {
      def areEqual(a: Html, b: Any): Boolean = {
        Jsoup.parse(a.toString()).text() == Jsoup.parse(b.toString).text()
      }
    }

  implicit val timeout: PatienceConfig = PatienceConfig(5.seconds)

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]

  implicit val mockImplicitDateFormatter: ImplicitDateFormatterImpl = new ImplicitDateFormatterImpl(languageUtils)

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  def messagesApi: MessagesApi = inject[MessagesApi]

  implicit val mockItvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter = mock(classOf[ItvcHeaderCarrierForPartialsConverter])

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session-123456")), deviceID = Some("some device Id")).withExtraHeaders(HeaderNames.REFERER -> testReferrerUrl)
  implicit val hcwc: HeaderCarrierForPartials = HeaderCarrierForPartials(headerCarrier)

  implicit val conf: Configuration = app.configuration
  implicit val environment: Environment = app.injector.instanceOf[Environment]
  implicit val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  implicit val dateService: DateService = app.injector.instanceOf[DateService]

  implicit val individualUser: MtdItUser[_] = getIndividualUser(FakeRequest())

  def getIndividualUser(request: FakeRequest[AnyContentAsEmpty.type]): MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = businessAndPropertyAligned,
    btaNavPartial = None,
    saUtr = Some(testSaUtr),
    credId = Some(testCredId),
    userType = Some(testUserTypeIndividual),
    arn = None
  )(request)

  def getIndividualUserIncomeSourcesConfigurable(request: FakeRequest[AnyContentAsEmpty.type], incomeSources: IncomeSourceDetailsModel)
  : MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = incomeSources,
    btaNavPartial = None,
    saUtr = Some(testSaUtr),
    credId = Some(testCredId),
    userType = Some(testUserTypeIndividual),
    arn = None
  )(request)

  def getIndividualUserWithTwoActiveForeignProperties(request: FakeRequest[AnyContentAsEmpty.type]): MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = twoActiveForeignPropertyIncomes,
    btaNavPartial = None,
    saUtr = Some(testSaUtr),
    credId = Some(testCredId),
    userType = Some(testUserTypeIndividual),
    arn = None
  )(request)

  implicit val serviceInfo: Html = Html("")

  implicit class JsoupParse(x: Future[Result]) {
    def toHtmlDocument: Document = Jsoup.parse(contentAsString(x))
  }

  lazy val fakeRequestWithActiveSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    SessionKeys.lastRequestTimestamp -> "1498236506662",
    SessionKeys.authToken -> "Bearer Token"
  ).withHeaders(
    HeaderNames.REFERER -> "/test/url",
    "X-Session-ID" -> testSessionId
  )

  lazy val fakePostRequestWithActiveSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withMethod("POST").withSession(
    SessionKeys.lastRequestTimestamp -> "1498236506662",
    SessionKeys.authToken -> "Bearer Token"
  ).withHeaders(
    HeaderNames.REFERER -> "/test/url",
    "X-Session-ID" -> testSessionId
  )

  lazy val fakeRequestWithActiveSessionWithBusinessName: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    SessionKeys.lastRequestTimestamp -> "1498236506662",
    SessionKeys.authToken -> "Bearer Token"
  ).withHeaders(
    HeaderNames.REFERER -> "/test/url",
    "X-Session-ID" -> testSessionId
  )

  def fakeRequestWithActiveSessionWithReferer(referer: String): FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    SessionKeys.lastRequestTimestamp -> "1498236506662",
    SessionKeys.authToken -> "Bearer Token"
  ).withHeaders(
    HeaderNames.REFERER -> referer
  )

  lazy val fakeRequestWithTimeoutSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    SessionKeys.lastRequestTimestamp -> "1498236506662"
  )

  lazy val fakeRequestWithClientUTR: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    utils.SessionKeys.clientUTR -> "1234567890"
  )

  lazy val fakeRequestWithActiveAndRefererToHomePage: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    SessionKeys.lastRequestTimestamp -> "1498236506662",
    SessionKeys.authToken -> "Bearer Token"
  ).withHeaders(
    HeaderNames.REFERER -> "http://www.somedomain.org/report-quarterly/income-and-expenses/view"
  )

  lazy val fakeRequestWithClientDetails: FakeRequest[AnyContentAsEmpty.type] = fakeRequestWithActiveSession.withSession(
    utils.SessionKeys.clientFirstName -> "Test",
    utils.SessionKeys.clientLastName -> "User",
    utils.SessionKeys.clientUTR -> "1234567890",
    utils.SessionKeys.clientMTDID -> "XAIT00000000015"
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

  def fakePostRequestConfirmedClient(clientNino: String = "AA111111A"): FakeRequest[AnyContentAsEmpty.type] =
    fakePostRequestWithActiveSession.withSession(
      utils.SessionKeys.clientFirstName -> "Test",
      utils.SessionKeys.clientLastName -> "User",
      utils.SessionKeys.clientUTR -> "1234567890",
      utils.SessionKeys.clientMTDID -> "XAIT00000000015",
      utils.SessionKeys.clientNino -> clientNino,
      utils.SessionKeys.confirmedClient -> "true"
    )

  def fakeRequestConfirmedClientwithBusinessName(clientNino: String = "AA111111A"): FakeRequest[AnyContentAsEmpty.type] =
    fakeRequestWithActiveSession.withMethod("POST").withSession(
      utils.SessionKeys.clientFirstName -> "Test",
      utils.SessionKeys.clientLastName -> "User",
      utils.SessionKeys.clientUTR -> "1234567890",
      utils.SessionKeys.clientMTDID -> "XAIT00000000015",
      utils.SessionKeys.clientNino -> clientNino,
      utils.SessionKeys.confirmedClient -> "true"
    )

  def fakeRequestConfirmedClientwithFullBusinessDetails(clientNino: String = "AA111111A"): FakeRequest[AnyContentAsEmpty.type] =
    fakeRequestWithActiveSession.withSession(
      utils.SessionKeys.clientFirstName -> "Test",
      utils.SessionKeys.clientLastName -> "User",
      utils.SessionKeys.clientUTR -> "1234567890",
      utils.SessionKeys.clientMTDID -> testMtditid,
      utils.SessionKeys.clientNino -> clientNino,
      utils.SessionKeys.confirmedClient -> "true"
    )

  def fakeRequestConfirmedClientWithCalculationId(clientNino: String = "AA111111A"): FakeRequest[AnyContentAsEmpty.type] =
    fakeRequestWithActiveSession.withSession(
      utils.SessionKeys.clientFirstName -> "Test",
      utils.SessionKeys.clientLastName -> "User",
      utils.SessionKeys.clientUTR -> "1234567890",
      utils.SessionKeys.clientMTDID -> testMtditid,
      utils.SessionKeys.clientNino -> clientNino,
      utils.SessionKeys.confirmedClient -> "true",
      forms.utils.SessionKeys.calculationId -> "1234567890"
    )

  def fakeRequestConfirmedClientWithReferer(clientNino: String = "AA111111A", referer: String): FakeRequest[AnyContentAsEmpty.type] =
    fakeRequestWithActiveSession.withSession(
      utils.SessionKeys.clientFirstName -> "Test",
      utils.SessionKeys.clientLastName -> "User",
      utils.SessionKeys.clientUTR -> "1234567890",
      utils.SessionKeys.clientMTDID -> "XAIT00000000015",
      utils.SessionKeys.clientNino -> clientNino,
      utils.SessionKeys.confirmedClient -> "true"
    ).withHeaders(
      HeaderNames.REFERER -> referer
    )

  def agentUserConfirmedClient(clientNino: String = "AA111111A"): MtdItUser[_] = MtdItUser(
    mtditid = "XAIT00000000015",
    nino = clientNino,
    userName = Some(Name(Some("Test"), Some("User"))),
    incomeSources = businessesAndPropertyIncome,
    btaNavPartial = None,
    saUtr = Some("1234567890"),
    credId = Some(testCredId),
    userType = Some(testUserTypeAgent),
    arn = Some(testArn)
  )(FakeRequest())

  lazy val fakeRequestWithNino: FakeRequest[AnyContentAsEmpty.type] = fakeRequestWithActiveSession.withSession("nino" -> testNino)

  def fakeRequestWithNinoAndOrigin(origin: String): FakeRequest[AnyContentAsEmpty.type] =
    fakeRequestWithActiveSession.withSession("nino" -> testNino, "origin" -> origin)

  def fakePostRequestWithNinoAndOrigin(origin: String): FakeRequest[AnyContentAsEmpty.type] =
    fakePostRequestWithActiveSession.withSession("nino" -> testNino, "origin" -> origin)

  lazy val fakeRequestWithNinoAndCalc: FakeRequest[AnyContentAsEmpty.type] = fakeRequestWithActiveSession.withSession(
    forms.utils.SessionKeys.calculationId -> "1234567890",
    "nino" -> testNino
  )

  lazy val fakeRequestNoSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  lazy val fakeRequestWithTestSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    "Gov-Test-Scenario" -> "data"
  )

  implicit class FakeRequestUtil[C](fakeRequest: FakeRequest[C]) {
    def addingToSession(newSessions: (String, String)*): FakeRequest[C] = {
      fakeRequest.withSession(fakeRequest.session.data ++: newSessions: _*)
    }
  }

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

}
