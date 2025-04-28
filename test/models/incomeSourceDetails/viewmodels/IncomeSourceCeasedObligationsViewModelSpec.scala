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

package models.incomeSourceDetails.viewmodels

import enums.IncomeSourceJourney.SelfEmployment
import models.incomeSourceDetails.TaxYear
import services.DateServiceInterface
import testConstants.incomeSources.IncomeSourcesObligationsTestConstants.{quarterlyDatesYearOneSimple, quarterlyObligationDatesFull}
import testUtils.UnitSpec

import java.time.{LocalDate, Month}

class IncomeSourceCeasedObligationsViewModelSpec extends UnitSpec {

  val day: LocalDate = LocalDate.of(2022, 1, 10)
  val taxYearEnd: Int = day.getYear

  val currentDate: LocalDate = day
  val finalDeclarationDates: DatesModel = DatesModel(day, day.plusDays(1), day.plusDays(2), "C", isFinalDec = true, obligationType = "Crystallisation")
  val finalDeclarationDatesOverDue: DatesModel = DatesModel(day, day.minusDays(1), day.minusDays(2), "C", isFinalDec = true, obligationType = "Crystallisation")
  val finalDeclarationDatesSeq: Seq[DatesModel] = Seq(finalDeclarationDates, finalDeclarationDates, finalDeclarationDates, finalDeclarationDates)

