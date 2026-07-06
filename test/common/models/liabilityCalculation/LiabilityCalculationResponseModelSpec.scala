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

package common.models.liabilityCalculation

import common.models.helpers.LiabilityCalculationDataHelper
import common.models.liabilitycalculation.*
import common.testUtils.TestSupport
import play.api.http.Status
import play.api.i18n.Lang
import play.api.libs.json.*

import scala.io.Source

class LiabilityCalculationResponseModelSpec extends LiabilityCalculationDataHelper with TestSupport {

  "LastTaxCalculationResponseMode model" when {

    "successful successModelMinimal" should {
      val successModelMinimal = LiabilityCalculationResponse(
        inputs = Inputs(personalInformation = PersonalInformation(taxRegime = "UK", class2VoluntaryContributions = None)),
        messages = None,
        calculation = None,
        metadata = Metadata(
          calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
          calculationType = "crystallisation",
          calculationReason = Some("customerRequest")
        ),
        submissionChannel = None
      )
      val expectedJson =
        s"""
           |{
           |  "inputs" : { "personalInformation" : { "taxRegime" : "UK" } },
           |  "metadata" : {
           |    "calculationTimestamp" : "2019-02-15T09:35:15.094Z",
           |    "calculationType" : "crystallisation",
           |    "calculationReason" : "customerRequest"
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

    "successful with zero length or null arrays" should {
      val source = Source.fromURL(getClass.getResource("/liabilityResponseArrayTest.json"))
      val arraysTestJson = try source.mkString finally source.close()

      "be translated to Json correctly" in {
        assertJsonEquals(Json.toJson(arrayTestFull), Json.parse(arraysTestJson))
      }
      "should convert from json to model" in {
        val calcModel = Json.fromJson[LiabilityCalculationResponse](Json.parse(arraysTestJson))
        assertJsonEquals(Json.toJson(calcModel.get), Json.parse(arraysTestJson))
      }
    }

    "successful successModelFull" should {

      val source = Source.fromURL(getClass.getResource("/liabilityResponsePruned.json"))
      val expectedJsonPruned = try source.mkString finally source.close()

      "be translated to Json correctly" in {
        assertJsonEquals(Json.toJson(liabilityCalculationModelSuccessful), Json.parse(expectedJsonPruned))
      }

      "should convert from json to model" in {
        val calcResponse = Json.fromJson[LiabilityCalculationResponse](Json.parse(expectedJsonPruned))
        assertJsonEquals(Json.toJson(calcResponse.get), Json.parse(expectedJsonPruned))
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

  "Messages" when {
    "variable values from message for individual" in {
      Messages(errors = errorMessagesIndividual).getErrorMessageVariables(messagesApi, isAgent = false) shouldBe Seq(
        Message("C55012", "05/01/2023"),
        Message("C15507", "£2000"),
        Message("C15510", "10"),
        Message("C55009", ""),
        Message("C55007", "2024-04-05")
      )
    }

    "variable values from message for agent" in {
      Messages(errors = errorMessagesAgent).getErrorMessageVariables(messagesApi, isAgent = true) shouldBe Seq(
        Message("C55012", "05/01/2023"),
        Message("C15507", "£2000"),
        Message("C15510", "10"),
        Message("C55009", ""),
        Message("C55007", "2024-04-05")
      )
    }

    "translate date variable values from messages for individual" in {
      val values = Messages(errors = errorMessagesIndividual).getErrorMessageVariables(messagesApi, isAgent = false)
      Messages.translateMessageDateVariables(values)(messagesApi.preferred(Seq(Lang("cy"))), mockImplicitDateFormatter) shouldBe Seq(
        Message("C55012", "5 Ionawr 2023"),
        Message("C15507", "£2000"),
        Message("C15510", "10"),
        Message("C55009", ""),
        Message("C55007", "5 Ebrill 2024")
      )
    }
    "translate date variable values from messages for agent" in {
      val values = Messages(errors = errorMessagesAgent).getErrorMessageVariables(messagesApi, isAgent = true)
      Messages.translateMessageDateVariables(values)(messagesApi.preferred(Seq(Lang("cy"))), mockImplicitDateFormatter) shouldBe Seq(
        Message("C55012", "5 Ionawr 2023"),
        Message("C15507", "£2000"),
        Message("C15510", "10"),
        Message("C55009", ""),
        Message("C55007", "5 Ebrill 2024")
      )
    }

    "Scottish tax regime info messages" in {
      Messages(info = InfoMessagesScottishTaxRegime).formatMessagesScottishWelshTaxRegime(InfoMessagesScottishTaxRegime.get.toSeq) shouldBe Seq(
        Message("C22225_Scottish", "Your tax has been reduced because of Gift Aid charity donations - the Scottish Basic Rate of Income Tax is higher than the rate at which charities have obtained relief."),
        Message("C22226_Scottish", "Your tax has increased because of Gift Aid charity donations - the Scottish Basic Rate of Income Tax is lower than the rate at which charities have obtained relief."),
      )
    }

    "Welsh tax regime info messages" in {
      Messages(info = InfoMessagesWelshTaxRegime).formatMessagesScottishWelshTaxRegime(InfoMessagesWelshTaxRegime.get.toSeq) shouldBe Seq(
        Message("C22225", "Your tax has been reduced because of Gift Aid charity donations - the Welsh Basic Rate of Income Tax is higher than the rate at which charities have obtained relief."),
        Message("C22226", "Your tax has increased because of Gift Aid charity donations - the Welsh Basic Rate of Income Tax is lower than the rate at which charities have obtained relief."),
      )
    }
  }

  "hasAnAmendment" should {
    "return true" when {
      "calculationType is AM" in {
        val successModelMinimal = LiabilityCalculationResponse(
          inputs = Inputs(personalInformation = PersonalInformation(taxRegime = "UK", class2VoluntaryContributions = None)),
          messages = None,
          calculation = None,
          metadata = Metadata(
            calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
            calculationType = "AM",
            calculationReason = Some("customerRequest"),
          ),
          submissionChannel = None
        )

        successModelMinimal.metadata.hasAnAmendment shouldBe true
      }
      "calculationType is CA" in {
        val successModelMinimal = LiabilityCalculationResponse(
          inputs = Inputs(personalInformation = PersonalInformation(taxRegime = "UK", class2VoluntaryContributions = None)),
          messages = None,
          calculation = None,
          metadata = Metadata(
            calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
            calculationType = "CA",
            calculationReason = Some("customerRequest"),
          ),
          submissionChannel = None
        )

        successModelMinimal.metadata.hasAnAmendment shouldBe true
      }
    }

    "return false" when {
      "calculationType is not AM or CA" in {
        val successModelMinimal = LiabilityCalculationResponse(
          inputs = Inputs(personalInformation = PersonalInformation(taxRegime = "UK", class2VoluntaryContributions = None)),
          messages = None,
          calculation = None,
          metadata = Metadata(
            calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
            calculationType = "DF",
            calculationReason = Some("customerRequest"),
          ),
          submissionChannel = None
        )

        successModelMinimal.metadata.hasAnAmendment shouldBe false
      }
    }
  }
}
