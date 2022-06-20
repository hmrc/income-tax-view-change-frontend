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
import org.scalacheck.Prop.{forAll, propBoolean}

object MfaCreditUtilsSpec extends Properties("MfaCreditUtils_validMFACreditDescription") {

  import org.scalacheck.Prop.forAll
  import MfaCreditUtils.validMFACreditDescription

  val defaultPayment = Payment(reference = Some("ref"), amount = Some(0.00), method = Some("method"),
    lot = Some("lot2"), lotItem = Some("lotItem2"), documentDescription = None,
    date = Some("2018-12-12"), transactionId = Some("DOCID02"))

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
    validMFACreditDescription(Some(documentDescription))
  }

  property("Not_validMFACreditDescription") = forAll { (documentDescription: String) =>
    !validMFACreditDescription(Some(documentDescription))
  }

  property("Credit payment") = forAll { negAmount: BigDecimal =>
    (negAmount < 0) ==> {
      val payment = defaultPayment.copy(amount = Some(negAmount))
      payment.validMFACredit()
    }
  }

  property("Debit Payment") = forAll { amount: BigDecimal =>
    (amount >= 0) ==> {
      val payment = defaultPayment.copy(amount = Some(amount))
      !payment.validMFACredit()
    }
  }

}
