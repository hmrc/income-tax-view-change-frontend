/*
 * Copyright 2021 HM Revenue & Customs
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

package views.obligationTemplates

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import assets.BaseTestConstants.{testMtdItUser, testPropertyIncomeId, testSelfEmploymentId}
import implicits.ImplicitDateFormatterImpl
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModel, ReportDeadlinesModel}
import org.jsoup.nodes.Element
import testUtils.ViewSpec
import views.html.obligationTemplates.previousObligationsHelper

class PreviousObligationsHelperSpec extends ViewSpec {

  object PreviousObligationsHelperMessages {
    def formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
    val noPreviousObligations: String = "No previously submitted updates"
    val propertyIncome: String = "Property Income"
    val finalDeclaration: String = "Tax year - Final check"
    val annualUpdate: String = "Annual update"
    val quarterlyUpdate: String = "Quarterly update"
    val finalUpdate: String = "Declaration"
    def dateFrom(start: LocalDate, end: LocalDate): String = {
      val from: String = formatDate(start)
      val to: String = formatDate(end)
      s"$from to $to"
    }
    def dateDue(date: LocalDate): String = s"Was due on ${formatDate(date)}"
    val submittedOn: String = "Submitted on"
  }

  implicit val mockImplicitDateFormatter: ImplicitDateFormatterImpl = new ImplicitDateFormatterImpl(mockLanguageUtils)


  class TestSetup(previousObligations: ObligationsModel) extends Setup(previousObligationsHelper(previousObligations, mockImplicitDateFormatter))

  val date: LocalDate = LocalDate.now.minusYears(1)
  val reportDeadline: ReportDeadlineModel = ReportDeadlineModel(date, date.plusMonths(1), date.plusMonths(2), "Quarterly", Some(date.plusMonths(1)), "#001")

  def basicDeadline(identification: String, obligationType: String): ReportDeadlinesModel = ReportDeadlinesModel(identification, List(reportDeadline.copy(obligationType = obligationType)))

  val basicBusinessDeadline: ReportDeadlinesModel = basicDeadline(testSelfEmploymentId, "Quarterly")
  val basicPropertyDeadline: ReportDeadlinesModel = basicDeadline(testPropertyIncomeId, "EOPS").copy(identification = testPropertyIncomeId)
  val basicCrystallisedDeadline: ReportDeadlinesModel = basicDeadline(testMtdItUser.mtditid, "Crystallised").copy(identification = testMtdItUser.mtditid)

  val obligationModelWithAllTypes: ObligationsModel = ObligationsModel(Seq(
    basicBusinessDeadline, basicPropertyDeadline, basicCrystallisedDeadline
  ))

  val obligationModelWithDifferentTimes: ObligationsModel = ObligationsModel(Seq(
    basicBusinessDeadline.copy(obligations = basicBusinessDeadline.obligations.map(_.copy(dateReceived = Some(date.minusYears(1))))),
    basicPropertyDeadline.copy(obligations = basicPropertyDeadline.obligations.map(_.copy(dateReceived = Some(date.minusYears(2))))),
    basicCrystallisedDeadline.copy(obligations = basicCrystallisedDeadline.obligations.map(_.copy(dateReceived = Some(date.minusYears(3)))))
  ))

  val obligationModelWithSingleProperty: ObligationsModel = ObligationsModel(Seq(basicPropertyDeadline))
  val obligationModelWithSingleBusiness: ObligationsModel = ObligationsModel(Seq(basicBusinessDeadline))
  val obligationModelWithFinalDeclaration: ObligationsModel = ObligationsModel(Seq(basicCrystallisedDeadline))

  "previousObligationsHelper" should {
    "display no previous obligations" when {
      "there are no previous obligations" in new TestSetup(previousObligations = ObligationsModel(Nil)) {
        document.selectHead("div").text shouldBe PreviousObligationsHelperMessages.noPreviousObligations
      }
    }

    "display an update which is for property" in new TestSetup(previousObligations = obligationModelWithSingleProperty) {
      val incomeSourceType: Element = document.selectHead("div > div:nth-of-type(1) > div:nth-of-type(1)")
      val updateType: Element = document.selectHead("div > div:nth-of-type(1) > div:nth-of-type(2)")
      val dateFrom: Element = document.selectHead("div > div:nth-of-type(1) > div:nth-of-type(3)")
      val dateDue: Element = document.selectHead("div > div:nth-of-type(1) > div:nth-of-type(4)")
      val submittedOnLabel: Element = document.selectHead("div > div:nth-of-type(2) > div:nth-of-type(1)")
      val submittedOnDate: Element = document.selectHead("div > div:nth-of-type(2) > div:nth-of-type(2)")

      incomeSourceType.text shouldBe PreviousObligationsHelperMessages.propertyIncome
      updateType.text shouldBe PreviousObligationsHelperMessages.annualUpdate
      dateFrom.text shouldBe PreviousObligationsHelperMessages.dateFrom(date, date.plusMonths(1))
      dateDue.text shouldBe PreviousObligationsHelperMessages.dateDue(date.plusMonths(2))
      submittedOnLabel.text shouldBe PreviousObligationsHelperMessages.submittedOn
      submittedOnDate.text shouldBe PreviousObligationsHelperMessages.formatDate(date.plusMonths(1))
    }

    "display an update which is for business" in new TestSetup(previousObligations = obligationModelWithSingleBusiness) {
      val incomeSourceType: Element = document.selectHead("div > div:nth-of-type(1) > div:nth-of-type(1)")
      val updateType: Element = document.selectHead("div > div:nth-of-type(1) > div:nth-of-type(2)")
      val dateFrom: Element = document.selectHead("div > div:nth-of-type(1) > div:nth-of-type(3)")
      val dateDue: Element = document.selectHead("div > div:nth-of-type(1) > div:nth-of-type(4)")
      val submittedOnLabel: Element = document.selectHead("div > div:nth-of-type(2) > div:nth-of-type(1)")
      val submittedOnDate: Element = document.selectHead("div > div:nth-of-type(2) > div:nth-of-type(2)")

      incomeSourceType.text shouldBe "business"
      updateType.text shouldBe PreviousObligationsHelperMessages.quarterlyUpdate
      dateFrom.text shouldBe PreviousObligationsHelperMessages.dateFrom(date, date.plusMonths(1))
      dateDue.text shouldBe PreviousObligationsHelperMessages.dateDue(date.plusMonths(2))
      submittedOnLabel.text shouldBe PreviousObligationsHelperMessages.submittedOn
      submittedOnDate.text shouldBe PreviousObligationsHelperMessages.formatDate(date.plusMonths(1))
    }

    "display an update for a final declaration" in new TestSetup(previousObligations = obligationModelWithFinalDeclaration) {
      val incomeSourceType: Element = document.selectHead("div > div:nth-of-type(1) > div:nth-of-type(1)")
      val updateType: Element = document.selectHead("div > div:nth-of-type(1) > div:nth-of-type(2)")
      val dateFrom: Element = document.selectHead("div > div:nth-of-type(1) > div:nth-of-type(3)")
      val dateDue: Element = document.selectHead("div > div:nth-of-type(1) > div:nth-of-type(4)")
      val submittedOnLabel: Element = document.selectHead("div > div:nth-of-type(2) > div:nth-of-type(1)")
      val submittedOnDate: Element = document.selectHead("div > div:nth-of-type(2) > div:nth-of-type(2)")

      incomeSourceType.text shouldBe PreviousObligationsHelperMessages.finalDeclaration
      updateType.text shouldBe PreviousObligationsHelperMessages.finalUpdate
      dateFrom.text shouldBe PreviousObligationsHelperMessages.dateFrom(date, date.plusMonths(1))
      dateDue.text shouldBe PreviousObligationsHelperMessages.dateDue(date.plusMonths(2))
      submittedOnLabel.text shouldBe PreviousObligationsHelperMessages.submittedOn
      submittedOnDate.text shouldBe PreviousObligationsHelperMessages.formatDate(date.plusMonths(1))
    }

    "display multiple updates in the correct order" in new TestSetup(previousObligations = obligationModelWithDifferentTimes) {
      val firstObligation: Element = document.selectHead("#row-0")
      val secondObligation: Element = document.selectHead("#row-1")
      val thirdObligation: Element = document.selectHead("#row-2")

      firstObligation.select("div > div:nth-of-type(1) > div:nth-of-type(1)").text shouldBe "business"
      secondObligation.select("div > div:nth-of-type(1) > div:nth-of-type(1)").text shouldBe PreviousObligationsHelperMessages.propertyIncome
      thirdObligation.select("div > div:nth-of-type(1) > div:nth-of-type(1)").text shouldBe PreviousObligationsHelperMessages.finalDeclaration
    }
  }

}
