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
import com.github.tomakehurst.wiremock.client.WireMock
import config.FrontendAppConfig
import config.featureswitch.{FeatureSwitch, FeatureSwitching}
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import forms.incomeSources.add.{AddIncomeSourceStartDateCheckForm, IncomeSourceReportingMethodForm}
import forms.incomeSources.cease.DeclarePropertyCeasedForm
import helpers.agent.SessionCookieBaker
import helpers.servicemocks.AuditStub
import implicits.ImplicitDateFormatterImpl
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
import services.{DateService, DateServiceInterface}
import testConstants.BaseIntegrationTestConstants.{testPropertyIncomeId, testSelfEmploymentId, testSelfEmploymentIdHashed, testSessionId}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, SessionId}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate
import java.time.Month.APRIL
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestHeaderExtractor extends HeaderExtractor {

  override def extractHeader(request: play.api.mvc.Request[_], session: play.api.mvc.Session): HeaderCarrier = {
    HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)
      .copy(authorization = Some(Authorization("Bearer")))
  }

}

@Singleton
class TestDateService extends DateServiceInterface {

  override def getCurrentDate(isTimeMachineEnabled: Boolean = false): LocalDate = LocalDate.of(2023, 4, 5)

  override def isBeforeLastDayOfTaxYear(isTimeMachineEnabled: Boolean = false): Boolean = true

  override def getCurrentTaxYearEnd(isTimeMachineEnabled: Boolean): Int = 2023

  override def getCurrentTaxYearStart(isTimeMachineEnabled: Boolean = false): LocalDate = LocalDate.of(2022, 4, 6)

