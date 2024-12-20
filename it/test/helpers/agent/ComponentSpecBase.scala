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

import java.time.LocalDate
//import controllers.agent.sessionUtils.SessionKeys
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import forms.agent.ClientsUTRForm
import forms.incomeSources.add.{AddIncomeSourceStartDateCheckForm, IncomeSourceReportingMethodForm}
import forms.incomeSources.cease.DeclareIncomeSourceCeasedForm
import forms.optOut.ConfirmOptOutSingleTaxYearForm
import helpers.servicemocks.AuditStub
import helpers.{CustomMatchers, GenericStubMethods, TestDateService, WiremockHelper}
import models.admin.FeatureSwitchName
import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
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
import repositories.OptOutSessionDataRepository
import services.DateServiceInterface
import testConstants.BaseIntegrationTestConstants._
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.{ExecutionContext, Future}

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
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  implicit val dateService: DateServiceInterface = app.injector.instanceOf[DateServiceInterface]

  def config: Map[String, Object] = Map(
    "play.filters.disabled" -> Seq("uk.gov.hmrc.play.bootstrap.frontend.filters.SessionIdFilter"),
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
    "microservice.services.income-tax-session-data.host" -> mockHost,
    "microservice.services.income-tax-session-data.port" -> mockPort,
    "microservice.services.address-lookup-frontend.host" -> mockHost,
    "microservice.services.address-lookup-frontend.port" -> mockPort,
    "auditing.consumer.baseUri.host" -> mockHost,
    "auditing.consumer.baseUri.port" -> mockPort,
    "auditing.enabled" -> "true",
    "microservice.services.contact-frontend.host" -> mockHost,
    "microservice.services.contact-frontend.port" -> mockPort,
    "feature-switches.read-from-mongo" -> "false",
    "feature-switch.enable-time-machine" -> "false",
    "feature-switch.enable-session-data-storage" -> "true",
    "time-machine.add-years" -> "0",
    "time-machine.add-days" -> "0"
  )

  val userDetailsUrl = "/user-details/id/5397272a3d00003d002f3ca9"
  val btaPartialUrl = "/business-account/partial/service-info"
  val testUserDetailsWiremockUrl: String = mockUrl + userDetailsUrl

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .overrides(bind[DateServiceInterface].to[TestDateService])
    .configure(config)
    .build()


  val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val cache: AsyncCacheApi = app.injector.instanceOf(classOf[AsyncCacheApi])
  val optOutSessionDataRepository: OptOutSessionDataRepository = app.injector.instanceOf[OptOutSessionDataRepository]

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    WireMock.reset()
    AuditStub.stubAuditing()
    cache.removeAll()
    FeatureSwitchName.allFeatureSwitches foreach disable
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
    FeatureSwitchName.allFeatureSwitches foreach disable
  }

  def getWithClientDetailsInSession(uri: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
    buildClient(uri)
      .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ additionalCookies), "Csrf-Token" -> "nocheck", "X-Session-ID" -> testSessionId)
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

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.of(2023, 4, 4)
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6)))
      LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }

  object IncomeTaxViewChangeFrontend {

    def get(uri: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      When(s"I call GET /report-quarterly/income-and-expenses/view/agents" + uri)
      buildClient("/agents" + uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ additionalCookies), "X-Session-ID" -> testSessionId)
        .get().futureValue
    }

    def getWithHeaders(uri: String, headers: (String, String)*): WSResponse = {
      buildClient("/agents" + uri)
        .withHttpHeaders(headers: _*)
        .get().futureValue
    }

    def post(uri: String, additionalCookies: Map[String, String] = Map.empty)(body: Map[String, Seq[String]]): WSResponse = {
      When(s"I call POST /report-quarterly/income-and-expenses/view/agents" + uri)
      buildClient("/agents" + uri).withMethod("POST")
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(additionalCookies),
          "Csrf-Token" -> "nocheck",
          "X-Session-ID" -> testSessionId,
          "X-Session-Id" -> testSessionId,
          "sessionId-qqq" -> testSessionId)
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

    def getAdjustPaymentsOnAccount(clientDetails: Map[String, String] = Map.empty): WSResponse = get(s"/adjust-poa/start", clientDetails)

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

    def getSingleYearOptOutWarning(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(s"/optout/single-taxyear-warning", additionalCookies)
    }

    def postSingleYearOptOutWarning(additionalCookies: Map[String, String] = Map.empty)(form: ConfirmOptOutSingleTaxYearForm): WSResponse = {
      val formData: Map[String, Seq[String]] = Map(
        ConfirmOptOutSingleTaxYearForm.confirmOptOutField -> Seq(if (form.confirmOptOut.isDefined) form.confirmOptOut.get.toString else ""),
        ConfirmOptOutSingleTaxYearForm.csrfToken -> Seq(""))
      post("/optout/single-taxyear-warning", additionalCookies)(formData)
    }

    def getConfirmOptOut(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get("/optout/review-confirm-taxyear", additionalCookies)
    }

    def postConfirmOptOut(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/optout/review-confirm-taxyear", additionalCookies)(Map.empty)
    }

    def getConfirmedOptOut(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get("/optout/confirmed", additionalCookies)
    }

    def getChargeSummaryLatePayment(taxYear: String, id: String, additionalCookies: Map[String, String]): WSResponse =
      getWithClientDetailsInSession(s"/agents/tax-years/$taxYear/charge?id=$id&isInterestCharge=true", additionalCookies)

    def getPay(amountInPence: BigDecimal, additionalCookies: Map[String, String]): WSResponse =
      getWithClientDetailsInSession(s"/agents/payment?amountInPence=$amountInPence", additionalCookies)

    def getTaxYears(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/tax-years", additionalCookies)

    def getPoaWhatYouNeedToKnow(clientDetails: Map[String, String] = Map.empty): WSResponse = get("/adjust-poa/what-you-need-to-know", clientDetails)

    def getCeaseUKProperty(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/manage-your-businesses/cease/uk-property-confirm-cease", additionalCookies)

    def getCeaseSelfEmployment(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession(s"/agents/manage-your-businesses/cease/business-confirm-cease?id=$testSelfEmploymentIdHashed", additionalCookies)

    def postCeaseUKProperty(answer: Option[String], additionalCookies: Map[String, String] = Map.empty): WSResponse =
      post(uri = "/manage-your-businesses/cease/uk-property-confirm-cease", additionalCookies)(
        answer.fold(Map.empty[String, Seq[String]])(
          declaration => DeclareIncomeSourceCeasedForm.form(UkProperty).fill(DeclareIncomeSourceCeasedForm(Some(declaration), "csrfToken")).data.map { case (k, v) => (k, Seq(v)) }
        )
      )

    def getUKPropertyEndDate(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/manage-your-businesses/cease/uk-property-end-date", additionalCookies)

    def getCeaseForeignProperty(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/manage-your-businesses/cease/foreign-property-confirm-cease", additionalCookies)

    def postCeaseForeignProperty(answer: Option[String], additionalCookies: Map[String, String] = Map.empty): WSResponse =
      post(uri = "/manage-your-businesses/cease/foreign-property-confirm-cease", additionalCookies)(
        answer.fold(Map.empty[String, Seq[String]])(
          declaration => DeclareIncomeSourceCeasedForm.form(ForeignProperty).fill(DeclareIncomeSourceCeasedForm(Some(declaration), "csrfToken")).data.map { case (k, v) => (k, Seq(v)) }
        )
      )

    def postCeaseSelfEmployment(answer: Option[String], additionalCookies: Map[String, String] = Map.empty): WSResponse =
      post(uri = s"/manage-your-businesses/cease/business-confirm-cease?id=$testSelfEmploymentIdHashed", additionalCookies)(
        answer.fold(Map.empty[String, Seq[String]])(
          declaration => DeclareIncomeSourceCeasedForm.form(ForeignProperty).fill(DeclareIncomeSourceCeasedForm(Some(declaration), "csrfToken")).data.map { case (k, v) => (k, Seq(v)) }
        )
      )

    def getForeignPropertyEndDate(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      getWithClientDetailsInSession("/agents/manage-your-businesses/cease/foreign-property-end-date", additionalCookies)

    def getForeignPropertyAddedObligations(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(
        uri = s"/manage-your-businesses/add-foreign-property/foreign-property-added",
        additionalCookies
      )
    }

    def postForeignPropertyAddedObligations(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/manage-your-businesses/add-foreign-property/foreign-property-added", additionalCookies)(Map.empty)
    }

    def getAddBusinessName(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      get("/manage-your-businesses/add-sole-trader/business-name", additionalCookies)

    def postAddBusinessName(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/manage-your-businesses/add-sole-trader/business-name", additionalCookies)(Map.empty)
    }

    def getChangeBusinessName(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      get("/manage-your-businesses/add-sole-trader/change-business-name", additionalCookies)

    def postChangeBusinessName(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/manage-your-businesses/add-sole-trader/change-business-name", additionalCookies)(Map.empty)
    }

    def getAddBusinessTrade(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      get("/manage-your-businesses/add-sole-trader/business-trade", additionalCookies)

    def postAddBusinessTrade(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/manage-your-businesses/add-sole-trader/business-trade", additionalCookies)(Map.empty)
    }

    def getAddChangeBusinessTrade(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      get("/manage-your-businesses/add-sole-trader/change-business-trade", additionalCookies)

    def postAddChangeBusinessTrade(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/manage-your-businesses/add-sole-trader/change-business-trade", additionalCookies)(Map.empty)
    }

    def getAddBusinessStartDateCheck(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(
        uri = "/manage-your-businesses/add-sole-trader/business-start-date-check", additionalCookies
      )
    }

    def getAddBusinessStartDateCheckChange(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(
        uri = "/manage-your-businesses/add-sole-trader/change-business-start-date-check", additionalCookies
      )
    }

    def getAddForeignPropertyStartDateCheckChange(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(
        uri = "/manage-your-businesses/add-foreign-property/change-business-start-date-check", additionalCookies
      )
    }

    def getAddUKPropertyStartDateCheckChange(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(
        uri = "/manage-your-businesses/add-uk-property/change-business-start-date-check", additionalCookies
      )
    }

    def getFeedbackPage(additionalCookies: Map[String, String] = Map.empty): WSResponse = get("/feedback", additionalCookies)

    def getThankyouPage(additionalCookies: Map[String, String] = Map.empty): WSResponse = get("/thankyou", additionalCookies)

    def postAddBusinessStartDateCheck(answer: Option[String])(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(
        uri = s"/manage-your-businesses/add-sole-trader/business-start-date-check",
        additionalCookies = additionalCookies
      )(
        answer.fold(Map.empty[String, Seq[String]])(
          selection => AddIncomeSourceStartDateCheckForm(SelfEmployment.addStartDateCheckMessagesPrefix)
            .fill(AddIncomeSourceStartDateCheckForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def postAddBusinessStartDateCheckChange(answer: Option[String])(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(
        uri = s"/manage-your-businesses/add-sole-trader/change-business-start-date-check",
        additionalCookies = additionalCookies
      )(
        answer.fold(Map.empty[String, Seq[String]])(
          selection => AddIncomeSourceStartDateCheckForm(SelfEmployment.addStartDateCheckMessagesPrefix)
            .fill(AddIncomeSourceStartDateCheckForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def postAddUKPropertyStartDateCheckChange(answer: Option[String])(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(
        uri = s"/manage-your-businesses/add-uk-property/change-business-start-date-check",
        additionalCookies = additionalCookies
      )(
        answer.fold(Map.empty[String, Seq[String]])(
          selection => AddIncomeSourceStartDateCheckForm(UkProperty.addStartDateCheckMessagesPrefix)
            .fill(AddIncomeSourceStartDateCheckForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def postAddForeignPropertyStartDateCheckChange(answer: Option[String])(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(
        uri = s"/manage-your-businesses/add-foreign-property/change-business-start-date-check",
        additionalCookies = additionalCookies
      )(
        answer.fold(Map.empty[String, Seq[String]])(
          selection => AddIncomeSourceStartDateCheckForm(ForeignProperty.addStartDateCheckMessagesPrefix)
            .fill(AddIncomeSourceStartDateCheckForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def postAddUKPropertyStartDateCheck(answer: Option[String])(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(
        uri = s"/manage-your-businesses/add-uk-property/business-start-date-check",
        additionalCookies = additionalCookies
      )(
        answer.fold(Map.empty[String, Seq[String]])(
          selection => AddIncomeSourceStartDateCheckForm(UkProperty.addStartDateCheckMessagesPrefix)
            .fill(AddIncomeSourceStartDateCheckForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def postAddForeignPropertyStartDateCheck(answer: Option[String])(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(
        uri = s"/manage-your-businesses/add-foreign-property/business-start-date-check",
        additionalCookies = additionalCookies
      )(
        answer.fold(Map.empty[String, Seq[String]])(
          selection => AddIncomeSourceStartDateCheckForm(ForeignProperty.addStartDateCheckMessagesPrefix)
            .fill(AddIncomeSourceStartDateCheckForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def getAddBusinessStartDate(additionalCookies: Map[String, String] = Map.empty): WSResponse =
      get("/manage-your-businesses/add-sole-trader/business-start-date", additionalCookies)

    def getAddBusinessObligations(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(
        uri = s"/manage-your-businesses/add-sole-trader/business-added",
        additionalCookies
      )
    }

    def postAddedBusinessObligations(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/manage-your-businesses/add-sole-trader/business-added", additionalCookies)(Map.empty)
    }

    def getCheckCeaseUKPropertyAnswers(additionalCookies: Map[String, String]): WSResponse =
      getWithClientDetailsInSession("/agents/manage-your-businesses/cease/uk-property-check-answers", additionalCookies)

    def postCheckCeaseUKPropertyAnswers(additionalCookies: Map[String, String]): WSResponse =
      post("/manage-your-businesses/cease/uk-property-check-answers", additionalCookies)(Map.empty)


    def getCheckCeaseForeignPropertyAnswers(session: Map[String, String]): WSResponse =
      getWithClientDetailsInSession("/agents/manage-your-businesses/cease/foreign-property-check-answers", session)

    def postCheckCeaseForeignPropertyAnswers(session: Map[String, String]): WSResponse =
      post(s"/manage-your-businesses/cease/foreign-property-check-answers", session)(Map.empty)

    def getManageIncomeSource(additionalCookies: Map[String, String]): WSResponse = get("/income-sources/manage/view-and-manage-income-sources", additionalCookies)

    def postAddBusinessReportingMethod(form: IncomeSourceReportingMethodForm)(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      val formData = form.toFormMap.map { case (k, v) => k -> Seq(v.getOrElse("")) }
      post(s"/income-sources/add/business-reporting-method?id=$testSelfEmploymentId", additionalCookies = additionalCookies)(formData)
    }

    def postAddUKPropertyReportingMethod(form: IncomeSourceReportingMethodForm)(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      val formData = form.toFormMap.map { case (k, v) => k -> Seq(v.getOrElse("")) }
      post(s"/income-sources/add/uk-property-reporting-method?id=$testPropertyIncomeId", additionalCookies = additionalCookies)(formData)
    }

    def postAddForeignPropertyReportingMethod(form: IncomeSourceReportingMethodForm)(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      val formData = form.toFormMap.map { case (k, v) => k -> Seq(v.getOrElse("")) }
      post(s"/income-sources/add/foreign-property-reporting-method?id=$testPropertyIncomeId", additionalCookies = additionalCookies)(formData)
    }

    def getConfirmSoleTraderBusinessReportingMethod(taxYear: String, changeTo: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(s"/manage-your-businesses/manage/confirm-you-want-to-report?id=$testSelfEmploymentId&taxYear=$taxYear&changeTo=$changeTo", additionalCookies)
    }

    def getConfirmUKPropertyReportingMethod(taxYear: String, changeTo: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(s"/manage-your-businesses/manage/confirm-you-want-to-report-uk-property?taxYear=$taxYear&changeTo=$changeTo", additionalCookies)
    }

    def getConfirmForeignPropertyReportingMethod(taxYear: String, changeTo: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(s"/manage-your-businesses/manage/confirm-you-want-to-report-foreign-property?taxYear=$taxYear&changeTo=$changeTo", additionalCookies)
    }

    def postConfirmSoleTraderBusinessReportingMethod(taxYear: String, changeTo: String, additionalCookies: Map[String, String] = Map.empty)(formData: Map[String, Seq[String]]): WSResponse = {
      post(s"/manage-your-businesses/manage/confirm-you-want-to-report?id=$testSelfEmploymentId&taxYear=$taxYear&changeTo=$changeTo", additionalCookies)(formData)
    }

    def postConfirmUKPropertyReportingMethod(taxYear: String, changeTo: String, additionalCookies: Map[String, String] = Map.empty)(formData: Map[String, Seq[String]]): WSResponse = {
      post(s"/manage-your-businesses/manage/confirm-you-want-to-report-uk-property?id=$testPropertyIncomeId&taxYear=$taxYear&changeTo=$changeTo", additionalCookies)(formData)
    }

    def postConfirmForeignPropertyReportingMethod(taxYear: String, changeTo: String, additionalCookies: Map[String, String] = Map.empty)(formData: Map[String, Seq[String]]): WSResponse = {
      post(s"/manage-your-businesses/manage/confirm-you-want-to-report-foreign-property?id=$testPropertyIncomeId&taxYear=$taxYear&changeTo=$changeTo", additionalCookies)(formData)
    }

    def getManageSEObligations(changeTo: String, taxYear: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(s"/income-sources/manage/business-will-report?changeTo=$changeTo&taxYear=$taxYear", additionalCookies)
    }

    def getManageUKObligations(changeTo: String, taxYear: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(s"/income-sources/manage/uk-property-will-report?changeTo=$changeTo&taxYear=$taxYear", additionalCookies)
    }

    def getManageFPObligations(changeTo: String, taxYear: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(s"/manage-your-businesses/manage/foreign-property-will-report?changeTo=$changeTo&taxYear=$taxYear", additionalCookies)
    }

    def postManageObligations(mode: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/manage-your-businesses/manage/$mode-will-report", additionalCookies)(Map.empty)
    }

    def getCheckCeaseBusinessAnswers(additionalCookies: Map[String, String]): WSResponse =
      getWithClientDetailsInSession("/agents/manage-your-businesses/cease/business-check-answers", additionalCookies)

    def postCheckCeaseBusinessAnswers(additionalCookies: Map[String, String]): WSResponse =
      post("/manage-your-businesses/cease/business-check-answers", additionalCookies)(Map.empty)

    def getForeignPropertyCeasedObligations(session: Map[String, String]): WSResponse = get(uri = "/manage-your-businesses/cease-foreign-property/cease-success", session)

    def getUkPropertyCeasedObligations(session: Map[String, String]): WSResponse = get(uri = "/manage-your-businesses/cease-uk-property/cease-success", session)

    def getBusinessCeasedObligations(session: Map[String, String]): WSResponse = get(uri = "/manage-your-businesses/cease-sole-trader/cease-success", session)

    def getAddChangeBusinessAddress: WSResponse =
      get("/income-sources/add/change-business-address-lookup")

    def getAddBusinessAddress: WSResponse =
      get("/income-sources/add/business-address")

    def getSEReportingMethodNotSaved(session: Map[String, String]): WSResponse = get(uri = s"/income-sources/add/error-business-reporting-method-not-saved", session)

    def getUkPropertyReportingMethodNotSaved(session: Map[String, String]): WSResponse = get(uri = s"/income-sources/add/error-uk-property-reporting-method-not-saved", session)

    def getForeignPropertyReportingMethodNotSaved(session: Map[String, String]): WSResponse = get(uri = s"/income-sources/add/error-foreign-property-reporting-method-not-saved", session)

    def getBusinessEndDate(session: Map[String, String]): WSResponse = get(s"/income-sources/cease/business-end-date?id=$testSelfEmploymentIdHashed", session)

    def getAddIncomeSource(session: Map[String, String]): WSResponse = get(uri = s"/income-sources/add/new-income-sources", session)

    def getManageSECannotGoBack: WSResponse = get(s"/income-sources/manage/manage-business-cannot-go-back", clientDetailsWithConfirmation)

    def getManageUKPropertyCannotGoBack: WSResponse = get(s"/income-sources/manage/manage-uk-property-cannot-go-back", clientDetailsWithConfirmation)

    def getManageForeignPropertyCannotGoBack: WSResponse = get(s"/income-sources/manage/manage-foreign-property-cannot-go-back", clientDetailsWithConfirmation)

    def getCeaseSECannotGoBack(): WSResponse = get("/income-sources/cease/cease-business-cannot-go-back", clientDetailsWithConfirmation)

    def getCeaseUKCannotGoBack(): WSResponse = get("/income-sources/cease/cease-uk-property-cannot-go-back", clientDetailsWithConfirmation)

    def getCeaseFPCannotGoBack(): WSResponse = get("/income-sources/cease/cease-foreign-property-cannot-go-back", clientDetailsWithConfirmation)

    def getManageYourBusinesses(additionalCookies: Map[String, String]): WSResponse = get("/manage-your-businesses", additionalCookies)

    def getViewAllCeasedBusinesses(): WSResponse = get("/manage-your-businesses/ceased-businesses", clientDetailsWithConfirmation)

    def getCheckYourAnswersUKProperty(): WSResponse = {
      get(s"/manage-your-businesses/manage/uk-property-check-your-answers", clientDetailsWithConfirmation)
    }

    def getCheckYourAnswersForeignProperty(): WSResponse = {
      get(s"/manage-your-businesses/manage/foreign-property-check-your-answers", clientDetailsWithConfirmation)
    }

    def getCheckYourAnswersSoleTrader(): WSResponse = {
      get(s"/manage-your-businesses/manage/business-check-your-answers", clientDetailsWithConfirmation)
    }

    def postCheckYourAnswersUKProperty(): WSResponse = {
      post(s"/manage-your-businesses/manage/uk-property-check-your-answers", clientDetailsWithConfirmation)(Map.empty)
    }

    def postCheckYourAnswersForeignProperty(): WSResponse = {
      post(s"/manage-your-businesses/manage/foreign-property-check-your-answers", clientDetailsWithConfirmation)(Map.empty)
    }

    def postCheckYourAnswersSoleTrader(): WSResponse = {
      post(s"/manage-your-businesses/manage/business-check-your-answers", clientDetailsWithConfirmation)(Map.empty)
    }

    def getBeforeYouStart(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get("/opt-in/start", additionalCookies)
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
}

