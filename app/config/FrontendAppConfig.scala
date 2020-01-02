/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.i18n.Lang
import com.google.inject.Inject
import config.features.Features
import play.api.Mode.Mode
import play.api.mvc.Call
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject()(val environment: Environment,
                                  val conf: Configuration) extends ServicesConfig {

  override protected def runModeConfiguration: Configuration = conf
  override protected def mode: Mode = environment.mode
  private def loadConfig(key: String) = runModeConfiguration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  //App
  private lazy val baseUrl: String = "report-quarterly/income-and-expenses/view"
  lazy val itvcFrontendEnvironment: String = loadConfig("base.url")
  lazy val appName: String = loadConfig("appName")

  //Feedback Config
  private lazy val contactHost: String = loadConfig(s"contact-frontend.host")
  private lazy val contactFrontendService: String = baseUrl("contact-frontend")
  lazy val contactFormServiceIdentifier: String = "ITVC"
  lazy val contactFrontendPartialBaseUrl: String = s"$contactFrontendService"
  lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val betaFeedbackUrl = s"/$baseUrl/feedback"

  //SA-API Config
  lazy val saApiService: String = baseUrl("self-assessment-api")

  //ITVC Protected Service
  lazy val itvcProtectedService: String = baseUrl("income-tax-view-change")

  //GA
  lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
  lazy val analyticsHost: String = loadConfig(s"google-analytics.host")

  //GG Sign In via Company Auth Frontend
  lazy val ggSignInContinueUrl: String = loadConfig("government-gateway.continue.url")
  lazy val signInUrl: String = loadConfig("base.sign-in")

  //Exit Survey
  lazy val exitSurveyBaseUrl:String = loadConfig("feedback-survey-frontend.host") + loadConfig("feedback-survey-frontend.url")
  lazy val exitSurveyUrl = s"$exitSurveyBaseUrl/?origin=$contactFormServiceIdentifier"

  //Sign out
  lazy val ggUrl: String = loadConfig(s"government-gateway.url")
  lazy val ggSignOutUrl = s"$ggUrl/gg/sign-out?continue=$exitSurveyUrl"

  //MTD Income Tax Enrolment
  lazy val mtdItEnrolmentKey: String = loadConfig("enrolments.mtd.key")
  lazy val mtdItIdentifierKey: String = loadConfig("enrolments.mtd.identifier")

  //NINO Enrolment
  lazy val ninoEnrolmentKey: String = loadConfig("enrolments.nino.key")
  lazy val ninoIdentifierKey: String = loadConfig("enrolments.nino.identifier")

  //SA Enrolment
  lazy val saEnrolmentKey: String = loadConfig("enrolments.sa.key")
  lazy val saIdentifierKey: String = loadConfig("enrolments.sa.identifier")

  //Business Tax Account
  lazy val btaService: String = baseUrl("business-account")
  lazy val businessTaxAccount: String = loadConfig("business-tax-account.url")
  lazy val btaManageAccountUrl: String = s"$businessTaxAccount/manage-account"
  lazy val btaMessagesUrl: String = s"$businessTaxAccount/messages"
  lazy val selfAssessmentUrl: String = s"$businessTaxAccount/self-assessment"

  //Subscription Service
  lazy val signUpUrl: String = loadConfig("mtd-subscription-service.url")

  lazy val ftUrl: String = baseUrl("financial-transactions")

  lazy val paymentsUrl: String = baseUrl("pay-api")

  lazy val enterSurveyUrl: String = loadConfig("enter-survey.url")

  //Payment Redirect route
  lazy val paymentRedirectUrl: String = s"$itvcFrontendEnvironment/$baseUrl/payments-due"

  //Accounting software guidance
  lazy val accountingSoftwareLinkUrl: String = "https://www.gov.uk/guidance/use-software-to-send-income-tax-updates"

  //Translation
  def languageMap: Map[String, Lang] = Map(
      "english" -> Lang("en"),
      "cymraeg" -> Lang("cy")
  )

  def routeToSwitchLanguage: String => Call = (lang: String) => controllers.routes.ItvcLanguageController.switchToLanguage(lang)

  val features = new Features(runModeConfiguration)
}
