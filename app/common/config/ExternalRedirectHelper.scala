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

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import hub.controllers.routes as hubRoutes
import hub.controllers.agent.routes as hubAgentRoutes
import businessDetails.controllers.manageBusinesses.routes as manageBusinessRoutes
import obligations.controllers.routes as obligationsRoutes
import obligations.controllers.reportingObligations.routes as reportingObligationRoutes
import obligations.controllers.reportingObligations.signUp.routes as signUpRoutes
import financials.controllers.claimToAdjustPoa.routes as claimToAdjustPoaRoutes
import financials.controllers.routes as financialsRoutes
import returns.controllers.routes as returnsRoutes
import businessDetails.controllers.triggeredMigration.routes as triggeredMigrationRoutes

trait ExternalRedirectHelper {

  val servicesConfig: ServicesConfig
  val config: Configuration
  
  // hub routes KEEP COMMENTED IN income-tax-view-change-frontend 
//  lazy val hubBaseUrl: String = servicesConfig.getString("income-tax-view-change-frontend.baseUrl")
//  lazy val hubAgentBaseUrl: String = s"${hubBaseUrl}/agents"
  
  lazy val individualHomeUrl: String =
    hubRoutes.HomeController.show().url
    //hubBaseUrl

  lazy val individualHomeUrlWithOrigin: Option[String] => String = origin =>
      hubRoutes.HomeController.show(origin).url
  //s"$hubBaseUrl?origin=$origin"

  lazy val homePageUrl: String = {
    servicesConfig.getString("base.fullUrl")
    //individualHomeUrl
  }


  lazy val agentHomeUrl: String =
    hubRoutes.HomeController.showAgent().url
    //hubAgentBaseUrl
    
  def homePageUrl(isAgent: Boolean): String = if (isAgent) agentHomeUrl else individualHomeUrl

  lazy val enterClientsUTRUrl: String =
    hubAgentRoutes.EnterClientsUTRController.show().url
    //s"$hubAgentBaseUrl/client-utr"
  lazy val confirmClientUTRUrl: String =
    hubAgentRoutes.ConfirmClientUTRController.show().url
    //s"$hubAgentBaseUrl/confirm-client-details"
  
  //Obligation routes
  
  lazy val obligationsBaseUrl: String = servicesConfig.getString("income-tax-obligations-frontend.baseUrl")
  lazy val obligationsAgentBaseUrl: String = s"$obligationsBaseUrl/agents"
  
  lazy val obligationsWaitToSignUpIndividualUrl: Boolean => String = newObligationsEnabled =>
    if (newObligationsEnabled)
      s"$obligationsBaseUrl/access-service-from-next-tax-year"
    else
      //s"$hubBaseUrl/access-service-from-next-tax-year"
      signUpRoutes.YouMustWaitToSignUpController.show(false).url

  lazy val obligationsWaitToSignUpAgentUrl: Boolean => String = newObligationsEnabled =>
    if (newObligationsEnabled)
      s"$obligationsAgentBaseUrl/view-client-from-next-tax-year"
    else
      //s"$hubAgentBaseUrl/view-client-from-next-tax-year"
      signUpRoutes.YouMustWaitToSignUpController.show(true).url

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
      //s"$hubBaseUrl/manage-your-businesses"
      manageBusinessRoutes.ManageYourBusinessesController.show().url

  lazy val businessDetailsManageBusinessesAgentUrl: Boolean => String = businessDetailsFrontendEnabled =>
    if (businessDetailsFrontendEnabled)
      s"$businessDetailsAgentBaseUrl/manage-your-businesses"
    else
      //s"$hubAgentBaseUrl/manage-your-businesses"
      manageBusinessRoutes.ManageYourBusinessesController.showAgent().url

  def manageBusinessesUrl(isAgent: Boolean, businessDetailsFrontendEnabled: Boolean): String =
    if (isAgent)
      businessDetailsManageBusinessesAgentUrl(businessDetailsFrontendEnabled)
    else
      businessDetailsManageBusinessesIndividualUrl(businessDetailsFrontendEnabled)
      
