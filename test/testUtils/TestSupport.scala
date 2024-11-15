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
import config.featureswitch.FeatureSwitching
import config.{FrontendAppConfig, ItvcHeaderCarrierForPartialsConverter}
import controllers.agent.sessionUtils
import enums.{MTDIndividual, MTDPrimaryAgent, MTDUserRole}
import implicits.ImplicitDateFormatterImpl
import models.admin.FeatureSwitchName.allFeatureSwitches
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.apache.pekko.actor.ActorSystem
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.mock
import org.scalactic.Equality
import org.scalatest._
import org.scalatestplus.play.guice._
import play.api.http.HeaderNames
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import play.api.{Configuration, Environment}
import services.DateService
import testConstants.BaseTestConstants._
import testConstants.incomeSources.IncomeSourceDetailsTestConstants._
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, SessionKeys}
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.play.partials.HeaderCarrierForPartials

import java.time.LocalDate
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import play.twirl.api.Html

trait TestSupport extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach with Injecting with FeatureSwitching {
  this: Suite =>

  implicit val actorSystem: ActorSystem = app.actorSystem

  implicit val htmlEq: Equality[Html] =
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

  // Set fixed date for DateService
  lazy val fixedDate: LocalDate = LocalDate.of(2023, 12, 15)
  
  implicit lazy val dateService: DateService = new DateService {

    override def getCurrentDate: LocalDate = fixedDate

    override def getCurrentTaxYearEnd: Int = fixedDate.getYear + 1

    override def getCurrentTaxYearStart: LocalDate = LocalDate.of(2023, 4, 6)

    override def isBeforeLastDayOfTaxYear: Boolean = false

    override def getAccountingPeriodEndDate(startDate: LocalDate): LocalDate = LocalDate.of(2024, 4, 5)
  }

