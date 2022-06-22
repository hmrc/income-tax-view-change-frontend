/*
 * Copyright 2022 HM Revenue & Customs
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

package models.financialDetails

object MfaCreditUtils {
  private val mfaCreditDescription: Map[Int, String] = Map(
    4004 -> "ITSA Overpayment Relief",
    4005 -> "ITSA Standalone Claim",
    4006 -> "ITSA Averaging Adjustment",
    4007 -> "ITSA Literary Artistic Spread",
    4008 -> "ITSA Loss Relief Claim",
    4009 -> "ITSA Post Cessation Claim",
    4025 -> "ITSA Pension Relief Claim",
    4011 -> "ITSA PAYE in year Repayment",
    4012 -> "ITSA NPS Overpayment",
    4013 -> "ITSA In year Rept pension schm",
    4014 -> "ITSA Increase in PAYE Credit",
    4015 -> "ITSA CIS Non Resident Subbie",
    4016 -> "ITSA CIS Incorrect Deductions",
    4017 -> "ITSA Stand Alone Assessment",
    4018 -> "ITSA Infml Dschrg Cntrct Sett",
    4019 -> "ITSA Third Party Rept - FIS",
    4024 -> "ITSA CGT Adjustments",
    4021 -> "ITSA EIS Carry Back Claims",
    4022 -> "ITSA Calc Error Correction",
    4023 -> "ITSA Misc Credit"
  )

  def validMFACreditDescription(documentDescription: Option[String]): Boolean = {
    documentDescription
      .map(mfaCreditDescription.values.toList.contains(_))
      .getOrElse(false)
  }
}
