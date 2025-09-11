/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.Configuration
import play.api.i18n.{Lang, Messages}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Singleton

@Singleton
class FrontendAppConfig @Inject()(val servicesConfig: ServicesConfig, val config: Configuration) {

  lazy val hasEnabledTestOnlyRoutes: Boolean = config.get[String]("play.http.router") == "testOnlyDoNotUseInAppConf.Routes"

  //App
  lazy val baseUrl: String = "report-quarterly/income-and-expenses/view"
  lazy val agentBaseUrl: String = s"$baseUrl/agents"
  lazy val itvcFrontendEnvironment: String = servicesConfig.getString("base.url")
  lazy val appName: String = servicesConfig.getString("appName")

  //Feedback Config
  private lazy val contactHost: String = servicesConfig.getString("contact-frontend.host")
  private lazy val contactFrontendService: String = servicesConfig.baseUrl("contact-frontend")
  lazy val contactFormServiceIdentifier: String = "ITVC"
  lazy val contactFrontendBaseUrl: String = s"$contactFrontendService"
  lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val betaFeedbackUrl = s"/$baseUrl/feedback"
  lazy val agentBetaFeedbackUrl = s"/$agentBaseUrl/feedback"

  //ITVC Protected Service
  lazy val itvcProtectedService: String = servicesConfig.baseUrl("income-tax-view-change")

  //Income tax calculation service
  lazy val incomeTaxCalculationService: String = servicesConfig.baseUrl("income-tax-calculation")

  //Address lookup service
  lazy val addressLookupService: String = servicesConfig.baseUrl("address-lookup-frontend")

  //View L&P
  def saViewLandPService(utr: String): String = servicesConfig.getString("old-sa-viewer-frontend.host") + s"/$utr/account"

  //GG Sign In via BAS Gateway
  lazy val signInUrl: String = servicesConfig.getString("base.sign-in")
  lazy val ggSignInUrl: String = servicesConfig.getString("government-gateway.sign-in.url")
  lazy val homePageUrl: String = servicesConfig.getString("base.fullUrl")

  //Exit Survey
  lazy val exitSurveyBaseUrl: String = servicesConfig.getString("feedback-frontend.host") + servicesConfig.getString("feedback-frontend.url")

  def exitSurveyUrl(identifier: String): String = s"$exitSurveyBaseUrl/$identifier"

  //Sign out
  lazy val ggUrl: String = servicesConfig.getString("government-gateway.url")

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
  lazy val personalTaxAccount: String = servicesConfig.getString("personal-tax-account.url")
  lazy val btaManageAccountUrl: String = s"$businessTaxAccount/manage-account"
  lazy val btaMessagesUrl: String = s"$businessTaxAccount/messages"
  lazy val selfAssessmentUrl: String = s"$businessTaxAccount/self-assessment"

  // NRS
  lazy val nrsBaseUrl: String = servicesConfig.baseUrl("non-repudiation")
  lazy val nrsApiKey: String = servicesConfig.getString("microservice.services.non-repudiation.xApiKey")
  lazy val nrsRetries: Int = config.get[Int]("microservice.services.non-repudiation.numberOfRetries")

  //Agent Services Account
  lazy val setUpAgentServicesAccountUrl: String = servicesConfig.getString("set-up-agent-services-account.url")

  //Subscription Service
  lazy val signUpUrl: String = servicesConfig.getString("mtd-subscription-service.url")

  lazy val ftUrl: String = servicesConfig.baseUrl("financial-transactions")

  lazy val citizenDetailsUrl: String = servicesConfig.baseUrl("citizen-details")

  lazy val paymentsUrl: String = servicesConfig.baseUrl("pay-api")

  lazy val setUpAPaymentPlanUrl: String = servicesConfig.baseUrl("set-up-a-payment-plan")

  lazy val enterSurveyUrl: String = servicesConfig.getString("enter-survey.url")

  lazy val paymentHistoryLimit: Int = config.get[Int]("payment-history.number-of-years")

  lazy val repaymentsUrl: String = servicesConfig.baseUrl("repayment-api")

  //Payment Redirect route
  lazy val paymentRedirectUrl: String = s"$itvcFrontendEnvironment/$baseUrl/what-you-owe"
  //Payment Redirect route
  lazy val agentPaymentRedirectUrl: String = s"$itvcFrontendEnvironment/$agentBaseUrl/payments-owed"

  //Calculation Polling config
  lazy val calcPollSchedulerInterval: Int = servicesConfig.getInt("calculation-polling.interval")
  lazy val calcPollSchedulerTimeout: Int = servicesConfig.getInt("calculation-polling.timeout")
  lazy val calcPollNumberOfAttempts: Int = servicesConfig.getInt("calculation-polling.attempts")
  lazy val calcPollDelayBetweenAttempts: Int = servicesConfig.getInt("calculation-polling.delayBetweenAttemptInMilliseconds")

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

