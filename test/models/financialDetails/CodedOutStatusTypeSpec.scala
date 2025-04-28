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

package models.financialDetails

import testUtils.UnitSpec

class CodedOutStatusTypeSpec extends UnitSpec{

  "CodedOutStatusType.fromCodedOutStatusAndDocumentText" should {
    "correctly identify the coded out status" when {
      "documentText corresponds to a known type" in {
        val resultAccepted = CodedOutStatusType.fromCodedOutStatusAndDocumentText(Some("Balancing payment collected through PAYE tax code"), None)
        resultAccepted shouldBe Some(Accepted)
        val resultCancelled = CodedOutStatusType.fromCodedOutStatusAndDocumentText(Some("Cancelled PAYE Self Assessment"), None)
        resultCancelled shouldBe Some(Cancelled)
        val resultNics2 = CodedOutStatusType.fromCodedOutStatusAndDocumentText(Some("Class 2 National Insurance"), None)
        resultNics2 shouldBe Some(Nics2)
        val resultFullyCollected = CodedOutStatusType.fromCodedOutStatusAndDocumentText(Some("Fully Collected"), None)
        resultFullyCollected shouldBe Some(FullyCollected)
      }
      "codedOutStatus corresponds to a known type" in {
        val resultAccepted = CodedOutStatusType.fromCodedOutStatusAndDocumentText(None, Some("I"))
        resultAccepted shouldBe Some(Accepted)
        val resultCancelled = CodedOutStatusType.fromCodedOutStatusAndDocumentText(None, Some("C"))
        resultCancelled shouldBe Some(Cancelled)
        val resultFullyCollected = CodedOutStatusType.fromCodedOutStatusAndDocumentText(None, Some("F"))
        resultFullyCollected shouldBe Some(FullyCollected)
      }
    }
    "return None" when {
      "inputs fields are empty" in {
        val result = CodedOutStatusType.fromCodedOutStatusAndDocumentText(None, None)
        result shouldBe None
      }
      "documentText and codedOutStatus are not recognised" in {
        val result = CodedOutStatusType.fromCodedOutStatusAndDocumentText(Some("Test"), Some("Nonsense"))
        result shouldBe None
      }
    }
  }


}
