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

import auth.authV2.models.AuthorisedAndEnrolledRequest
import auth.{HeaderExtractor, MtdItUser}
import com.github.tomakehurst.wiremock.client.WireMock
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.AuditStub
import implicits.ImplicitDateFormatterImpl
import models.admin.FeatureSwitchName
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.cache.AsyncCacheApi
import play.api.i18n.{Lang, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.DefaultCookieSigner
import play.api.test.FakeRequest
import play.api.{Application, Environment, Mode}
import repositories.{OptOutSessionDataRepository, UIJourneySessionDataRepository}
import services.{DateService, DateServiceInterface}
import testConstants.BaseIntegrationTestConstants._
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, SessionId}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate
import java.time.Month.APRIL
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

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

  override def getCurrentDate: LocalDate = LocalDate.of(2023, 4, 5)

  override protected def now(): LocalDate = LocalDate.of(2023, 4, 5)

  override def isBeforeLastDayOfTaxYear: Boolean = true

  override def getCurrentTaxYearEnd: Int = 2023

  override def getCurrentTaxYearStart: LocalDate = LocalDate.of(2022, 4, 6)

  override def getCurrentTaxYear: TaxYear = TaxYear.forYearEnd(getCurrentTaxYearEnd)

  override def getAccountingPeriodEndDate(startDate: LocalDate): LocalDate = {
    val startDateYear = startDate.getYear
    val accountingPeriodEndDate = LocalDate.of(startDateYear, APRIL, 5)

    if (startDate.isBefore(accountingPeriodEndDate) || startDate.isEqual(accountingPeriodEndDate)) {
      accountingPeriodEndDate
    } else {
      accountingPeriodEndDate.plusYears(1)
    }
  }

  override def isAfterTaxReturnDeadlineButBeforeTaxYearEnd: Boolean = true

  override def isWithin30Days(date: LocalDate): Boolean = {
    val currentDate = getCurrentDate
    date.minusDays(30).isBefore(currentDate)
  }
}

