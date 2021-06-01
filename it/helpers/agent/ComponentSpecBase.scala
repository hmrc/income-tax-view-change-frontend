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

package helpers.agent

import com.github.tomakehurst.wiremock.client.WireMock
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import forms.agent.ClientsUTRForm
import helpers.servicemocks.AuditStub
import helpers.servicemocks.AuthStub.getWithClientDetailsInSession
import helpers.{CustomMatchers, GenericStubMethods, WiremockHelper}
import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.HeaderNames
import play.api.http.Status.SEE_OTHER
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.ws.WSResponse
import play.api.{Application, Environment, Mode}

trait ComponentSpecBase extends TestSuite with CustomMatchers
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with Eventually
  with GenericStubMethods with SessionCookieBaker with FeatureSwitching {

  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: String = WiremockHelper.wiremockPort.toString
  val mockUrl: String = s"http://$mockHost:$mockPort"

  override lazy val cookieSigner: DefaultCookieSigner = app.injector.instanceOf[DefaultCookieSigner]

  val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  def config: Map[String, String] = Map(
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
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
    "microservice.services.citizen-details.host" -> mockHost,
    "microservice.services.citizen-details.port" -> mockPort,
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
    AuditStub.stubAuditing()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  def getWithHeaders(uri: String, headers: (String, String)*): WSResponse = {
    await(
      buildClient(uri)
        .withHttpHeaders(headers: _*)
        .get()
    )
  }

  object IncomeTaxViewChangeFrontend {

    def get(uri: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      When(s"I call GET /report-quarterly/income-and-expenses/view/agents" + uri)
      await(buildClient("/agents" + uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ additionalCookies))
        .get())
    }

    def post(uri: String, additionalCookies: Map[String, String] = Map.empty)(body: Map[String, Seq[String]]): WSResponse = {
      When(s"I call POST /report-quarterly/income-and-expenses/view/agents" + uri)
      await(buildClient("/agents" + uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ additionalCookies), "Csrf-Token" -> "nocheck")
        .post(body)
      )
    }

    def getEnterClientsUTR: WSResponse = get("/client-utr")

    def postEnterClientsUTR(answer: Option[String]): WSResponse = post("/client-utr")(
      answer.fold(Map.empty[String, Seq[String]])(
        utr => ClientsUTRForm.form.fill(utr).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def getConfirmClientUTR(clientDetails: Map[String, String] = Map.empty): WSResponse = get("/confirm-client", clientDetails)

    def postConfirmClientUTR(clientDetails: Map[String, String] = Map.empty): WSResponse = post("/confirm-client", clientDetails)(Map.empty)

    def getPaymentsDue: WSResponse = get("/payments-owed")

    def getClientRelationshipFailure: WSResponse = get("/client-relationship-problem")

    def getUTRError(clientUTR: Map[String, String] = Map.empty): WSResponse = get("/utr-problem", clientUTR)

    def postUTRError: WSResponse = post("/utr-problem")(Map.empty)

    def getAgentHome(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/income-tax-account", additionalCookies)

    def getPaymentsDue(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/payments-owed", additionalCookies)

    def getTaxYearOverview(taxYear: Int)(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession(s"/agents/calculation/$taxYear", additionalCookies)

    def getIncomeSummary(taxYear: Int)(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession(s"/agents/calculation/$taxYear/income", additionalCookies)

    def getTaxCalcBreakdown(taxYear: Int)(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession(s"/agents/calculation/$taxYear/tax-due", additionalCookies)

    def getChargeSummary(taxYear: String, id: String, additionalCookies: Map[String, String]): WSResponse =
      getWithClientDetailsInSession(s"/agents/tax-years/$taxYear/charge?id=$id", additionalCookies)

    def getPaymentHistory(additionalCookies: Map[String, String]): WSResponse =
      getWithClientDetailsInSession("/agents/payments/history", additionalCookies)

    def getDeductionsSummary(year: String, additionalCookies: Map[String, String]): WSResponse =
      getWithClientDetailsInSession(s"/agents/calculation/$year/deductions", additionalCookies)

    def getAgentNextUpdates(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/next-updates", additionalCookies)

    def getPay(amountInPence: BigDecimal, additionalCookies: Map[String, String]): WSResponse =
      getWithClientDetailsInSession(s"/agents/payment?amountInPence=$amountInPence", additionalCookies)

    def getTaxYears(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/tax-years", additionalCookies)

  }

  def unauthorisedTest(uri: String): Unit = {
    "unauthorised" should {

      "redirect to sign in" in {

        isAuthorisedUser(false)

        When(s"I call GET /report-quarterly/income-and-expenses/view/agents/$uri")
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

