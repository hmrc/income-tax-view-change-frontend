/*
 * Copyright 2023 HM Revenue & Customs
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

package financials.models

import financials.models.MfaDebitUtils.{filterMFADebits, isMFADebitMainType}
import financials.testConstants.FinancialDetailsTestConstants.MFADebitsDocumentDetailsWithDueDates
import org.scalacheck.*
import org.scalacheck.Prop.forAll


object MfaDebitUtilsSpec extends Properties("MFADebitType"){

    val MFADebitType: Gen[String] = Gen.oneOf("ITSA PAYE Charge", "ITSA Calc Error Correction", "ITSA Manual Penalty Pre CY-4", "ITSA Misc Charge")
    property("validMFADebitMainType") = forAll(MFADebitType) { mainType =>
        filterMFADebits(MFADebitsEnabled = false, MFADebitsDocumentDetailsWithDueDates.head) &&
          !filterMFADebits(MFADebitsEnabled = true, MFADebitsDocumentDetailsWithDueDates.head) &&
        isMFADebitMainType(Some(mainType))
    }
}
