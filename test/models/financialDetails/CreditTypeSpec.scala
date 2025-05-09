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

package models.financialDetails

import testUtils.UnitSpec

class CreditTypeSpec extends UnitSpec {

  "CreditType" should {
    "be MFA" when {
      "mainTransaction is between 4004 and 4025, excluding 4010 and 4020" in {
        val mfaCodes = Range.inclusive(4004, 4025)
          .filterNot(_ == 4010).filterNot(_ == 4020)
          .toList

        for (mainTransaction <- mfaCodes) {
          TransactionType.fromCode(s"$mainTransaction") shouldBe Some(MfaCreditType)
        }
      }
    }
    "be CutOver" when {
      "mainTransaction is 6110" in {
        TransactionType.fromCode("6110") shouldBe Some(CutOverCreditType)
      }
    }

    "be BalancingCharge" when {
      "mainTransaction is 4905" in {
        TransactionType.fromCode("4905") shouldBe Some(BalancingChargeCreditType)
      }
    }

    "be RepaymentInterest" when {
      "mainTransaction is 6020" in {
        TransactionType.fromCode("6020") shouldBe Some(RepaymentInterest)
      }
    }

    "be None" when {
      "mainTransaction is any other" in {
        TransactionType.fromCode("9999") shouldBe None
      }
    }
  }
}