trait ComponentSpecBase extends TestSuite with CustomMatchers
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with Eventually
  with FeatureSwitching with SessionCookieBaker {

  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: String = WiremockHelper.wiremockPort.toString
  val mockUrl: String = s"http://$mockHost:$mockPort"
  val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val cache: AsyncCacheApi = app.injector.instanceOf[AsyncCacheApi]
  val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]
  implicit val messagesAPI: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockLanguageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]
  implicit val lang: Lang = Lang("GB")
  implicit val mockImplicitDateFormatter: ImplicitDateFormatterImpl = new ImplicitDateFormatterImpl(mockLanguageUtils)
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))
  implicit val testAppConfig: FrontendAppConfig = appConfig
  implicit val optOutSessionDataRepository: OptOutSessionDataRepository = app.injector.instanceOf[OptOutSessionDataRepository]
  implicit val uiRepository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  implicit val dateService: DateService = new DateService()(frontendAppConfig = testAppConfig) {
    override def getCurrentDate: LocalDate = LocalDate.of(2023, 4, 5)

    override def getCurrentTaxYearEnd: Int = 2023
  }

  override val cookieSigner: DefaultCookieSigner = app.injector.instanceOf[DefaultCookieSigner]

  val titleInternalServer: String = "standardError.heading"
  val titleTechError = "Sorry, we are experiencing technical difficulties - 500"
  val titleNotFound = "Page not found - 404"
  val titleProbWithService = "Sorry, there is a problem with the service"
  val titleThereIsAProblem = "There’s a problem"
  implicit val csbTestUser: MtdItUser[_] = getTestUser(MTDIndividual,
    IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty))

  def getTestUser(mtdUserRole: MTDUserRole, incomeSources: IncomeSourceDetailsModel): MtdItUser[_] = {
    MtdItUser(
      testMtditid,
      testNino,
      mtdUserRole,
      defaultAuthUserDetails(mtdUserRole),
      if(mtdUserRole == MTDIndividual) None else Some(defaultClientDetails),
      incomeSources
    )(FakeRequest())
  }

  def getAuthorisedAndEnrolledUser(mtdUserRole: MTDUserRole): AuthorisedAndEnrolledRequest[_] = {
    AuthorisedAndEnrolledRequest(
      testMtditid,
      mtdUserRole,
      defaultAuthUserDetails(mtdUserRole),
      if(mtdUserRole == MTDIndividual) None else Some(defaultClientDetails)
    )(FakeRequest())
  }

  def config: Map[String, Object] = Map(
    "play.filters.disabled" -> Seq("uk.gov.hmrc.play.bootstrap.frontend.filters.SessionIdFilter"),
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.auth.host" -> mockHost,
    "microservice.services.auth.port" -> mockPort,
    "microservice.services.identity-verification-frontend.host" -> "http://stubIV.com",
    "microservice.services.income-tax-view-change.host" -> mockHost,
    "microservice.services.income-tax-view-change.port" -> mockPort,
    "microservice.services.self-assessment-api.host" -> mockHost,
    "microservice.services.self-assessment-api.port" -> mockPort,
    "microservice.services.business-account.host" -> mockHost,
    "microservice.services.business-account.port" -> mockPort,
    "microservice.services.financial-transactions.host" -> mockHost,
    "microservice.services.financial-transactions.port" -> mockPort,
    "microservice.services.repayment-api.host" -> mockHost,
    "microservice.services.repayment-api.port" -> mockPort,
    "microservice.services.pay-api.host" -> mockHost,
    "microservice.services.pay-api.port" -> mockPort,
    "microservice.services.income-tax-calculation.host" -> mockHost,
    "microservice.services.income-tax-calculation.port" -> mockPort,
    "microservice.services.income-tax-penalties-stub.host" -> mockHost,
    "microservice.services.income-tax-penalties-stub.port" -> mockPort,
    "microservice.services.penalties.host" -> mockHost,
    "microservice.services.penalties.port" -> mockPort,
    "microservice.services.penalties.host" -> mockHost,
    "microservice.services.stub.port" -> mockPort,
    "calculation-polling.interval" -> "500",
    "calculation-polling.timeout" -> "3000",
    "calculation-polling.attempts" -> "10",
    "calculation-polling.delayBetweenAttemptInMilliseconds" -> "500",
    "auditing.consumer.baseUri.host" -> mockHost,
    "auditing.consumer.baseUri.port" -> mockPort,
    "microservice.services.address-lookup-frontend.port" -> mockPort,
    "auditing.enabled" -> "true",
    "encryption.key" -> "QmFyMTIzNDVCYXIxMjM0NQ==",
    "encryption.isEnabled" -> "false",
    "microservice.services.non-repudiation.numberOfRetries" -> "10",
    "microservice.services.non-repudiation.host" -> mockHost,
    "microservice.services.non-repudiation.port" -> mockPort,
    "microservice.services.non-repudiation.xApiKey" -> "dummy-api-key",
    "microservice.services.contact-frontend.host" -> mockHost,
    "microservice.services.contact-frontend.port" -> mockPort,
    "microservice.services.income-tax-session-data.host" -> mockHost,
    "microservice.services.income-tax-session-data.port" -> mockPort,
    "microservice.services.citizen-details.host" -> mockHost,
    "microservice.services.citizen-details.port" -> mockPort,
    "microservice.services.set-up-a-payment-plan.host" -> mockHost,
    "microservice.services.set-up-a-payment-plan.port" -> mockPort,
    "feature-switches.read-from-mongo" -> "false",
    "feature-switch.enable-time-machine" -> "false",
    "time-machine.add-years" -> "0",
    "time-machine.add-days" -> "0"
  )

  val btaPartialUrl = "/business-account/partial/service-info"

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.of(2023, 4, 4)
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6)))
      LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }


  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
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
    AuditStub.stubAuditing()
    cache.removeAll()
    FeatureSwitchName.allFeatureSwitches foreach disable
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
    FeatureSwitchName.allFeatureSwitches foreach disable
  }
}

