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

package models

import mocks.services.MockDateService
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import org.scalatest.Assertion
import services.optout.{OptOutProposition, OptOutTestSupport}
import testUtils.{TestSupport, UnitSpec}

class ReportingFrequencyViewModelSpec extends UnitSpec with MockDateService with TestSupport {

  val optOutProposition = OptOutTestSupport.buildThreeYearOptOutProposition()

  val listOfITSAStatusCombinations = List(
    (true, NoStatus, Voluntary, Voluntary, List(None, Some("optOut.currentYear.onwards"), Some("optOut.nextYear"))),
    (true, NoStatus, Voluntary, Annual, List(None, Some("optOut.currentYear.single"), Some("signUp.nextYear"))),
    (true, NoStatus, Voluntary, Mandated, List(None, Some("optOut.currentYear.single"), None)),
    (true, NoStatus, Annual, Voluntary, List(None, Some("signUp.currentYear.single"), Some("optOut.nextYear"))),
    (true, NoStatus, Annual, Annual, List(None, Some("signUp.currentYear.onwards"), Some("signUp.nextYear"))),
    (true, NoStatus, Annual, Mandated, List(None, Some("signUp.currentYear.single"), None)),
    (true, NoStatus, Mandated, Voluntary, List(None, None, Some("optOut.nextYear"))),
    (true, NoStatus, Mandated, Annual, List(None, None, Some("signUp.nextYear"))),
    (true, NoStatus, Mandated, Mandated, List(None, None, None)),
    (false, Voluntary, Voluntary, Voluntary, List(Some("optOut.previousYear.onwards"), Some("optOut.currentYear.onwards"), Some("optOut.nextYear"))),
    (false, Voluntary, Voluntary, Annual, List(Some("optOut.previousYear.onwards"), Some("optOut.currentYear.single"), Some("signUp.nextYear"))),
    (false, Voluntary, Voluntary, Mandated, List(Some("optOut.previousYear.onwards"), Some("optOut.currentYear.single"), None)),
    (false, Voluntary, Annual, Voluntary, List(Some("optOut.previousYear.single"), Some("signUp.currentYear.single"), Some("optOut.nextYear"))),
    (false, Voluntary, Annual, Annual, List(Some("optOut.previousYear.single"), Some("signUp.currentYear.onwards"), Some("signUp.nextYear"))),
    (false, Voluntary, Annual, Mandated, List(Some("optOut.previousYear.single"), Some("signUp.currentYear.single"), None)),
    (false, Voluntary, Mandated, Voluntary, List(Some("optOut.previousYear.single"), None, Some("optOut.nextYear"))),
    (false, Voluntary, Mandated, Annual, List(Some("optOut.previousYear.single"), None, Some("signUp.nextYear"))),
    (false, Voluntary, Mandated, Mandated, List(Some("optOut.previousYear.single"), None, None))
  )

  "ReportingFrequencyViewModel" when {
    ".getChangeLinkText" should {
      "return the opt out message key" when {
        "the suffix contains optOut" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq(),
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = true,
            isOptOutEnabled = true
          )(mockDateService)

          model.getChangeLinkText("optOut.previousYear.single") shouldBe "optOut.link.text"
          model.getChangeLinkText("optOut.previousYear.onwards") shouldBe "optOut.link.text"
          model.getChangeLinkText("optOut.currentYear") shouldBe "optOut.link.text"
          model.getChangeLinkText("optOut.nextYear") shouldBe "optOut.link.text"
        }
        "the suffix contains signUp" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq(),
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = true,
            isOptOutEnabled = true
          )(mockDateService)

