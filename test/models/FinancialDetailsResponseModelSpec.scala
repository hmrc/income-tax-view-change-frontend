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

package models

import testConstants.FinancialDetailsTestConstants._

import java.time.LocalDate
import models.financialDetails._
import org.scalatest.Matchers
import play.api.libs.json.{JsSuccess, Json}
import testUtils.UnitSpec

class FinancialDetailsResponseModelSpec  extends UnitSpec with Matchers {

  "The ChargesModel" should {

    "be formatted to JSON correctly" in {
      Json.toJson[FinancialDetailsModel](testValidFinancialDetailsModel) shouldBe testValidFinancialDetailsModelJson
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[FinancialDetailsModel](testValidFinancialDetailsModelJson).fold(
        invalid => invalid,
        valid => valid) shouldBe testValidFinancialDetailsModel
    }

  }

  "The ChargesErrorModel" should {

    "be formatted to JSON correctly" in {
      Json.toJson[FinancialDetailsErrorModel](testFinancialDetailsErrorModel) shouldBe testFinancialDetailsErrorModelJson
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[FinancialDetailsErrorModel](testFinancialDetailsErrorModelJson) shouldBe JsSuccess(testFinancialDetailsErrorModel)
    }
  }

  "dunningLockExists" should {
    val dunningLock = Some("Stand over order")
    val unsupportedLock = Some("Disputed debt")

    def financialDetailsModelWithDunningLock: FinancialDetailsModel = financialDetailsModel(dunningLock = dunningLock)
    def financialDetailsModelWithUnsupportedLock: FinancialDetailsModel = financialDetailsModel(dunningLock = unsupportedLock)
    def financialDetailsModelWithoutDunningLock: FinancialDetailsModel = financialDetailsModel()

    "return true when there is a dunningLock against a charge" in {
      financialDetailsModelWithDunningLock.dunningLockExists(id1040000123) shouldBe true
    }

    "return false when there is an unsupported dunningLock against a charge" in {
      financialDetailsModelWithUnsupportedLock.dunningLockExists(id1040000123) shouldBe false
    }

    "return true when there is not a dunningLock against a charge" in {
      financialDetailsModelWithoutDunningLock.dunningLockExists(id1040000123) shouldBe false
    }
  }

  "getDueDateFor" should {
    val fd1 = FinancialDetail(
      taxYear = "2018",
      mainType = Some("SA Payment on Account 2"),
      transactionId = Some("transid1"),
      items = Some(Seq(SubItem(Some("2017-01-31")), SubItem(Some("2018-01-31"))))
    )
    val fd2 = FinancialDetail(
      taxYear = "2017",
      mainType = Some("SA Payment on Account 1"),
      transactionId = Some("transid2"),
      items = Some(Seq(SubItem(Some("2017-02-28")), SubItem(Some("2018-02-28"))))
    )
    val dd1 = DocumentDetail(taxYear = "2017",
      transactionId = "transid2",
      documentDescription = Some("ITSA- POA 1"),
      documentText = Some("documentText"),
      outstandingAmount = None,
      originalAmount = None,
      documentDate = LocalDate.parse("2018-03-21"))

    val fdm: FinancialDetailsModel = FinancialDetailsModel(BalanceDetails(1,2,3), List.empty, List(fd1, fd2))

    "return the right due date" in {
      fdm.getDueDateFor(dd1).get.toString shouldBe "2017-02-28"
    }
  }


  "getAllDueDates" should {

    val fd1 = FinancialDetail(
      taxYear = "2017",
      mainType = Some("SA Payment on Account 1"),
      transactionId = Some("transid1"),
      items = Some(Seq(SubItem(Some("2017-01-31")), SubItem(Some("2018-01-31"))))
    )
    val fd2 = FinancialDetail(
      taxYear = "2017",
      mainType = Some("SA Payment on Account 2"),
      transactionId = Some("transid2"),
      items = Some(Seq(SubItem(Some("2017-02-28")), SubItem(Some("2018-02-28"))))
    )
    val fd3 = FinancialDetail(
      taxYear = "2018",
      mainType = Some("SA Payment on Account 1"),
      transactionId = Some("transid1"),
      items = Some(Seq(SubItem(Some("2017-02-28")), SubItem(Some("2018-02-28"))))
    )
    val fd4 = FinancialDetail(
      taxYear = "2018",
      mainType = Some("SA Payment on Account 2"),
      transactionId = Some("transid2"),
      items = Some(Seq(SubItem(Some("2017-02-28")), SubItem(Some("2018-02-28"))))
    )

    val dd1 = DocumentDetail(taxYear = "2017",
      transactionId = "transid1",
      documentDescription = Some("ITSA- POA 1"),
      documentText = Some("documentText"),
      outstandingAmount = None,
      originalAmount = None,
      documentDate = LocalDate.parse("2018-03-21"))

    val dd2 = DocumentDetail(taxYear = "2018",
      transactionId = "transid2",
      documentDescription = Some("ITSA - POA 2"),
      documentText = Some("documentText2"),
      outstandingAmount = None,
      originalAmount = None,
      documentDate = LocalDate.parse("2018-03-21"))

    val fdm: FinancialDetailsModel = FinancialDetailsModel(BalanceDetails(1,2,3), List(dd1, dd2), List(fd1, fd2, fd3, fd4))

    "return a list of due dates" in {
      fdm.getAllDueDates.map(_.toString) shouldBe List("2017-01-31", "2017-02-28")
    }
  }
}