  //Tax account router url
  lazy val taxAccountRouterUrl: String = servicesConfig.getString("tax-account-router.url")

  // income-tax-session-data url
  lazy val incomeTaxSessionDataUrl: String = servicesConfig.baseUrl("income-tax-session-data")

  // penalties stub url
  lazy val incomeTaxPenaltiesStubBase: String = servicesConfig.baseUrl("income-tax-penalties-stub") + "/income-tax-penalties-stubs"

  lazy val penaltiesBackendBase: String = servicesConfig.baseUrl("penalties")

  //penalties frontend
  lazy val incomeTaxPenaltiesFrontend: String = servicesConfig.getString("income-tax-penalties-frontend.homeUrl")
  lazy val incomeTaxPenaltiesFrontendLPP1Calculation: String => String = chargeRef => servicesConfig.getString("income-tax-penalties-frontend.homeUrl") + s"/first-lpp-calculation?penaltyId=$chargeRef"
  lazy val incomeTaxPenaltiesFrontendLPP1CalculationAgent: String => String = chargeRef => servicesConfig.getString("income-tax-penalties-frontend.homeUrl") + s"/agent-first-lpp-calculation?penaltyId=$chargeRef"
  lazy val incomeTaxPenaltiesFrontendLPP2Calculation: String => String = chargeRef => servicesConfig.getString("income-tax-penalties-frontend.homeUrl") + s"/second-lpp-calculation?penaltyId=$chargeRef"
  lazy val incomeTaxPenaltiesFrontendLPP2CalculationAgent: String => String = chargeRef => servicesConfig.getString("income-tax-penalties-frontend.homeUrl") + s"/agent-second-lpp-calculation?penaltyId=$chargeRef"


  // API timeout

  lazy val claimToAdjustTimeout: Int = servicesConfig.getInt("claim-to-adjust.timeout")

  // enrolment-store-proxy url
  lazy val enrolmentStoreProxyUrl: String = servicesConfig.baseUrl("enrolment-store-proxy")

  lazy val agentServicesAccountFrontend: String = servicesConfig.baseUrl("agent-services-account-frontend")

  //Translation
  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  //Auth variables
  lazy val requiredConfidenceLevel: Int = servicesConfig.getInt("auth.confidenceLevel")

  lazy val ivUrl = servicesConfig.getString("identity-verification-frontend.host")
  lazy val relativeIVUpliftParams = servicesConfig.getBoolean("identity-verification-frontend.use-relative-params")

  def incomeSourceOverrides(): Option[Seq[String]] = config.getOptional[Seq[String]]("afterIncomeSourceCreated")

  def poaAdjustmentOverrides(): Option[Seq[String]] = config.getOptional[Seq[String]]("afterPoaAmountAdjusted")

  val cacheTtl: Int = config.get[Int]("mongodb.timeToLiveInSeconds")

  val encryptionIsEnabled: Boolean = config.get[Boolean]("encryption.isEnabled")

  lazy val readFeatureSwitchesFromMongo: Boolean = servicesConfig.getBoolean("feature-switches.read-from-mongo")

  lazy val isTimeMachineEnabled: Boolean = servicesConfig.getBoolean("feature-switch.enable-time-machine")
  lazy val timeMachineAddYears: Int = servicesConfig.getInt("time-machine.add-years")
  lazy val timeMachineAddDays: Int = servicesConfig.getInt("time-machine.add-days")

  lazy val isSessionDataStorageEnabled: Boolean = servicesConfig.getBoolean("feature-switch.enable-session-data-storage")

  //External-Urls
  def selfAssessmentTaxReturnLink(implicit messages: Messages): String =
    messages.lang.code match {
      case "en" => "https://www.gov.uk/log-in-file-self-assessment-tax-return"
      case "cy" => "https://www.gov.uk/cyflwyno-ch-ffurflen-dreth-hunanasesiad-ar-lein"
      case _ => "https://www.gov.uk/log-in-file-self-assessment-tax-return"
    }

def compatibleSoftwareLink(implicit messages: Messages): String =
    messages.lang.code match {
      case "en" => "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
      case "cy" => "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax.cy"
      case _ => "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"
    }
  lazy val preThreshold2027 = servicesConfig.getString("thresholds.prethreshold2027")
  lazy val threshold2027 = servicesConfig.getString("thresholds.threshold2027")
  lazy val threshold2028 = servicesConfig.getString("thresholds.threshold2028")
}
