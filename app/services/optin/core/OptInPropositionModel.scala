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

package services.optin.core

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, NoStatus, Voluntary}

/* *******************************************************************************************************************
  OptInTaxYear
 ******************************************************************************************************************* */
trait OptInTaxYear {
  val taxYear: TaxYear
  def canOptIn: Boolean
}
case class CurrentOptInTaxYear(status: ITSAStatus, taxYear: TaxYear) extends OptInTaxYear {
  def canOptIn: Boolean = status == Annual
}
case class NextOptInTaxYear(status: ITSAStatus, taxYear: TaxYear, currentOptInTaxYear: CurrentOptInTaxYear) extends OptInTaxYear {
  def canOptIn: Boolean = (status == Annual) || (currentOptInTaxYear.status == Annual && status == NoStatus)
  //todo check the no-status rule here?
}

/* *******************************************************************************************************************
  OptInPropositionTypes
 ******************************************************************************************************************* */
sealed trait OptInPropositionTypes {
  val proposition: OptInProposition
}
case class OneYearOptInProposition private(proposition: OptInProposition) extends OptInPropositionTypes {
  val intent: OptInTaxYear = proposition.availableOptInYears.head
}
case class MultiYearOptInProposition private(proposition: OptInProposition) extends OptInPropositionTypes