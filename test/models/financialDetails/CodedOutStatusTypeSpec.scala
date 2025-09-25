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

import enums.CodingOutType
import play.api.libs.json.{JsString, JsSuccess, Json}
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
        val resultNotCollected = CodedOutStatusType.fromCodedOutStatusAndDocumentText(Some("Not Collected"), None)
        resultNotCollected shouldBe Some(Cancelled)
        val resultPartlyCollected = CodedOutStatusType.fromCodedOutStatusAndDocumentText(Some("Partly Collected"), None)
        resultPartlyCollected shouldBe Some(Cancelled)
      }
      "codedOutStatus corresponds to a known type" in {
        val resultAccepted = CodedOutStatusType.fromCodedOutStatusAndDocumentText(None, Some("I"))
        resultAccepted shouldBe Some(Accepted)
        val resultCancelled = CodedOutStatusType.fromCodedOutStatusAndDocumentText(None, Some("C"))
        resultCancelled shouldBe Some(Cancelled)
        val resultFullyCollected = CodedOutStatusType.fromCodedOutStatusAndDocumentText(None, Some("F"))
        resultFullyCollected shouldBe Some(FullyCollected)
        val resultNotCollected = CodedOutStatusType.fromCodedOutStatusAndDocumentText(None, Some("N"))
        resultNotCollected shouldBe Some(Cancelled)
        val resultPartlyCollected = CodedOutStatusType.fromCodedOutStatusAndDocumentText(None, Some("P"))
        resultPartlyCollected shouldBe Some(Cancelled)
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

  "write" should {
    "return a string representation of a type passed" in {
      Accepted.toString shouldBe "Accepted"
    }
  }

  "read" should {
    "return type based on the String passed" in {
      Json.fromJson[CodedOutStatusType](JsString("Class 2 National Insurance")) shouldBe(JsSuccess(Nics2))
      Json.fromJson[CodedOutStatusType](JsString("Balancing payment collected through PAYE tax code")) shouldBe(JsSuccess(Accepted))
      Json.fromJson[CodedOutStatusType](JsString("Fully Collected")) shouldBe(JsSuccess(FullyCollected))
      Json.fromJson[CodedOutStatusType](JsString("Cancelled PAYE Self Assessment")) shouldBe(JsSuccess(Cancelled))
      Json.fromJson[CodedOutStatusType](JsString("Not Collected")) shouldBe(JsSuccess(Cancelled))
      Json.fromJson[CodedOutStatusType](JsString("Partly Collected")) shouldBe(JsSuccess(Cancelled))
    }
  }
}
