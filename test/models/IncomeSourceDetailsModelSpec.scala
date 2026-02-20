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

package models

import auth.MtdItUser
import enums.IncomeSourceJourney.SelfEmployment
import exceptions.{MultipleIncomeSourcesFound, NoIncomeSourceFound}
import forms.IncomeSourcesFormsSpec.{fakeRequestWithActiveSession, getIndividualUserIncomeSourcesConfigurable}
import mocks.services.MockDateService
import models.core.IncomeSourceId.mkIncomeSourceId
import models.core.IncomeSourceIdHash.mkFromQueryString
import models.core.{IncomeSourceId, IncomeSourceIdHash}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import org.scalatest.matchers.should.Matchers
import testConstants.BaseTestConstants.*
import testConstants.BusinessDetailsTestConstants.{testLatencyDetails, *}
import testConstants.PropertyDetailsTestConstants.*
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.*
import testUtils.UnitSpec

import java.time.LocalDate

class IncomeSourceDetailsModelSpec extends UnitSpec with Matchers with MockDateService {

  val testQueryString: String = mkIncomeSourceId("XA00001234").toHash.hash
  val testSelfEmploymentIdHash: Either[Throwable, IncomeSourceIdHash] = mkFromQueryString(testQueryString)
  val testSelfEmploymentIdMaybe: Option[IncomeSourceId] = Option(mkIncomeSourceId("XA00001234"))
  val testSelfEmploymentIdHashValueMaybe: Option[String] = Option(testQueryString)
  val emptyIncomeSourceIdHash: IncomeSourceIdHash = mkIncomeSourceId("").toHash

  lazy val fixedDate: LocalDate = LocalDate.of(2023, 12, 4)

  "The IncomeSourceDetailsModel" when {

    "the user has both businesses and property income sources" should {

      //Test Business details
      s"have a business ID of $testSelfEmploymentId" in {
        businessesAndPropertyIncome.businesses.head.incomeSourceId shouldBe testSelfEmploymentId
      }
      s"have the businesses accounting period start date of ${testBusinessAccountingPeriod.start}" in {
        businessesAndPropertyIncome.businesses.head.accountingPeriod.get.start shouldBe testBusinessAccountingPeriod.start
      }
      s"have the businesses accounting period end date of ${testBusinessAccountingPeriod.end}" in {
        businessesAndPropertyIncome.businesses.head.accountingPeriod.get.end shouldBe testBusinessAccountingPeriod.end
      }
      s"should have the trading name of 'Test Business'" in {
        businessesAndPropertyIncome.businesses.head.tradingName.get shouldBe testTradeName
      }
      //Test Property details
      s"have the property accounting period start date of ${testPropertyAccountingPeriod.start}" in {
        businessesAndPropertyIncome.properties.head.accountingPeriod.get.start shouldBe testPropertyAccountingPeriod.start
      }
      s"have the property accounting period end date of ${testPropertyAccountingPeriod.end}" in {
        businessesAndPropertyIncome.properties.head.accountingPeriod.get.end shouldBe testPropertyAccountingPeriod.end
      }
    }

    "the user has just a business income source" should {
      s"have a business ID of $testSelfEmploymentId" in {
        singleBusinessIncome.businesses.head.incomeSourceId shouldBe testSelfEmploymentId
      }
      s"have the businesses accounting period start date of ${testBusinessAccountingPeriod.start}" in {
        singleBusinessIncome.businesses.head.accountingPeriod.get.start shouldBe testBusinessAccountingPeriod.start
      }
      s"have the businesses accounting period end date of ${testBusinessAccountingPeriod.end}" in {
        singleBusinessIncome.businesses.head.accountingPeriod.get.end shouldBe testBusinessAccountingPeriod.end
      }
      s"should have the trading name of 'Test Business'" in {
        singleBusinessIncome.businesses.head.tradingName.get shouldBe testTradeName
      }
      "should have latency details" in {
        singleBusinessIncomeWithLatency2019.businesses.head.latencyDetails.get shouldBe testLatencyDetails
      }
      "should have an address" in {
        singleBusinessIncomeWithLatency2019.businesses.head.address.get shouldBe testBizAddress
      }

      //Test Property details
      s"should not have property details" in {
        singleBusinessIncome.properties shouldBe Nil
      }
    }
    "the user has just a property income source" should {
      //Test Property details
      s"have the property accounting period start date of ${testPropertyAccountingPeriod.start}" in {
        propertyIncomeOnly.properties.head.accountingPeriod.get.start shouldBe testPropertyAccountingPeriod.start
      }
      s"have the property accounting period end date of ${testPropertyAccountingPeriod.end}" in {
        propertyIncomeOnly.properties.head.accountingPeriod.get.end shouldBe testPropertyAccountingPeriod.end
      }
      //Test Business Details
      "should not have business details" in {
        propertyIncomeOnly.businesses shouldBe List.empty
      }
    }
    "the user has no income source" should {
      "return None for both business and property sources" in {
        noIncomeDetails.properties shouldBe List.empty
        noIncomeDetails.businesses shouldBe List.empty
      }
    }
    "the sanitise method" should {
      "remove all unnecessary fields" in {
        val expected = IncomeSourceDetailsModel(
          testNino,
          testMtditid,
          Some((fixedDate.getYear - 1).toString),
          List(
            BusinessDetailsModel(
              incomeSourceId = "",
              incomeSource = Some(testIncomeSource),
              accountingPeriod = None,
              tradingName = Some("nextUpdates.business"),
              firstAccountingPeriodEndDate = None,
              tradingStartDate = Some(LocalDate.parse("2022-01-01")),
              contextualTaxYear = None,
              cessation = None,
              latencyDetails = None,
              address = Some(address),
            ),
            BusinessDetailsModel(
              incomeSourceId = "",
              incomeSource = Some(testIncomeSource),
              accountingPeriod = None,
              tradingName = Some("nextUpdates.business"),
              tradingStartDate = Some(LocalDate.parse("2022-01-01")),
              contextualTaxYear = None,
              firstAccountingPeriodEndDate = Some(getCurrentTaxYearEnd.minusYears(1)),
              cessation = None,
              latencyDetails = None,
              address = Some(address),
            )
          ),
          List(PropertyDetailsModel(
            incomeSourceId = "",
            accountingPeriod = None,
            firstAccountingPeriodEndDate = None,
            incomeSourceType = Some("property-unspecified"),
            tradingStartDate = Some(LocalDate.parse("2022-01-01")),
            contextualTaxYear = None,
            cessation = None,
          )
          ))
        preSanitised.sanitise shouldBe expected
      }
    }
  }

