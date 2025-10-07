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

import enums.ReportingObligations.{MultiYearCard, NoCard, ReportingObligationSummaryCardState, SingleYearCard}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, Voluntary}
import services.DateServiceInterface
import services.optout.OptOutProposition

case class ReportingFrequencyViewModel(
                                        isAgent: Boolean,
                                        optOutJourneyUrl: Option[String],
                                        optInTaxYears: Seq[TaxYear],
                                        itsaStatusTable: Seq[(String, Option[String], Option[String])],
                                        displayCeasedBusinessWarning: Boolean,
                                        isAnyOfBusinessLatent: Boolean,
                                        displayManageYourReportingFrequencySection: Boolean = true,
                                        mtdThreshold: String,
                                        proposition: OptOutProposition,
                                        isSignUpEnabled: Boolean,
                                        isOptOutEnabled: Boolean
                                      )(implicit dateService: DateServiceInterface) {

  private val currentTaxYear: TaxYear = dateService.getCurrentTaxYear
  private val previousTaxYear: TaxYear = currentTaxYear.previousYear
  private val nextTaxYear: TaxYear = currentTaxYear.nextYear

  val optOutTaxYears: Seq[TaxYear] = proposition.availableTaxYearsForOptOut

  val isOptInLinkOnward: Boolean =
    optInTaxYears.size == 1 && optInTaxYears.head == currentTaxYear.nextYear

  val isOptOutLinkOnward: Boolean =
    optOutTaxYears.size == 1 && optOutTaxYears.head == currentTaxYear.nextYear

  val isOptInLinkFirst: Boolean =
    optOutTaxYears.isEmpty ||
      (optInTaxYears.nonEmpty && (optInTaxYears.minBy(_.startYear).startYear < optOutTaxYears.minBy(_.startYear).startYear))

  val optOutExistsWhileEnabled: Boolean = isOptOutEnabled && optOutTaxYears.nonEmpty
  val signUpExistsWhileEnabled: Boolean = isSignUpEnabled && optInTaxYears.nonEmpty

  private def selectYearSuffix(year: TaxYear, optOutSingle: String, optOutOnwards: String, signUpLabel: String): Option[String] = {
    if (optOutTaxYears.contains(year)) {
      if (optOutTaxYears.size == 1) Some(optOutSingle) else Some(optOutOnwards)
    } else if (optInTaxYears.contains(year)) {
      Some(signUpLabel)
    } else None
  }

  private val previousYearSuffix = selectYearSuffix(previousTaxYear, "optOut.previousYear.single", "optOut.previousYear.onwards", "")
  private val currentYearSuffix = selectYearSuffix(currentTaxYear, "optOut.currentYear", "optOut.currentYear", "signUp.currentYear")
  private val nextYearSuffix = selectYearSuffix(nextTaxYear, "optOut.nextYear", "optOut.nextYear", "signUp.nextYear")

  def getChangeLinkText(suffix: String): String = if (suffix.contains("optOut")) "optOut.link.text" else "signUp.link.text"

  def getSecondDescText(suffix: String): String = {
    val base = suffix.stripSuffix(".single").stripSuffix(".onwards")

    if (suffix.contains("currentYear") && previousYearSuffix.isDefined) {
      if (suffix.contains("optOut")) {
        if (optOutTaxYears.size > 1) base + ".withDate" else base
      } else {
        if (optInTaxYears.size > 1) base + ".withDate" else base
      }
    } else {
      base
    }
  }

  def taxYearFromSuffix(suffix: String): TaxYear = suffix match {
    case s if s.contains("previousYear") => previousTaxYear
    case s if s.contains("currentYear")  => currentTaxYear
    case s if s.contains("nextYear")     => nextTaxYear
    case _ => throw new RuntimeException("Invalid suffix passed to taxYearFromSuffix")
  }

  def getOptOutSignUpLink(taxYear: TaxYear, suffix: String): String = {
    if (suffix.contains("optOut")) {
      controllers.optOut.newJourney.routes.OptOutTaxYearQuestionController.show(isAgent, Some(taxYear.startYear.toString)).url
    } else {
      controllers.optIn.newJourney.routes.SignUpStartController.show(isAgent, Some(taxYear.startYear.toString)).url
    }
  }

  private val checkIfOnwards: List[ReportingObligationSummaryCardState] = {
    val currentYearStatus = proposition.currentTaxYear.status
    val nextYearStatus = proposition.nextTaxYear.status

    val previousYearCheck = (proposition.previousTaxYear.canOptOut, currentYearStatus) match {
      case (false, _)                            => NoCard
      case (true, _) if !isOptOutEnabled         => NoCard
      case (true, Voluntary)                     => MultiYearCard
      case (true, _)                             => SingleYearCard
    }

    val currentYearCheck = (currentYearStatus, nextYearStatus) match {
      case (Annual, _) if !isSignUpEnabled    => NoCard
      case (Voluntary, _) if !isOptOutEnabled => NoCard
      case (Voluntary, Voluntary)             => MultiYearCard
      case (Annual, Annual)                   => MultiYearCard
      case (Voluntary | Annual, _)            => SingleYearCard
      case _                                  => NoCard
    }

    val nextYearCheck = nextYearStatus match {
      case Voluntary if !isOptOutEnabled => NoCard
      case Annual if !isSignUpEnabled    => NoCard
      case Voluntary | Annual            => MultiYearCard
      case _                             => NoCard
    }

    List(previousYearCheck, currentYearCheck, nextYearCheck)
  }

  val getSummaryCardSuffixes: List[Option[String]] = {
    val test = checkIfOnwards.zipWithIndex.map {
      case (MultiYearCard, 0)  => Some("optOut.previousYear.onwards")
      case (SingleYearCard, 0) => Some("optOut.previousYear.single")
      case (MultiYearCard, 1)  => currentYearSuffix.map(_ + ".onwards")
      case (SingleYearCard, 1) => currentYearSuffix.map(_ + ".single")
      case (MultiYearCard, 2)  => nextYearSuffix
      case _ => None
    }
    test
  }
}