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
import returns.controllers.routes as returnsRoutes

def nextUpdatesIndividualUrl(origin: Option[String] = None): String = obligationsRoutes.NextUpdatesController.show(origin).url
lazy val nextUpdatesAgentUrl: String = obligationsRoutes.NextUpdatesController.showAgent().url

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
  //hubBaseUrl?origin=origin

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

  lazy val obligationsNextUpdatesIndividualUrl: Boolean => String = newObligationsEnabled =>
    if (newObligationsEnabled)
      s"$obligationsBaseUrl/submission-deadlines"
    else
      //s"$hubBaseUrl/submission-deadlines"
      obligationsRoutes.NextUpdatesController.show().url

  lazy val obligationsNextUpdatesAgentUrl: Boolean => String = newObligationsEnabled =>
    if (newObligationsEnabled)
      s"$obligationsAgentBaseUrl/submission-deadlines"
    else
      //s"$hubAgentBaseUrl/submission-deadlines"
      obligationsRoutes.NextUpdatesController.showAgent().url

  def obligationsNextUpdatesUrl(isAgent: Boolean, newObligationsEnabled: Boolean): String = {
    if (isAgent)
      obligationsNextUpdatesAgentUrl(newObligationsEnabled)
    else
      obligationsNextUpdatesIndividualUrl(newObligationsEnabled)
  }

  lazy val obligationsReportingFrequencyIndividualUrl: Boolean => String = newObligationsEnabled =>
    if (newObligationsEnabled)
      s"$obligationsBaseUrl/reporting-frequency"
    else
      //s"$hubBaseUrl/reporting-frequency"
      reportingObligationRoutes.ReportingFrequencyPageController.show(false).url

  lazy val obligationsReportingFrequencyAgentUrl: Boolean => String = newObligationsEnabled =>
    if (newObligationsEnabled)
      s"$obligationsAgentBaseUrl/reporting-frequency"
    else
      //s"$hubAgentBaseUrl/reporting-frequency"
      reportingObligationRoutes.ReportingFrequencyPageController.show(true).url


  def obligationsReportingFrequencyUrl(isAgent: Boolean, newObligationsEnabled: Boolean): String = {
    if (isAgent)
      obligationsReportingFrequencyAgentUrl(newObligationsEnabled)
    else
      obligationsReportingFrequencyIndividualUrl(newObligationsEnabled)
  }

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

  def manageYourBusinessUrl(isAgent: Boolean): String = if isAgent
  then manageBusinessRoutes.ManageYourBusinessesController.showAgent().url
  else manageBusinessRoutes.ManageYourBusinessesController.show().url

  //Financials routes

  lazy val financialsBaseUrl: String = servicesConfig.getString("income-tax-financials-frontend.baseUrl")
  lazy val financialsAgentBaseUrl: String = s"$financialsBaseUrl/agents"

  //Returns routes

  def taxYearsUrl(isAgent: Boolean): String = if isAgent
  then returnsRoutes.TaxYearsController.showAgentTaxYears().url
  else returnsRoutes.TaxYearsController.showTaxYears().url
  
  lazy val returnsBaseUrl: String = servicesConfig.getString("income-tax-returns-frontend.baseUrl")
  lazy val returnsAgentBaseUrl: String = s"$returnsBaseUrl/agents"

}
