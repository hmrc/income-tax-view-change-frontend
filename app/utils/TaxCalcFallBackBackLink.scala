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

package utils

trait TaxCalcFallBackBackLink {

  def getFallbackUrl(calcPageBackLink: Option[String], isAgent: Boolean, isCrystallised: Boolean, taxYear: Int, origin: Option[String]): String = {
    if (isAgent)
      fallbackUrlForAgent(calcPageBackLink, isCrystallised, taxYear)
    else
      fallbackUrlForIndividual(calcPageBackLink, isCrystallised, taxYear, origin)
  }

  private def fallbackUrlForAgent(calcPageBackLink: Option[String], isCrystallised: Boolean, taxYear: Int) = {
    if (isSubmission(calcPageBackLink)) {
      if (isCrystallised)
        controllers.routes.FinalTaxCalculationController.showAgent(taxYear).url
      else
        controllers.routes.InYearTaxCalculationController.showAgent().url
    }
    else
      controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(taxYear).url
  }

  private def fallbackUrlForIndividual(calcPageBackLink: Option[String], isCrystallised: Boolean, taxYear: Int, origin: Option[String]) = {
    if (isSubmission(calcPageBackLink)) {
      if (isCrystallised)
        controllers.routes.FinalTaxCalculationController.show(taxYear, origin).url
      else
        controllers.routes.InYearTaxCalculationController.show(origin).url
    }
    else
      controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).url
  }

  private def isSubmission(calcPageBackLink: Option[String]): Boolean = calcPageBackLink match {
    case Some("submission") => true
    case _ => false
  }
}