          model.getChangeLinkText("signUp.currentYear") shouldBe "signUp.link.text"
          model.getChangeLinkText("signUp.nextYear") shouldBe "signUp.link.text"
        }
      }
    }
    ".getSecondDescText" should {
      "return the correct message key" when {
        "the suffix is for the previous year" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq(),
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = true,
            isOptOutEnabled = true
          )(mockDateService)

          model.getSecondDescText("optOut.previousYear.single") shouldBe "optOut.previousYear"
          model.getSecondDescText("optOut.previousYear.onwards") shouldBe "optOut.previousYear"
        }
        "the suffix is for the next year" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq(),
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = true,
            isOptOutEnabled = true
          )(mockDateService)

          model.getSecondDescText("optOut.nextYear") shouldBe "optOut.nextYear"
          model.getSecondDescText("signUp.nextYear") shouldBe "signUp.nextYear"
        }
        "the suffix is for the current year and there is no previous year suffix" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))
          val optOutProposition = OptOutTestSupport.buildOneYearOptOutPropositionForCurrentYear()

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq(),
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = true,
            isOptOutEnabled = true
          )(mockDateService)

          model.getSecondDescText("optOut.currentYear.single") shouldBe "optOut.currentYear"
          model.getSecondDescText("optOut.currentYear.onwards") shouldBe "optOut.currentYear"
          model.getSecondDescText("signUp.currentYear.single") shouldBe "signUp.currentYear"
          model.getSecondDescText("signUp.currentYear.onwards") shouldBe "signUp.currentYear"
        }
        "the suffix is for the current year and there is a previous year suffix - opt out and multiple tax years" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq(),
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = true,
            isOptOutEnabled = true
          )(mockDateService)

          model.getSecondDescText("optOut.currentYear.onwards") shouldBe "optOut.currentYear.withDate"
        }
        "the suffix is for the current year and there is a previous year suffix - sign up and multiple tax years" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq(TaxYear(2025, 2026), TaxYear(2026, 2027)),
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = true,
            isOptOutEnabled = true
          )(mockDateService)

          model.getSecondDescText("signUp.currentYear.onwards") shouldBe "signUp.currentYear.withDate"
        }
        "the suffix is for the current year and there is a previous year suffix - sign up and single tax year" in {
          val optOutProposition = OptOutTestSupport.buildOneYearOptOutPropositionForCurrentYear()

          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq(TaxYear(2025, 2026)),
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = true,
            isOptOutEnabled = true
          )(mockDateService)

          model.getSecondDescText("signUp.currentYear.single") shouldBe "signUp.currentYear"
        }
      }
    }
    ".taxYearFromSuffix" should {
      "return the correct tax year" when {
        "the suffix is for the previous year" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq(),
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = true,
            isOptOutEnabled = true
          )(mockDateService)

          model.taxYearFromSuffix("optOut.previousYear.single") shouldBe TaxYear(2024, 2025)
          model.taxYearFromSuffix("optOut.previousYear.onwards") shouldBe TaxYear(2024, 2025)
        }
        "the suffix is for the current year" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq(),
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = true,
            isOptOutEnabled = true
          )(mockDateService)

          model.taxYearFromSuffix("optOut.currentYear.single") shouldBe TaxYear(2025, 2026)
          model.taxYearFromSuffix("optOut.currentYear.onwards") shouldBe TaxYear(2025, 2026)
          model.taxYearFromSuffix("signUp.currentYear.single") shouldBe TaxYear(2025, 2026)
          model.taxYearFromSuffix("signUp.currentYear.onwards") shouldBe TaxYear(2025, 2026)
        }
        "the suffix is for the next year" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq(),
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = true,
            isOptOutEnabled = true
          )(mockDateService)

          model.taxYearFromSuffix("optOut.nextYear") shouldBe TaxYear(2026, 2027)
          model.taxYearFromSuffix("signUp.nextYear") shouldBe TaxYear(2026, 2027)
        }
      }
    }
      ".getOptOutSignUpLink" should {
        "return the opt out link" when {
          "the suffix contains optOut" in {
            val currentTaxYear = TaxYear(2025, 2026)
            setupMockGetCurrentTaxYear(currentTaxYear)

            val model = ReportingFrequencyViewModel(
              isAgent = false,
              optOutJourneyUrl = Some("/opt-out-url"),
              optInTaxYears = Seq(),
              itsaStatusTable = Seq(),
              displayCeasedBusinessWarning = false,
              isAnyOfBusinessLatent = false,
              mtdThreshold = "",
              proposition = optOutProposition,
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )(mockDateService)

            model.getOptOutSignUpLink(currentTaxYear, "optOut.previousYear.single") shouldBe "/report-quarterly/income-and-expenses/view/optout?taxYear=2025"
          }
        }
        "return the sign up link" when {
          "the suffix contains signUp" in {
            val currentTaxYear = TaxYear(2025, 2026)
            setupMockGetCurrentTaxYear(currentTaxYear)

            val model = ReportingFrequencyViewModel(
              isAgent = false,
              optOutJourneyUrl = Some("/sign-up-url"),
              optInTaxYears = Seq(),
              itsaStatusTable = Seq(),
              displayCeasedBusinessWarning = false,
              isAnyOfBusinessLatent = false,
              mtdThreshold = "",
              proposition = optOutProposition,
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )(mockDateService)

            model.getOptOutSignUpLink(currentTaxYear, "signUp.currentYear") shouldBe "/report-quarterly/income-and-expenses/view/sign-up/start?taxYear=2025"
          }
        }
      }
    ".getSummaryCardSuffixes" should {
      "return the correct suffixes" when {
        listOfITSAStatusCombinations.foreach {
          case (previousYearCrystallisation, previousYearStatus, currentYearStatus, nextYearStatus, expectedResult) =>
            s"the previous year crystallisation is $previousYearCrystallisation, the previous year status is $previousYearStatus, the current year status is $currentYearStatus and the next year status is $nextYearStatus" in {
              testSuffixes(previousYearCrystallisation, previousYearStatus, currentYearStatus, nextYearStatus, expectedResult)
            }
        }
      }

      "return no cards" when {
        "sign up and opt out are disabled" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val optOutProposition = OptOutProposition.createOptOutProposition(
            currentYear = TaxYear(2025, 2026),
            previousYearCrystallised = false,
            previousYearItsaStatus = Voluntary,
            currentYearItsaStatus = Annual,
            nextYearItsaStatus = Voluntary
          )

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq(TaxYear(2025, 2026)),
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = false,
            isOptOutEnabled = false
          )(mockDateService)

          model.signUpExistsWhileEnabled shouldBe false
          model.optOutExistsWhileEnabled shouldBe false
          model.getSummaryCardSuffixes shouldBe List(None, None, None)
        }

        "sign up is disabled and there are no opt out years" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val optOutProposition = OptOutProposition.createOptOutProposition(
            currentYear = TaxYear(2025, 2026),
            previousYearCrystallised = false,
            previousYearItsaStatus = Annual,
            currentYearItsaStatus = Annual,
            nextYearItsaStatus = Annual
          )

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq(TaxYear(2025, 2026)),
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = false,
            isOptOutEnabled = true
          )(mockDateService)

          model.signUpExistsWhileEnabled shouldBe false
          model.optOutExistsWhileEnabled shouldBe false
          model.getSummaryCardSuffixes shouldBe List(None, None, None)
        }

        "opt out is disabled and there are no sign up years" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val optOutProposition = OptOutProposition.createOptOutProposition(
            currentYear = TaxYear(2025, 2026),
            previousYearCrystallised = false,
            previousYearItsaStatus = Voluntary,
            currentYearItsaStatus = Voluntary,
            nextYearItsaStatus = Voluntary
          )

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq.empty,
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = true,
            isOptOutEnabled = false
          )(mockDateService)

          model.signUpExistsWhileEnabled shouldBe false
          model.optOutExistsWhileEnabled shouldBe false
          model.getSummaryCardSuffixes shouldBe List(None, None, None)
        }

        "there is an exempt status" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val optOutProposition = OptOutProposition.createOptOutProposition(
            currentYear = TaxYear(2025, 2026),
            previousYearCrystallised = false,
            previousYearItsaStatus = Voluntary,
            currentYearItsaStatus = Exempt,
            nextYearItsaStatus = Annual
          )

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq.empty,
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = true,
            isOptOutEnabled = true
          )(mockDateService)

          model.signUpExistsWhileEnabled shouldBe false
          model.optOutExistsWhileEnabled shouldBe true
          model.getSummaryCardSuffixes shouldBe List()
        }
      }

      "return only sign up cards" when {
        "opt out is disabled and there are sign up tax years" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val optOutProposition = OptOutProposition.createOptOutProposition(
            currentYear = TaxYear(2025, 2026),
            previousYearCrystallised = false,
            previousYearItsaStatus = Voluntary,
            currentYearItsaStatus = Annual,
            nextYearItsaStatus = Voluntary
          )

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq(TaxYear(2025, 2026)),
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = true,
            isOptOutEnabled = false
          )(mockDateService)

          model.signUpExistsWhileEnabled shouldBe true
          model.optOutExistsWhileEnabled shouldBe false
          model.getSummaryCardSuffixes shouldBe List(None, Some("signUp.currentYear.single"), None)
        }
      }

      "return only opt out cards" when {
        "sign up is disabled and there are opt out tax years" in {
          setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

          val optOutProposition = OptOutProposition.createOptOutProposition(
            currentYear = TaxYear(2025, 2026),
            previousYearCrystallised = false,
            previousYearItsaStatus = Voluntary,
            currentYearItsaStatus = Annual,
            nextYearItsaStatus = Voluntary
          )

          val model = ReportingFrequencyViewModel(
            isAgent = false,
            optOutJourneyUrl = None,
            optInTaxYears = Seq.empty,
            itsaStatusTable = Seq(),
            displayCeasedBusinessWarning = false,
            isAnyOfBusinessLatent = false,
            mtdThreshold = "",
            proposition = optOutProposition,
            isSignUpEnabled = false,
            isOptOutEnabled = true
          )(mockDateService)

          model.signUpExistsWhileEnabled shouldBe false
          model.optOutExistsWhileEnabled shouldBe true
          model.getSummaryCardSuffixes shouldBe List(Some("optOut.previousYear.single"), None, Some("optOut.nextYear"))
        }
      }
    }

    "exemptStatusCount" should {
      "handle no exempt status" in {
        setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

        val optOutProposition = OptOutProposition.createOptOutProposition(
          currentYear = TaxYear(2025, 2026),
          previousYearCrystallised = true,
          previousYearItsaStatus = Voluntary,
          currentYearItsaStatus = Annual,
          nextYearItsaStatus = Voluntary
        )

        val model = ReportingFrequencyViewModel(
          isAgent = false,
          optOutJourneyUrl = None,
          optInTaxYears = Seq(TaxYear(2025, 2026)),
          itsaStatusTable = Seq(),
          displayCeasedBusinessWarning = false,
          isAnyOfBusinessLatent = false,
          mtdThreshold = "",
          proposition = optOutProposition,
          isSignUpEnabled = true,
          isOptOutEnabled = true
        )(mockDateService)

        model.exemptStatusCount shouldBe(0, 3)
      }

      "handle one exempt status" in {
        setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

        val optOutProposition = OptOutProposition.createOptOutProposition(
          currentYear = TaxYear(2025, 2026),
          previousYearCrystallised = true,
          previousYearItsaStatus = Voluntary,
          currentYearItsaStatus = Exempt,
          nextYearItsaStatus = Voluntary
        )

        val model = ReportingFrequencyViewModel(
          isAgent = false,
          optOutJourneyUrl = None,
          optInTaxYears = Seq(TaxYear(2025, 2026)),
          itsaStatusTable = Seq(),
          displayCeasedBusinessWarning = false,
          isAnyOfBusinessLatent = false,
          mtdThreshold = "",
          proposition = optOutProposition,
          isSignUpEnabled = true,
          isOptOutEnabled = true
        )(mockDateService)

        model.exemptStatusCount shouldBe(1, 2)
      }

      "handle multiple exempt status" in {
        setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

        val optOutProposition = OptOutProposition.createOptOutProposition(
          currentYear = TaxYear(2025, 2026),
          previousYearCrystallised = true,
          previousYearItsaStatus = Mandated,
          currentYearItsaStatus = Exempt,
          nextYearItsaStatus = Exempt
        )

        val model = ReportingFrequencyViewModel(
          isAgent = false,
          optOutJourneyUrl = None,
          optInTaxYears = Seq(TaxYear(2025, 2026)),
          itsaStatusTable = Seq(),
          displayCeasedBusinessWarning = false,
          isAnyOfBusinessLatent = false,
          mtdThreshold = "",
          proposition = optOutProposition,
          isSignUpEnabled = true,
          isOptOutEnabled = true
        )(mockDateService)

        model.exemptStatusCount shouldBe(2, 1)
      }
    }
  }

  def testSuffixes(previousYearCrystallisation: Boolean,
                   previousYearStatus: ITSAStatus,
                   currentYearStatus: ITSAStatus,
                   nextYearStatus: ITSAStatus,
                   expectedResult: List[Option[String]]): Assertion
  = {
    setupMockGetCurrentTaxYear(TaxYear(2025, 2026))
    val optOutProposition = OptOutProposition.createOptOutProposition(
      currentYear = TaxYear(2025, 2026),
      previousYearCrystallised = previousYearCrystallisation,
      previousYearItsaStatus = previousYearStatus,
      currentYearItsaStatus = currentYearStatus,
      nextYearItsaStatus = nextYearStatus
    )

    val currentOptInYear = if(currentYearStatus == Annual) Seq(TaxYear(2025, 2026)) else Seq.empty
    val nextYearOptInYear = if(nextYearStatus == Annual) Seq(TaxYear(2026, 2027)) else Seq.empty

    val optInYears = currentOptInYear ++ nextYearOptInYear

    val model = ReportingFrequencyViewModel(
      isAgent = false,
      optOutJourneyUrl = None,
      optInTaxYears = optInYears,
      itsaStatusTable = Seq(),
      displayCeasedBusinessWarning = false,
      isAnyOfBusinessLatent = false,
      mtdThreshold = "",
      proposition = optOutProposition,
      isSignUpEnabled = true,
      isOptOutEnabled = true
    )(mockDateService)

    model.getSummaryCardSuffixes shouldBe expectedResult
  }
}


