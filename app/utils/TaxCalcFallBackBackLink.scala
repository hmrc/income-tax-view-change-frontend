/*
 * Copyright 2022 HM Revenue & Customs
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

  def getFallbackUrl(isAgent: Boolean, isCrystallised: Boolean, taxYear: Int, origin: Option[String]): String = {
    if (isCrystallised) {
      if (isAgent) {
        controllers.routes.FinalTaxCalculationController.showAgent(taxYear).url
      } else controllers.routes.FinalTaxCalculationController.show(taxYear, origin).url
    } else {
      if (isAgent) {
        controllers.agent.routes.TaxYearSummaryController.show(taxYear).url
      } else controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).url
    }
  }
}
