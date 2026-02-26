/*
 * Copyright 2024 HM Revenue & Customs
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

package models.reportingObligations.signUp

import models.incomeSourceDetails.TaxYear

import java.time.format.DateTimeFormatter

case class OptInCompletedViewModel(
                                    isAgent: Boolean,
                                    optInTaxYear: TaxYear,
                                    isCurrentYear: Boolean,
                                    showAnnualReportingAdvice: Boolean,
                                    optInIncludedNextYear: Boolean,
                                    annualWithFollowingYearMandated: Boolean
                                  ) {

  val startYear: String = optInTaxYear.startYear.toString
  val endYear: String = optInTaxYear.endYear.toString
  val nextYear: String = optInTaxYear.nextYear.endYear.toString
  private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu")
  val startDateNextYear: String = optInTaxYear.toFinancialYearEnd.plusDays(1).format(dateTimeFormatter)

  def headingMessageKey: String = {
    (isCurrentYear, optInIncludedNextYear, annualWithFollowingYearMandated) match {
      case (true, _, true) => "optin.completedOptIn.followingVoluntary.heading.desc"
      case (true, true, _) => "optin.completedOptIn.followingVoluntary.heading.desc"
      case (true, false, _) => "optin.completedOptIn.cy.heading.desc"
      case _ => "optin.completedOptIn.ny.heading.desc"
    }
  }

  val nextUpdatesLink = {
    if (isAgent) controllers.routes.NextUpdatesController.showAgent().url
    else controllers.routes.NextUpdatesController.show().url
  }

}