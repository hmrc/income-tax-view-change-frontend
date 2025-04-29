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

package connectors

import _root_.helpers.servicemocks.AuditStub
import _root_.helpers.{ComponentSpecBase, WiremockHelper}
import com.github.tomakehurst.wiremock.client.WireMock
import models.core.Nino
import models.repaymentHistory._
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.test.Injecting

import java.time.LocalDate

class RepaymentHistoryConnectorISpec extends AnyWordSpec with ComponentSpecBase with Injecting {

  lazy val connector: RepaymentHistoryConnector = app.injector.instanceOf[RepaymentHistoryConnector]

  val nino = "AA123456A"
  val repaymentId = "ID"

  override def beforeEach(): Unit = {
    WireMock.reset()
    AuditStub.stubAuditing()
  }

  "RepaymentHistoryConnector" when {
    ".getRepaymentHistoryByRepaymentId()" when {
      "sending a request" should {
        "return a successful response" in {
          val responseBody =
            """
              |{
              |  "repaymentsViewerDetails": [
              |    {
              |      "amountApprovedforRepayment": 100.0,
              |      "amountRequested": 200.0,
              |      "repaymentMethod": "BACD",
              |      "totalRepaymentAmount": 300.0,
              |      "repaymentItems": [
              |        {
              |          "repaymentSupplementItem": [
              |            {
              |              "parentCreditReference": "002420002231",
              |              "amount": 400.0,
              |              "fromDate": "2021-07-23",
              |              "toDate": "2021-08-23",
              |              "rate": 12.12
              |            }
              |          ]
              |        }
              |      ],
              |      "estimatedRepaymentDate": "2021-08-21",
              |      "creationDate": "2021-07-21",
              |      "repaymentRequestNumber": "000000003135",
              |      "status": "A"
              |    }
              |  ]
              |}
              |""".stripMargin

          val responseModel = RepaymentHistoryModel(
            List(RepaymentHistory(
                Some(100.0),
                200.0,
                Some("BACD"),
                Some(300.0),
                Some(
                  List(
                    RepaymentItem(
                      List(RepaymentSupplementItem(
                          Some("002420002231"),
                          Some(400.0),
                          Some(LocalDate.parse("2021-07-23")),
                          Some(LocalDate.parse("2021-08-23")),
                          Some(12.12)))))),
                Some(LocalDate.parse("2021-08-21")),
                Some(LocalDate.parse("2021-07-21")),
                "000000003135",
                RepaymentHistoryStatus("A")
              )
            )
          )

          WiremockHelper.stubGet(s"/income-tax-view-change/repayments/$nino/repaymentId/$repaymentId", OK, responseBody)

          val result = connector.getRepaymentHistoryByRepaymentId(Nino(nino), repaymentId).futureValue

          result shouldBe responseModel
          WiremockHelper.verifyGet(s"/income-tax-view-change/repayments/$nino/repaymentId/$repaymentId")
        }
        "return an error when the request fails" in {
          WiremockHelper.stubGet(s"/income-tax-view-change/repayments/$nino/repaymentId/$repaymentId", INTERNAL_SERVER_ERROR, "{}")

          val result = connector.getRepaymentHistoryByRepaymentId(Nino(nino), repaymentId).futureValue

          result shouldBe RepaymentHistoryErrorModel(500, "{}")
          WiremockHelper.verifyGet(s"/income-tax-view-change/repayments/$nino/repaymentId/$repaymentId")
        }
      }
    }
    ".getRepaymentHistoryByNino()" when {
      "sending a request" should {
        "return a successful response" in {
          val responseBody =
            """
              |{
              |  "repaymentsViewerDetails": [
              |    {
              |      "amountApprovedforRepayment": 100.0,
              |      "amountRequested": 200.0,
              |      "repaymentMethod": "BACD",
              |      "totalRepaymentAmount": 300.0,
              |      "repaymentItems": [
              |        {
              |          "repaymentSupplementItem": [
              |            {
              |              "parentCreditReference": "002420002231",
              |              "amount": 400.0,
              |              "fromDate": "2021-07-23",
              |              "toDate": "2021-08-23",
              |              "rate": 12.12
              |            }
              |          ]
              |        }
              |      ],
              |      "estimatedRepaymentDate": "2021-08-21",
              |      "creationDate": "2021-07-21",
              |      "repaymentRequestNumber": "000000003135",
              |      "status": "A"
              |    }
              |  ]
              |}
              |""".stripMargin

          val responseModel = RepaymentHistoryModel(
            List(RepaymentHistory(
              Some(100.0),
              200.0,
              Some("BACD"),
              Some(300.0),
              Some(
                List(
                  RepaymentItem(
                    List(RepaymentSupplementItem(
                      Some("002420002231"),
                      Some(400.0),
                      Some(LocalDate.parse("2021-07-23")),
                      Some(LocalDate.parse("2021-08-23")),
                      Some(12.12)))))),
              Some(LocalDate.parse("2021-08-21")),
              Some(LocalDate.parse("2021-07-21")),
              "000000003135",
              RepaymentHistoryStatus("A")
            )
            )
          )

          WiremockHelper.stubGet(s"/income-tax-view-change/repayments/$nino", OK, responseBody)

          val result = connector.getRepaymentHistoryByNino(Nino(nino)).futureValue

          result shouldBe responseModel
          WiremockHelper.verifyGet(s"/income-tax-view-change/repayments/$nino")
        }
        "return an error when the request fails" in {
          WiremockHelper.stubGet(s"/income-tax-view-change/repayments/$nino", INTERNAL_SERVER_ERROR, "{}")

          val result = connector.getRepaymentHistoryByNino(Nino(nino)).futureValue

          result shouldBe RepaymentHistoryErrorModel(500, "{}")
          WiremockHelper.verifyGet(s"/income-tax-view-change/repayments/$nino")
        }
      }
    }
  }
}
