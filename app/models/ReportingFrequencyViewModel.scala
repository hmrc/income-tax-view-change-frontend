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

package models

import models.incomeSourceDetails.TaxYear
import services.DateServiceInterface

case class ReportingFrequencyViewModel(
                                        isAgent: Boolean,
                                        optOutJourneyUrl: Option[String],
                                        optOutTaxYears: Seq[TaxYear],
                                        optInTaxYears: Seq[TaxYear],
                                        itsaStatusTable: Seq[(String, Option[String], Option[String])],
                                        displayCeasedBusinessWarning: Boolean,
                                        isAnyOfBusinessLatent: Boolean,
                                        displayManageYourReportingFrequencySection: Boolean = true,
                                        mtdThreshold: String
                                      )(implicit dateService: DateServiceInterface) {

  val isOptInLinkOnward: Boolean =
    optInTaxYears.size == 1 && optInTaxYears.head == dateService.getCurrentTaxYear.nextYear

  val isOptOutLinkOnward: Boolean =
    optOutTaxYears.size == 1 && optOutTaxYears.head == dateService.getCurrentTaxYear.nextYear

  val isOptInLinkFirst: Boolean =
    optOutTaxYears.isEmpty ||
      (optInTaxYears.nonEmpty && (optInTaxYears.minBy(_.startYear).startYear < optOutTaxYears.minBy(_.startYear).startYear))

  val atLeastOneOfOptInOrOptOutExists: Boolean = optOutTaxYears.nonEmpty || optInTaxYears.nonEmpty
}