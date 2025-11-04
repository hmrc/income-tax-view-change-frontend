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

package services

import audit.reporting_obligations._
import config.featureswitch.FeatureSwitching
import enums.MTDIndividual
import models.admin.OptInOptOutContentUpdateR17
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Injecting
import services.optIn.core.{CurrentOptInTaxYear, NextOptInTaxYear, OptInProposition}
import services.optout.{CurrentOptOutTaxYear, NextOptOutTaxYear, OptOutProposition, PreviousOptOutTaxYear}
import services.reporting_frequency.ReportingObligationsAuditService
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

    ".buildOptOutCards()" when {

      "CY-1 is crystallised and all years are Voluntary" should {

        "return CY and CY+1 cards" in {

          val optOutProposition =
            OptOutProposition(
              previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), true),
              currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
              nextTaxYear = NextOptOutTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
            )


          val result = auditService.buildOptOutCards(optOutProposition)

          result shouldBe List(ReportingObligationCard(OptOut, "2025", Onwards), ReportingObligationCard(OptOut, "2026", SingleTaxYear))
        }
      }

      "CY-1 is NOT crystallised all years are Voluntary" should {

        "return all cards" in {

          val optOutProposition =
            OptOutProposition(
              previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), false),
              currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
              nextTaxYear = NextOptOutTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
            )


          val result = auditService.buildOptOutCards(optOutProposition)

          result shouldBe List(ReportingObligationCard(OptOut, "2024", Onwards), ReportingObligationCard(OptOut, "2025", Onwards), ReportingObligationCard(OptOut, "2026", SingleTaxYear))
        }
      }

      "CY-1 is NOT crystallised and years are Voluntary, Mandated, Voluntary" should {

        "return only Cy-1 and CY+1 cards" in {

          val optOutProposition =
            OptOutProposition(
              previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), false),
              currentTaxYear = CurrentOptOutTaxYear(Mandated, TaxYear(2025, 2026)),
              nextTaxYear = NextOptOutTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
            )


          val result = auditService.buildOptOutCards(optOutProposition)

          result shouldBe List(ReportingObligationCard(OptOut, "2024", Onwards), ReportingObligationCard(OptOut, "2026", SingleTaxYear))
        }
      }

      "CY-1 is NOT crystallised and years are other statuses such as Annual" should {

        "return only Cy-1 and CY+1 cards" in {

          val optOutProposition =
            OptOutProposition(
              previousTaxYear = PreviousOptOutTaxYear(Annual, TaxYear(2024, 2025), false),
              currentTaxYear = CurrentOptOutTaxYear(Annual, TaxYear(2025, 2026)),
              nextTaxYear = NextOptOutTaxYear(Annual, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
            )


          val result = auditService.buildOptOutCards(optOutProposition)

          result shouldBe List()
        }
      }
    }

    ".buildSignUpCards()" when {

      "both years are Annual" should {

        "return only Cy-1 and CY+1 cards" in {

          val optInProposition =
            OptInProposition(
              currentTaxYear = CurrentOptInTaxYear(Annual, TaxYear(2025, 2026)),
              nextTaxYear = NextOptInTaxYear(Annual, TaxYear(2026, 2027), CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)))
            )

          val result = auditService.buildSignUpCards(optInProposition)

          result shouldBe List(ReportingObligationCard(SignUp, "2025", Onwards), ReportingObligationCard(SignUp, "2026", SingleTaxYear))
        }
      }

      "years are Voluntary, Annual" should {

        "return CY+1 card only" in {

          val optInProposition =
            OptInProposition(
              currentTaxYear = CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)),
              nextTaxYear = NextOptInTaxYear(Annual, TaxYear(2026, 2027), CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)))
            )

          val result = auditService.buildSignUpCards(optInProposition)

          result shouldBe List(ReportingObligationCard(SignUp, "2026", SingleTaxYear))
        }
      }

      "years are Annual, Voluntary" should {

        "return CY+1 card only" in {

          val optInProposition =
            OptInProposition(
              currentTaxYear = CurrentOptInTaxYear(Annual, TaxYear(2025, 2026)),
              nextTaxYear = NextOptInTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)))
            )

          val result = auditService.buildSignUpCards(optInProposition)

          result shouldBe List(ReportingObligationCard(SignUp, "2025", SingleTaxYear))
        }
      }

      "all years are Voluntary" should {

        "return No cards" in {

          val optInProposition =
            OptInProposition(
              currentTaxYear = CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)),
              nextTaxYear = NextOptInTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)))
            )


          val result = auditService.buildSignUpCards(optInProposition)

          result shouldBe List()
        }
      }

      "years are other statuses Mandated" should {

        "return No cards" in {

          val optInProposition =
            OptInProposition(
              currentTaxYear = CurrentOptInTaxYear(Mandated, TaxYear(2025, 2026)),
              nextTaxYear = NextOptInTaxYear(Mandated, TaxYear(2026, 2027), CurrentOptInTaxYear(Mandated, TaxYear(2025, 2026)))
            )


          val result = auditService.buildSignUpCards(optInProposition)

          result shouldBe List()
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

          val optInProposition =
            OptInProposition(
              currentTaxYear = CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)),
              nextTaxYear = NextOptInTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)))
            )

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

          val result = auditService.createAuditEvent(optOutProposition, optInProposition)

          val expected =
            ReportingObligationsAuditModel(
              agentReferenceNumber = None,
              auditType = "ReportingObligationsPage",
              credId = Some("testCredId"),
              mtditid = "XAIT0000123456",
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

          val optInProposition =
            OptInProposition(
              currentTaxYear = CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)),
              nextTaxYear = NextOptInTaxYear(Annual, TaxYear(2026, 2027), CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)))
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

          val result = auditService.createAuditEvent(optOutProposition, optInProposition)

          val expected =
            ReportingObligationsAuditModel(
              agentReferenceNumber = None,
              auditType = "ReportingObligationsPage",
              credId = Some("testCredId"),
              mtditid = "XAIT0000123456",
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

          val optInProposition =
            OptInProposition(
              currentTaxYear = CurrentOptInTaxYear(Annual, TaxYear(2025, 2026)),
              nextTaxYear = NextOptInTaxYear(Annual, TaxYear(2026, 2027), CurrentOptInTaxYear(Annual, TaxYear(2025, 2026)))
            )

          val mockContentToReturn =
            Seq(
              ("2025 to 2026", Some("No"), Some(messages("reporting.frequency.table.annual.r17"))),
              ("2026 to 2027", Some("No"), Some(messages("reporting.frequency.table.annual.r17")))
            )

          when(mockDateService.getCurrentDate)
            .thenReturn(LocalDate.of(2025, 1, 1))

          when(mockDateService.getCurrentTaxYear)
            .thenReturn(TaxYear(2025, 2026))

          when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
            .thenReturn(mockContentToReturn)

          val result = auditService.createAuditEvent(optOutProposition, optInProposition)

          val expected =
            ReportingObligationsAuditModel(
              agentReferenceNumber = None,
              auditType = "ReportingObligationsPage",
              credId = Some("testCredId"),
              mtditid = "XAIT0000123456",
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

          val optInProposition =
            OptInProposition(
              currentTaxYear = CurrentOptInTaxYear(Annual, TaxYear(2025, 2026)),
              nextTaxYear = NextOptInTaxYear(Annual, TaxYear(2026, 2027), CurrentOptInTaxYear(Annual, TaxYear(2025, 2026)))
            )

          val mockContentToReturn =
            Seq(
              ("2024 to 2025", Some("No"), Some(messages("reporting.frequency.table.annual.r17"))),
              ("2025 to 2026", Some("No"), Some(messages("reporting.frequency.table.annual.r17"))),
              ("2026 to 2027", Some("No"), Some(messages("reporting.frequency.table.annual.r17")))
            )

          when(mockDateService.getCurrentDate)
            .thenReturn(LocalDate.of(2025, 1, 1))

          when(mockDateService.getCurrentTaxYear)
            .thenReturn(TaxYear(2025, 2026))

          when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
            .thenReturn(mockContentToReturn)

          val result = auditService.createAuditEvent(optOutProposition, optInProposition)

          val expected =
            ReportingObligationsAuditModel(
              agentReferenceNumber = None,
              auditType = "ReportingObligationsPage",
              credId = Some("testCredId"),
              mtditid = "XAIT0000123456",
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

            val optInProposition =
              OptInProposition(
                currentTaxYear = CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)),
                nextTaxYear = NextOptInTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)))
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

            val result = auditService.sendAuditEvent(optOutProposition, optInProposition)

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

            val optInProposition =
              OptInProposition(
                currentTaxYear = CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)),
                nextTaxYear = NextOptInTaxYear(Annual, TaxYear(2026, 2027), CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)))
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

            when(mockAuditingService.sendExtendedEvent(any())(any(), any()))
              .thenReturn(Future(Success))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.sendAuditEvent(optOutProposition, optInProposition)

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

            val optInProposition =
              OptInProposition(
                currentTaxYear = CurrentOptInTaxYear(Annual, TaxYear(2025, 2026)),
                nextTaxYear = NextOptInTaxYear(Annual, TaxYear(2026, 2027), CurrentOptInTaxYear(Annual, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2025 to 2026", Some("No"), Some(messages("reporting.frequency.table.annual.r17"))),
                ("2026 to 2027", Some("No"), Some(messages("reporting.frequency.table.annual.r17")))
              )

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2025, 1, 1))

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockAuditingService.sendExtendedEvent(any())(any(), any()))
              .thenReturn(Future(Success))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.sendAuditEvent(optOutProposition, optInProposition)

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

            val optInProposition =
              OptInProposition(
                currentTaxYear = CurrentOptInTaxYear(Annual, TaxYear(2025, 2026)),
                nextTaxYear = NextOptInTaxYear(Annual, TaxYear(2026, 2027), CurrentOptInTaxYear(Annual, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2024 to 2025", Some("No"), Some(messages("reporting.frequency.table.annual.r17"))),
                ("2025 to 2026", Some("No"), Some(messages("reporting.frequency.table.annual.r17"))),
                ("2026 to 2027", Some("No"), Some(messages("reporting.frequency.table.annual.r17")))
              )

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2025, 1, 1))

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockAuditingService.sendExtendedEvent(any())(any(), any()))
              .thenReturn(Future(Success))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.sendAuditEvent(optOutProposition, optInProposition)

            val expected: AuditResult = Success

            whenReady(result) {
              result =>
                result shouldBe expected
            }
          }
        }
      }

      "Audit failed" should {

        "all years are Voluntary" should {

          "return Audit Success result" in {

            enable(OptInOptOutContentUpdateR17)

            val optOutProposition =
              OptOutProposition(
                previousTaxYear = PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), true),
                currentTaxYear = CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)),
                nextTaxYear = NextOptOutTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)))
              )

            val optInProposition =
              OptInProposition(
                currentTaxYear = CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)),
                nextTaxYear = NextOptInTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026)))
              )

            val mockContentToReturn =
              Seq(
                ("2025 to 2026", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17"))),
                ("2026 to 2027", Some("Yes"), Some(messages("reporting.frequency.table.voluntary.r17")))
              )

            val failedAuditResponse = Failure("some failed audit message")

            when(mockDateService.getCurrentDate)
              .thenReturn(LocalDate.of(2025, 1, 1))

            when(mockAuditingService.sendExtendedEvent(any())(any(), any()))
              .thenReturn(Future(failedAuditResponse))

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(TaxYear(2025, 2026))

            when(mockReportingFrequencyViewUtils.itsaStatusTable(any())(any(), any()))
              .thenReturn(mockContentToReturn)

            val result = auditService.sendAuditEvent(optOutProposition, optInProposition)

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