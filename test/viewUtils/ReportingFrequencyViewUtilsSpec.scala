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

package viewUtils

import auth.MtdItUser
import authV2.AuthActionsTestData.defaultAuthUserDetails
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import enums.MTDIndividual
import implicits.ImplicitDateFormatter
import models.admin.OptInOptOutContentUpdateR17
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.itsaStatus.ITSAStatus.{Annual, DigitallyExempt, Exempt, Mandated, Voluntary}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import services.DateService
import services.optout.OptOutProposition
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testUtils.UnitSpec
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class ReportingFrequencyViewUtilsSpec extends UnitSpec with FeatureSwitching with ImplicitDateFormatter with GuiceOneAppPerSuite {

  override implicit val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  override implicit val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  implicit val csbTestUser: MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    MTDIndividual,
    defaultAuthUserDetails(MTDIndividual),
    None,
    IncomeSourceDetailsModel(
      nino = testNino,
      mtdbsa = testMtditid,
      yearOfMigration = None,
      businesses = List.empty,
      properties = List.empty)
  )(FakeRequest())

  lazy val fixedDate: LocalDate = LocalDate.of(2023, 12, 15)

  implicit lazy val dateService: DateService = new DateService {

    override def getCurrentDate: LocalDate = fixedDate

    override def getCurrentTaxYearEnd: Int = fixedDate.getYear + 1

    override def getCurrentTaxYearStart: LocalDate = LocalDate.of(2023, 4, 6)

    override def isBeforeLastDayOfTaxYear: Boolean = false

    override def getAccountingPeriodEndDate(startDate: LocalDate): LocalDate = LocalDate.of(2024, 4, 5)
  }

  val reportingFrequencyViewUtils = new ReportingFrequencyViewUtils()

  "ReportingFrequencyViewUtils" when {
    ".itsaStatusString()" when {
      "the ITSAStatus == Mandated" should {
        disable(OptInOptOutContentUpdateR17)

        "return 'Quarterly (mandatory)'" in {

          val actual = reportingFrequencyViewUtils.itsaStatusString(Mandated)
          val expected = Some("Quarterly (mandatory)")
          actual shouldBe expected
        }
      }

      "the ITSAStatus == Voluntary" should {

        "return 'Quarterly'" in {

          val actual = reportingFrequencyViewUtils.itsaStatusString(Voluntary)
          val expected = Some("Quarterly")
          actual shouldBe expected
        }
      }

      "the ITSAStatus == Annual" should {

        "return 'Annual'" in {

          val actual = reportingFrequencyViewUtils.itsaStatusString(Annual)
          val expected = Some("Annual")
          actual shouldBe expected
        }
      }

      "the ITSAStatus == Exempt" should {

        "return 'Exempt'" in {

          val actual = reportingFrequencyViewUtils.itsaStatusString(Exempt)
          val expected = Some("Exempt")
          actual shouldBe expected
        }
      }

      "the ITSAStatus == DigitallyExempt" should {

        "return 'Exempt'" in {

          val actual = reportingFrequencyViewUtils.itsaStatusString(DigitallyExempt)
          val expected = Some("Exempt")
          actual shouldBe expected
        }
      }
    }

    ".itsaStatusTable()" when {

      "the CY-1 == Mandated , CY == Voluntary, CY+1 == Mandated" should {
        "return the correct content" in {
          disable(OptInOptOutContentUpdateR17)

          val optOutProposition: OptOutProposition =
            OptOutProposition.createOptOutProposition(
              currentYear = TaxYear(2024, 2025),
              previousYearCrystallised = false,
              previousYearItsaStatus = Mandated,
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = Mandated
            )

          val actual = reportingFrequencyViewUtils.itsaStatusTable(optOutProposition)
          val expected =
            List(
              ("2023 to 2024", None, Some("Quarterly (mandatory)")),
              ("2024 to 2025", None, Some("Quarterly")),
              ("2025 to 2026", None, Some("Quarterly (mandatory)"))
            )
          actual shouldBe expected
        }
      }
      "(new content FS enabled) the CY-1 == Mandated , CY == Voluntary, CY+1 == Mandated" should {

        "return the correct content" in {
          enable(OptInOptOutContentUpdateR17)
          val optOutProposition: OptOutProposition =
            OptOutProposition.createOptOutProposition(
              currentYear = TaxYear(2024, 2025),
              previousYearCrystallised = false,
              previousYearItsaStatus = Mandated,
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = Mandated
            )

          val actual = reportingFrequencyViewUtils.itsaStatusTable(optOutProposition)
          val expected =
            List(
              ("2023 to 2024", Some("Yes"), Some("Required")),
              ("2024 to 2025", Some("Yes"), Some("Voluntarily signed up")),
              ("2025 to 2026", Some("Yes"), Some("Required"))
            )
          actual shouldBe expected
        }
      }
      "(new content FS enabled) the CY-1 == Mandated(crystallised) , CY == Voluntary, CY+1 == Mandated" should {

        "return the correct content" in {
          enable(OptInOptOutContentUpdateR17)
          val optOutProposition: OptOutProposition =
            OptOutProposition.createOptOutProposition(
              currentYear = TaxYear(2024, 2025),
              previousYearCrystallised = true,
              previousYearItsaStatus = Mandated,
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = Mandated
            )

          val actual = reportingFrequencyViewUtils.itsaStatusTable(optOutProposition)
          val expected =
            List(
              ("2024 to 2025", Some("Yes"), Some("Voluntarily signed up")),
              ("2025 to 2026", Some("Yes"), Some("Required"))
            )
          actual shouldBe expected
        }
      }

      "the CY-1 == Mandated , CY == Voluntary, CY+1 == Annual" should {

        "return the correct content" in {
          disable(OptInOptOutContentUpdateR17)

          val optOutProposition: OptOutProposition =
            OptOutProposition.createOptOutProposition(
              currentYear = TaxYear(2024, 2025),
              previousYearCrystallised = false,
              previousYearItsaStatus = Mandated,
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = Annual
            )

          val actual = reportingFrequencyViewUtils.itsaStatusTable(optOutProposition)
          val expected =
            List(
              ("2023 to 2024", None, Some("Quarterly (mandatory)")),
              ("2024 to 2025", None, Some("Quarterly")),
              ("2025 to 2026", None, Some("Annual"))
            )
          actual shouldBe expected
        }
      }
      "(new content) the CY-1 == Mandated , CY == Voluntary, CY+1 == Annual" should {

        "return the correct content" in {
          enable(OptInOptOutContentUpdateR17)

          val optOutProposition: OptOutProposition =
            OptOutProposition.createOptOutProposition(
              currentYear = TaxYear(2024, 2025),
              previousYearCrystallised = false,
              previousYearItsaStatus = Mandated,
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = Annual
            )

          val actual = reportingFrequencyViewUtils.itsaStatusTable(optOutProposition)
          val expected =
            List(
              ("2023 to 2024", Some("Yes"), Some("Required")),
              ("2024 to 2025", Some("Yes"), Some("Voluntarily signed up")),
              ("2025 to 2026", Some("No"), Some("Opted out"))
            )
          actual shouldBe expected
        }
      }
      "(new content) the CY-1 == Exempt , CY == Voluntary, CY+1 == Digitally Exempt" should {

        "return the correct content" in {
          enable(OptInOptOutContentUpdateR17)

          val optOutProposition: OptOutProposition =
            OptOutProposition.createOptOutProposition(
              currentYear = TaxYear(2024, 2025),
              previousYearCrystallised = false,
              previousYearItsaStatus = Exempt,
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = DigitallyExempt
            )

          val actual = reportingFrequencyViewUtils.itsaStatusTable(optOutProposition)
          val expected =
            List(
              ("2023 to 2024", Some("No"), Some("Exempt")),
              ("2024 to 2025", Some("Yes"), Some("Voluntarily signed up")),
              ("2025 to 2026", Some("No"), Some("Exempt"))
            )
          actual shouldBe expected
        }
      }
    }
  }
}