  val viewModelWithAllData: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationDatesFull,
    finalDeclarationDatesSeq,
    currentDate.getYear,
    showPrevTaxYears = true
  )

  val viewModelWithSingleQuarterlyObligation: ObligationsViewModel = ObligationsViewModel(
    Seq(quarterlyDatesYearOneSimple),
    Seq(finalDeclarationDates),
    currentDate.getYear,
    showPrevTaxYears = true
  )


  def mockDateService(currentDate: LocalDate = currentDate): DateServiceInterface = new DateServiceInterface {

    override def getCurrentDate: LocalDate = currentDate

    override protected def now(): LocalDate = currentDate

    override def getCurrentTaxYear: TaxYear = TaxYear.forYearEnd(currentDate.getYear)

    override def getCurrentTaxYearEnd: Int = currentDate.getYear

    override def getCurrentTaxYearStart: LocalDate = currentDate

    override def isBeforeLastDayOfTaxYear: Boolean = false

    override def isAfterTaxReturnDeadlineButBeforeTaxYearEnd: Boolean = false

    override def getAccountingPeriodEndDate(startDate: LocalDate): LocalDate = {
      val startDateYear = startDate.getYear
      val accountingPeriodEndDate = LocalDate.of(startDateYear, Month.APRIL, 5)

      if (startDate.isBefore(accountingPeriodEndDate) || startDate.isEqual(accountingPeriodEndDate)) {
        accountingPeriodEndDate
      } else {
        accountingPeriodEndDate.plusYears(1)
      }
    }

    override def isWithin30Days(date: LocalDate): Boolean = false
  }

  object insetWarningMessages {
    val multipleObligationsDuePreviousYear: Option[(String, String)] = Some(("business-ceased.obligation.inset.multiple.text", "business-ceased.obligation.inset.previous-year.text"))
    val singleObligationsDuePreviousYear: Option[(String, String)] = Some(("business-ceased.obligation.inset.single.text", "business-ceased.obligation.inset.previous-year.text"))
    val multipleObligationsDueAnnually: Option[(String, String)] = Some(("business-ceased.obligation.inset.multiple.text", "business-ceased.obligation.inset.annually.text"))
    val singleObligationsDueAnnually: Option[(String, String)] = Some(("business-ceased.obligation.inset.single.text", "business-ceased.obligation.inset.annually.text"))
    val multipleObligationsDueQuarterly: Option[(String, String)] = Some(("business-ceased.obligation.inset.multiple.text", "business-ceased.obligation.inset.quarterly.multiple.text"))
    val singleObligationsDueQuarterly: Option[(String, String)] = Some(("business-ceased.obligation.inset.single.text", "business-ceased.obligation.inset.quarterly.single.text"))
  }


  val taxYear: TaxYear = TaxYear(mockDateService().getCurrentTaxYearEnd - 1, mockDateService().getCurrentTaxYearEnd)
  val viewUpcomingUpdatesLinkNoDueMessageKey: String = "business-ceased.obligation.view-updates.text"
  val viewUpcomingUpdatesLinkDueMessageKeyDue: String = "business-ceased.obligation.view-updates-overdue.text"


  "IncomeSourceCeasedObligationsViewModel apply" when {
    "provided with Obligation view model" should {
      "return IncomeSourceCeasedObligationsViewModel " +
        "with no obligations over due," +
        " one quarterly obligation," +
        " one final declaration obligation, " +
        s"viewUpcomingUpdatesLinkMessageKey - $viewUpcomingUpdatesLinkNoDueMessageKey " in {

        val expected = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          firstQuarterlyUpdate = quarterlyObligationDatesFull.flatten.headOption,
          finalDeclarationUpdate = finalDeclarationDatesSeq.take(1),
          numberOfOverdueObligationCount = 0,
          viewUpcomingUpdatesLinkMessageKey = viewUpcomingUpdatesLinkNoDueMessageKey,
          insetWarningMessageKey = None,
          currentTaxYear = taxYear,
          businessName = None, isAgent = false)


        val actual = IncomeSourceCeasedObligationsViewModel(obligationsViewModel = viewModelWithSingleQuarterlyObligation,
          incomeSourceType = SelfEmployment,
          cessationDate = day,
          businessName = None, isAgent = false
        )(mockDateService())


        actual shouldBe expected
      }
    }
    "provided with no Quarterly Obligation view model" should {
      "return IncomeSourceCeasedObligationsViewModel " +
        "with two final declaration dates," +
        s"viewUpcomingUpdatesLinkMessageKey - $viewUpcomingUpdatesLinkNoDueMessageKey " in {

        val expected = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          firstQuarterlyUpdate = None,
          finalDeclarationUpdate = finalDeclarationDatesSeq.take(2),
          numberOfOverdueObligationCount = 0,
          viewUpcomingUpdatesLinkMessageKey = viewUpcomingUpdatesLinkNoDueMessageKey,
          insetWarningMessageKey = None,
          currentTaxYear = taxYear,
          businessName = None, isAgent = false)


        val actual = IncomeSourceCeasedObligationsViewModel(
          obligationsViewModel = viewModelWithAllData.copy(quarterlyObligationsDates = Seq.empty),
          incomeSourceType = SelfEmployment,
          cessationDate = day,
          businessName = None, isAgent = false
        )(mockDateService())


        actual shouldBe expected
      }
    }
    "provided with single over due Quarterly Obligation view model" should {
      "return IncomeSourceCeasedObligationsViewModel " +
        "with one obligations over due and warning message, " +
        s"viewUpcomingUpdatesLinkMessageKey - $viewUpcomingUpdatesLinkDueMessageKeyDue " in {

        val currentDate: LocalDate = LocalDate.of(2022, 5, 1)
        val dateService = mockDateService(currentDate)
        val taxYear: TaxYear = TaxYear(dateService.getCurrentTaxYearEnd - 1, dateService.getCurrentTaxYearEnd)

        val expected = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          firstQuarterlyUpdate = quarterlyDatesYearOneSimple.headOption,
          finalDeclarationUpdate = finalDeclarationDatesSeq.take(1),
          numberOfOverdueObligationCount = 1,
          viewUpcomingUpdatesLinkMessageKey = viewUpcomingUpdatesLinkDueMessageKeyDue,
          insetWarningMessageKey = insetWarningMessages.singleObligationsDueQuarterly,
          currentTaxYear = taxYear,
          businessName = None, isAgent = false)


        val actual = IncomeSourceCeasedObligationsViewModel(obligationsViewModel = viewModelWithSingleQuarterlyObligation,
          incomeSourceType = SelfEmployment,
          cessationDate = day,
          businessName = None, isAgent = false
        )(dateService)


        actual shouldBe expected
      }

    }
    "provided with multiple over due Quarterly Obligation view model" should {
      "return IncomeSourceCeasedObligationsViewModel " +
        "with multiple overdue obligations warning message, " +
        s"viewUpcomingUpdatesLinkMessageKey - $viewUpcomingUpdatesLinkDueMessageKeyDue " in {

        val currentDate: LocalDate = LocalDate.of(2022, 5, 1)
        val dateService = mockDateService(currentDate)
        val taxYear: TaxYear = TaxYear(dateService.getCurrentTaxYearEnd - 1, dateService.getCurrentTaxYearEnd)

        val expected = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          firstQuarterlyUpdate = quarterlyDatesYearOneSimple.headOption,
          finalDeclarationUpdate = finalDeclarationDatesSeq.take(1),
          numberOfOverdueObligationCount = 4,
          viewUpcomingUpdatesLinkMessageKey = viewUpcomingUpdatesLinkDueMessageKeyDue,
          insetWarningMessageKey = insetWarningMessages.multipleObligationsDueQuarterly,
          currentTaxYear = taxYear,
          businessName = None, isAgent = false)


        val actual = IncomeSourceCeasedObligationsViewModel(obligationsViewModel = viewModelWithAllData,
          incomeSourceType = SelfEmployment,
          cessationDate = day,
          businessName = None, isAgent = false
        )(dateService)


        actual shouldBe expected
      }
    }
    "provided with single overdue final declaration view model" should {
      "return IncomeSourceCeasedObligationsViewModel " +
        "with single overdue obligations warning message, " +
        s"viewUpcomingUpdatesLinkMessageKey - $viewUpcomingUpdatesLinkDueMessageKeyDue" in {

        val currentDate: LocalDate = LocalDate.of(2022, 5, 1)
        val dateService = mockDateService(currentDate)
        val taxYear: TaxYear = TaxYear(dateService.getCurrentTaxYearEnd - 1, dateService.getCurrentTaxYearEnd)

        val expected = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          firstQuarterlyUpdate = None,
          finalDeclarationUpdate = finalDeclarationDatesSeq.take(1),
          numberOfOverdueObligationCount = 1,
          viewUpcomingUpdatesLinkMessageKey = viewUpcomingUpdatesLinkDueMessageKeyDue,
          insetWarningMessageKey = insetWarningMessages.singleObligationsDueAnnually,
          currentTaxYear = taxYear,
          businessName = None, isAgent = false)


        val actual = IncomeSourceCeasedObligationsViewModel(obligationsViewModel = viewModelWithSingleQuarterlyObligation.copy(quarterlyObligationsDates = Seq.empty),
          incomeSourceType = SelfEmployment,
          cessationDate = day,
          businessName = None, isAgent = false
        )(dateService)


        actual shouldBe expected
      }
    }
    "provided with multiple overdue final declaration view model" should {
      "return IncomeSourceCeasedObligationsViewModel " +
        "with multiple overdue obligations warning message, " +
        s"viewUpcomingUpdatesLinkMessageKey - $viewUpcomingUpdatesLinkDueMessageKeyDue" in {

        val currentDate: LocalDate = LocalDate.of(2022, 5, 1)
        val dateService = mockDateService(currentDate)
        val taxYear: TaxYear = TaxYear(dateService.getCurrentTaxYearEnd - 1, dateService.getCurrentTaxYearEnd)

        val expected = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          firstQuarterlyUpdate = None,
          finalDeclarationUpdate = finalDeclarationDatesSeq.take(2),
          numberOfOverdueObligationCount = 4,
          viewUpcomingUpdatesLinkMessageKey = viewUpcomingUpdatesLinkDueMessageKeyDue,
          insetWarningMessageKey = insetWarningMessages.multipleObligationsDueAnnually,
          currentTaxYear = taxYear,
          businessName = None, isAgent = false)


        val actual = IncomeSourceCeasedObligationsViewModel(obligationsViewModel = viewModelWithAllData.copy(quarterlyObligationsDates = Seq.empty),
          incomeSourceType = SelfEmployment,
          cessationDate = day,
          businessName = None, isAgent = false
        )(dateService)


        actual shouldBe expected
      }

    }

    "provided with single overdue obligation view model with cessation date of business is before previous year" should {
      "return IncomeSourceCeasedObligationsViewModel " +
        "with single overdue obligations warning message, " +
        s"viewUpcomingUpdatesLinkMessageKey - $viewUpcomingUpdatesLinkDueMessageKeyDue" in {

        val currentDate: LocalDate = LocalDate.of(2022, 5, 1)
        val dateService = mockDateService(currentDate)
        val taxYear: TaxYear = TaxYear(dateService.getCurrentTaxYearEnd - 1, dateService.getCurrentTaxYearEnd)

        val expected = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          firstQuarterlyUpdate = None,
          finalDeclarationUpdate = finalDeclarationDatesSeq.take(1),
          numberOfOverdueObligationCount = 1,
          viewUpcomingUpdatesLinkMessageKey = viewUpcomingUpdatesLinkDueMessageKeyDue,
          insetWarningMessageKey = insetWarningMessages.singleObligationsDuePreviousYear,
          currentTaxYear = taxYear,
          businessName = None, isAgent = false)


        val actual = IncomeSourceCeasedObligationsViewModel(obligationsViewModel = viewModelWithSingleQuarterlyObligation.copy(quarterlyObligationsDates = Seq.empty),
          incomeSourceType = SelfEmployment,
          cessationDate = day.minusYears(1),
          businessName = None, isAgent = false
        )(dateService)


        actual shouldBe expected
      }
    }

    "provided with multiple overdue obligation view model with cessation date of business is before previous year" should {
      "return IncomeSourceCeasedObligationsViewModel " +
        "with multiple overdue obligations warning message, " +
        s" viewUpcomingUpdatesLinkMessageKey - $viewUpcomingUpdatesLinkDueMessageKeyDue " in {

        val currentDate: LocalDate = LocalDate.of(2022, 5, 1)
        val dateService = mockDateService(currentDate)
        val taxYear: TaxYear = TaxYear(dateService.getCurrentTaxYearEnd - 1, dateService.getCurrentTaxYearEnd)

        val expected = IncomeSourceCeasedObligationsViewModel(
          incomeSourceType = SelfEmployment,
          firstQuarterlyUpdate = None,
          finalDeclarationUpdate = finalDeclarationDatesSeq.take(2),
          numberOfOverdueObligationCount = 4,
          viewUpcomingUpdatesLinkMessageKey = viewUpcomingUpdatesLinkDueMessageKeyDue,
          insetWarningMessageKey = insetWarningMessages.multipleObligationsDuePreviousYear,
          currentTaxYear = taxYear,
          businessName = None, isAgent = false)


        val actual = IncomeSourceCeasedObligationsViewModel(obligationsViewModel = viewModelWithAllData.copy(quarterlyObligationsDates = Seq.empty),
          incomeSourceType = SelfEmployment,
          cessationDate = day.minusYears(1),
          businessName = None, isAgent = false
        )(dateService)


        actual shouldBe expected
      }
    }
  }
}
