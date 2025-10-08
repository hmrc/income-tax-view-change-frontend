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

package services.optIn.core

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, NoStatus, Voluntary}

trait OptInTaxYear {
  val taxYear: TaxYear

  def canOptIn: Boolean
}

case class CurrentOptInTaxYear(status: ITSAStatus, taxYear: TaxYear) extends OptInTaxYear {

  def canOptIn: Boolean = status == Annual

  def expectedItsaStatusAfter(customerIntent: TaxYear): ITSAStatus =
    if (customerIntent == taxYear && canOptIn)
      Voluntary
    else
      status
}

case class NextOptInTaxYear(
                             status: ITSAStatus,
                             taxYear: TaxYear,
                             currentOptInTaxYear: CurrentOptInTaxYear
                           ) extends OptInTaxYear {

  def canOptIn: Boolean = canOptInDirectly || canOptInDueToRollover

  private def canOptInDirectly: Boolean = status == Annual

  private def canOptInDueToRollover: Boolean = status == NoStatus && currentOptInTaxYear.status == Annual

  def expectedItsaStatusAfter(customerIntent: TaxYear): ITSAStatus =
    if (canOptInDirectly ||
      (isNextYear(customerIntent) && canOptInDueToRollover))
      Voluntary
    else
      status

  private def isNextYear(customerIntent: TaxYear) = {
    customerIntent == taxYear
  }

}