  //ToDo in business-details-frontend, remove the below method and use the routes directly.
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
      //s"$hubBaseUrl/what-you-owe${origin.fold("")(o => s"?origin=$o")}"
      financialsRoutes.WhatYouOweController.show(origin).url

  lazy val financialsWhatYouOweAgentUrl: Boolean => String = financialsFrontendEnabled =>
    if (financialsFrontendEnabled)
      s"$financialsAgentBaseUrl/what-your-client-owes"
    else
      //s"$hubAgentBaseUrl/what-your-client-owes"
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
      //s"$hubBaseUrl/adjust-poa/start"
      claimToAdjustPoaRoutes.AmendablePoaController.show(false).url

  lazy val financialsAmendablePoaAgentUrl: Boolean => String = financialsFrontendEnabled =>
    if (financialsFrontendEnabled)
      s"$financialsAgentBaseUrl/adjust-poa/start"
    else
      //s"$hubAgentBaseUrl/adjust-poa/start"
      claimToAdjustPoaRoutes.AmendablePoaController.show(true).url

  def financialsAmendablePoaUrl(isAgent: Boolean, financialsFrontendEnabled: Boolean): String =
    if (isAgent)
      financialsAmendablePoaAgentUrl(financialsFrontendEnabled)
    else
      financialsAmendablePoaIndividualUrl(financialsFrontendEnabled)

  def financialsChargeSummaryIndividualUrl(taxYear: Int,
                                           transactionId: String,
                                           isAccruingInterest: Boolean,
                                           origin: Option[String] = None,
                                           financialsFrontendEnabled: Boolean): String = {
    lazy val queryPathNoOrigin = s"?id=$transactionId&isInterestCharge=$isAccruingInterest"
    lazy val queryPathString = origin.fold(queryPathNoOrigin)(o => s"$queryPathNoOrigin&origin=$o")
    if (financialsFrontendEnabled) {
      s"$financialsBaseUrl/tax-years/$taxYear/charge$queryPathString"
    } else
      //s"$hubBaseUrl/tax-years/$taxYear/charge$queryPathString"
      financialsRoutes.ChargeSummaryController.show(taxYear, transactionId, isAccruingInterest, origin).url
  }

  def financialsChargeSummaryAgentUrl(taxYear: Int,
                                      transactionId: String,
                                      isAccruingInterest: Boolean,
                                      financialsFrontendEnabled: Boolean): String = {
    lazy val queryPathString = s"?id=$transactionId&isInterestCharge=$isAccruingInterest"
    if (financialsFrontendEnabled)
      s"$financialsAgentBaseUrl/tax-years/$taxYear/charge$queryPathString"
    else
      //s"$hubAgentBaseUrl/tax-years/$taxYear/charge$queryPathString"
      financialsRoutes.ChargeSummaryController.showAgent(taxYear, transactionId, isAccruingInterest).url
  }

  //Returns routes

  lazy val returnsBaseUrl: String = servicesConfig.getString("income-tax-returns-frontend.baseUrl")
  lazy val returnsAgentBaseUrl: String = s"$returnsBaseUrl/agents"


  lazy val returnsTaxYearsIndividualUrl: Boolean => String = returnsFrontendEnabled =>
    if (returnsFrontendEnabled)
      s"$returnsBaseUrl/tax-years"
    else
      //s"$hubBaseUrl/tax-years"
      returnsRoutes.TaxYearsController.showTaxYears().url

  lazy val returnsTaxYearsAgentUrl: Boolean => String = returnsFrontendEnabled =>
    if (returnsFrontendEnabled)
      s"$returnsAgentBaseUrl/tax-years"
    else
      //s"$hubAgentBaseUrl/tax-years"
      returnsRoutes.TaxYearsController.showAgentTaxYears().url

  def returnsTaxYearsUrl(isAgent: Boolean, returnsFrontendEnabled: Boolean): String =
    if (isAgent)
      returnsTaxYearsAgentUrl(returnsFrontendEnabled)
    else
      returnsTaxYearsIndividualUrl(returnsFrontendEnabled)

}