  val tsTestUser: MtdItUser[AnyContentAsEmpty.type] =
    MtdItUser(
      mtditid = testMtditid, nino = testNino, userName = None, incomeSources = IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty), btaNavPartial = None,
      saUtr = Some("1234567890"), credId = Some("12345-credId"), userType = Some(Individual), arn = None
    )(FakeRequest())

  val tsTestUserAgent: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty), None,
    Some("1234567890"), Some("12345-credId"), Some(Agent), None
  )(FakeRequest())

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

  def getAgentUser(request: FakeRequest[AnyContentAsEmpty.type]): MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = businessAndPropertyAligned,
    btaNavPartial = None,
    saUtr = Some(testSaUtr),
    credId = Some(testCredId),
    userType = Some(testUserTypeAgent),
    arn = Some(testArn)
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

  def fakeGetRequestBasedOnMTDUserType(mtdUserRole: MTDUserRole): FakeRequest[AnyContentAsEmpty.type] = {
    mtdUserRole match {
      case MTDIndividual => fakeRequestWithActiveSession
      case MTDPrimaryAgent => fakeRequestConfirmedClient()
      case _ => fakeRequestConfirmedClient(isSupportingAgent = true)
    }
  }

  def fakePostRequestBasedOnMTDUserType(mtdUserRole: MTDUserRole): FakeRequest[AnyContentAsEmpty.type] = {
    mtdUserRole match {
      case MTDIndividual => fakePostRequestWithActiveSession
      case MTDPrimaryAgent => fakePostRequestConfirmedClient()
      case _ => fakePostRequestConfirmedClient(isSupportingAgent = true)
    }
  }

  lazy val fakeRequestWithActiveSession: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(
      SessionKeys.lastRequestTimestamp -> "1498236506662",
      SessionKeys.authToken -> "Bearer Token"
    ).withHeaders(
      HeaderNames.REFERER -> "/test/url",
      "X-Session-ID" -> testSessionId
    )

  lazy val fakePostRequestWithActiveSession: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withMethod("POST").withSession(
      SessionKeys.lastRequestTimestamp -> "1498236506662",
      SessionKeys.authToken -> "Bearer Token"
    ).withHeaders(
      HeaderNames.REFERER -> "/test/url",
      "X-Session-ID" -> testSessionId
    )

  lazy val fakeRequestWithActiveSessionWithBusinessName: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(
      SessionKeys.lastRequestTimestamp -> "1498236506662",
      SessionKeys.authToken -> "Bearer Token"
    ).withHeaders(
      HeaderNames.REFERER -> "/test/url",
      "X-Session-ID" -> testSessionId
    )

  def fakeRequestWithActiveSessionWithReferer(referer: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(
      SessionKeys.lastRequestTimestamp -> "1498236506662",
      SessionKeys.authToken -> "Bearer Token"
    ).withHeaders(
      HeaderNames.REFERER -> referer
    )

  lazy val fakeRequestWithTimeoutSession: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(
      SessionKeys.lastRequestTimestamp -> "1498236506662"
    )

  lazy val fakeRequestWithClientUTR: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(
      sessionUtils.SessionKeys.clientUTR -> "1234567890"
    )

  lazy val fakeRequestWithActiveAndRefererToHomePage: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(
      SessionKeys.lastRequestTimestamp -> "1498236506662",
      SessionKeys.authToken -> "Bearer Token"
    ).withHeaders(
      HeaderNames.REFERER -> "http://www.somedomain.org/report-quarterly/income-and-expenses/view"
    )

  lazy val fakeRequestWithClientDetails: FakeRequest[AnyContentAsEmpty.type] = fakeRequestWithActiveSession.withSession(
    sessionUtils.SessionKeys.clientFirstName -> "Test",
    sessionUtils.SessionKeys.clientLastName -> "User",
    sessionUtils.SessionKeys.clientUTR -> "1234567890",
    sessionUtils.SessionKeys.clientMTDID -> "XAIT00000000015",
    sessionUtils.SessionKeys.clientNino -> testNino,
    sessionUtils.SessionKeys.isSupportingAgent -> "false"
  )

  def fakeRequestUnconfirmedClient(clientNino: String = "AA111111A", isSupportingAgent: Boolean = false): FakeRequest[AnyContentAsEmpty.type] =
    fakeRequestWithActiveSession.withSession(
      sessionUtils.SessionKeys.clientFirstName -> "Test",
      sessionUtils.SessionKeys.clientLastName -> "User",
      sessionUtils.SessionKeys.clientUTR -> "1234567890",
      sessionUtils.SessionKeys.clientMTDID -> "XAIT00000000015",
      sessionUtils.SessionKeys.clientNino -> clientNino,
      sessionUtils.SessionKeys.isSupportingAgent -> isSupportingAgent.toString
    )

  def fakeRequestConfirmedClient(clientNino: String = "AA111111A", isSupportingAgent: Boolean = false): FakeRequest[AnyContentAsEmpty.type] =
    fakeRequestWithActiveSession.withSession(
      sessionUtils.SessionKeys.clientFirstName -> "Test",
      sessionUtils.SessionKeys.clientLastName -> "User",
      sessionUtils.SessionKeys.clientUTR -> "1234567890",
      sessionUtils.SessionKeys.clientMTDID -> "XAIT00000000015",
      sessionUtils.SessionKeys.clientNino -> clientNino,
      sessionUtils.SessionKeys.confirmedClient -> "true",
      sessionUtils.SessionKeys.isSupportingAgent -> isSupportingAgent.toString
    )

  def fakeRequestConfirmedClientTimeout(clientNino: String = "AA111111A"): FakeRequest[AnyContentAsEmpty.type] =
    fakeRequestWithTimeoutSession.withSession(
      sessionUtils.SessionKeys.clientFirstName -> "Test",
      sessionUtils.SessionKeys.clientLastName -> "User",
      sessionUtils.SessionKeys.clientUTR -> "1234567890",
      sessionUtils.SessionKeys.clientMTDID -> "XAIT00000000015",
      sessionUtils.SessionKeys.clientNino -> clientNino,
      sessionUtils.SessionKeys.confirmedClient -> "true",
      sessionUtils.SessionKeys.isSupportingAgent -> "false"
    )

  def fakePostRequestConfirmedClient(clientNino: String = "AA111111A", isSupportingAgent: Boolean = false): FakeRequest[AnyContentAsEmpty.type] =
    fakePostRequestWithActiveSession.withSession(
      sessionUtils.SessionKeys.clientFirstName -> "Test",
      sessionUtils.SessionKeys.clientLastName -> "User",
      sessionUtils.SessionKeys.clientUTR -> "1234567890",
      sessionUtils.SessionKeys.clientMTDID -> "XAIT00000000015",
      sessionUtils.SessionKeys.clientNino -> clientNino,
      sessionUtils.SessionKeys.confirmedClient -> "true",
      sessionUtils.SessionKeys.isSupportingAgent -> isSupportingAgent.toString
    )

  def fakeRequestConfirmedClientwithBusinessName(clientNino: String = "AA111111A"): FakeRequest[AnyContentAsEmpty.type] =
    fakeRequestWithActiveSession.withMethod("POST").withSession(
      sessionUtils.SessionKeys.clientFirstName -> "Test",
      sessionUtils.SessionKeys.clientLastName -> "User",
      sessionUtils.SessionKeys.clientUTR -> "1234567890",
      sessionUtils.SessionKeys.clientMTDID -> "XAIT00000000015",
      sessionUtils.SessionKeys.clientNino -> clientNino,
      sessionUtils.SessionKeys.confirmedClient -> "true"
    )

  def fakeRequestConfirmedClientwithFullBusinessDetails(clientNino: String = "AA111111A"): FakeRequest[AnyContentAsEmpty.type] =
    fakeRequestWithActiveSession.withSession(
      sessionUtils.SessionKeys.clientFirstName -> "Test",
      sessionUtils.SessionKeys.clientLastName -> "User",
      sessionUtils.SessionKeys.clientUTR -> "1234567890",
      sessionUtils.SessionKeys.clientMTDID -> testMtditid,
      sessionUtils.SessionKeys.clientNino -> clientNino,
      sessionUtils.SessionKeys.confirmedClient -> "true"
    )

  def fakeRequestConfirmedClientWithCalculationId(clientNino: String = "AA111111A"): FakeRequest[AnyContentAsEmpty.type] =
    fakeRequestWithActiveSession.withSession(
      sessionUtils.SessionKeys.clientFirstName -> "Test",
      sessionUtils.SessionKeys.clientLastName -> "User",
      sessionUtils.SessionKeys.clientUTR -> "1234567890",
      sessionUtils.SessionKeys.clientMTDID -> testMtditid,
      sessionUtils.SessionKeys.clientNino -> clientNino,
      sessionUtils.SessionKeys.confirmedClient -> "true",
      forms.utils.SessionKeys.calculationId -> "1234567890"
    )

  def fakeRequestConfirmedClientWithReferer(clientNino: String = "AA111111A", referer: String): FakeRequest[AnyContentAsEmpty.type] =
    fakeRequestWithActiveSession.withSession(
      sessionUtils.SessionKeys.clientFirstName -> "Test",
      sessionUtils.SessionKeys.clientLastName -> "User",
      sessionUtils.SessionKeys.clientUTR -> "1234567890",
      sessionUtils.SessionKeys.clientMTDID -> "XAIT00000000015",
      sessionUtils.SessionKeys.clientNino -> clientNino,
      sessionUtils.SessionKeys.confirmedClient -> "true"
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
    allFeatureSwitches.foreach(switch => disable(switch))
  }

}
