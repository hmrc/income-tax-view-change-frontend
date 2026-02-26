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

package services.reportingObligations

import audit.reporting_obligations.*
import config.featureswitch.FeatureSwitching
import enums.MTDIndividual
import models.admin.OptInOptOutContentUpdateR17
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Injecting
import services.DateService
import services.reportingObligations.ReportingObligationsAuditService
import services.reportingObligations.optOut.{CurrentOptOutTaxYear, NextOptOutTaxYear, OptOutProposition, PreviousOptOutTaxYear}
import services.reportingObligations.signUp.core.{CurrentOptInTaxYear, NextOptInTaxYear, OptInProposition}
import testUtils.{TestSupport, UnitSpec}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Failure, Success}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import viewUtils.ReportingFrequencyViewUtils

import java.time.LocalDate
import scala.concurrent.Future

class ReportingObligationsAuditServiceSpec extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach with Injecting with FeatureSwitching with TestSupport {

  val mockReportingFrequencyViewUtils: ReportingFrequencyViewUtils = mock(classOf[ReportingFrequencyViewUtils])
  lazy val mockAuditingService: AuditConnector = mock(classOf[AuditConnector])

  implicit val mockDateService: DateService = mock(classOf[DateService])

  val auditService =
    new ReportingObligationsAuditService(
      auditConnector = mockAuditingService,
      reportingFrequencyViewUtils = mockReportingFrequencyViewUtils
    )

