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
import authV2.AuthActionsTestData.*
import config.featureswitch.FeatureSwitching
import config.{FrontendAppConfig, ItvcHeaderCarrierForPartialsConverter}
import controllers.agent.sessionUtils
import enums.{MTDIndividual, MTDPrimaryAgent, MTDUserRole}
import implicits.ImplicitDateFormatterImpl
import models.admin.FeatureSwitchName
import models.admin.FeatureSwitchName.allFeatureSwitches
import models.financialDetails.ChargeItem
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear, TaxYearRange}
import org.apache.pekko.actor.ActorSystem
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Mockito.mock
import org.scalactic.Equality
import org.scalatest.*
import org.scalatestplus.play.guice.*
import play.api.http.HeaderNames
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json._
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Injecting}
import play.api.{Configuration, Environment}
import play.twirl.api.Html
import services.DateService
import testConstants.BaseTestConstants.*
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.*
import testOnly.repository.FeatureSwitchRepository
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, SessionKeys}
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.play.partials.HeaderCarrierForPartials

import java.time.LocalDate
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}

trait TestSupport extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach with Injecting with FeatureSwitching {

  implicit val actorSystem: ActorSystem = app.actorSystem

  implicit val htmlEq: Equality[Html] =
    new Equality[Html] {
      def areEqual(a: Html, b: Any): Boolean = {
        Jsoup.parse(a.toString()).text() == Jsoup.parse(b.toString).text()
      }
    }

  def normalise(js: JsValue): JsValue = js match {

    case JsObject(fields) =>
      JsObject(
        fields.collect { case (k, v) if v != JsNull => k -> normalise(v) }
          .toSeq.sortBy(_._1)
      )

    case JsArray(values) =>
      val normValues = values.collect { case v if v != JsNull => normalise(v) }
      if (normValues.forall(_.isInstanceOf[JsObject])) {
        JsArray(normValues.sortBy(_.toString))
      } else {
        JsArray(normValues)
      }
    case JsNumber(n) =>
      JsNumber(n.bigDecimal.stripTrailingZeros())

    case other => other
  }

  def assertJsonEquals(actual: JsValue, expected: JsValue): Assertion =
    normalise(actual) shouldEqual normalise(expected)
  val featureSwitchRepository = app.injector.instanceOf[FeatureSwitchRepository]

