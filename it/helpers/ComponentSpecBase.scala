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
import helpers.servicemocks.AuditStub
import implicits.ImplicitDateFormatterImpl
import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.HeaderNames
import play.api.http.Status.SEE_OTHER
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.ws.WSResponse
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.play.language.LanguageUtils

import scala.concurrent.Future

trait ComponentSpecBase extends TestSuite with CustomMatchers
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with Eventually
  with GenericStubMethods with FeatureSwitching with SessionCookieBaker {

  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: String = WiremockHelper.wiremockPort.toString
  val mockUrl: String = s"http://$mockHost:$mockPort"

  val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

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
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
    FeatureSwitch.switches foreach disable
  }

  def getWithHeaders(uri: String, headers: (String, String)*): WSResponse = {
    await(
      buildClient(uri)
        .withHttpHeaders(headers: _*)
        .get()
    )
  }

  def getWithClientDetailsInSession(uri: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
    await(
      buildClient(uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ additionalCookies), "Csrf-Token" -> "nocheck")
        .get()
    )
  }

  def getWithCalcIdInSession(uri: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
    await(
      buildClient(uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ additionalCookies), "Csrf-Token" -> "nocheck")
        .get()
    )
  }

  def getWithCalcIdInSessionAndWithoutAwait(uri: String, additionalCookies: Map[String, String] = Map.empty): Future[WSResponse] = {
    buildClient(uri)
      .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ additionalCookies), "Csrf-Token" -> "nocheck")
      .get()
  }

  object IncomeTaxViewChangeFrontend {
    def get(uri: String): WSResponse = await(buildClient(uri).get())

    def getTaxYears: WSResponse = get("/tax-years")

    def getCalculation(year: String): WSResponse = get(s"/calculation/$year")

    def getCalculationPoller(year: String, additionalCookies: Map[String, String]): WSResponse =
      getWithCalcIdInSession(s"/calculation/$year/submitted", additionalCookies)

    def getCalculationPollerWithoutAwait(year: String, additionalCookies: Map[String, String]): Future[WSResponse] =
      getWithCalcIdInSessionAndWithoutAwait(s"/calculation/$year/submitted", additionalCookies)

    def getIncomeSummary(year: String): WSResponse = get(s"/calculation/$year/income")

    def getTaxDueSummary(year: String): WSResponse = get(s"/calculation/$year/tax-due")

    def getDeductionsSummary(year: String): WSResponse = get(s"/calculation/$year/deductions")

    def getReportDeadlines: WSResponse = get(s"/obligations")

    def getPreviousObligations: WSResponse = get(s"/previous-obligations")

    def getBtaPartial: WSResponse = get(s"/partial")

    def getHome: WSResponse = get("/")

    def getPaymentsDue: WSResponse = get("/payments-owed")

    def getChargeSummary(taxYear: String, id: String): WSResponse = get(s"/tax-years/$taxYear/charge?id=$id")

    def getPay(amountInPence: BigDecimal): WSResponse = get(s"/payment?amountInPence=$amountInPence")

    def getPaymentHistory: WSResponse = get(s"/payments/history")
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

}

