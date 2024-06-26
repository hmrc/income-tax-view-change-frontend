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

package services.optout

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{ITSAStatus, NoStatus, Voluntary}

trait OptOutTaxYear {
  val taxYear: TaxYear

  def canOptOut: Boolean

  def shouldBeUpdated(intent: OptOutTaxYear): Boolean
}

case class CurrentOptOutTaxYear(status: ITSAStatus, taxYear: TaxYear) extends OptOutTaxYear {
  def canOptOut: Boolean = status == Voluntary

  override def shouldBeUpdated(intent: OptOutTaxYear): Boolean = {
    val intentTaxYear = intent.taxYear
    canOptOut && (taxYear.isSameAs(intentTaxYear) || taxYear.isAfter(intentTaxYear))
  }
}

case class NextOptOutTaxYear(status: ITSAStatus, taxYear: TaxYear, currentTaxYear: CurrentOptOutTaxYear) extends OptOutTaxYear {
  def canOptOut: Boolean = status == Voluntary ||
    (currentTaxYear.status == Voluntary && status == NoStatus)

  override def shouldBeUpdated(intent: OptOutTaxYear): Boolean = {
    val intentTaxYear = intent.taxYear
    val isIntentOrAfter = taxYear.isSameAs(intentTaxYear) || taxYear.isAfter(intentTaxYear)
    val nextYearIsVoluntary = status == ITSAStatus.Voluntary
    val nextYearIsIntent = intentTaxYear.isSameAs(taxYear)
    val shouldUpdateNextYear = (isIntentOrAfter && nextYearIsVoluntary) || nextYearIsIntent
    canOptOut && shouldUpdateNextYear
  }
}

case class PreviousOptOutTaxYear(status: ITSAStatus, taxYear: TaxYear, crystallised: Boolean) extends OptOutTaxYear {
  def canOptOut: Boolean = status == Voluntary && !crystallised

  override def shouldBeUpdated(intent: OptOutTaxYear): Boolean = {
    val intentTaxYear = intent.taxYear
    canOptOut && taxYear.isSameAs(intentTaxYear)
  }
}