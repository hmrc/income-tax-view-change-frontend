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
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated}

object SignUpProposition {

  def createSignUpProposition(currentYear: TaxYear,
                              currentYearItsaStatus: ITSAStatus,
                              nextYearItsaStatus: ITSAStatus
                            ): SignUpProposition = {

    val currentSignUpTaxYear = CurrentSignUpTaxYear(
      status = currentYearItsaStatus,
      taxYear = currentYear
    )

    val nextYearOptOut = NextSignUpTaxYear(
      status = nextYearItsaStatus,
      taxYear = currentYear.nextYear,
      currentSignUpTaxYear = currentSignUpTaxYear
    )

    SignUpProposition(currentSignUpTaxYear, nextYearOptOut)
  }

}

case class SignUpProposition(currentTaxYear: CurrentSignUpTaxYear, nextTaxYear: NextSignUpTaxYear) {

  private val signUpYears: Seq[SignUpTaxYear] = Seq[SignUpTaxYear](currentTaxYear, nextTaxYear)

  lazy val availableSignUpYears: Seq[SignUpTaxYear] = signUpYears.filter(_.canSignUp)
  
  def isCurrentTaxYear(target: TaxYear): Boolean = currentTaxYear.taxYear == target

  def expectedItsaStatusesAfter(customerIntent: TaxYear): Seq[ITSAStatus] =
    Seq(
      currentTaxYear.expectedItsaStatusAfter(customerIntent),
      nextTaxYear.expectedItsaStatusAfter(customerIntent)
    )
  
}