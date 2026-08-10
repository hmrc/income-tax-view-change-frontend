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

package common.models.liabilitycalculation

import common.testUtils.UnitSpec
import common.models.liabilitycalculation.CalculationRevisionType.*

class CalculationRevisionTypeSpec extends UnitSpec {

  "CalculationRevisionType.getCalculationRevisionType" when {

    "calculationType is Amendment (AM)" should {

      "return HmrcAutoCorrection when calculationReason is HMRCAutoCorrection" in {
        CalculationRevisionType.getCalculationRevisionType("AM", Some("HMRCAutoCorrection")) shouldBe Some(HmrcAutoCorrection)
      }

      "return HmrcManualCorrection when calculationReason is HMRCmanualCorrection" in {
        CalculationRevisionType.getCalculationRevisionType("AM", Some("HMRCmanualCorrection")) shouldBe Some(HmrcManualCorrection)
      }

      "return CustomerRejection when calculationReason is customerRejectionOfaCorrection" in {
        CalculationRevisionType.getCalculationRevisionType("AM", Some("customerRejectionOfaCorrection")) shouldBe Some(CustomerRejection)
      }

      "return RevenueAmendment when calculationReason is HMRCrevenueamendment" in {
        CalculationRevisionType.getCalculationRevisionType("AM", Some("HMRCrevenueamendment")) shouldBe Some(RevenueAmendment)
      }

      "return Amendment when calculationReason is None" in {
        CalculationRevisionType.getCalculationRevisionType("AM", None) shouldBe Some(Amendment)
      }

      "return Amendment when calculationReason is unrecognized value" in {
        CalculationRevisionType.getCalculationRevisionType("AM", Some("UnknownReason")) shouldBe Some(Amendment)
      }

      "return Amendment when calculationReason is empty string" in {
        CalculationRevisionType.getCalculationRevisionType("AM", Some("")) shouldBe Some(Amendment)
      }
    }

    "calculationType is ConfirmAmendment (CA)" should {

      "return HmrcAutoCorrection when calculationReason is HMRCAutoCorrection" in {
        CalculationRevisionType.getCalculationRevisionType("CA", Some("HMRCAutoCorrection")) shouldBe Some(HmrcAutoCorrection)
      }

      "return HmrcManualCorrection when calculationReason is HMRCmanualCorrection" in {
        CalculationRevisionType.getCalculationRevisionType("CA", Some("HMRCmanualCorrection")) shouldBe Some(HmrcManualCorrection)
      }

      "return CustomerRejection when calculationReason is customerRejectionOfaCorrection" in {
        CalculationRevisionType.getCalculationRevisionType("CA", Some("customerRejectionOfaCorrection")) shouldBe Some(CustomerRejection)
      }

      "return RevenueAmendment when calculationReason is HMRCrevenueamendment" in {
        CalculationRevisionType.getCalculationRevisionType("CA", Some("HMRCrevenueamendment")) shouldBe Some(RevenueAmendment)
      }

      "return Amendment when calculationReason is None" in {
        CalculationRevisionType.getCalculationRevisionType("CA", None) shouldBe Some(Amendment)
      }

      "return Amendment when calculationReason is unrecognized value" in {
        CalculationRevisionType.getCalculationRevisionType("CA", Some("UnknownReason")) shouldBe Some(Amendment)
      }
    }

    "calculationType is not an amendment type" should {

      "return None when calculationType is CR (DeclareCrystallisation)" in {
        CalculationRevisionType.getCalculationRevisionType("CR", Some("HMRCAutoCorrection")) shouldBe None
      }

      "return None when calculationType is DF (DeclareFinalisation)" in {
        CalculationRevisionType.getCalculationRevisionType("DF", Some("HMRCAutoCorrection")) shouldBe None
      }

      "return None when calculationType is CO (Correction)" in {
        CalculationRevisionType.getCalculationRevisionType("CO", None) shouldBe None
      }

      "return None when calculationType is IY (InYear)" in {
        CalculationRevisionType.getCalculationRevisionType("IY", Some("HMRCAutoCorrection")) shouldBe None
      }

      "return None when calculationType is an unrecognized value" in {
        CalculationRevisionType.getCalculationRevisionType("XX", Some("HMRCAutoCorrection")) shouldBe None
      }

      "return None when calculationType is empty string" in {
        CalculationRevisionType.getCalculationRevisionType("", Some("HMRCAutoCorrection")) shouldBe None
      }

      "return None when calculationType is not an amendment type regardless of calculationReason" in {
        CalculationRevisionType.getCalculationRevisionType("IY", None) shouldBe None
      }
    }
  }

  "CalculationRevisionType.correctionAndRevenueAmendmentTypes" should {

    "contain HmrcAutoCorrection" in {
      CalculationRevisionType.correctionAndRevenueAmendmentTypes should contain(HmrcAutoCorrection)
    }

    "contain HmrcManualCorrection" in {
      CalculationRevisionType.correctionAndRevenueAmendmentTypes should contain(HmrcManualCorrection)
    }

    "contain RevenueAmendment" in {
      CalculationRevisionType.correctionAndRevenueAmendmentTypes should contain(RevenueAmendment)
    }

    "not contain Amendment" in {
      CalculationRevisionType.correctionAndRevenueAmendmentTypes should not contain Amendment
    }

    "not contain CustomerRejection" in {
      CalculationRevisionType.correctionAndRevenueAmendmentTypes should not contain CustomerRejection
    }

    "have exactly 3 elements" in {
      CalculationRevisionType.correctionAndRevenueAmendmentTypes.size shouldBe 3
    }

    "be exactly the set of HmrcAutoCorrection, HmrcManualCorrection, and RevenueAmendment" in {
      CalculationRevisionType.correctionAndRevenueAmendmentTypes shouldBe Set(HmrcAutoCorrection, HmrcManualCorrection, RevenueAmendment)
    }
  }
}

