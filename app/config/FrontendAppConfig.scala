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

  lazy val hasEnabledTestOnlyRoutes: Boolean = config.get[String]("play.http.router") == "testOnlyDoNotUseInAppConf.Routes"

  //App
  lazy val baseUrl: String = "report-quarterly/income-and-expenses/view"
  lazy val agentBaseUrl: String = s"$baseUrl/agents"
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

	//View L&P
  def saViewLandPService(utr: String): String = servicesConfig.getString("old-sa-viewer-frontend.host") + s"/$utr/account"

  //GG Sign In via BAS Gateway
  lazy val signInUrl: String = servicesConfig.getString("base.sign-in")
  lazy val ggSignInUrl: String = servicesConfig.getString("government-gateway.sign-in.url")
  lazy val homePageUrl: String = servicesConfig.getString("base.fullUrl")

  //Exit Survey
  lazy val exitSurveyBaseUrl:String = servicesConfig.getString("feedback-frontend.host") + servicesConfig.getString("feedback-frontend.url")
  def exitSurveyUrl(identifier: String): String = s"$exitSurveyBaseUrl/$identifier"

  //Sign out
  lazy val ggUrl: String = servicesConfig.getString(s"government-gateway.url")
  def ggSignOutUrl(identifier: String): String = s"$ggUrl/bas-gateway/sign-out-without-state?continue=${exitSurveyUrl(identifier)}"

  //MTD Income Tax Enrolment
  lazy val mtdItEnrolmentKey: String = servicesConfig.getString("enrolments.mtd.key")
  lazy val mtdItIdentifierKey: String = servicesConfig.getString("enrolments.mtd.identifier")

  //ARN Enrolment
  lazy val arnEnrolmentKey: String = servicesConfig.getString("enrolments.arn.key")
  lazy val arnIdentifierKey: String = servicesConfig.getString("enrolments.arn.identifier")

  //NINO Enrolment
  lazy val ninoEnrolmentKey: String = servicesConfig.getString("enrolments.nino.key")
  lazy val ninoIdentifierKey: String = servicesConfig.getString("enrolments.nino.identifier")

  //SA Enrolment
  lazy val saEnrolmentKey: String = servicesConfig.getString("enrolments.sa.key")
  lazy val saIdentifierKey: String = servicesConfig.getString("enrolments.sa.identifier")

  //Business Tax Account
  lazy val btaService: String = servicesConfig.baseUrl("business-account")
  lazy val businessTaxAccount: String = servicesConfig.getString("business-tax-account.url")
  lazy val btaManageAccountUrl: String = s"$businessTaxAccount/manage-account"
  lazy val btaMessagesUrl: String = s"$businessTaxAccount/messages"
  lazy val selfAssessmentUrl: String = s"$businessTaxAccount/self-assessment"

  //Agent Services Account
  lazy val setUpAgentServicesAccountUrl: String = servicesConfig.getString("set-up-agent-services-account.url")

  //Subscription Service
  lazy val signUpUrl: String = servicesConfig.getString("mtd-subscription-service.url")

  lazy val ftUrl: String = servicesConfig.baseUrl("financial-transactions")

  lazy val citizenDetailsUrl: String = servicesConfig.baseUrl("citizen-details")

  lazy val paymentsUrl: String = servicesConfig.baseUrl("pay-api")

  lazy val enterSurveyUrl: String = servicesConfig.getString("enter-survey.url")

  lazy val paymentHistoryLimit: Int = config.get[Int]("payment-history.number-of-years")

  lazy val repaymentUrl: String = servicesConfig.baseUrl("repayment")

  //Payment Redirect route
  lazy val paymentRedirectUrl: String = s"$itvcFrontendEnvironment/$baseUrl/payments-owed"
  //Payment Redirect route
  lazy val agentPaymentRedirectUrl: String = s"$itvcFrontendEnvironment/$agentBaseUrl/payments-owed"

  //Accounting software guidance
  lazy val accountingSoftwareLinkUrl: String = "https://www.gov.uk/guidance/use-software-to-send-income-tax-updates"

  lazy val clientAuthorisationGuidance: String = "https://www.gov.uk/guidance/client-authorisation-an-overview"

  //Calculation Polling config
  lazy val calcPollSchedulerInterval: Int = servicesConfig.getInt("calculation-polling.interval")
  lazy val calcPollSchedulerTimeout: Int = servicesConfig.getInt("calculation-polling.timeout")

  // Submission service
  // This URL has a set year and environment. Please use submissionFrontendTaxOverviewUrl instead.
  lazy val submissionFrontendUrl: String = servicesConfig.getString("income-tax-submission-frontend.url")
  lazy val submissionFrontendTaxOverviewUrl: Int => String = taxYear =>
    servicesConfig.getString("income-tax-submission-frontend.host") + s"/update-and-submit-income-tax-return/$taxYear/view"

  lazy val submissionFrontendFinalDeclarationUrl: Int => String = taxYear =>
    servicesConfig.getString("income-tax-submission-frontend.host") + s"/update-and-submit-income-tax-return/$taxYear/declaration"

  lazy val submissionFrontendTaxYearsPage: Int => String = taxYear =>
    servicesConfig.getString("income-tax-submission-frontend.host") + s"/update-and-submit-income-tax-return/$taxYear/start"

  // Disagree with a tax decision
  lazy val taxAppealsUrl: String = servicesConfig.getString("tax-appeals.url")

  //Translation
  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

	//Auth variables
	lazy val requiredConfidenceLevel: Int = servicesConfig.getInt("auth.confidenceLevel")

  def routeToSwitchLanguage: String => Call = (lang: String) => controllers.routes.ItvcLanguageController.switchToLanguage(lang)

  def routeToSwitchAgentLanguage: String => Call = (lang: String) => controllers.agent.routes.AgentLanguageController.switchToLanguage(lang)

}
