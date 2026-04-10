/*
 * Copyright 2026 HM Revenue & Customs
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

package testOnly.utils

import testOnly.forms.customLoginV2.CustomUserBuilderForm

object UserCodeUtils {
  def generateUserCode(customUserInfo: CustomUserBuilderForm): String = {

    val userTypeCode = getUserTypeCode(customUserInfo.userType)
    val incomeSourceCode = getIncomeSourceCode(customUserInfo.activeSoleTrader, customUserInfo.activeUkProperty, customUserInfo.activeForeignProperty)
    val itsaStatusCode = getItsaStatusCode(
      customUserInfo.previousYearCrystallisationStatus,
      customUserInfo.previousYearItsaStatus,
      customUserInfo.currentYearItsaStatus,
      customUserInfo.nextYearItsaStatus
    )

    s"$userTypeCode-$incomeSourceCode-$itsaStatusCode"
  }

  private def getUserTypeCode(userType: String) = {
    userType match {
      case "individual"      => "U1"
      case "primaryAgent"    => "U2"
      case "supportingAgent" => "U3"
      case _ => throw new IllegalArgumentException(s"Invalid user type: $userType")
    }
  }

  private def getIncomeSourceCode(activeSoleTrader: Boolean, activeUkProperty: Boolean, activeForeignProperty: Boolean) = {
    val soleTraderCode = if (activeSoleTrader) "S1" else "S0"
    val ukPropertyCode = if (activeUkProperty) "P1" else "P0"
    val foreignPropertyCode = if (activeForeignProperty) "F1" else "F0"
    s"$soleTraderCode-$ukPropertyCode-$foreignPropertyCode"
  }

  private def getItsaStatusCode(cyMinusOneCrystallisationStatus: String, previousYearItsaStatus: String, currentYearItsaStatus: String, nextYearItsaStatus: String) = {
    def itsaCode(status: String) = {
      status match {
        case "Annual" => "1"
        case "MTD Voluntary" => "2"
        case "MTD Mandated" => "3"
        case "MTD Exempt" => "4"
        case "Digitally Exempt" => "5"
        case "No Status" => "6"
        case "Dormant" => "7"
        case _ => throw new IllegalArgumentException(s"Invalid ITSA status: $status")
      }
    }

    val crystallisationStatusCode = cyMinusOneCrystallisationStatus match {
      case "Crystallised" => "PYF1"
      case "Not Crystallised" => "PYF1"
      case _ => throw new IllegalArgumentException(s"Invalid crystallisation status: $cyMinusOneCrystallisationStatus")
    }

    val previousYearItsaCode = s"PY${itsaCode(previousYearItsaStatus)}"
    val currentYearItsaCode = s"CY${itsaCode(currentYearItsaStatus)}"
    val nextYearItsaCode = s"NY${itsaCode(nextYearItsaStatus)}"

    s"$crystallisationStatusCode-$previousYearItsaCode-$currentYearItsaCode-$nextYearItsaCode"
  }
}
