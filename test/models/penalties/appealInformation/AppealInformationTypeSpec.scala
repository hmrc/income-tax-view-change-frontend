/*
 * Copyright 2025 HM Revenue & Customs
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

package models.penalties.appealInformation

import play.api.libs.json.{JsValue, Json}
import testUtils.TestSupport

class AppealInformationTypeSpec extends TestSupport {

  val appealInfoAsModel: AppealInformationType = AppealInformationType(
    appealStatus = Some(UnderAppealStatus),
    appealLevel = Some(HmrcAppealLevel),
    appealDescription = Some("Some value")
  )

  val appealInfoAsModelWithDefaultedAppealLevel: AppealInformationType = AppealInformationType(
    appealStatus = Some(UnappealableStatus),
    appealLevel = None,
    appealDescription = Some("Some value")
  )

  val appealInfoAsJson: JsValue = Json.parse(
    """
      |{
      |    "appealStatus": "A",
      |    "appealLevel": "01",
      |    "appealDescription": "Some value"
      |}
      |""".stripMargin)

  val appealInfoAsJsonEmptyAppealLevel: JsValue = Json.parse(
    """
      |{
      |    "appealStatus": "99",
      |    "appealDescription": "Some value"
      |}
      |""".stripMargin)

  val appealInfoAsJsonDefaultAppealLevel: JsValue = Json.parse(
    """
      |{
      |    "appealStatus": "99",
      |    "appealLevel": "01",
      |    "appealDescription": "Some value"
      |}
      |""".stripMargin)

  "AppealInformationType" should {
    "be readable from JSON" in {
      val result = Json.fromJson(appealInfoAsJson)(AppealInformationType.reads)
      result.isSuccess shouldBe true
      result.get shouldBe appealInfoAsModel
    }

    "be readable from JSON when the appealLevel is missing (when appealStatus is UNAPPEALABLE)" in {
      val result = Json.fromJson(appealInfoAsJsonEmptyAppealLevel)(AppealInformationType.reads)
      result.isSuccess shouldBe true
      result.get shouldBe appealInfoAsModelWithDefaultedAppealLevel
    }

    "be writable to JSON" in {
      val result = Json.toJson(appealInfoAsModel)(AppealInformationType.writes)
      result shouldBe appealInfoAsJson
    }

    "be writable to JSON - setting EMPTY appeal level to HMRC for unappealable status" in {
      val result = Json.toJson(appealInfoAsModelWithDefaultedAppealLevel)(AppealInformationType.writes)
      result shouldBe appealInfoAsJsonDefaultAppealLevel
    }
  }
}
