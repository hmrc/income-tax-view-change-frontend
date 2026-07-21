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

import businessDetails.controllers.manageBusinesses.routes as manageBusinessRoutes
import businessDetails.controllers.triggeredMigration.routes as triggeredMigrationRoutes
import financials.controllers.claimToAdjustPoa.routes as claimToAdjustPoaRoutes
import financials.controllers.routes as financialsRoutes
import hub.controllers.agent.routes as hubAgentRoutes
import obligations.controllers.reportingObligations.routes as reportingObligationRoutes
import obligations.controllers.routes as obligationsRoutes
import play.api.Configuration
import returns.controllers.routes as returnsRoutes
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait ExternalRedirectHelper {

  val servicesConfig: ServicesConfig
  val config: Configuration
  
  lazy val hubBaseUrl: String = servicesConfig.getString("base.fullUrl")
  lazy val hubAgentBaseUrl: String = s"${hubBaseUrl}/agents"
  
  lazy val individualHomeUrl: String =
    s"$hubBaseUrl/income-tax"

  lazy val individualHomeUrlWithOrigin: Option[String] => String = origin =>
      origin.fold(individualHomeUrl)(o => s"$individualHomeUrl?origin=$o")

  lazy val agentHomeUrl: String =
    s"$hubAgentBaseUrl/client-income-tax"
    
  def homePageUrl(isAgent: Boolean): String = if (isAgent) agentHomeUrl else individualHomeUrl

  lazy val enterClientsUTRUrl: String =
    hubAgentRoutes.EnterClientsUTRController.show().url
  lazy val confirmClientUTRUrl: String =
    hubAgentRoutes.ConfirmClientUTRController.show().url

  //Obligation routes
  
  lazy val obligationsBaseUrl: String = servicesConfig.getString("income-tax-obligations-frontend.baseUrl")
  lazy val obligationsAgentBaseUrl: String = s"$obligationsBaseUrl/agents"

  def obligationsNextUpdatesUrl(isAgent: Boolean, newObligationsEnabled: Boolean): String =
    val newBaseUrl = if isAgent then obligationsAgentBaseUrl else obligationsBaseUrl
    val oldUrl: String = if isAgent 
      then obligationsRoutes.NextUpdatesController.showAgent().url
      else obligationsRoutes.NextUpdatesController.show().url
    
    if newObligationsEnabled 
    then s"$newBaseUrl/submission-deadlines"
    else oldUrl

  def obligationsReportingFrequencyUrl(isAgent: Boolean, newObligationsEnabled: Boolean): String =
    val newBaseUrl = if isAgent then obligationsAgentBaseUrl else obligationsBaseUrl
    
    if newObligationsEnabled 
    then s"$newBaseUrl/reporting-frequency"
    else reportingObligationRoutes.ReportingFrequencyPageController.show(isAgent).url

  //Business Details routes
  lazy val businessDetailsBaseUrl: String = servicesConfig.getString("income-tax-business-details-frontend.baseUrl")
  lazy val businessDetailsAgentBaseUrl: String = s"$businessDetailsBaseUrl/agents"

  lazy val businessDetailsManageBusinessesIndividualUrl: Boolean => String = businessDetailsFrontendEnabled =>
    if (businessDetailsFrontendEnabled)
      s"$businessDetailsBaseUrl/manage-your-businesses"
    else
      manageBusinessRoutes.ManageYourBusinessesController.show().url

  lazy val businessDetailsManageBusinessesAgentUrl: Boolean => String = businessDetailsFrontendEnabled =>
    if (businessDetailsFrontendEnabled)
      s"$businessDetailsAgentBaseUrl/manage-your-businesses"
    else
      manageBusinessRoutes.ManageYourBusinessesController.showAgent().url

  def manageBusinessesUrl(isAgent: Boolean, businessDetailsFrontendEnabled: Boolean): String =
    if (isAgent)
      businessDetailsManageBusinessesAgentUrl(businessDetailsFrontendEnabled)
    else
      businessDetailsManageBusinessesIndividualUrl(businessDetailsFrontendEnabled)
      
  def triggeredMigrationCheckHMRCRecordsUrl(isAgent: Boolean, businessDetailsFrontendEnabled: Boolean): String = {
    if(businessDetailsFrontendEnabled) {
      val baseUri = if(isAgent) businessDetailsAgentBaseUrl else businessDetailsBaseUrl
      s"$baseUri/check-your-active-businesses/hmrc-record"
    } else {
        triggeredMigrationRoutes.CheckHmrcRecordsController.show(isAgent).url
    }
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
      s"$financialsAgentBaseUrl/what-your-client-owes"
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


  lazy val returnsTaxYearsIndividualUrl: Boolean => String = returnsFrontendEnabled =>
    if (returnsFrontendEnabled)
      s"$returnsBaseUrl/tax-years"
    else
      returnsRoutes.TaxYearsController.showTaxYears().url

  lazy val returnsTaxYearsAgentUrl: Boolean => String = returnsFrontendEnabled =>
    if (returnsFrontendEnabled)
      s"$returnsAgentBaseUrl/tax-years"
    else
      returnsRoutes.TaxYearsController.showAgentTaxYears().url

  def returnsTaxYearsUrl(isAgent: Boolean, returnsFrontendEnabled: Boolean): String =
    if (isAgent)
      returnsTaxYearsAgentUrl(returnsFrontendEnabled)
    else
      returnsTaxYearsIndividualUrl(returnsFrontendEnabled)

  def returnsTaxYearSummaryIndividualUrl(taxYear: Int, origin: Option[String] = None,
                                         fragment: Option[String] = None, returnsFrontendEnabled: Boolean): String = {
    if (returnsFrontendEnabled) {
      val baseUri = s"$returnsBaseUrl/tax-year-summary/$taxYear"
      val baseUriWithOptOrigin = origin.fold(baseUri)(o => s"$baseUri?origin=$o")
      fragment.fold(baseUriWithOptOrigin)(f => s"$baseUriWithOptOrigin#$f")
    } else {
      val baseUri = returnsRoutes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear)
      fragment.fold(baseUri)(f => baseUri.withFragment(f)).path
    }
  }

  def returnsTaxYearSummaryAgentUrl(taxYear: Int, fragment: Option[String] = None, returnsFrontendEnabled: Boolean): String = {
    if (returnsFrontendEnabled) {
      val baseUri = s"$returnsAgentBaseUrl/tax-year-summary/$taxYear"
      fragment.fold(baseUri)(f => s"$baseUri#$f")
    } else {
      val baseUri = returnsRoutes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear)
      fragment.fold(baseUri)(f => baseUri.withFragment(f)).path
    }
  }

}
