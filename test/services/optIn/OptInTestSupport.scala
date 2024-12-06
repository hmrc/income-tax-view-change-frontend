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

package services.optIn

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Mandated, Voluntary}
import services.optIn.core.{CurrentOptInTaxYear, NextOptInTaxYear, OptInProposition}
import services.optout.OptOutProposition

object OptInTestSupport {

  val currentTaxYear = TaxYear.forYearEnd(2024)
  val previousTaxYear = currentTaxYear.previousYear
  val nextTaxYear = currentTaxYear.nextYear

  object Crystallised {
    val YES = true
    val NO = false
  }

  object OneYearOptIn {
    val YES = true
    val NO = false
  }
  object MultiYearOptIn {
    val YES = true
    val NO = false
  }

  object ToBeOffered {

    val NoOffers = Seq()

    val PY = Seq("PY")
    val CY = Seq("CY")
    val NY = Seq("NY")

    val PY_CY_NY = Seq("PY", "CY", "NY")

    val PY_CY = Seq("PY", "CY")
    val CY_NY = Seq("CY", "NY")
    val PY_NY = Seq("PY", "NY")
  }

  object Intent {
    val PY = "PY"
    val CY = "CY"
    val NY = "NY"
  }

  def buildOptInProposition(chosenCurrentYear: Int = 2024): OptInProposition = {

    val currentYear = TaxYear.forYearEnd(chosenCurrentYear)
    val nextYear = currentYear.nextYear

    val currentTaxYearOptIn = CurrentOptInTaxYear(Voluntary, currentYear)
    val nextTaxYearOptIn = NextOptInTaxYear(Voluntary, nextYear, currentTaxYearOptIn)

    OptInProposition(
      currentTaxYearOptIn,
      nextTaxYearOptIn
    )
  }
}