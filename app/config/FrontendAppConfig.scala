/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject()(val servicesConfig: ServicesConfig, val config: Configuration) {


  //App
  private lazy val baseUrl: String = "report-quarterly/income-and-expenses/view"
  lazy val itvcFrontendEnvironment: String = servicesConfig.getString("base.url")
  lazy val appName: String = servicesConfig.getString("appName")

  //Feedback Config
  private lazy val contactHost: String = servicesConfig.getString(s"contact-frontend.host")
  private lazy val contactFrontendService: String = servicesConfig.baseUrl("contact-frontend")
  lazy val contactFormServiceIdentifier: String = "ITVC"
  lazy val contactFrontendPartialBaseUrl: String = s"$contactFrontendService"
  lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val betaFeedbackUrl = s"/$baseUrl/feedback"

  //SA-API Config
  lazy val saApiService: String = servicesConfig.baseUrl("self-assessment-api")

  //ITVC Protected Service
  lazy val itvcProtectedService: String = servicesConfig.baseUrl("income-tax-view-change")

  //Individual Calculation Service
  lazy val individualCalculationsService: String = servicesConfig.baseUrl("individual-calculations")

  //GG Sign In via Company Auth Frontend
  lazy val ggSignInContinueUrl: String = servicesConfig.getString("government-gateway.continue.url")
  lazy val signInUrl: String = servicesConfig.getString("base.sign-in")

  //Exit Survey
  lazy val exitSurveyBaseUrl:String = servicesConfig.getString("feedback-frontend.host") + servicesConfig.getString("feedback-frontend.url")
  lazy val exitSurveyUrl = s"$exitSurveyBaseUrl/$contactFormServiceIdentifier"

  //Sign out
  lazy val ggUrl: String = servicesConfig.getString(s"government-gateway.url")
  lazy val ggSignOutUrl = s"$ggUrl/gg/sign-out?continue=$exitSurveyUrl"

  //MTD Income Tax Enrolment
  lazy val mtdItEnrolmentKey: String = servicesConfig.getString("enrolments.mtd.key")
  lazy val mtdItIdentifierKey: String = servicesConfig.getString("enrolments.mtd.identifier")

  //NINO Enrolment
  lazy val ninoEnrolmentKey: String = servicesConfig.getString("enrolments.nino.key")
  lazy val ninoIdentifierKey: String = servicesConfig.getString("enrolments.nino.identifier")

  //ARN Enrolment
  lazy val arnEnrolmentKey: String = servicesConfig.getString("enrolments.arn.key")
  lazy val arnIdentifierKey: String = servicesConfig.getString("enrolments.arn.identifier")

  //SA Enrolment
  lazy val saEnrolmentKey: String = servicesConfig.getString("enrolments.sa.key")
  lazy val saIdentifierKey: String = servicesConfig.getString("enrolments.sa.identifier")

  //Business Tax Account
  lazy val btaService: String = servicesConfig.baseUrl("business-account")
  lazy val businessTaxAccount: String = servicesConfig.getString("business-tax-account.url")
  lazy val btaManageAccountUrl: String = s"$businessTaxAccount/manage-account"
  lazy val btaMessagesUrl: String = s"$businessTaxAccount/messages"
  lazy val selfAssessmentUrl: String = s"$businessTaxAccount/self-assessment"

  //Subscription Service
  lazy val signUpUrl: String = servicesConfig.getString("mtd-subscription-service.url")

  lazy val ftUrl: String = servicesConfig.baseUrl("financial-transactions")

  lazy val citizenDetailsUrl: String = servicesConfig.baseUrl("citizen-details")

  lazy val paymentsUrl: String = servicesConfig.baseUrl("pay-api")

  lazy val enterSurveyUrl: String = servicesConfig.getString("enter-survey.url")

  lazy val agentClientRelationshipUrl: String = servicesConfig.baseUrl("agent-client-relationships")

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

}
