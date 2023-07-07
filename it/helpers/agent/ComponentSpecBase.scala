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
import forms.CeaseForeignPropertyForm
import forms.agent.ClientsUTRForm
import forms.incomeSources.cease.CeaseUKPropertyForm
import helpers.servicemocks.AuditStub
import helpers.{CustomMatchers, GenericStubMethods, WiremockHelper}
import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.cache.AsyncCacheApi
import play.api.http.HeaderNames
import play.api.http.Status.SEE_OTHER
import play.api.i18n.{Lang, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.ws.WSResponse
import play.api.{Application, Environment, Mode}
import services.{DateService, DateServiceInterface}

import java.time.LocalDate
import javax.inject.Singleton
import scala.concurrent.Future
import forms.BusinessStartDateCheckForm
import forms.incomeSources.add.AddBusinessReportingMethodForm
import testConstants.BaseIntegrationTestConstants.testSelfEmploymentId
import java.time.Month.APRIL

@Singleton
class TestDateService extends DateServiceInterface {

  override def getCurrentDate(isTimeMachineEnabled: Boolean = false): LocalDate = LocalDate.of(2023, 4, 5)

  override def isBeforeLastDayOfTaxYear(isTimeMachineEnabled: Boolean = false): Boolean = true

  override def getCurrentTaxYearEnd(isTimeMachineEnabled: Boolean): Int = {
    val currentDate = getCurrentDate(isTimeMachineEnabled)
    if (isBeforeLastDayOfTaxYear(isTimeMachineEnabled)) currentDate.getYear else currentDate.getYear + 1
  }

  override def getAccountingPeriodEndDate(startDate: LocalDate): String = {
    val startDateYear = startDate.getYear
    val accountingPeriodEndDate = LocalDate.of(startDateYear, APRIL, 5)

    if (startDate.isBefore(accountingPeriodEndDate) || startDate.isEqual(accountingPeriodEndDate)) {
      accountingPeriodEndDate.toString
    } else {
      accountingPeriodEndDate.plusYears(1).toString
    }
  }
}

trait ComponentSpecBase extends TestSuite with CustomMatchers
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with Eventually
  with GenericStubMethods with SessionCookieBaker with FeatureSwitching {

  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: String = WiremockHelper.wiremockPort.toString
  val mockUrl: String = s"http://$mockHost:$mockPort"

  override lazy val cookieSigner: DefaultCookieSigner = app.injector.instanceOf[DefaultCookieSigner]

  implicit val lang: Lang = Lang("GB")
  val messagesAPI: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val testAppConfig: FrontendAppConfig = appConfig

  implicit val dateService: DateService = new DateService() {
    override def getCurrentDate(isTimeMachineEnabled: Boolean = false): LocalDate = LocalDate.of(2023, 4, 5)

    override def getCurrentTaxYearEnd(isTimeMachineEnabled: Boolean = false): Int = 2023
  }

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
    "microservice.services.income-tax-calculation.host" -> mockHost,
    "microservice.services.income-tax-calculation.port" -> mockPort,
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
    .overrides(bind[DateServiceInterface].to[TestDateService])
    .configure(config)
    .build


  val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val cache: AsyncCacheApi = app.injector.instanceOf(classOf[AsyncCacheApi])

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    WireMock.reset()
    AuditStub.stubAuditing()
    cache.removeAll()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
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

    def get(uri: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      When(s"I call GET /report-quarterly/income-and-expenses/view/agents" + uri)
      buildClient("/agents" + uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ additionalCookies))
        .get().futureValue
    }

    def getWithHeaders(uri: String, headers: (String, String)*): WSResponse = {
      buildClient("/agents" + uri)
        .withHttpHeaders(headers: _*)
        .get().futureValue
    }

    def post(uri: String, additionalCookies: Map[String, String] = Map.empty)(body: Map[String, Seq[String]]): WSResponse = {
      When(s"I call POST /report-quarterly/income-and-expenses/view/agents" + uri)
      buildClient("/agents" + uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ additionalCookies), "Csrf-Token" -> "nocheck")
        .post(body).futureValue
    }

    def getEnterClientsUTR: WSResponse = get("/client-utr")

    def postEnterClientsUTR(answer: Option[String]): WSResponse = post("/client-utr")(
      answer.fold(Map.empty[String, Seq[String]])(
        utr => ClientsUTRForm.form.fill(utr).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def getConfirmClientUTR(clientDetails: Map[String, String] = Map.empty): WSResponse = get("/confirm-client-details", clientDetails)

    def postConfirmClientUTR(clientDetails: Map[String, String] = Map.empty): WSResponse = post("/confirm-client-details", clientDetails)(Map.empty)

    def getPaymentsDue: WSResponse = get("/payments-owed")

    def getClientRelationshipFailure: WSResponse = get("/not-authorised-to-view-client")

    def getUTRError(clientUTR: Map[String, String] = Map.empty): WSResponse = get("/cannot-view-client", clientUTR)

    def getAgentError: WSResponse = get("/agent-error")

    def postUTRError: WSResponse = post("/cannot-view-client")(Map.empty)

    def getAgentHome(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/client-income-tax", additionalCookies)

    def getCreditAndRefunds(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/claim-refund", additionalCookies)

    def getPaymentsDue(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/what-your-client-owes", additionalCookies)

    def getTaxYearSummary(taxYear: Int)(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession(s"/agents/tax-year-summary/$taxYear", additionalCookies)

    def getIncomeSummaryAgent(taxYear: Int)(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession(s"/agents/$taxYear/income", additionalCookies)

    def getForecastIncomeSummaryAgent(taxYear: Int)(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession(s"/agents/$taxYear/forecast-income", additionalCookies)

    def getTaxCalcBreakdown(taxYear: Int)(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession(s"/agents/$taxYear/tax-calculation", additionalCookies)

    def getForecastTaxCalcSummary(taxYear: Int)(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession(s"/agents/$taxYear/forecast-tax-calculation", additionalCookies)

    def getChargeSummary(taxYear: String, id: String, additionalCookies: Map[String, String]): WSResponse =
      getWithClientDetailsInSession(s"/agents/tax-years/$taxYear/charge?id=$id", additionalCookies)

    def getPaymentHistory(additionalCookies: Map[String, String]): WSResponse =
      getWithClientDetailsInSession("/agents/payment-refund-history", additionalCookies)

    def getPaymentAllocation(docNumber: String, additionCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession(s"/agents/payment-made-to-hmrc?documentNumber=$docNumber", additionCookies)

    def getDeductionsSummary(year: String, additionalCookies: Map[String, String]): WSResponse =
      getWithClientDetailsInSession(s"/agents/$year/allowances-and-deductions", additionalCookies)

    def getAgentNextUpdates(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/next-updates", additionalCookies)

    def getChargeSummaryLatePayment(taxYear: String, id: String, additionalCookies: Map[String, String]): WSResponse =
      getWithClientDetailsInSession(s"/agents/tax-years/$taxYear/charge?id=$id&latePaymentCharge=true", additionalCookies)

    def getPay(amountInPence: BigDecimal, additionalCookies: Map[String, String]): WSResponse =
      getWithClientDetailsInSession(s"/agents/payment?amountInPence=$amountInPence", additionalCookies)

    def getTaxYears(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/tax-years", additionalCookies)

    def getCeaseUKProperty(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/income-sources/cease/uk-property-declare", additionalCookies)

    def postCeaseUKProperty(answer: Option[String], additionalCookies: Map[String, String] = Map.empty): WSResponse =
      post(uri = "/income-sources/cease/uk-property-declare", additionalCookies)(
        answer.fold(Map.empty[String, Seq[String]])(
          declaration => CeaseUKPropertyForm.form.fill(CeaseUKPropertyForm(Some(declaration), "csrfToken")).data.map { case (k, v) => (k, Seq(v)) }
        )
      )

    def getUKPropertyEndDate(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/income-sources/cease/uk-property-end-date", additionalCookies)

    def getCeaseForeignProperty(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/income-sources/cease/foreign-property-declare", additionalCookies)

    def postCeaseForeignProperty(answer: Option[String], additionalCookies: Map[String, String] = Map.empty): WSResponse =
      post(uri = "/income-sources/cease/foreign-property-declare", additionalCookies)(
        answer.fold(Map.empty[String, Seq[String]])(
          declaration => CeaseForeignPropertyForm.form.fill(CeaseForeignPropertyForm(Some(declaration), "csrfToken")).data.map { case (k, v) => (k, Seq(v)) }
        )
      )

    def getForeignPropertyEndDate(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/income-sources/cease/foreign-property-end-date", additionalCookies)

    def getAddBusinessStartDateCheck(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(
        uri = "/income-sources/add/business-start-date-check", additionalCookies
      )
    }

    def postAddBusinessStartDateCheck(answer: Option[String])(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(
        uri = s"/income-sources/add/business-start-date-check",
        additionalCookies = additionalCookies
      )(
        answer.fold(Map.empty[String, Seq[String]])(
          selection => BusinessStartDateCheckForm.form.fill(BusinessStartDateCheckForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def getAddBusinessStartDate(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      get("/income-sources/add/business-start-date", additionalCookies)


    def getCheckCeaseUKPropertyDetails(additionalCookies: Map[String, String]): WSResponse =
      getWithClientDetailsInSession("/agents/income-sources/cease/uk-property-check-details", additionalCookies)

    def postCheckCeaseUKPropertyDetails(additionalCookies: Map[String, String]): WSResponse =
      post("/income-sources/cease/uk-property-check-details", additionalCookies)(Map.empty)

    def postAddBusinessReportingMethod(form: AddBusinessReportingMethodForm)(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      val formData = form.toFormMap.map { case (k, v) => (k -> Seq(v.getOrElse(""))) }
      post(s"/income-sources/add/business-reporting-method?id=$testSelfEmploymentId", additionalCookies = additionalCookies)(formData)
    }
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
          redirectURI(controllers.routes.SignInController.signIn.url)
        )
      }
    }
  }

  def testIncomeSourceDetailsCaching(resetCacheAfterFirstCall: Boolean, noOfCalls: Int, callback: () => Unit): Unit = {
    // tests to be reimplemented after hmrc-mongo caching
  }
}

