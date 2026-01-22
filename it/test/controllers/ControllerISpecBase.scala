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

package controllers

import auth.HeaderExtractor
import config.FrontendAppConfig
import helpers.{SessionCookieBaker, TestDateService, TestHeaderExtractor, WiremockHelper}
import implicits.ImplicitDateFormatterImpl
import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.cache.AsyncCacheApi
import play.api.i18n.{Lang, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.DefaultCookieSigner
import play.api.{Application, Environment, Mode}
import repositories.{OptOutSessionDataRepository, UIJourneySessionDataRepository}
import services.{DateService, DateServiceInterface}
import testConstants.BaseIntegrationTestConstants.testSessionId
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext

trait ControllerISpecBase
  extends AnyWordSpecLike
    with OptionValues
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with WiremockHelper
    with GuiceOneServerPerSuite
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with Eventually
    with SessionCookieBaker {

  override val cookieSigner: DefaultCookieSigner = app.injector.instanceOf[DefaultCookieSigner]

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort.toString
  val mockUrl = s"http://$mockHost:$mockPort"

  val testAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val cache: AsyncCacheApi = app.injector.instanceOf[AsyncCacheApi]
  val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]
  val messagesAPI: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockLanguageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]

  implicit val lang: Lang = Lang("GB")

  implicit val mockImplicitDateFormatter: ImplicitDateFormatterImpl =
    new ImplicitDateFormatterImpl(mockLanguageUtils)

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  implicit val optOutSessionDataRepository: OptOutSessionDataRepository = app.injector.instanceOf[OptOutSessionDataRepository]
  implicit val uiRepository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  implicit val dateService: DateService =
    new DateService()(frontendAppConfig = testAppConfig) {

      override def getCurrentDate: LocalDate = LocalDate.of(2023, 4, 5)

      override def getCurrentTaxYearEnd: Int = 2023

    }

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .in(Environment.simple(mode = Mode.Dev))
      .overrides(bind[HeaderExtractor].to[TestHeaderExtractor])
      .overrides(bind[DateServiceInterface].to[TestDateService])
      .configure(config)
      .build()


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
    "microservice.services.address-lookup-frontend.port" -> mockPort,
    "encryption.key" -> "QmFyMTIzNDVCYXIxMjM0NQ==",
    "encryption.isEnabled" -> "false",
    "microservice.services.contact-frontend.host" -> mockHost,
    "microservice.services.contact-frontend.port" -> mockPort,
    "feature-switches.read-from-mongo" -> "false",
    "feature-switch.enable-time-machine" -> "false",
    "time-machine.add-years" -> "0",
    "time-machine.add-days" -> "0"
  )


}