  override def getAccountingPeriodEndDate(startDate: LocalDate): LocalDate = {
    val startDateYear = startDate.getYear
    val accountingPeriodEndDate = LocalDate.of(startDateYear, APRIL, 5)

    if (startDate.isBefore(accountingPeriodEndDate) || startDate.isEqual(accountingPeriodEndDate)) {
      accountingPeriodEndDate
    } else {
      accountingPeriodEndDate.plusYears(1)
    }
  }
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
  val messagesAPI: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockLanguageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]
  implicit val lang: Lang = Lang("GB")
  implicit val mockImplicitDateFormatter: ImplicitDateFormatterImpl = new ImplicitDateFormatterImpl(mockLanguageUtils)
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))
  implicit val testAppConfig: FrontendAppConfig = appConfig
  implicit val dateService: DateService = new DateService() {
    override def getCurrentDate(isTimeMachineEnabled: Boolean = false): LocalDate = LocalDate.of(2023, 4, 5)

    override def getCurrentTaxYearEnd(isTimeMachineEnabled: Boolean = false): Int = 2023
  }

  override lazy val cookieSigner: DefaultCookieSigner = app.injector.instanceOf[DefaultCookieSigner]

  val titleInternalServer: String = "standardError.heading"
  val titleTechError = "Sorry, we are experiencing technical difficulties - 500"
  val titleNotFound = "Page not found - 404"
  val titleProbWithService = "There is a problem with the service"
  val titleThereIsAProblem = "There’s a problem"
  val titleClientRelationshipFailure: String = "agent.client_relationship_failure.heading"

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
    "calculation-polling.interval" -> "500",
    "calculation-polling.timeout" -> "3000",
    "calculation-polling.attempts" -> "10",
    "calculation-polling.delayBetweenAttemptInMilliseconds" -> "500",
    "auditing.consumer.baseUri.host" -> mockHost,
    "auditing.consumer.baseUri.port" -> mockPort,
    "microservice.services.address-lookup-frontend.port" -> mockPort,
    "auditing.enabled" -> "true",
    "encryption.key" -> "QmFyMTIzNDVCYXIxMjM0NQ==",
    "encryption.isEnabled" -> "false"
  )

  val userDetailsUrl = "/user-details/id/5397272a3d00003d002f3ca9"
  val btaPartialUrl = "/business-account/partial/service-info"
  val testUserDetailsWiremockUrl: String = mockUrl + userDetailsUrl

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.of(2023, 4, 4)
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6)))
      LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }


  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .overrides(bind[HeaderExtractor].to[TestHeaderExtractor])
    .overrides(bind[DateServiceInterface].to[TestDateService])
    .configure(config)
    .build()

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

  object IncomeTaxViewChangeFrontend {
    def get(uri: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      When(s"I call GET /report-quarterly/income-and-expenses/view" + uri)
      buildClient(uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(additionalCookies), "X-Session-ID" -> testSessionId)
        .get().futureValue
    }

    def getWithHeaders(uri: String, headers: (String, String)*): WSResponse = {
      buildClient(uri)
        .withHttpHeaders(headers: _*)
        .get().futureValue
    }

    def post(uri: String, additionalCookies: Map[String, String] = Map.empty)(body: Map[String, Seq[String]]): WSResponse = {
      When(s"I call POST /report-quarterly/income-and-expenses/view" + uri)
      buildClient(uri)
        .withFollowRedirects(false)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(additionalCookies),
          "Csrf-Token" -> "nocheck",
          "X-Session-ID" -> testSessionId)
        .post(body).futureValue
    }

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

    def getMessagesCheck: WSResponse = get(s"/test-only/message-check")

    def getPaymentAllocationCharges(docNumber: String): WSResponse = get(s"/payment-made-to-hmrc?documentNumber=$docNumber")

    def getCreditsSummary(calendarYear: String): WSResponse = get(s"/credits-from-hmrc/$calendarYear")

    def getRefundToTaxPayer(repaymentRequestNumber: String): WSResponse = get(s"/refund-to-taxpayer/$repaymentRequestNumber ")

    def getCeaseUKProperty: WSResponse = get("/income-sources/cease/uk-property-declare")

    def getCeaseIncomeSourcesIndividual: WSResponse = get("/income-sources/cease/cease-an-income-source")

    def postCeaseUKProperty(answer: Option[String]): WSResponse = post("/income-sources/cease/uk-property-declare")(
      answer.fold(Map.empty[String, Seq[String]])(
        declaration => DeclarePropertyCeasedForm.form(UkProperty).fill(DeclarePropertyCeasedForm(Some(declaration), "csrfToken")).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def getBusinessEndDate: WSResponse = get(s"/income-sources/cease/business-end-date?id=$testSelfEmploymentIdHashed")

    def getUKPropertyEndDate: WSResponse = get("/income-sources/cease/uk-property-end-date")

    def getCeaseForeignProperty: WSResponse = get("/income-sources/cease/foreign-property-declare")

    def postCeaseForeignProperty(answer: Option[String]): WSResponse = post("/income-sources/cease/foreign-property-declare")(
      answer.fold(Map.empty[String, Seq[String]])(
        declaration => DeclarePropertyCeasedForm.form(ForeignProperty).fill(DeclarePropertyCeasedForm(Some(declaration), "csrfToken")).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def getForeignPropertyEndDate: WSResponse = get("/income-sources/cease/foreign-property-end-date")

    def getForeignPropertyAddedObligations: WSResponse = {
      get(
        uri = s"/income-sources/add/foreign-property-added"
      )
    }


    def getAddBusinessStartDateCheckChange(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(
        uri = "/income-sources/add/change-business-start-date-check", additionalCookies
      )
    }

    def getAddForeignPropertyStartDateCheckChange(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(
        uri = "/income-sources/add/change-foreign-property-start-date-check", additionalCookies
      )
    }

    def getAddUKPropertyStartDateCheckChange(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(
        uri = "/income-sources/add/change-uk-property-start-date-check", additionalCookies
      )
    }

    def postForeignPropertyAddedObligations(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/income-sources/add/foreign-property-added", additionalCookies)(Map.empty)
    }

    def getAddBusinessName: WSResponse = getWithHeaders("/income-sources/add/business-name",
      "X-Session-ID" -> testSessionId)

    def postAddBusinessName(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/income-sources/add/business-name", additionalCookies)(Map.empty)
    }

    def getAddBusinessTrade: WSResponse = getWithHeaders("/income-sources/add/business-trade",
      "X-Session-ID" -> testSessionId)

    def postAddBusinessTrade(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/income-sources/add/business-trade", additionalCookies)(Map.empty)
    }

    def getChangeAddBusinessTrade: WSResponse = getWithHeaders("/income-sources/add/change-business-trade",
      "X-Session-ID" -> testSessionId)

    def getAddBusinessStartDate: WSResponse = get("/income-sources/add/business-start-date")

    def getAddBusinessStartDateCheck(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(
        uri = "/income-sources/add/business-start-date-check", additionalCookies
      )
    }

    def postAddBusinessStartDateCheck(answer: Option[String])(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/income-sources/add/business-start-date-check",
        additionalCookies = additionalCookies)(
        answer.fold(Map.empty[String, Seq[String]])(
          selection => AddIncomeSourceStartDateCheckForm(SelfEmployment.addStartDateCheckMessagesPrefix)
            .fill(AddIncomeSourceStartDateCheckForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def postAddForeignPropertyStartDateCheck(answer: Option[String])(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(
        uri = s"/income-sources/add/foreign-property-start-date-check",
        additionalCookies = additionalCookies
      )(
        answer.fold(Map.empty[String, Seq[String]])(
          selection => AddIncomeSourceStartDateCheckForm(ForeignProperty.addStartDateCheckMessagesPrefix)
            .fill(AddIncomeSourceStartDateCheckForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def postAddBusinessStartDateCheckChange(answer: Option[String])(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(
        uri = s"/income-sources/add/change-business-start-date-check",
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
        uri = s"/income-sources/add/change-uk-property-start-date-check",
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
        uri = s"/income-sources/add/change-foreign-property-start-date-check",
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
        uri = s"/income-sources/add/uk-property-start-date-check",
        additionalCookies = additionalCookies
      )(
        answer.fold(Map.empty[String, Seq[String]])(
          selection => AddIncomeSourceStartDateCheckForm(UkProperty.addStartDateCheckMessagesPrefix)
            .fill(AddIncomeSourceStartDateCheckForm(Some(selection))).data.map { case (k, v) => (k, Seq(v)) }
        )
      )
    }

    def getAddBusinessObligations: WSResponse = {
      get(
        uri = s"/income-sources/add/business-added"
      )
    }

    def postAddedBusinessObligations(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/income-sources/add/business-added", additionalCookies)(Map.empty)
    }

    def getCheckCeaseUKPropertyDetails: WSResponse =
      getWithClientDetailsInSession("/income-sources/cease/uk-property-check-details")

    def postCheckCeaseUKPropertyDetails: WSResponse =
      post("/income-sources/cease/uk-property-check-details")(Map.empty)

    def getManageIncomeSource: WSResponse = get("/income-sources/manage/view-and-manage-income-sources")

    def getConfirmSoleTraderBusinessReportingMethod(taxYear: String, changeTo: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(s"/income-sources/manage/confirm-you-want-to-report?id=$testSelfEmploymentId&taxYear=$taxYear&changeTo=$changeTo", additionalCookies)
    }

    def getConfirmUKPropertyReportingMethod(taxYear: String, changeTo: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(s"/income-sources/manage/confirm-you-want-to-report-uk-property?taxYear=$taxYear&changeTo=$changeTo", additionalCookies)
    }

    def getConfirmForeignPropertyReportingMethod(taxYear: String, changeTo: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(s"/income-sources/manage/confirm-you-want-to-report-foreign-property?taxYear=$taxYear&changeTo=$changeTo", additionalCookies)
    }

    def postConfirmSoleTraderBusinessReportingMethod(taxYear: String, changeTo: String, additionalCookies: Map[String, String] = Map.empty)(formData: Map[String, Seq[String]]): WSResponse = {
      post(s"/income-sources/manage/confirm-you-want-to-report?id=$testSelfEmploymentId&taxYear=$taxYear&changeTo=$changeTo", additionalCookies)(formData)
    }

    def postConfirmUKPropertyReportingMethod(taxYear: String, changeTo: String, additionalCookies: Map[String, String] = Map.empty)(formData: Map[String, Seq[String]]): WSResponse = {
      post(s"/income-sources/manage/confirm-you-want-to-report-uk-property?id=$testPropertyIncomeId&taxYear=$taxYear&changeTo=$changeTo", additionalCookies)(formData)
    }

    def postConfirmForeignPropertyReportingMethod(taxYear: String, changeTo: String, additionalCookies: Map[String, String] = Map.empty)(formData: Map[String, Seq[String]]): WSResponse = {
      post(s"/income-sources/manage/confirm-you-want-to-report-foreign-property?id=$testPropertyIncomeId&taxYear=$taxYear&changeTo=$changeTo", additionalCookies)(formData)
    }

    def getManageSEObligations(changeTo: String, taxYear: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(s"/income-sources/manage/business-will-report?changeTo=$changeTo&taxYear=$taxYear", additionalCookies)
    }

    def getManageUKObligations(changeTo: String, taxYear: String): WSResponse = {
      get(s"/income-sources/manage/uk-property-will-report?changeTo=$changeTo&taxYear=$taxYear")
    }

    def getManageFPObligations(changeTo: String, taxYear: String): WSResponse = {
      get(s"/income-sources/manage/foreign-property-will-report?changeTo=$changeTo&taxYear=$taxYear")
    }

    def postManageObligations(mode: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/income-sources/manage/$mode-will-report", additionalCookies)(Map.empty)
    }

    def getCheckCeaseForeignPropertyDetails: WSResponse =
      getWithClientDetailsInSession("/income-sources/cease/foreign-property-check-details")

    def postCheckCeaseForeignPropertyDetails: WSResponse =
      post(s"/income-sources/cease/foreign-property-check-details/")(Map.empty)


    def postAddBusinessReportingMethod(form: IncomeSourceReportingMethodForm)(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      val formData = form.toFormMap.map { case (k, v) => (k -> Seq(v.getOrElse(""))) }
      post(s"/income-sources/add/business-reporting-method?id=$testSelfEmploymentId", additionalCookies = additionalCookies)(formData)
    }

    def postAddUKPropertyReportingMethod(form: IncomeSourceReportingMethodForm)(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      val formData = form.toFormMap.map { case (k, v) => (k -> Seq(v.getOrElse(""))) }
      post(s"/income-sources/add/uk-property-reporting-method?id=$testPropertyIncomeId", additionalCookies = additionalCookies)(formData)
    }

    def postAddForeignPropertyReportingMethod(form: IncomeSourceReportingMethodForm)(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      val formData = form.toFormMap.map { case (k, v) => (k -> Seq(v.getOrElse(""))) }
      post(s"/income-sources/add/foreign-property-reporting-method?id=$testPropertyIncomeId", additionalCookies = additionalCookies)(formData)
    }

    def getCheckCeaseBusinessDetails: WSResponse =
      getWithClientDetailsInSession("/income-sources/cease/business-check-details")

    def postCheckCeaseBusinessDetails: WSResponse =
      post("/income-sources/cease/business-check-details")(Map.empty)

    def getForeignPropertyCeasedObligations(session: Map[String, String]): WSResponse = get(uri = "/income-sources/cease/cease-foreign-property-success", session)

    def getUkPropertyCeasedObligations(session: Map[String, String]): WSResponse = get(uri = "/income-sources/cease/cease-uk-property-success", session)

    def getBusinessCeasedObligations: WSResponse = get(uri = "/income-sources/cease/cease-business-success")

    def getAddChangeBusinessAddress: WSResponse =
      get("/income-sources/add/change-business-address-lookup")

    def getAddBusinessAddress: WSResponse =
      get("/income-sources/add/business-address")

    def getSEReportingMethodNotSaved(session: Map[String, String]): WSResponse = get(uri = s"/income-sources/add/error-business-reporting-method-not-saved", session)

    def getUkPropertyReportingMethodNotSaved(session: Map[String, String]): WSResponse = get(uri = s"/income-sources/add/error-uk-property-reporting-method-not-saved", session)

    def getForeignPropertyReportingMethodNotSaved(session: Map[String, String]): WSResponse = get(uri = s"/income-sources/add/error-foreign-property-reporting-method-not-saved", session)

    def getAddIncomeSource(): WSResponse = get(uri = s"/income-sources/add/new-income-sources")

    def getCeaseSECannotGoBack(): WSResponse = get("/income-sources/cease/cease-business-cannot-go-back")

    def getCeaseUKCannotGoBack(): WSResponse = get("/income-sources/cease/cease-uk-property-cannot-go-back")

    def getCeaseFPCannotGoBack(): WSResponse = get("/income-sources/cease/cease-foreign-property-cannot-go-back")

    def getManageSECannotGoBack: WSResponse = get(s"/income-sources/manage/manage-business-cannot-go-back")

    def getManageUKPropertyCannotGoBack: WSResponse = get(s"/income-sources/manage/manage-uk-property-cannot-go-back")

    def getManageForeignPropertyCannotGoBack: WSResponse = get(s"/income-sources/manage/manage-foreign-property-cannot-go-back")
  }


  // TODO: Replace IncomeTaxViewChangeFrontend with this implementation
  object IncomeTaxViewChangeFrontendManageBusinesses {
    def get(uri: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      When(s"I call GET /report-quarterly/income-and-expenses/view" + uri)
      buildClient(uri)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(additionalCookies), "X-Session-ID" -> testSessionId)
        .get().futureValue
    }

    def getWithHeaders(uri: String, headers: (String, String)*): WSResponse = {
      buildClient(uri)
        .withHttpHeaders(headers: _*)
        .get().futureValue
    }

    def post(uri: String, additionalCookies: Map[String, String] = Map.empty)(body: Map[String, Seq[String]]): WSResponse = {
      When(s"I call POST /report-quarterly/income-and-expenses/view" + uri)
      buildClient(uri)
        .withFollowRedirects(false)
        .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(additionalCookies),
          "Csrf-Token" -> "nocheck",
          "X-Session-ID" -> testSessionId)
        .post(body).futureValue
    }

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

    def getMessagesCheck: WSResponse = get(s"/test-only/message-check")

    def getPaymentAllocationCharges(docNumber: String): WSResponse = get(s"/payment-made-to-hmrc?documentNumber=$docNumber")

    def getCreditsSummary(calendarYear: String): WSResponse = get(s"/credits-from-hmrc/$calendarYear")

    def getRefundToTaxPayer(repaymentRequestNumber: String): WSResponse = get(s"/refund-to-taxpayer/$repaymentRequestNumber ")

    def getCeaseUKProperty: WSResponse = get("/manage-your-businesses/cease/uk-property-declare")

    def getCeaseIncomeSourcesIndividual: WSResponse = get("/manage-your-businesses/cease/cease-an-income-source")

    def postCeaseUKProperty(answer: Option[String]): WSResponse = post("/manage-your-businesses/cease/uk-property-declare")(
      answer.fold(Map.empty[String, Seq[String]])(
        declaration => DeclarePropertyCeasedForm.form(UkProperty).fill(DeclarePropertyCeasedForm(Some(declaration), "csrfToken")).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def getBusinessEndDate: WSResponse = get(s"/manage-your-businesses/cease/business-end-date?id=$testSelfEmploymentIdHashed")

    def getUKPropertyEndDate: WSResponse = get("/manage-your-businesses/cease/uk-property-end-date")

    def getCeaseForeignProperty: WSResponse = get("/manage-your-businesses/cease/foreign-property-declare")

    def postCeaseForeignProperty(answer: Option[String]): WSResponse = post("/manage-your-businesses/cease/foreign-property-declare")(
      answer.fold(Map.empty[String, Seq[String]])(
        declaration => DeclarePropertyCeasedForm.form(ForeignProperty).fill(DeclarePropertyCeasedForm(Some(declaration), "csrfToken")).data.map { case (k, v) => (k, Seq(v)) }
      )
    )

    def getForeignPropertyEndDate: WSResponse = get("/manage-your-businesses/cease/foreign-property-end-date")

    def getForeignPropertyAddedObligations: WSResponse = {
      get(
        uri = s"/manage-your-businesses/add/foreign-property-added"
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

    def postForeignPropertyAddedObligations(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/manage-your-businesses/add/foreign-property-added", additionalCookies)(Map.empty)
    }

    def getAddBusinessName: WSResponse = getWithHeaders("/manage-your-businesses/add-sole-trader/business-name",
      "X-Session-ID" -> testSessionId)

    def postAddBusinessName(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"manage-your-businesses/add-sole-trader/business-name", additionalCookies)(Map.empty)
    }

    def getAddBusinessTrade: WSResponse = getWithHeaders("/manage-your-businesses/add-sole-trader/business-trade",
      "X-Session-ID" -> testSessionId)

    def postAddBusinessTrade(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/manage-your-businesses/add-sole-trader/business-trade", additionalCookies)(Map.empty)
    }

    def getChangeAddBusinessTrade: WSResponse = getWithHeaders("/manage-your-businesses/add-sole-trader/change-business-trade",
      "X-Session-ID" -> testSessionId)

    def getAddBusinessStartDate: WSResponse = get("/manage-your-businesses/add-sole-trader/business-start-date")

    def getAddBusinessStartDateCheck(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      get(
        uri = "/manage-your-businesses/add-sole-trader/business-start-date-check", additionalCookies
      )
    }

    def postAddBusinessStartDateCheck(answer: Option[String])(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/manage-your-businesses/add-sole-trader/business-start-date-check",
        additionalCookies = additionalCookies)(
        answer.fold(Map.empty[String, Seq[String]])(
          selection => AddIncomeSourceStartDateCheckForm(SelfEmployment.addStartDateCheckMessagesPrefix)
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

    def getAddBusinessObligations: WSResponse = {
      get(
        uri = s"/manage-your-businesses/add/business-added"
      )
    }

    def postAddedBusinessObligations(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/manage-your-businesses/add/business-added", additionalCookies)(Map.empty)
    }

    def getCheckCeaseUKPropertyDetails: WSResponse =
      getWithClientDetailsInSession("/manage-your-businesses/cease/uk-property-check-details")

    def postCheckCeaseUKPropertyDetails: WSResponse =
      post("/manage-your-businesses/cease/uk-property-check-details")(Map.empty)

    def getManageIncomeSource: WSResponse = get("/manage-your-businesses/manage/view-and-manage-income-sources")

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
      get(s"/manage-your-businesses/manage/business-will-report?changeTo=$changeTo&taxYear=$taxYear", additionalCookies)
    }

    def getManageUKObligations(changeTo: String, taxYear: String): WSResponse = {
      get(s"/manage-your-businesses/manage/uk-property-will-report?changeTo=$changeTo&taxYear=$taxYear")
    }

    def getManageFPObligations(changeTo: String, taxYear: String): WSResponse = {
      get(s"/manage-your-businesses/manage/foreign-property-will-report?changeTo=$changeTo&taxYear=$taxYear")
    }

    def postManageObligations(mode: String, additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      post(s"/manage-your-businesses/manage/$mode-will-report", additionalCookies)(Map.empty)
    }

    def getCheckCeaseForeignPropertyDetails: WSResponse =
      getWithClientDetailsInSession("/manage-your-businesses/cease/foreign-property-check-details")

    def postCheckCeaseForeignPropertyDetails: WSResponse =
      post(s"/manage-your-businesses/cease/foreign-property-check-details/")(Map.empty)


    def postAddBusinessReportingMethod(form: IncomeSourceReportingMethodForm)(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      val formData = form.toFormMap.map { case (k, v) => (k -> Seq(v.getOrElse(""))) }
      post(s"/manage-your-businesses/add/business-reporting-method?id=$testSelfEmploymentId", additionalCookies = additionalCookies)(formData)
    }

    def postAddUKPropertyReportingMethod(form: IncomeSourceReportingMethodForm)(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      val formData = form.toFormMap.map { case (k, v) => (k -> Seq(v.getOrElse(""))) }
      post(s"/manage-your-businesses/add/uk-property-reporting-method?id=$testPropertyIncomeId", additionalCookies = additionalCookies)(formData)
    }

    def postAddForeignPropertyReportingMethod(form: IncomeSourceReportingMethodForm)(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
      val formData = form.toFormMap.map { case (k, v) => (k -> Seq(v.getOrElse(""))) }
      post(s"/manage-your-businesses/add/foreign-property-reporting-method?id=$testPropertyIncomeId", additionalCookies = additionalCookies)(formData)
    }

    def getCheckCeaseBusinessDetails: WSResponse =
      getWithClientDetailsInSession("/manage-your-businesses/cease/business-check-details")

    def postCheckCeaseBusinessDetails: WSResponse =
      post("/manage-your-businesses/cease/business-check-details")(Map.empty)

    def getForeignPropertyCeasedObligations(session: Map[String, String]): WSResponse = get(uri = "/manage-your-businesses/cease/cease-foreign-property-success", session)

    def getUkPropertyCeasedObligations(session: Map[String, String]): WSResponse = get(uri = "/manage-your-businesses/cease/cease-uk-property-success", session)

    def getBusinessCeasedObligations: WSResponse = get(uri = "/manage-your-businesses/cease/cease-business-success")

    def getAddBusinessAddress: WSResponse =
      get("/manage-your-businesses/add-sole-trader/business-address")

    def getAddChangeBusinessAddress: WSResponse =
      get("/manage-your-businesses/add-sole-trader/change-business-address-lookup")

    def getSEReportingMethodNotSaved(session: Map[String, String]): WSResponse = get(uri = s"/manage-your-businesses/add/error-business-reporting-method-not-saved", session)

    def getUkPropertyReportingMethodNotSaved(session: Map[String, String]): WSResponse = get(uri = s"/manage-your-businesses/add/error-uk-property-reporting-method-not-saved", session)

    def getForeignPropertyReportingMethodNotSaved(session: Map[String, String]): WSResponse = get(uri = s"/manage-your-businesses/add/error-foreign-property-reporting-method-not-saved", session)

    def getAddIncomeSource(): WSResponse = get(uri = s"/manage-your-businesses/add/new-income-sources")

    def getCeaseSECannotGoBack(): WSResponse = get("/manage-your-businesses/cease/cease-business-cannot-go-back")

    def getCeaseUKCannotGoBack(): WSResponse = get("/manage-your-businesses/cease/cease-uk-property-cannot-go-back")

    def getCeaseFPCannotGoBack(): WSResponse = get("/manage-your-businesses/cease/cease-foreign-property-cannot-go-back")

    def getManageSECannotGoBack: WSResponse = get(s"/manage-your-businesses/manage/manage-business-cannot-go-back")

    def getManageUKPropertyCannotGoBack: WSResponse = get(s"/manage-your-businesses/manage/manage-uk-property-cannot-go-back")

    def getManageForeignPropertyCannotGoBack: WSResponse = get(s"/manage-your-businesses/manage/manage-foreign-property-cannot-go-back")

    def getManageYourBusinesses: WSResponse = get("/manage-your-businesses")
  }

  def unauthorisedTest(uri: String): Unit = {
    "unauthorised" should {

      "redirect to sign in" in {

        isAuthorisedUser(false)

        When(s"I call GET /report-quarterly/income-and-expenses/view$uri")
        val res = IncomeTaxViewChangeFrontendManageBusinesses.get(uri)

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

