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

  private val currentTaxYear: TaxYear = dateService.getCurrentTaxYear
  private val previousTaxYear: TaxYear = currentTaxYear.previousYear
  private val nextTaxYear: TaxYear = currentTaxYear.nextYear

  val isOptInLinkOnward: Boolean =
    optInTaxYears.size == 1 && optInTaxYears.head == currentTaxYear.nextYear

  val isOptOutLinkOnward: Boolean =
    optOutTaxYears.size == 1 && optOutTaxYears.head == currentTaxYear.nextYear

  val isOptInLinkFirst: Boolean =
    optOutTaxYears.isEmpty ||
      (optInTaxYears.nonEmpty && (optInTaxYears.minBy(_.startYear).startYear < optOutTaxYears.minBy(_.startYear).startYear))

  val atLeastOneOfOptInOrOptOutExists: Boolean = optOutTaxYears.nonEmpty || optInTaxYears.nonEmpty

  private val previousYearSuffix = if (optOutTaxYears.contains(currentTaxYear.previousYear)) {
    if (optOutTaxYears.size == 1) {
      Some("optOut.previousYear.single")
    } else {
      Some("optOut.previousYear.onwards")
    }
  } else {
    None
  }

  private val currentYearSuffix: Option[String] = {
    if (optOutTaxYears.contains(currentTaxYear)) {
      if (optOutTaxYears.contains(nextTaxYear) || (!optInTaxYears.contains(nextTaxYear) && !optOutTaxYears.contains(nextTaxYear))) {
        Some("optOut.currentYear.onwards")
      } else {
        Some("optOut.currentYear.single")
      }
    } else if (optInTaxYears.contains(currentTaxYear)) {
      if (optInTaxYears.contains(nextTaxYear)) {
        Some("signUp.currentYear.onwards")
      } else {
        Some("signUp.currentYear.single")
      }
    } else {
      None
    }
  }

  private val nextYearSuffix: Option[String] = {
    if (optOutTaxYears.contains(currentTaxYear.nextYear)) Some("optOut.nextYear")
    else if (optInTaxYears.contains(currentTaxYear.nextYear)) Some("signUp.nextYear")
    else None
  }

  val listOfMessageSuffixes: Seq[String] = Seq(previousYearSuffix, currentYearSuffix, nextYearSuffix).flatten

  def getChangeLinkText(suffix: String): String = if (suffix.contains("optOut")) "optOut.link.text" else "signUp.link.text"

  def getSecondDescText(suffix: String): String = if (!suffix.contains("currentYear")) {
    suffix.replaceAll("\\.(single|onwards)$", "")
  } else {
    if (previousYearSuffix.isDefined) {
      if(suffix.contains("optOut")) {
        suffix.replaceAll("\\.(single|onwards)$", ".withDate")
      } else {
        if(nextYearSuffix.exists(_.contains("optOut"))) {
          suffix.replaceAll("\\.(single|onwards)$", "")
        } else {
          suffix.replaceAll("\\.(single|onwards)$", ".withDate")
        }
      }
    } else {
      suffix.replaceAll("\\.(single|onwards)$", "")
    }
  }

  def taxYearFromSuffix(suffix: String): TaxYear = suffix match {
    case s if s.contains("previousYear") => previousTaxYear
    case s if s.contains("currentYear")  => currentTaxYear
    case s if s.contains("nextYear")     => nextTaxYear
    case _ => throw new RuntimeException("Invalid suffix passed to taxYearFromSuffix")
  }
}