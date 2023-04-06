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

package helpers

import auth.HeaderExtractor

import java.time.LocalDate
import com.github.tomakehurst.wiremock.client.WireMock
import config.FrontendAppConfig
import config.featureswitch.{FeatureSwitch, FeatureSwitching}
import helpers.agent.SessionCookieBaker
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import implicits.ImplicitDateFormatterImpl
import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.cache.AsyncCacheApi
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.{Lang, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.ws.WSResponse
import play.api.{Application, Environment, Mode}
import services.{DateService, DateServiceInterface}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants._
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.language.LanguageUtils

import javax.inject.Singleton
import scala.concurrent.Future
import play.api.inject.bind

@Singleton
class TestHeaderExtractor extends HeaderExtractor {

  override def extractHeader(request: play.api.mvc.Request[_], session: play.api.mvc.Session): HeaderCarrier = {
    HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)
      .copy(authorization = Some(Authorization("Bearer")))
  }

}

@Singleton
class TestDateService  extends DateServiceInterface  {

  def getCurrentDate: LocalDate = LocalDate.of(2023, 4, 5)
  def isDayBeforeTaxYearLastDay: Boolean = false

  def getCurrentTaxYearEnd: Int = 2023
//    val currentDate = getCurrentDate
//    if (isDayBeforeTaxYearLastDay) currentDate.getYear else currentDate.getYear + 1
//  }
}

