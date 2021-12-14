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
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.ws.WSResponse
import play.api.{Application, Environment, Mode}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, testChargeHistoryJson, testValidFinancialDetailsModelJson, twoDunningLocks, twoInterestLocks}
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate
import scala.concurrent.Future

trait ComponentSpecBase extends TestSuite with CustomMatchers
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with Eventually
  with GenericStubMethods with FeatureSwitching with SessionCookieBaker {

  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: String = WiremockHelper.wiremockPort.toString
  val mockUrl: String = s"http://$mockHost:$mockPort"

  val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val cache: AsyncCacheApi = app.injector.instanceOf[AsyncCacheApi]

  val mockLanguageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]
  implicit val mockImplicitDateFormatter: ImplicitDateFormatterImpl = new ImplicitDateFormatterImpl(mockLanguageUtils)

  override lazy val cookieSigner: DefaultCookieSigner = app.injector.instanceOf[DefaultCookieSigner]

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
    "microservice.services.individual-calculations.host" -> mockHost,
    "microservice.services.individual-calculations.port" -> mockPort,
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
    val currentDate: LocalDate = LocalDate.now
    if(currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6)))LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
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
    def get(uri: String): WSResponse = buildClient(uri).get().futureValue

    def getTaxYears: WSResponse = get("/tax-years")

    def getCalculation(year: String): WSResponse = get(s"/calculation/$year")

    def getCalculationPoller(year: String, additionalCookies: Map[String, String], isAgent: Boolean = false): WSResponse =
      getWithCalcIdInSession(s"${if(isAgent) "/agents" else ""}/calculation/$year/submitted", additionalCookies)
      
    def getFinalTaxCalculationPoller(taxYear: String, additionalCookies: Map[String, String], isAgent: Boolean = false): WSResponse = {
      val agentString = if(isAgent) "/agents" else ""
      getWithCalcIdInSession(s"$agentString/$taxYear/final-tax-overview/calculate", additionalCookies)
    }

    def getCalculationPollerWithoutAwait(year: String, additionalCookies: Map[String, String], isAgent: Boolean = false): Future[WSResponse] =
      getWithCalcIdInSessionAndWithoutAwait(s"${if(isAgent) "/agents" else ""}/calculation/$year/submitted", additionalCookies)

    def getIncomeSummary(year: String): WSResponse = get(s"/calculation/$year/income")

    def getTaxDueSummary(year: String): WSResponse = get(s"/calculation/$year/tax-due")

    def getDeductionsSummary(year: String): WSResponse = get(s"/calculation/$year/deductions")

    def getNextUpdates: WSResponse = get(s"/next-updates")

    def getPreviousObligations: WSResponse = get(s"/previous-obligations")

    def getBtaPartial: WSResponse = get(s"/partial")

    def getHome: WSResponse = get("/")

    def getPaymentsDue: WSResponse = get("/payments-owed")

    def getChargeSummary(taxYear: String, id: String): WSResponse = get(s"/tax-years/$taxYear/charge?id=$id")

    def getChargeSummaryLatePayment(taxYear: String, id: String): WSResponse = get(s"/tax-years/$taxYear/charge?id=$id&latePaymentCharge=true")

    def getPay(amountInPence: BigDecimal): WSResponse = get(s"/payment?amountInPence=$amountInPence")

    def getPaymentHistory: WSResponse = get(s"/payments/history")

    def getPaymentAllocationCharges(docNumber: String): WSResponse = get(s"/charges/payments-made?documentNumber=$docNumber")
  }

  def unauthorisedTest(uri: String): Unit = {
    "unauthorised" should {

      "redirect to sign in" in {

        isAuthorisedUser(false)

        When(s"I call GET /report-quarterly/income-and-expenses/view/$uri")
        val res = IncomeTaxViewChangeFrontend.get(uri)

        Then("the http response for an unauthorised user is returned")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
  }

  def testIncomeSourceDetailsCaching(resetCacheAfterFirstCall: Boolean, noOfCalls:Int, callback: () => Unit): Unit = {
    Given("I wiremock stub a successful Income Source Details response with property only")
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

    And("I wiremock stub a single financial transaction response")
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2,
      dunningLock = twoDunningLocks, interestLocks = twoInterestLocks))

    And("I wiremock stub a charge history response")
    IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "1040000124")(OK, testChargeHistoryJson(testMtditid, "1040000124", 2500))

    callback()
    if(resetCacheAfterFirstCall) cache.removeAll()
    callback()
    verifyIncomeSourceDetailsCall(testMtditid, noOfCalls)
  }
}

