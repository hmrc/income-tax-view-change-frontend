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
  val signInUrl: String
  val ggUrl: String
  val ggSignOutUrl: String
  val mtdItEnrolmentKey: String
  val mtdItIdentifierKey: String
  val ninoEnrolmentKey: String
  val ninoIdentifierKey: String
  val businessTaxAccount: String
  val btaManageAccountUrl: String
  val btaMessagesUrl: String
  val signUpUrl: String
  val selfAssessmentApi: String
}

@Singleton
class FrontendAppConfig @Inject()(configuration: Configuration) extends AppConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private lazy val baseUrl = "report-quarterly/income-and-expenses/view"
  private lazy val contactHost = loadConfig(s"contact-frontend.host")

  //Feedback Config
  private lazy val contactFrontendService = baseUrl("contact-frontend")
  override lazy val contactFormServiceIdentifier = "ITVC"
  override lazy val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  override lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override lazy val betaFeedbackUrl = s"$baseUrl/feedback"
  override lazy val betaFeedbackUnauthenticatedUrl: String = betaFeedbackUrl

  //GA
  override lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
  override lazy val analyticsHost: String = loadConfig(s"google-analytics.host")

  //GG Sign In via Company Auth Frontend
  override lazy val ggSignInContinueUrl: String = loadConfig("government-gateway.continue.url")
  override lazy val signInUrl: String = loadConfig("base.sign-in")

  //Sign out
  override lazy val ggUrl: String = loadConfig(s"government-gateway.url")
  override lazy val ggSignOutUrl = s"$ggUrl/gg/sign-out?continue=$signInUrl"

  //MTD Income Tax Enrolment
  override lazy val mtdItEnrolmentKey: String = loadConfig("enrolments.mtd.key")
  override lazy val mtdItIdentifierKey: String = loadConfig("enrolments.mtd.identifier")

  //NINO Enrolment
  override lazy val ninoEnrolmentKey: String = loadConfig("enrolments.nino.key")
  override lazy val ninoIdentifierKey: String = loadConfig("enrolments.nino.identifier")

  //Business Tax Account
  override lazy val businessTaxAccount: String = loadConfig("business-tax-account.url")
  override lazy val btaManageAccountUrl: String = s"$businessTaxAccount/manage-account"
  override lazy val btaMessagesUrl: String = s"$businessTaxAccount/messages"

  //Subscription Service
  override lazy val signUpUrl: String = loadConfig("mtd-subscription-service.url")

  //SelfAssessmentApi
  override lazy val selfAssessmentApi: String = baseUrl("self-assessment-api") + getConfString("self-assessment-api.contextRoute", throw new RuntimeException(s"sCould not find config self-assessment-api.contextRoute"))
}