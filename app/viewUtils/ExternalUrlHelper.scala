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

package viewUtils

import play.api.i18n.{Lang, Messages}

object ExternalUrlHelper {

  val homepageLink = "https://www.gov.uk"

  //SA for Agents Online Service
  val saForAgents: String = "https://www.gov.uk/guidance/self-assessment-for-agents-online-service"

  val clientAuthorisationGuidance: String = "https://www.gov.uk/government/collections/making-tax-digital-for-income-tax-as-an-agent-step-by-step"

  def seNationalInsuranceRatesUrl(implicit messages: Messages): String  =
  messages.lang.code match {
    case "en" => "https://www.gov.uk/self-employed-national-insurance-rates"
    case "cy" => "https://www.gov.uk/cyfraddau-yswiriant-gwladol-ir-hunangyflogedig"
  }

  def chooseAgentGuidanceUrl(implicit messages: Messages): String =
    messages.lang.code match {
      case "en" => "https://www.gov.uk/guidance/choose-agents-for-making-tax-digital-for-income-tax"
      case "cy" => "https://www.gov.uk/guidance/choose-agents-for-making-tax-digital-for-income-tax.cy"
    }
  def quarterlyUpdatesGuidanceUrl(implicit messages: Messages): String =
  messages.lang.code match {
    case "en" => "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax/send-quarterly-updates"
    case "cy" => "https://www.gov.uk/guidance/defnyddio-r-cynllun-troi-treth-yn-ddigidol-ar-gyfer-treth-incwm/anfon-diweddariadau-chwarterol"
  }

  def saWhoNeedsToSignUpUrl(implicit messages: Messages): String =
    messages.lang.code match {
      case "en" => "https://www.gov.uk/guidance/check-if-youre-eligible-for-making-tax-digital-for-income-tax#who-will-need-to-sign-up"
      case "cy" => "https://www.gov.uk/guidance/check-if-youre-eligible-for-making-tax-digital-for-income-tax.cy#pwy-fydd-angen-cofrestru"
    }

  val interestRateBankRateUrl = "https://www.bankofengland.co.uk/monetary-policy/the-interest-rate-bank-rate"

  val currentLPAndRepaymentInterestRatesUrl = "https://www.gov.uk/government/publications/rates-and-allowances-hmrc-interest-rates-for-late-and-early-payments/rates-and-allowances-hmrc-interest-rates#current-late-payment-and-repayment-interest-rates"

  val saThroughYourTaxCodeUrl = "https://www.gov.uk/pay-self-assessment-tax-bill/through-your-tax-code"

  val mtdForIncomeTaxUrl = "https://www.gov.uk/government/collections/making-tax-digital-for-income-tax"

  val saPayTaxBillUrl = "https://www.gov.uk/pay-self-assessment-tax-bill"

  val checkPayeTaxCodeUrl = "https://www.tax.service.gov.uk/check-income-tax/tax-codes"
}
