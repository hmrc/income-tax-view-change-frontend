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

package services.reportingObligations.signUp.core

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, NoStatus, Voluntary}

trait SignUpTaxYear {
  val taxYear: TaxYear

  def canSignUp: Boolean
}

case class CurrentSignUpTaxYear(status: ITSAStatus, taxYear: TaxYear) extends SignUpTaxYear {

  def canSignUp: Boolean = status == Annual

  def expectedItsaStatusAfter(customerIntent: TaxYear): ITSAStatus =
    if (customerIntent == taxYear && canSignUp)
      Voluntary
    else
      status
}

case class NextSignUpTaxYear(
                              status: ITSAStatus,
                              taxYear: TaxYear,
                              currentSignUpTaxYear: CurrentSignUpTaxYear
                           ) extends SignUpTaxYear {

  def canSignUp: Boolean = canSignUpDirectly || canSignUpDueToRollover

  private def canSignUpDirectly: Boolean = status == Annual

  private def canSignUpDueToRollover: Boolean = status == NoStatus && currentSignUpTaxYear.status == Annual

  def expectedItsaStatusAfter(customerIntent: TaxYear): ITSAStatus =
    if (canSignUpDirectly ||
      (isNextYear(customerIntent) && canSignUpDueToRollover))
      Voluntary
    else
      status

  private def isNextYear(customerIntent: TaxYear) = {
    customerIntent == taxYear
  }

}