  "ReportingObligationsAuditService" when {

    ".buildCards()" when {

      "given a list of all opt out years card suffixes" should {

        "return 3 cards with the correct data" in {

          when(mockDateService.getCurrentTaxYear)
            .thenReturn(TaxYear(2025, 2026))

          val summaryCardSuffixes = List(Some("optOut.previousYear.onwards"), Some("optOut.currentYear.onwards"), Some("optOut.nextYear"))

          val result = auditService.buildCards(summaryCardSuffixes)

          result shouldBe List(ReportingObligationCard(OptOut, "2024", Onwards), ReportingObligationCard(OptOut, "2025", Onwards), ReportingObligationCard(OptOut, "2026", SingleTaxYear))
        }
      }

      "given a list of only current onwards opt out years card suffixes" should {

        "return 2 cards with the correct data" in {

          when(mockDateService.getCurrentTaxYear)
            .thenReturn(TaxYear(2025, 2026))

          val summaryCardSuffixes = List(Some("optOut.currentYear.onwards"), Some("optOut.nextYear"))

          val result = auditService.buildCards(summaryCardSuffixes)

          result shouldBe List(ReportingObligationCard(OptOut, "2025", Onwards), ReportingObligationCard(OptOut, "2026", SingleTaxYear))
        }
      }

      "given a list of only CY+1 opt out years card suffixes" should {

        "return 1 card with the correct data" in {

          when(mockDateService.getCurrentTaxYear)
            .thenReturn(TaxYear(2025, 2026))

          val summaryCardSuffixes = List(Some("optOut.nextYear"))

          val result = auditService.buildCards(summaryCardSuffixes)

          result shouldBe List(ReportingObligationCard(OptOut, "2026", SingleTaxYear))
        }
      }

      "given a scenario where CY-1 missing from list of opt out years card suffixes" should {

        "return 2 cards with the correct data" in {

          when(mockDateService.getCurrentTaxYear)
            .thenReturn(TaxYear(2025, 2026))

          val summaryCardSuffixes = List(Some("optOut.previousYear.single"), None, Some("optOut.nextYear"))

          val result = auditService.buildCards(summaryCardSuffixes)

          result shouldBe List(ReportingObligationCard(OptOut, "2024", SingleTaxYear), ReportingObligationCard(OptOut, "2026", SingleTaxYear))
        }
      }



      "given a list of both sign up years card suffixes" should {

        "return 2 cards with the correct data" in {

          when(mockDateService.getCurrentTaxYear)
            .thenReturn(TaxYear(2025, 2026))

          val summaryCardSuffixes = List(Some("signUp.currentYear.onwards"), Some("signUp.nextYear"))

          val result = auditService.buildCards(summaryCardSuffixes)

          result shouldBe List(ReportingObligationCard(SignUp, "2025", Onwards), ReportingObligationCard(SignUp, "2026", SingleTaxYear))
        }
      }

      "given a list of only sign up for CY+1 card suffixes" should {

        "return 1 card with the correct data" in {

          when(mockDateService.getCurrentTaxYear)
            .thenReturn(TaxYear(2025, 2026))

          val summaryCardSuffixes = List(None, Some("signUp.nextYear"))

          val result = auditService.buildCards(summaryCardSuffixes)

          result shouldBe List(ReportingObligationCard(SignUp, "2026", SingleTaxYear))
        }
      }
    }

    ".buildItsaTableContentToCapture()" when {

      "OptInOptOutContentUpdateR17 is enabled" when {

        "CY-1 is crystallised and all years are Voluntary" should {

          "capture the correct table content shown to the user" in {

            enable(OptInOptOutContentUpdateR17)

            val optOutProposition =
              OptOutProposition(
                previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), true),
                currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
                nextTaxYear = NextOptOutTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2025 to 2026", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17"))),
                ("2026 to 2027", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17")))
              )

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.buildItsaTableContentToCapture(optOutProposition)

            result shouldBe
              List(
                ItsaStatusTableDetails("CurrentTaxYear", "2025 to 2026", Some("Yes"), "MTD Voluntary"),
                ItsaStatusTableDetails("NextTaxYear", "2026 to 2027", Some("Yes"), "MTD Voluntary")
              )
          }
        }

        "CY-1 is crystallised and CY == Voluntary and CY+1 == Annual" should {

          "capture the table content shown to the user" in {

            enable(OptInOptOutContentUpdateR17)

            val optOutProposition =
              OptOutProposition(
                previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), true),
                currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
                nextTaxYear = NextOptOutTaxYear(Annual, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2025 to 2026", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17"))),
                ("2026 to 2027", Some("No"), Some(messages("reporting.frequency.table.annual.r17")))
              )

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.buildItsaTableContentToCapture(optOutProposition)

            result shouldBe
              List(
                ItsaStatusTableDetails("CurrentTaxYear", "2025 to 2026", Some("Yes"), "MTD Voluntary"),
                ItsaStatusTableDetails("NextTaxYear", "2026 to 2027", Some("No"), "Annual")
              )
          }
        }

        "CY-1 is crystallised and CY == Voluntary and CY+1 == Mandated" should {

          "capture the table content shown to the user" in {

            enable(OptInOptOutContentUpdateR17)

            val optOutProposition =
              OptOutProposition(
                previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), true),
                currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
                nextTaxYear = NextOptOutTaxYear(Mandated, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2025 to 2026", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17"))),
                ("2026 to 2027", Some("Yes"), Some(messages("reporting.frequency.table.mandated.r17")))
              )

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.buildItsaTableContentToCapture(optOutProposition)

            result shouldBe
              List(
                ItsaStatusTableDetails("CurrentTaxYear", "2025 to 2026", Some("Yes"), "MTD Voluntary"),
                ItsaStatusTableDetails("NextTaxYear", "2026 to 2027", Some("Yes"), "MTD Mandated")
              )
          }
        }

        "CY-1 is NOT crystallised and CY-1 == Voluntary, CY == Voluntary and CY+1 == Mandated" should {

          "capture the table content shown to the user" in {

            enable(OptInOptOutContentUpdateR17)

            val optOutProposition =
              OptOutProposition(
                previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), false),
                currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
                nextTaxYear = NextOptOutTaxYear(Mandated, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2024 to 2025", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17"))),
                ("2025 to 2026", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17"))),
                ("2026 to 2027", Some("Yes"), Some(messages("reporting.frequency.table.mandated.r17")))
              )

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.buildItsaTableContentToCapture(optOutProposition)

            result shouldBe
              List(
                ItsaStatusTableDetails("PreviousTaxYear", "2024 to 2025", Some("Yes"), "MTD Voluntary"),
                ItsaStatusTableDetails("CurrentTaxYear", "2025 to 2026", Some("Yes"), "MTD Voluntary"),
                ItsaStatusTableDetails("NextTaxYear", "2026 to 2027", Some("Yes"), "MTD Mandated")
              )
          }
        }
      }

      "OptInOptOutContentUpdateR17 is disabled" when {

        "CY-1 is crystallised and all years are Voluntary" should {

          "capture the correct table content shown to the user" in {

            disable(OptInOptOutContentUpdateR17)

            val optOutProposition =
              OptOutProposition(
                previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), true),
                currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
                nextTaxYear = NextOptOutTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2025 to 2026", Some("Yes"), Some(messages("reporting.frequency.table.voluntary"))),
                ("2026 to 2027", Some("Yes"), Some(messages("reporting.frequency.table.voluntary")))
              )

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.buildItsaTableContentToCapture(optOutProposition)

            result shouldBe
              List(
                ItsaStatusTableDetails("CurrentTaxYear", "2025 to 2026", Some("Yes"), "MTD Voluntary"),
                ItsaStatusTableDetails("NextTaxYear", "2026 to 2027", Some("Yes"), "MTD Voluntary")
              )
          }
        }

        "CY-1 is crystallised and CY == Voluntary and CY+1 == Annual" should {

          "capture the table content shown to the user" in {

            disable(OptInOptOutContentUpdateR17)

            val optOutProposition =
              OptOutProposition(
                previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), true),
                currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
                nextTaxYear = NextOptOutTaxYear(Annual, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2025 to 2026", Some("Yes"), Some(messages("reporting.frequency.table.voluntary"))),
                ("2026 to 2027", Some("No"), Some(messages("reporting.frequency.table.annual")))
              )

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.buildItsaTableContentToCapture(optOutProposition)

            result shouldBe
              List(
                ItsaStatusTableDetails("CurrentTaxYear", "2025 to 2026", Some("Yes"), "MTD Voluntary"),
                ItsaStatusTableDetails("NextTaxYear", "2026 to 2027", Some("No"), "Annual")
              )
          }
        }

        "CY-1 is crystallised and CY == Voluntary and CY+1 == Mandated" should {

          "capture the table content shown to the user" in {

            disable(OptInOptOutContentUpdateR17)

            val optOutProposition =
              OptOutProposition(
                previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), true),
                currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
                nextTaxYear = NextOptOutTaxYear(Mandated, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2025 to 2026", Some("Yes"), Some(messages("reporting.frequency.table.voluntary"))),
                ("2026 to 2027", Some("Yes"), Some(messages("reporting.frequency.table.mandated")))
              )

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.buildItsaTableContentToCapture(optOutProposition)

            result shouldBe
              List(
                ItsaStatusTableDetails("CurrentTaxYear", "2025 to 2026", Some("Yes"), "MTD Voluntary"),
                ItsaStatusTableDetails("NextTaxYear", "2026 to 2027", Some("Yes"), "MTD Mandated")
              )
          }
        }

        "CY-1 is NOT crystallised and CY-1 == Voluntary, CY == Voluntary and CY+1 == Mandated" should {

          "capture the table content shown to the user" in {

            disable(OptInOptOutContentUpdateR17)

            val optOutProposition =
              OptOutProposition(
                previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), false),
                currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
                nextTaxYear = NextOptOutTaxYear(Mandated, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2024 to 2025", Some("Yes"), Some(messages("reporting.frequency.table.voluntary"))),
                ("2025 to 2026", Some("Yes"), Some(messages("reporting.frequency.table.voluntary"))),
                ("2026 to 2027", Some("Yes"), Some(messages("reporting.frequency.table.mandated")))
              )

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.buildItsaTableContentToCapture(optOutProposition)

            result shouldBe
              List(
                ItsaStatusTableDetails("PreviousTaxYear", "2024 to 2025", Some("Yes"), "MTD Voluntary"),
                ItsaStatusTableDetails("CurrentTaxYear", "2025 to 2026", Some("Yes"), "MTD Voluntary"),
                ItsaStatusTableDetails("NextTaxYear", "2026 to 2027", Some("Yes"), "MTD Mandated")
              )
          }
        }
      }
    }

    ".createAuditEvent()" when {

      "all years are Voluntary" should {

        "return the correct audit event model" in {

          enable(OptInOptOutContentUpdateR17)

          val optOutProposition =
            OptOutProposition(
              previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), true),
              currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
              nextTaxYear = NextOptOutTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
            )

          val summaryCardSuffixes = List(Some("optOut.currentYear.onwards"), Some("optOut.nextYear"))

          val mockContentToReturn =
            Seq(
              ("2025 to 2026", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17"))),
              ("2026 to 2027", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17")))
            )

          when(mockDateService.getCurrentDate)
            .thenReturn(LocalDate.of(2025, 1, 1))

          when(mockDateService.getCurrentTaxYear)
            .thenReturn(TaxYear(2025, 2026))

          when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
            .thenReturn(mockContentToReturn)

          val result = auditService.createAuditEvent(optOutProposition, summaryCardSuffixes)

          val expected =
            ReportingObligationsAuditModel(
              agentReferenceNumber = None,
              auditType = "ReportingObligationsPage",
              credId = Some("testCredId"),
              mtditid = "XAIT00001234567",
              nino = "AB123456C",
              saUtr = Some("1234567890"),
              userType = MTDIndividual,
              grossIncomeThreshold = "£50,000",
              crystallisationStatusForPreviousTaxYear = true,
              itsaStatusTable = List(ItsaStatusTableDetails("CurrentTaxYear", "2025 to 2026", Some("Yes"), "MTD Voluntary"), ItsaStatusTableDetails("NextTaxYear", "2026 to 2027", Some("Yes"), "MTD Voluntary")),
              links = List("OptOut2025Onwards", "OptOut2026SingleTaxYear")
            )

          result shouldBe expected
        }
      }

      "CY-1 is crystallised, CY == Voluntary, CY+1 == Annual" should {

        "return the correct audit event model" in {

          enable(OptInOptOutContentUpdateR17)

          val optOutProposition =
            OptOutProposition(
              previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), true),
              currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
              nextTaxYear = NextOptOutTaxYear(Annual, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
            )

          val mockContentToReturn =
            Seq(
              ("2025 to 2026", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17"))),
              ("2026 to 2027", Some("No"), Some(messages("reporting.frequency.table.annual.r17")))
            )

          when(mockDateService.getCurrentDate)
            .thenReturn(LocalDate.of(2025, 1, 1))

          when(mockDateService.getCurrentTaxYear)
            .thenReturn(TaxYear(2025, 2026))

          when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
            .thenReturn(mockContentToReturn)

          val summaryCardSuffixes = List(Some("optOut.currentYear.single"), Some("signUp.nextYear"))

          val result = auditService.createAuditEvent(optOutProposition, summaryCardSuffixes)

          val expected =
            ReportingObligationsAuditModel(
              agentReferenceNumber = None,
              auditType = "ReportingObligationsPage",
              credId = Some("testCredId"),
              mtditid = "XAIT00001234567",
              nino = "AB123456C",
              saUtr = Some("1234567890"),
              userType = MTDIndividual,
              grossIncomeThreshold = "£50,000",
              crystallisationStatusForPreviousTaxYear = true,
              itsaStatusTable = List(ItsaStatusTableDetails("CurrentTaxYear", "2025 to 2026", Some("Yes"), "MTD Voluntary"), ItsaStatusTableDetails("NextTaxYear", "2026 to 2027", Some("No"), "Annual")),
              links = List("OptOut2025SingleTaxYear", "SignUp2026SingleTaxYear")
            )

          result shouldBe expected
        }
      }

      "CY-1 is crystallised, all years are Annual" should {

        "return the correct audit event model" in {

          enable(OptInOptOutContentUpdateR17)

          val optOutProposition =
            OptOutProposition(
              previousTaxYear = PreviousOptOutTaxYear(Annual, TaxYear(2024, 2025), true),
              currentTaxYear = CurrentOptOutTaxYear(Annual, TaxYear(2025, 2026)),
              nextTaxYear = NextOptOutTaxYear(Annual, TaxYear(2026, 2027), CurrentOptOutTaxYear(Annual, TaxYear(2025, 2026)))
            )

          val mockContentToReturn =
            Seq(
              ("2025 to 2026", Some("No"), Some(messages("reporting.frequency.table.annual.r17"))),
              ("2026 to 2027", Some("No"), Some(messages("reporting.frequency.table.annual.r17")))
            )

          val summaryCardSuffixes = List(Some("signUp.currentYear.onwards"), Some("signUp.nextYear"))

          when(mockDateService.getCurrentDate)
            .thenReturn(LocalDate.of(2025, 1, 1))

          when(mockDateService.getCurrentTaxYear)
            .thenReturn(TaxYear(2025, 2026))

          when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
            .thenReturn(mockContentToReturn)

          val result = auditService.createAuditEvent(optOutProposition, summaryCardSuffixes)

          val expected =
            ReportingObligationsAuditModel(
              agentReferenceNumber = None,
              auditType = "ReportingObligationsPage",
              credId = Some("testCredId"),
              mtditid = "XAIT00001234567",
              nino = "AB123456C",
              saUtr = Some("1234567890"),
              userType = MTDIndividual,
              grossIncomeThreshold = "£50,000",
              crystallisationStatusForPreviousTaxYear = true,
              itsaStatusTable = List(ItsaStatusTableDetails("CurrentTaxYear", "2025 to 2026", Some("No"), "Annual"), ItsaStatusTableDetails("NextTaxYear", "2026 to 2027", Some("No"), "Annual")),
              links = List("SignUp2025Onwards", "SignUp2026SingleTaxYear")
            )

          result shouldBe expected
        }
      }

      "CY-1 is NOT crystallised, all years Annual" should {

        "return the correct audit event model" in {

          enable(OptInOptOutContentUpdateR17)

          val optOutProposition =
            OptOutProposition(
              previousTaxYear = PreviousOptOutTaxYear(Annual, TaxYear(2024, 2025), false),
              currentTaxYear = CurrentOptOutTaxYear(Annual, TaxYear(2025, 2026)),
              nextTaxYear = NextOptOutTaxYear(Annual, TaxYear(2026, 2027), CurrentOptOutTaxYear(Annual, TaxYear(2025, 2026)))
            )

          val mockContentToReturn =
            Seq(
              ("2024 to 2025", Some("No"), Some(messages("reporting.frequency.table.annual.r17"))),
              ("2025 to 2026", Some("No"), Some(messages("reporting.frequency.table.annual.r17"))),
              ("2026 to 2027", Some("No"), Some(messages("reporting.frequency.table.annual.r17")))
            )

          val summaryCardSuffixes = List(None, Some("signUp.currentYear.onwards"), Some("signUp.nextYear"))

          when(mockDateService.getCurrentDate)
            .thenReturn(LocalDate.of(2025, 1, 1))

          when(mockDateService.getCurrentTaxYear)
            .thenReturn(TaxYear(2025, 2026))

          when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
            .thenReturn(mockContentToReturn)

          val result = auditService.createAuditEvent(optOutProposition, summaryCardSuffixes)

          val expected =
            ReportingObligationsAuditModel(
              agentReferenceNumber = None,
              auditType = "ReportingObligationsPage",
              credId = Some("testCredId"),
              mtditid = "XAIT00001234567",
              nino = "AB123456C",
              saUtr = Some("1234567890"),
              userType = MTDIndividual,
              grossIncomeThreshold = "£50,000",
              crystallisationStatusForPreviousTaxYear = false,
              itsaStatusTable =
                List(
                  ItsaStatusTableDetails("PreviousTaxYear", "2024 to 2025", Some("No"), "Annual"),
                  ItsaStatusTableDetails("CurrentTaxYear", "2025 to 2026", Some("No"), "Annual"),
                  ItsaStatusTableDetails("NextTaxYear", "2026 to 2027", Some("No"), "Annual")
                ),
              links = List("SignUp2025Onwards", "SignUp2026SingleTaxYear")
            )

          result shouldBe expected
        }
      }
    }

    ".sendAuditEvent()" when {

      "Audit was sent successfully" should {

        "all years are Voluntary" should {

          "return Audit Success result" in {

            enable(OptInOptOutContentUpdateR17)

            val optOutProposition =
              OptOutProposition(
                previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), true),
                currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
                nextTaxYear = NextOptOutTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2025 to 2026", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17"))),
                ("2026 to 2027", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17")))
              )

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2025, 1, 1))

            when(mockAuditingService.sendExtendedEvent(any())(any(), any()))
              .thenReturn(Future(Success))

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val summaryCardSuffixes = List(Some("optOut.currentYear.onwards"), Some("optOut.nextYear"))

            val result = auditService.sendAuditEvent(optOutProposition, summaryCardSuffixes)

            val expected: AuditResult = Success

            whenReady(result) {
              result =>
                result shouldBe expected
            }
          }
        }

        "CY-1 is crystallised, CY == Voluntary, CY+1 == Annual" should {

          "return Audit Success result" in {

            enable(OptInOptOutContentUpdateR17)

            val optOutProposition =
              OptOutProposition(
                previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), true),
                currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
                nextTaxYear = NextOptOutTaxYear(Annual, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2025 to 2026", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17"))),
                ("2026 to 2027", Some("No"), Some(messages("reporting.frequency.table.annual.r17")))
              )

            val summaryCardSuffixes = List(Some("optOut.currentYear.single"), Some("signUp.nextYear"))

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2025, 1, 1))

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockAuditingService.sendExtendedEvent(any())(any(), any()))
              .thenReturn(Future(Success))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.sendAuditEvent(optOutProposition, summaryCardSuffixes)

            val expected: AuditResult = Success

            whenReady(result) {
              result =>
                result shouldBe expected
            }
          }
        }

        "CY-1 is crystallised, all years are Annual" should {

          "return Audit Success result" in {

            enable(OptInOptOutContentUpdateR17)

            val optOutProposition =
              OptOutProposition(
                previousTaxYear = PreviousOptOutTaxYear(Annual, TaxYear(2024, 2025), true),
                currentTaxYear = CurrentOptOutTaxYear(Annual, TaxYear(2025, 2026)),
                nextTaxYear = NextOptOutTaxYear(Annual, TaxYear(2026, 2027), CurrentOptOutTaxYear(Annual, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2025 to 2026", Some("No"), Some(messages("reporting.frequency.table.annual.r17"))),
                ("2026 to 2027", Some("No"), Some(messages("reporting.frequency.table.annual.r17")))
              )

            val summaryCardSuffixes = List(Some("signUp.currentYear.onwards"), Some("signUp.nextYear"))

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2025, 1, 1))

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockAuditingService.sendExtendedEvent(any())(any(), any()))
              .thenReturn(Future(Success))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.sendAuditEvent(optOutProposition, summaryCardSuffixes)

            val expected: AuditResult = Success

            whenReady(result) {
              result =>
                result shouldBe expected
            }
          }
        }

        "CY-1 is NOT crystallised, all years Annual" should {

          "return Audit Success result" in {

            enable(OptInOptOutContentUpdateR17)

            val optOutProposition =
              OptOutProposition(
                previousTaxYear = PreviousOptOutTaxYear(Annual, TaxYear(2024, 2025), false),
                currentTaxYear = CurrentOptOutTaxYear(Annual, TaxYear(2025, 2026)),
                nextTaxYear = NextOptOutTaxYear(Annual, TaxYear(2026, 2027), CurrentOptOutTaxYear(Annual, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2024 to 2025", Some("No"), Some(messages("reporting.frequency.table.annual.r17"))),
                ("2025 to 2026", Some("No"), Some(messages("reporting.frequency.table.annual.r17"))),
                ("2026 to 2027", Some("No"), Some(messages("reporting.frequency.table.annual.r17")))
              )

            val summaryCardSuffixes = List(None, Some("signUp.currentYear.onwards"), Some("signUp.nextYear"))

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2025, 1, 1))

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockAuditingService.sendExtendedEvent(any())(any(), any()))
              .thenReturn(Future(Success))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.sendAuditEvent(optOutProposition, summaryCardSuffixes)

            val expected: AuditResult = Success

            whenReady(result) {
              result =>
                result shouldBe expected
            }
          }
        }
      }

      "Audit failed" when {

        "all years are Voluntary" should {

          "return Audit Success result" in {

            enable(OptInOptOutContentUpdateR17)

            val optOutProposition =
              OptOutProposition(
                previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), true),
                currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
                nextTaxYear = NextOptOutTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2025 to 2026", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17"))),
                ("2026 to 2027", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17")))
              )

            val summaryCardSuffixes = List(Some("optOut.currentYear.onwards"), Some("optOut.nextYear"))

            val failedAuditResponse = Failure("some failed audit message")

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2025, 1, 1))

            when(mockAuditingService.sendExtendedEvent(any())(any(), any()))
              .thenReturn(Future(failedAuditResponse))

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.sendAuditEvent(optOutProposition, summaryCardSuffixes)

            val expected: AuditResult = failedAuditResponse

            whenReady(result) {
              result =>
                result shouldBe expected
            }
          }
        }
      }
    }
  }
}