trait ComponentSpecBase extends TestSuite with CustomMatchers
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with Eventually
  with GenericStubMethods with FeatureSwitching with SessionCookieBaker {

  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: String = WiremockHelper.wiremockPort.toString
  val mockUrl: String = s"http://$mockHost:$mockPort"
  val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val cache: AsyncCacheApi = app.injector.instanceOf[AsyncCacheApi]
  val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]
  implicit val lang: Lang = Lang("GB")
  val messagesAPI: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockLanguageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]
  implicit val mockImplicitDateFormatter: ImplicitDateFormatterImpl = new ImplicitDateFormatterImpl(mockLanguageUtils)

  implicit val testAppConfig: FrontendAppConfig = appConfig
  implicit val dateService: DateService = new DateService(){
    override def getCurrentDate: LocalDate = LocalDate.of(2023, 4, 5)

    override def getCurrentTaxYearEnd: Int = 2023
  }

  override lazy val cookieSigner: DefaultCookieSigner = app.injector.instanceOf[DefaultCookieSigner]

  val titleInternalServer: String = "standardError.heading"
  val titleTechError = "Sorry, we are experiencing technical difficulties - 500"
  val titleNotFound = "Page not found - 404"
  val titleProbWithService = "There is a problem with the service"
  val titleThereIsAProblem = "There’s a problem"
  val titleClientRelationshipFailure: String = "agent.client_relationship_failure.heading"

  def config: Map[String, String] = Map(
    "microservice.services.auth.host" -> mockHost,
    "microservice.services.auth.port" -> mockPort,
    "microservice.services.income-tax-view-change.host" -> mockHost,
    "microservice.services.income-tax-view-change.port" -> mockPort,
    "microservice.services.self-assessment-api.host" -> mockHost,
    "microservice.services.self-assessment-api.port" -> mockPort,
    "microservice.services.business-account.host" -> mockHost,
    "microservice.services.business-account.port" -> mockPort,
    "microservice.services.financial-transactions.host" -> mockHost,
    "microservice.services.financial-transactions.port" -> mockPort,
    "microservice.services.pay-api.host" -> mockHost,
    "microservice.services.pay-api.port" -> mockPort,
    "microservice.services.income-tax-calculation.host" -> mockHost,
    "microservice.services.income-tax-calculation.port" -> mockPort,
    "calculation-polling.interval" -> "500",
    "calculation-polling.timeout" -> "3000",
    "auditing.consumer.baseUri.host" -> mockHost,
    "auditing.consumer.baseUri.port" -> mockPort,
    "auditing.enabled" -> "true"
  )

  val userDetailsUrl = "/user-details/id/5397272a3d00003d002f3ca9"
  val btaPartialUrl = "/business-account/partial/service-info"
  val testUserDetailsWiremockUrl: String = mockUrl + userDetailsUrl

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.of(2023, 4, 4)
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }


  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .overrides(bind[HeaderExtractor].to[TestHeaderExtractor]) // adding dumy Authorization header in order for it:tests to pass
    .overrides(bind[DateServiceInterface].to[TestDateService])
    .configure(config)
    .build

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    WireMock.reset()
    isAuthorisedUser(true)
    stubUserDetails()
    AuditStub.stubAuditing()
    cache.removeAll()
    FeatureSwitch.switches foreach disable
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
    FeatureSwitch.switches foreach disable
  }

  def getWithHeaders(uri: String, headers: (String, String)*): WSResponse = {
    buildClient(uri)
      .withHttpHeaders(headers: _*)
      .get().futureValue
  }

  def getWithClientDetailsInSession(uri: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
    buildClient(uri)
      .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ additionalCookies), "Csrf-Token" -> "nocheck")
      .get().futureValue
  }

  def getWithCalcIdInSession(uri: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
    buildClient(uri)
      .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ additionalCookies), "Csrf-Token" -> "nocheck")
      .get().futureValue
  }

  def getWithCalcIdInSessionAndWithoutAwait(uri: String, additionalCookies: Map[String, String] = Map.empty): Future[WSResponse] = {
    buildClient(uri)
      .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ additionalCookies), "Csrf-Token" -> "nocheck")
      .get()
  }

  object IncomeTaxViewChangeFrontend {
    def get(uri: String): WSResponse = buildClient(uri)
      .get().futureValue

    def getCreditAndRefunds(): WSResponse = get("/claim-refund")

    def getTaxYears: WSResponse = get("/tax-years")

    def getTaxYearSummary(year: String): WSResponse = get(s"/tax-year-summary/$year")

    def getCalculationPoller(year: String, additionalCookies: Map[String, String], isAgent: Boolean = false): WSResponse =
      getWithCalcIdInSession(s"${if (isAgent) "/agents" else ""}/calculation/$year/submitted", additionalCookies)

    def getFinalTaxCalculationPoller(taxYear: String, additionalCookies: Map[String, String], isAgent: Boolean = false): WSResponse = {
      val agentString = if (isAgent) "/agents" else ""
      getWithCalcIdInSession(s"$agentString/$taxYear/final-tax-overview/calculate", additionalCookies)
    }

    def getCalculationPollerWithoutAwait(year: String, additionalCookies: Map[String, String], isAgent: Boolean = false): Future[WSResponse] =
      getWithCalcIdInSessionAndWithoutAwait(s"${if (isAgent) "/agents" else ""}/calculation/$year/submitted", additionalCookies)

    def getIncomeSummary(year: String): WSResponse = get(s"/$year/income")

    def getForecastIncomeSummary(year: String): WSResponse = get(s"/$year/forecast-income")

    def getTaxDueSummary(year: String): WSResponse = get(s"/$year/tax-calculation")

    def getForecastTaxCalcSummary(year: String): WSResponse = get(s"/$year/forecast-tax-calculation")

    def getDeductionsSummary(year: String): WSResponse = get(s"/$year/allowances-and-deductions")

    def getNextUpdates: WSResponse = get(s"/next-updates")

    def getPreviousObligations: WSResponse = get(s"/previous-obligations")

    def getBtaPartial: WSResponse = get(s"/partial")

    def getHome: WSResponse = get("/")

    def getPaymentsDue: WSResponse = get("/what-you-owe?origin=PTA")

    def getChargeSummary(taxYear: String, id: String): WSResponse = get(s"/tax-years/$taxYear/charge?id=$id")

    def getChargeSummaryLatePayment(taxYear: String, id: String): WSResponse = get(s"/tax-years/$taxYear/charge?id=$id&latePaymentCharge=true")

    def getPay(amountInPence: BigDecimal): WSResponse = get(s"/payment?amountInPence=$amountInPence")

    def getPaymentHistory: WSResponse = get(s"/payment-refund-history")

    def getPaymentAllocationCharges(docNumber: String): WSResponse = get(s"/payment-made-to-hmrc?documentNumber=$docNumber")

    def getCreditsSummary(calendarYear: String): WSResponse = get(s"/credits-from-hmrc/$calendarYear")

    def getRefundToTaxPayer(repaymentRequestNumber: String): WSResponse = get(s"/refund-to-taxpayer/$repaymentRequestNumber ")

  }

  def unauthorisedTest(uri: String): Unit = {
    "unauthorised" should {

      "redirect to sign in" in {

        isAuthorisedUser(false)

        When(s"I call GET /report-quarterly/income-and-expenses/view$uri")
        val res = IncomeTaxViewChangeFrontend.get(uri)

        Then("the http response for an unauthorised user is returned")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn.url)
        )
      }
    }
  }

  def testIncomeSourceDetailsCaching(resetCacheAfterFirstCall: Boolean, noOfCalls: Int, callback: () => Unit): Unit = {
    Given("I wiremock stub a successful Income Source Details response with property only")
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

    And("I wiremock stub a single financial transaction response")
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2,
      dunningLock = twoDunningLocks, interestLocks = twoInterestLocks))

    And("I wiremock stub a charge history response")
    IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "1040000124")(OK, testChargeHistoryJson(testMtditid, "1040000124", 2500))

    callback()
    if (resetCacheAfterFirstCall) cache.removeAll()
    callback()

    verifyIncomeSourceDetailsCall(testMtditid, noOfCalls)
  }
}