  implicit val timeout: PatienceConfig = PatienceConfig(5.seconds)

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]

  implicit val mockImplicitDateFormatter: ImplicitDateFormatterImpl = new ImplicitDateFormatterImpl(languageUtils)

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  lazy val messagesApi: MessagesApi = inject[MessagesApi]

  implicit lazy val mockItvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter = mock(classOf[ItvcHeaderCarrierForPartialsConverter])

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session-123456")), deviceID = Some("some device Id")).withExtraHeaders(HeaderNames.REFERER -> testReferrerUrl)
  implicit val hcwc: HeaderCarrierForPartials = HeaderCarrierForPartials(headerCarrier)

  implicit val conf: Configuration = app.configuration
  implicit val environment: Environment = app.injector.instanceOf[Environment]
  implicit val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  // Set fixed date for DateService
  lazy val fixedDate: LocalDate = LocalDate.of(2023, 12, 15)
  lazy val fixedTaxYear: TaxYear = TaxYear(2023, 2024)
  lazy val fixedTaxYearRange: TaxYearRange = TaxYearRange(fixedTaxYear, fixedTaxYear)

  def multiYearRange: TaxYearRange = TaxYearRange(fixedTaxYear.previousYear, fixedTaxYear)

  implicit val dateService: DateService = new DateService {

    override def getCurrentDate: LocalDate = fixedDate

    override def getCurrentTaxYearEnd: Int = fixedDate.getYear + 1

    override def getCurrentTaxYearStart: LocalDate = LocalDate.of(2023, 4, 6)

    override def isBeforeLastDayOfTaxYear: Boolean = false

    override def getAccountingPeriodEndDate(startDate: LocalDate): LocalDate = LocalDate.of(2024, 4, 5)

    override def isWithin30Days(date: LocalDate): Boolean = {
      date.minusDays(30).isBefore(fixedDate)
    }
  }

  lazy val tsTestUser: MtdItUser[_] =
    defaultMTDITUser(Some(testUserTypeIndividual), IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty))

  lazy val tsTestUserAgent: MtdItUser[_] =
    defaultMTDITUser(Some(testUserTypeAgent), IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty))

  implicit val individualUser: MtdItUser[_] = getIndividualUser(FakeRequest())

  def commonAuditDetails(af:AffinityGroup, isSupportingAgent: Boolean = false): JsObject = {
    val commonDetails = Json.obj(
      "mtditid" -> testMtditid,
      "nino" -> testNino,
      "saUtr" -> testSaUtr,
      "credId" -> testCredId,
      "userType" -> af
    )
    if(af == Agent) commonDetails ++ Json.obj(
      "isSupportingAgent" -> isSupportingAgent,
      "agentReferenceNumber" -> testArn,
    )
     else commonDetails
  }

  def getIndividualUser(request: FakeRequest[AnyContentAsEmpty.type]): MtdItUser[_] = {
    defaultMTDITUser(Some(testUserTypeIndividual), businessAndPropertyAligned, request)
  }

  def getAgentUser(request: FakeRequest[AnyContentAsEmpty.type]): MtdItUser[_] =
    defaultMTDITUser(Some(testUserTypeAgent), businessAndPropertyAligned, request)

  def getIndividualUserIncomeSourcesConfigurable(request: FakeRequest[AnyContentAsEmpty.type], incomeSources: IncomeSourceDetailsModel)
  : MtdItUser[_] =     defaultMTDITUser(Some(testUserTypeIndividual), incomeSources, request)

  def getIndividualUserWithTwoActiveForeignProperties(request: FakeRequest[AnyContentAsEmpty.type]): MtdItUser[_] =
    defaultMTDITUser(Some(testUserTypeIndividual), twoActiveForeignPropertyIncomes, request)

  implicit val serviceInfo: Html = Html("")

  implicit class JsoupParse(x: Future[Result]) {
    def toHtmlDocument: Document = Jsoup.parse(contentAsString(x))
  }

  implicit class Ops[A](a: A) {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }

  type AuthRetrievals =
    Enrolments ~ Option[Name] ~ Option[Credentials] ~ Option[AffinityGroup]  ~ ConfidenceLevel

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
    sessionUtils.SessionKeys.clientMTDID -> testMtditid,
    sessionUtils.SessionKeys.clientNino -> testNino,
    sessionUtils.SessionKeys.isSupportingAgent -> "false"
  )

  def fakeRequestUnconfirmedClient(clientNino: String = testNino, isSupportingAgent: Boolean = false): FakeRequest[AnyContentAsEmpty.type] =
    fakeRequestWithActiveSession.withSession(
      sessionUtils.SessionKeys.clientFirstName -> "Test",
      sessionUtils.SessionKeys.clientLastName -> "User",
      sessionUtils.SessionKeys.clientUTR -> "1234567890",
      sessionUtils.SessionKeys.clientMTDID -> testMtditid,
      sessionUtils.SessionKeys.clientNino -> clientNino,
      sessionUtils.SessionKeys.isSupportingAgent -> isSupportingAgent.toString
    )

  def fakeRequestConfirmedClient(isSupportingAgent: Boolean = false): FakeRequest[AnyContentAsEmpty.type] =
    fakeRequestWithActiveSession.withSession(
      sessionUtils.SessionKeys.clientFirstName -> "Test",
      sessionUtils.SessionKeys.clientLastName -> "User",
      sessionUtils.SessionKeys.clientUTR -> "1234567890",
      sessionUtils.SessionKeys.clientMTDID -> testMtditid,
      sessionUtils.SessionKeys.clientNino -> testNino,
      sessionUtils.SessionKeys.confirmedClient -> "true",
      sessionUtils.SessionKeys.isSupportingAgent -> isSupportingAgent.toString
    )

  def fakePostRequestConfirmedClient(isSupportingAgent: Boolean = false): FakeRequest[AnyContentAsEmpty.type] =
    fakePostRequestWithActiveSession.withSession(
      sessionUtils.SessionKeys.clientFirstName -> "Test",
      sessionUtils.SessionKeys.clientLastName -> "User",
      sessionUtils.SessionKeys.clientUTR -> "1234567890",
      sessionUtils.SessionKeys.clientMTDID -> testMtditid,
      sessionUtils.SessionKeys.clientNino -> testNino,
      sessionUtils.SessionKeys.confirmedClient -> "true",
      sessionUtils.SessionKeys.isSupportingAgent -> isSupportingAgent.toString
    )

  def agentUserConfirmedClient(isSupportingAgent: Boolean = false): MtdItUser[_] = defaultMTDITUser(
    Some(testUserTypeAgent), businessesAndPropertyIncome, isSupportingAgent = isSupportingAgent)

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

  def disableAllSwitches(): Unit =
    if (appConfig.readFeatureSwitchesFromMongo)
      Await.result(featureSwitchRepository.setFeatureSwitches(allFeatureSwitches.map(_ -> false).toMap), 5.seconds)
    else
      allFeatureSwitches.foreach(switch => disable(switch))

  override def enable(featureSwitch: FeatureSwitchName): Unit =
    if (appConfig.readFeatureSwitchesFromMongo)
      Await.result(featureSwitchRepository.setFeatureSwitch(featureSwitch, true), 5.seconds)
    else
      sys.props += featureSwitch.name -> FEATURE_SWITCH_ON

  override def enable(featureSwitchNames: FeatureSwitchName*): Unit = {
    featureSwitchNames.foreach{ featureSwitch =>
      if (appConfig.readFeatureSwitchesFromMongo)
        Await.result(featureSwitchRepository.setFeatureSwitch(featureSwitch, true), 5.seconds)
      else
        sys.props += featureSwitch.name -> FEATURE_SWITCH_ON
    }
  }

  override def disable(featureSwitch: FeatureSwitchName): Unit =
    if (appConfig.readFeatureSwitchesFromMongo)
      Await.result(featureSwitchRepository.setFeatureSwitch(featureSwitch, false), 5.seconds)
    else
      sys.props += featureSwitch.name -> FEATURE_SWITCH_OFF

  def mainChargeIsNotPaidFilter: PartialFunction[ChargeItem, ChargeItem]  = {
    case x if x.remainingToPayByChargeOrInterest > 0 => x
  }
}
