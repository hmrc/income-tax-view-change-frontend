/*
 * Copyright 2026 HM Revenue & Customs
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

package common.config

import financials.controllers.claimToAdjustPoa.routes as claimToAdjustPoaRoutes
import financials.controllers.routes as financialsRoutes
import hub.v2.controllers.agent.routes as hubV2AgentRoutes
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait ExternalRedirectHelper {

  val servicesConfig: ServicesConfig
  val config: Configuration

  lazy val basePath: String = servicesConfig.getString("base.context-root")
  lazy val agentBasePath: String = s"$basePath/agents"
  lazy val baseUrl: String = servicesConfig.getString("base.url")
  lazy val baseFullUrl: String = s"$baseUrl$basePath"
  lazy val agentBaseFullUrl: String = s"$baseUrl$agentBasePath"

  lazy val hubBasePath: String = servicesConfig.getString("base.context-root-hub")

  lazy val agentHubBasePath: String = s"$hubBasePath/agents"

  lazy val hubBaseUrl: String = s"$baseUrl$hubBasePath"

  lazy val hubAgentBaseUrl: String = s"$hubBaseUrl/agents"

  lazy val individualHomeUrl: String = s"$hubBaseUrl/income-tax"

  def individualHomeUrlWithOrigin(origin: Option[String]): String =
    origin.fold(individualHomeUrl)(o => s"$individualHomeUrl?origin=$o")

  lazy val agentHomeUrl: String = s"$hubAgentBaseUrl/client-income-tax"

  def homePageUrl(isAgent: Boolean, origin: Option[String] = None): String = {
    if (isAgent) agentHomeUrl else individualHomeUrlWithOrigin(origin)
  }

  lazy val individualYourTasksUrl: String = s"$hubBaseUrl/your-tasks"

  lazy val agentYourTasksUrl: String = s"$hubAgentBaseUrl/your-tasks"

  def enterClientsUTRUrl(utr: Option[String] = None): String = {
    utr.fold(hubV2AgentRoutes.EnterClientsUTRController.show().url)(
      utrValue => hubV2AgentRoutes.EnterClientsUTRController.showWithUtr(utrValue).url)
  }

  lazy val confirmClientUTRUrl: String = hubV2AgentRoutes.ConfirmClientUTRController.show().url

  //Obligation routes

  lazy val obligationsBaseUrl: String = servicesConfig.getString("income-tax-obligations-frontend.baseUrl")
  lazy val obligationsAgentBaseUrl: String = s"$obligationsBaseUrl/agents"

  def obligationsNextUpdatesUrl(isAgent: Boolean): String =
    val newBaseUrl = if isAgent then obligationsAgentBaseUrl else obligationsBaseUrl
    s"$newBaseUrl/submission-deadlines"

  def obligationsReportingFrequencyUrl(isAgent: Boolean): String =
    val newBaseUrl = if isAgent then obligationsAgentBaseUrl else obligationsBaseUrl
    s"$newBaseUrl/reporting-frequency"

  //Business Details routes
  lazy val businessDetailsBaseUrl: String = servicesConfig.getString("income-tax-business-details-frontend.baseUrl")
  lazy val businessDetailsAgentBaseUrl: String = s"$businessDetailsBaseUrl/agents"

  lazy val businessDetailsManageBusinessesIndividualUrl: String = s"$businessDetailsBaseUrl/manage-your-businesses"

  lazy val businessDetailsManageBusinessesAgentUrl: String = s"$businessDetailsAgentBaseUrl/manage-your-businesses"

  def manageBusinessesUrl(isAgent: Boolean): String =
    if (isAgent)
      businessDetailsManageBusinessesAgentUrl
    else
      businessDetailsManageBusinessesIndividualUrl

  def triggeredMigrationCompleteStepsUrl(isAgent: Boolean): String = {
    val baseUri = if (isAgent) businessDetailsAgentBaseUrl else businessDetailsBaseUrl
    s"$baseUri/complete-steps"
  }

  def triggeredMigrationCheckHMRCRecordsUrl(isAgent: Boolean): String = {
    val baseUri = if (isAgent) businessDetailsAgentBaseUrl else businessDetailsBaseUrl
    s"$baseUri/check-your-active-businesses/hmrc-record"
  }

  //Financials routes

  lazy val financialsBaseUrl: String = servicesConfig.getString("income-tax-financials-frontend.baseUrl")
  lazy val financialsAgentBaseUrl: String = s"$financialsBaseUrl/agents"

  lazy val financialsWhatYouOweIndividualUrl: (Boolean, Option[String]) => String = (financialsFrontendEnabled, origin) =>
    if (financialsFrontendEnabled)
      s"$financialsBaseUrl/what-you-owe${origin.fold("")(o => s"?origin=$o")}"
    else
      financialsRoutes.WhatYouOweController.show(origin).url

  lazy val financialsWhatYouOweAgentUrl: Boolean => String = financialsFrontendEnabled =>
    if (financialsFrontendEnabled)
      s"$financialsAgentBaseUrl/what-you-owe"
    else
      financialsRoutes.WhatYouOweController.showAgent().url

  def financialsWhatYouOweUrl(isAgent: Boolean, origin: Option[String] = None, financialsFrontendEnabled: Boolean): String =
    if (isAgent)
      financialsWhatYouOweAgentUrl(financialsFrontendEnabled)
    else
      financialsWhatYouOweIndividualUrl(financialsFrontendEnabled, origin)

  lazy val financialsAmendablePoaIndividualUrl: (Boolean) => String = financialsFrontendEnabled =>
    if (financialsFrontendEnabled)
      s"$financialsBaseUrl/adjust-poa/start"
    else
      claimToAdjustPoaRoutes.AmendablePoaController.show(false).url

  lazy val financialsAmendablePoaAgentUrl: Boolean => String = financialsFrontendEnabled =>
    if (financialsFrontendEnabled)
      s"$financialsAgentBaseUrl/adjust-poa/start"
    else
      claimToAdjustPoaRoutes.AmendablePoaController.show(true).url

  def financialsAmendablePoaUrl(isAgent: Boolean, financialsFrontendEnabled: Boolean): String =
    if (isAgent)
      financialsAmendablePoaAgentUrl(financialsFrontendEnabled)
    else
      financialsAmendablePoaIndividualUrl(financialsFrontendEnabled)

  def financialsChargeSummaryIndividualUrl(taxYear: Int,
                                           transactionId: String,
                                           isAccruingInterest: Boolean = false,
                                           origin: Option[String] = None,
                                           financialsFrontendEnabled: Boolean): String = {
    lazy val queryPathNoOrigin = s"?id=$transactionId&isInterestCharge=$isAccruingInterest"
    lazy val queryPathString = origin.fold(queryPathNoOrigin)(o => s"$queryPathNoOrigin&origin=$o")
    if (financialsFrontendEnabled) {
      s"$financialsBaseUrl/tax-years/$taxYear/charge$queryPathString"
    } else
      financialsRoutes.ChargeSummaryController.show(taxYear, transactionId, isAccruingInterest, origin).url
  }

  def financialsChargeSummaryAgentUrl(taxYear: Int,
                                      transactionId: String,
                                      isAccruingInterest: Boolean = false,
                                      financialsFrontendEnabled: Boolean): String = {
    lazy val queryPathString = s"?id=$transactionId&isInterestCharge=$isAccruingInterest"
    if (financialsFrontendEnabled)
      s"$financialsAgentBaseUrl/tax-years/$taxYear/charge$queryPathString"
    else
      financialsRoutes.ChargeSummaryController.showAgent(taxYear, transactionId, isAccruingInterest).url
  }

  lazy val financialsMoneyInAccountIndividualUrl: Boolean => String = financialsFrontendEnabled =>
    if (financialsFrontendEnabled)
      s"$financialsBaseUrl/money-in-your-account"
    else
      financialsRoutes.MoneyInYourAccountController.show().url

  lazy val financialsMoneyInAccountAgentUrl: Boolean => String = financialsFrontendEnabled =>
    if (financialsFrontendEnabled)
      s"$financialsAgentBaseUrl/money-in-your-account"
    else
      financialsRoutes.MoneyInYourAccountController.showAgent().url

  def financialsMoneyInAccountUrl(isAgent: Boolean, financialsFrontendEnabled: Boolean): String =
    if (isAgent)
      financialsMoneyInAccountAgentUrl(financialsFrontendEnabled)
    else
      financialsMoneyInAccountIndividualUrl(financialsFrontendEnabled)

  lazy val financialsPaymentHistoryIndividualUrl: Boolean => String = financialsFrontendEnabled =>
    if (financialsFrontendEnabled)
      s"$financialsBaseUrl/payment-refund-history"
    else
      financialsRoutes.PaymentHistoryController.show().url

  lazy val financialsPaymentHistoryAgentUrl: Boolean => String = financialsFrontendEnabled =>
    if (financialsFrontendEnabled)
      s"$financialsAgentBaseUrl/payment-refund-history"
    else
      financialsRoutes.PaymentHistoryController.showAgent().url

  def financialsPaymentHistoryUrl(isAgent: Boolean, financialsFrontendEnabled: Boolean): String =
    if (isAgent)
      financialsPaymentHistoryAgentUrl(financialsFrontendEnabled)
    else
      financialsPaymentHistoryIndividualUrl(financialsFrontendEnabled)

  //Returns routes

  lazy val returnsBaseUrl: String = servicesConfig.getString("income-tax-returns-frontend.baseUrl")
  lazy val returnsAgentBaseUrl: String = s"$returnsBaseUrl/agents"


  lazy val returnsTaxYearsIndividualUrl: String = s"$returnsBaseUrl/tax-years"

  lazy val returnsTaxYearsAgentUrl: String = s"$returnsAgentBaseUrl/tax-years"

  def returnsTaxYearsUrl(isAgent: Boolean): String =
    if (isAgent)
      returnsTaxYearsAgentUrl
    else
      returnsTaxYearsIndividualUrl

  def taxYearSummaryUrl(isAgent: Boolean, taxYear: Int, origin: Option[String] = None, fragment: Option[String] = None): String =
    val baseUri = s"${if isAgent then returnsAgentBaseUrl else returnsBaseUrl}/tax-year-summary/$taxYear"
    val baseUriWithMaybeOrigin = origin.fold(baseUri)(o => s"$baseUri?origin=$o")

    fragment.fold(baseUriWithMaybeOrigin)(fragment => s"$baseUriWithMaybeOrigin#$fragment")

  def proofOfIncomeUrl(isAgent: Boolean): String =
    if isAgent
    then returnsAgentBaseUrl + "/mortgage-evidence/proof-of-income"
    else returnsBaseUrl + "/mortgage-evidence/proof-of-income"

}
