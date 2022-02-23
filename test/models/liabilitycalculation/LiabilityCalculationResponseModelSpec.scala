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

package models.liabilitycalculation

import play.api.http.Status
import play.api.libs.json._
import testConstants.NewCalcBreakdownUnitTestConstants.liabilityCalculationModelSuccessFull
import testUtils.UnitSpec

import scala.io.Source

class LiabilityCalculationResponseModelSpec extends UnitSpec {

  "LastTaxCalculationResponseMode model" when {
    "successful successModelMinimal" should {
      val successModelMinimal = LiabilityCalculationResponse(
        inputs = Inputs(personalInformation = PersonalInformation(taxRegime = "UK", class2VoluntaryContributions = None)),
        messages = None,
        calculation = None,
        metadata = Metadata(
          calculationTimestamp = "2019-02-15T09:35:15.094Z",
          crystallised = true)
      )
      val expectedJson =
        s"""
           |{
           |  "inputs" : { "personalInformation" : { "taxRegime" : "UK" } },
           |  "metadata" : {
           |    "calculationTimestamp" : "2019-02-15T09:35:15.094Z",
           |    "crystallised" : true
           |  }
           |}
           |""".stripMargin.trim


      "be translated to Json correctly" in {
        Json.toJson(successModelMinimal) shouldBe Json.parse(expectedJson)
      }
      "should convert from json to model" in {
        val calcResponse = Json.fromJson[LiabilityCalculationResponse](Json.parse(expectedJson))
        Json.toJson(calcResponse.get) shouldBe Json.parse(expectedJson)
      }
    }

    "successful successModelFull" should {

      val source = Source.fromURL(getClass.getResource("/liabilityResponsePruned.json"))
      val expectedJsonPruned = try source.mkString finally source.close()

      "be translated to Json correctly" in {
        Json.toJson(liabilityCalculationModelSuccessFull) shouldBe Json.parse(expectedJsonPruned)
      }

      "should convert from json to model" in {
        val calcResponse = Json.fromJson[LiabilityCalculationResponse](Json.parse(expectedJsonPruned))
        Json.toJson(calcResponse.get) shouldBe Json.parse(expectedJsonPruned)
      }
    }

    "not successful" should {
      val errorStatus = 500
      val errorMessage = "Error Message"
      val errorModel = LiabilityCalculationError(Status.INTERNAL_SERVER_ERROR, "Error Message")

      "have the correct Status (500)" in {
        errorModel.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
      "have the correct message" in {
        errorModel.message shouldBe "Error Message"
      }
      "be translated into Json correctly" in {
        Json.prettyPrint(Json.toJson(errorModel)) shouldBe
          (s"""
              |{
              |  "status" : $errorStatus,
              |  "message" : "$errorMessage"
              |}
           """.stripMargin.trim)
      }
    }
  }

}
