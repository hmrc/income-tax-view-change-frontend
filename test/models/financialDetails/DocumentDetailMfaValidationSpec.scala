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

import org.scalacheck.{Gen, Properties}
import testConstants.FinancialDetailsTestConstants.fullDocumentDetailModel

object DocumentDetailMfaValidationSpec extends Properties("DocumentDetail_validMFACreditDescription") {

    import org.scalacheck.Prop.forAll

    val validMfaCreditDescription = Gen.oneOf(
      "ITSA Overpayment Relief",
      "ITSA Standalone Claim",
      "ITSA Averaging Adjustment",
      "ITSA Literary Artistic Spread",
      "ITSA Loss Relief Claim",
      "ITSA Post Cessation Claim",
      "ITSA Pension Relief Claim",
      "ITSA PAYE in year Repayment",
      "ITSA NPS Overpayment",
      "ITSA In year Rept pension schm",
      "ITSA Increase in PAYE Credit",
      "ITSA CIS Non Resident Subbie",
      "ITSA CIS Incorrect Deductions",
      "ITSA Stand Alone Assessment",
      "ITSA Infml Dschrg Cntrct Sett",
      "ITSA Third Party Rept - FIS",
      "ITSA CGT Adjustments",
      "ITSA EIS Carry Back Claims",
      "ITSA Calc Error Correction",
      "ITSA Misc Credit"
    )

    property("ValidMFACreditDescription") = forAll(validMfaCreditDescription) { documentDescription =>
      fullDocumentDetailModel
        .copy(documentDescription = Some(documentDescription))
        .validMFACreditDescription()
    }

  property("Not_validMFACreditDescription") = forAll{ ( documentDescription : String) =>
    fullDocumentDetailModel
      .copy(documentDescription = Some(documentDescription))
      .validMFACreditDescription() == false
  }

}