  ".compareHashToQueryString method" when {
    "user has income incomeSourceIdHashes matching the url incomeSourceIdHash" should {
      "return the matching incomeSourceId inside an Option" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, singleBusinessIncome)

        val result = user.incomeSources.compareHashToQueryString(incomeSourceIdHash = testSelfEmploymentIdHash.toOption.get)

        result shouldBe Right(testSelfEmploymentIdMaybe.get)
      }
    }
    "user has multiple incomeSourceIdHashes matching the url incomeSourceIdHash" should {
      "return the matching incomeSourceId inside an Option" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, dualBusinessIncome)

        val listOfIncomeSourceIds: List[String] = user.incomeSources.businesses.filterNot(_.isCeased).map(_.incomeSourceId)

        val result = user.incomeSources.compareHashToQueryString(incomeSourceIdHash = testSelfEmploymentIdHash.toOption.get)

        result shouldBe Left(MultipleIncomeSourcesFound(testSelfEmploymentIdHash.toOption.get.hash, listOfIncomeSourceIds))
      }
    }
    "user has no incomeSourceIdHashes matching the url incomeSourceIdHash" should {
      "return an exception" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, singleBusinessIncome2023)

        val result = user.incomeSources.compareHashToQueryString(incomeSourceIdHash = emptyIncomeSourceIdHash)

        result shouldBe Left(NoIncomeSourceFound(emptyIncomeSourceIdHash.hash))
      }
    }
    "user has no incomeSources" should {
      "return None" in {
        implicit val user: MtdItUser[_] = getIndividualUserIncomeSourcesConfigurable(fakeRequestWithActiveSession, noIncomeDetails)

        val result = user.incomeSources.compareHashToQueryString(incomeSourceIdHash = testSelfEmploymentIdHash.toOption.get)

        result shouldBe Left(NoIncomeSourceFound(testSelfEmploymentIdHash.toOption.get.hash))
      }
    }
  }


  "getIncomeSourceBusinessName" should {
    "return None when we don't pass sole trader business id for SelfEmployment" in {
      singleBusinessIncome.getIncomeSourceBusinessName(SelfEmployment, None) shouldBe None
    }
  }

  "earliestSubmissionTaxYear" should {

    "return the earliest accounting period end year across businesses and properties" in {
      val business1 = BusinessDetailsModel(
        incomeSourceId = "BUS1",
        incomeSource = None,
        accountingPeriod = None,
        tradingName = Some("Biz One"),
        firstAccountingPeriodEndDate = Some(LocalDate.of(2024, 4, 5)),
        tradingStartDate = None,
        contextualTaxYear = None,
        cessation = None,
        latencyDetails = None,
        address = None,
      )

      val business2 = business1.copy(
        incomeSourceId = "BUS2",
        firstAccountingPeriodEndDate = Some(LocalDate.of(2021, 4, 5))
      )

      val property1 = PropertyDetailsModel(
        incomeSourceId = "PROP1",
        accountingPeriod = None,
        firstAccountingPeriodEndDate = Some(LocalDate.of(2023, 4, 5)),
        incomeSourceType = None,
        tradingStartDate = None,
        contextualTaxYear = None,
        cessation = None,
      )

      val model = IncomeSourceDetailsModel(
        nino = testNino,
        mtdbsa = testMtditid,
        yearOfMigration = Some("2020"),
        businesses = List(business1, business2),
        properties = List(property1)
      )

      model.earliestSubmissionTaxYear shouldBe Some(2021)
    }

    "return None when there are no accounting period end dates" in {
      val model = IncomeSourceDetailsModel(
        nino = testNino,
        mtdbsa = testMtditid,
        yearOfMigration = Some("2020"),
        businesses = Nil,
        properties = Nil
      )

      model.earliestSubmissionTaxYear shouldBe None
    }
  }

  "orderedTaxYearsByAccountingPeriods" should {

    "return all tax years from 2018" when {

      "the date service returns a tax year of 2025-26 and the earliest first accounting period end date is in 2017-18" in {
        val model = IncomeSourceDetailsModel(
          nino = testNino,
          mtdbsa = testMtditid,
          yearOfMigration = Some("2020"),
          businesses = List(business1, business2),
          properties = Nil
        )

        setupMockGetCurrentTaxYearEnd(mockDateService)(2026)

        model.orderedTaxYearsByAccountingPeriods(mockDateService) shouldBe List(2018, 2019, 2020, 2021, 2022, 2023, 2024, 2025, 2026)
      }
    }
  }

  "orderedTaxYearsByYearOfMigration" should {

    "return all tax years from 2020" when {

      "a year of migration is defined as 2020" in {
        val model = IncomeSourceDetailsModel(
          nino = testNino,
          mtdbsa = testMtditid,
          yearOfMigration = Some("2020"),
          businesses = List(business1, business2),
          properties = Nil
        )

        setupMockGetCurrentTaxYearEnd(mockDateService)(2026)

        model.orderedTaxYearsByYearOfMigration(mockDateService) shouldBe List(2020, 2021, 2022, 2023, 2024, 2025, 2026)
      }
    }

    "return all tax years from 2018" when {

      "a year of migration is not defined and it falls back to the accounting period method" in {
        val model = IncomeSourceDetailsModel(
          nino = testNino,
          mtdbsa = testMtditid,
          yearOfMigration = None,
          businesses = List(business1, business2),
          properties = Nil
        )

        setupMockGetCurrentTaxYearEnd(mockDateService)(2026)

        model.orderedTaxYearsByAccountingPeriods(mockDateService) shouldBe List(2018, 2019, 2020, 2021, 2022, 2023, 2024, 2025, 2026)
      }
    }
  }

  "return all active business addresses" when {

    "getAllUniqueBusinessAddresses finds an international address" in {
      val model = IncomeSourceDetailsModel(
        nino = testNino,
        mtdbsa = testMtditid,
        yearOfMigration = None,
        businesses = List(business1International),
        properties = Nil
      )
      model.getAllUniqueBusinessAddresses shouldBe List("31 Some street")
    }
    "getAllUniqueBusinessAddresses finds a UK address" in {
      val model = IncomeSourceDetailsModel(
        nino = testNino,
        mtdbsa = testMtditid,
        yearOfMigration = None,
        businesses = List(business1),
        properties = Nil
      )
      model.getAllUniqueBusinessAddresses shouldBe List("8 Test, NE12 6CI")
    }

    "getAllUniqueBusinessAddressesWithIndex finds two international address that are not distinct" in {
      val model = IncomeSourceDetailsModel(
        nino = testNino,
        mtdbsa = testMtditid,
        yearOfMigration = None,
        businesses = List(business1International, business1International),
        properties = Nil
      )
      model.getAllUniqueBusinessAddressesWithIndex shouldBe List(("31 Some street", 0))
    }
    "getAllUniqueBusinessAddressesWithIndex finds a UK address" in {
      val model = IncomeSourceDetailsModel(
        nino = testNino,
        mtdbsa = testMtditid,
        yearOfMigration = None,
        businesses = List(business1),
        properties = Nil
      )
      model.getAllUniqueBusinessAddressesWithIndex shouldBe List(("8 Test, NE12 6CI", 0))
    }
  }


}