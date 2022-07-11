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

import org.scalacheck.Prop.propBoolean
import org.scalacheck.{Gen, Properties}

object MfaDebitUtilsSpec extends Properties("MfaDebitUtils_validMFADebitDescription") {
  import MfaDebitUtils.validMFADebitDescription
  import org.scalacheck.Prop.forAll

  val validMfaDebitDescription = Gen.oneOf(
  "ITSA PAYE Charge",
    "ITSA Calc Error Correction",
    "ITSA Manual Penalty Pre CY-4",
    "ITSA Misc Charge"
  )

  property("ValidMFADebitDescription") = forAll(validMfaDebitDescription) { documentDescription =>
    validMFADebitDescription(Some(documentDescription))
  }

  property("Not_validMFADebitDescription") = forAll { (documentDescription: String) =>
    !validMFADebitDescription(Some(documentDescription))
  }

}
