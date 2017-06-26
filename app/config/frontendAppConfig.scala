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

package config

import javax.inject.Singleton
import com.google.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {
  val analyticsToken: String
  val analyticsHost: String
  val contactFormServiceIdentifier: String
  val contactFrontendPartialBaseUrl: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val betaFeedbackUrl: String
  val betaFeedbackUnauthenticatedUrl: String
  val ggSignInContinueUrl: String
  val ggUrl: String
  val ggSignOutUrl: String
  val mtdItEnrolmentKey: String
  val mtdItIdentifierKey: String
  val ninoEnrolmentKey: String
  val ninoIdentifierKey: String
  val businessTaxAccount: String
}

@Singleton
class FrontendAppConfig @Inject()(configuration: Configuration) extends AppConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private lazy val baseUrl = "check-your-income-tax-and-expenses"
  private lazy val contactHost = configuration.getString(s"contact-frontend.host").getOrElse("")

  //Feedback Config
  private lazy val contactFrontendService = baseUrl("contact-frontend")
  override lazy val contactFormServiceIdentifier = "ITVC"
  override lazy val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  override lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override lazy val betaFeedbackUrl = s"$baseUrl/feedback"
  override lazy val betaFeedbackUnauthenticatedUrl = betaFeedbackUrl

  //GA
  override lazy val analyticsToken = loadConfig(s"google-analytics.token")
  override lazy val analyticsHost = loadConfig(s"google-analytics.host")

  //GG Sign In via Company Auth Fronetned
  override lazy val ggSignInContinueUrl = loadConfig("government-gateway.continue.url")

  //Sign out
  override lazy val ggUrl = loadConfig(s"government-gateway.url")
  override lazy val ggSignOutUrl = s"$ggUrl/gg/sign-out?continue=$ggSignInContinueUrl"

  //MTD Income Tax Enrolment
  override lazy val mtdItEnrolmentKey: String = loadConfig("enrolments.mtd.key")
  override lazy val mtdItIdentifierKey: String = loadConfig("enrolments.mtd.identifier")

  //NINO Enrolment
  override lazy val ninoEnrolmentKey: String = loadConfig("enrolments.nino.key")
  override lazy val ninoIdentifierKey: String = loadConfig("enrolments.nino.identifier")

  //Business Tax Account
  override lazy val businessTaxAccount: String = s"${baseUrl("business-tax-account")}/business-account"
}
