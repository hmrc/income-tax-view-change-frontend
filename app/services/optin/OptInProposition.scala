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

package services.optin

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._

//todo: to be changed and updated according business rules

/*
* Please note:
* The purpose of this is to offer a starting point code structure that can be refined
* and refined again during the coding, development and delivery of opt-in features
*
* It mimics opt-out in it's structure but is supposed be changed and updated to meet agreed requirements from BAs
*
* These comments are to be appropriately removed as we progress with implementation
* */

////// OptInProposition ////////////////////////////////////////////////

case class OptInProposition(previousTaxYear: PreviousOptInTaxYear,
                             currentTaxYear: CurrentOptInTaxYear,
                             nextTaxYear: NextOptInTaxYear) {

  private val optInYears: Seq[OptInTaxYear] = Seq[OptInTaxYear](
    previousTaxYear,
    currentTaxYear,
    nextTaxYear)

  val availableTaxYearsForOptIn: Seq[TaxYear] = availableOptInYears.map(_.taxYear)

  lazy val availableOptInYears: Seq[OptInTaxYear] = optInYears.filter(_.canOptIn)

  val isOneYearOptIn: Boolean = availableOptInYears.size == 1
  val isMultiYearOptIn: Boolean = availableOptInYears.size > 1
  val isNoOptInAvailable: Boolean = availableOptInYears.isEmpty

  def optInYearsToUpdate(intent: TaxYear): Seq[TaxYear] = {
    availableOptInYears.filter(_.shouldBeUpdated(intent)).map(_.taxYear)
  }

  def optInPropositionType: Option[OptInPropositionType] = {
    (isOneYearOptIn, isMultiYearOptIn) match {
      case (true, false) => Some(OneYearOptInProposition(this))
      case (false, true) => Some(MultiYearOptInProposition(this))
      case _ => None
    }
  }
}

////// OptInTaxYear ////////////////////////////////////////////////

trait OptInTaxYear {
  val taxYear: TaxYear
  def canOptIn: Boolean
  def shouldBeUpdated(intent: TaxYear): Boolean
}

case class PreviousOptInTaxYear(status: ITSAStatus, taxYear: TaxYear, crystallised: Boolean) extends OptInTaxYear {
  def canOptIn: Boolean = status == Annual && !crystallised //todo: very simple for now until we have clear rules from BAs
  override def shouldBeUpdated(intent: TaxYear): Boolean =
    canOptIn && taxYear.isSameAs(intent)
}

case class CurrentOptInTaxYear(status: ITSAStatus, taxYear: TaxYear) extends OptInTaxYear {
  def canOptIn: Boolean = status == Annual //todo: very simple for now until we have clear rules from BAs

  override def shouldBeUpdated(intent: TaxYear): Boolean =
    canOptIn && (taxYear.isSameAs(intent) || taxYear.isAfter(intent)) //todo: very simple for now until we have clear rules from BAs
}

case class NextOptInTaxYear(status: ITSAStatus, taxYear: TaxYear, currentTaxYear: CurrentOptInTaxYear) extends OptInTaxYear {
  def canOptIn: Boolean = status == Annual ||
    (currentTaxYear.status == Annual && status == NoStatus) //todo: very simple for now until we have clear rules from BAs

  override def shouldBeUpdated(intent: TaxYear): Boolean = {
    status == Annual //todo: very simple for now until we have clear rules from BAs
  }
}


////// OptInState ////////////////////////////////////////////////

sealed trait OptInState
trait OneYearOptInState extends OptInState
trait MultiYearOptInState extends OptInState

////// OptInPropositionTypes /////////////////////////////////////

sealed trait OptInPropositionType {
  val proposition: OptInProposition
  def state(): Option[OptInState]
}

case class OneYearOptInProposition private(proposition: OptInProposition) extends OptInPropositionType {
  val intent: OptInTaxYear = proposition.availableOptInYears.head //todo: very simple for now until we have clear rules from BAs

  override def state(): Option[OneYearOptInState] = {
    proposition match {
      //todo: very simple for now until we have clear rules from BAs
      case _ => None
    }
  }
}

case class MultiYearOptInProposition private(proposition: OptInProposition) extends OptInPropositionType {
  override def state(): Option[OptInState] = Some(MultiYearOptInDefault)
}

object MultiYearOptInDefault extends MultiYearOptInState
