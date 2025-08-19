/*
 * Copyright 2025 HM Revenue & Customs
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

package models.optin

import models.incomeSourceDetails.TaxYear

case class SignUpCompletedViewModel(isAgent: Boolean,
                                    optInTaxYear: TaxYear,
                                    isCurrentYear: Boolean,
                                    isCurrentYearAnnual: Boolean,
                                    isNextYearMandated: Boolean,
                                    compatibleSoftwareLink: String,
                                    criteriaForMtdLink: String,
                                    fileYourReturnLink: String) {

  val messageKey = if (isCurrentYear) "cy" else "ny"

  val startYear: String = optInTaxYear.startYear.toString
  val endYear: String = optInTaxYear.endYear.toString
  val nextYear: String = optInTaxYear.nextYear.endYear.toString

  val updatesAndDeadlinesLink: String = {
    if (isAgent) controllers.routes.NextUpdatesController.showAgent().url
    else controllers.routes.NextUpdatesController.show().url
  }

  val reportingObligationsLink: String = controllers.routes.ReportingFrequencyPageController.show(isAgent).url

}


