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

package models.liabilitycalculation

import models.liabilitycalculation.taxcalculation._
import play.api.http.Status
import play.api.libs.json._
import testUtils.UnitSpec

import scala.io.Source

class LiabilityCalculationResponseModelSpec extends UnitSpec {

  "LastTaxCalculationResponseMode model" when {
    "successful successModelMinimal" should {
      val successModelMinimal = LiabilityCalculationResponse(
        calculation = None,
        metadata = Metadata(
          calculationTimestamp = "2019-02-15T09:35:15.094Z",
          crystallised = true)
      )
      val expectedJson = s"""
                            |{
                            |  "metadata" : {
                            |    "calculationTimestamp" : "2019-02-15T09:35:15.094Z",
                            |    "crystallised" : true
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

    "successful successModelFull" should {
      val successModelFull = LiabilityCalculationResponse(
        calculation = Some(Calculation(
          allowancesAndDeductions = Some(AllowancesAndDeductions(
            personalAllowance = Some(12500),
            reducedPersonalAllowance = Some(12500),
            marriageAllowanceTransferOut = Some(MarriageAllowanceTransferOut(
              personalAllowanceBeforeTransferOut = 5000.99,
              transferredOutAmount = 5000.99)),
            pensionContributions = Some(5000.99),
            lossesAppliedToGeneralIncome = Some(12500),
            giftOfInvestmentsAndPropertyToCharity = Some(12500),
            grossAnnuityPayments = Some(5000.99),
            qualifyingLoanInterestFromInvestments = Some(5000.99),
            postCessationTradeReceipts = Some(5000.99),
            paymentsToTradeUnionsForDeathBenefits = Some(5000.99))),
          chargeableEventGainsIncome = Some(ChargeableEventGainsIncome(
            totalOfAllGains = 12500
          )),
          dividendsIncome = Some(DividendsIncome(chargeableForeignDividends = Some(12500))),
          employmentAndPensionsIncome = Some(EmploymentAndPensionsIncome(
            totalPayeEmploymentAndLumpSumIncome = Some(5000.99),
            totalBenefitsInKind = Some(5000.99),
            totalOccupationalPensionIncome = Some(5000.99)
          )),
          employmentExpenses = Some(EmploymentExpenses(totalEmploymentExpenses = Some(5000.99))),
          foreignIncome = Some(ForeignIncome(
            chargeableOverseasPensionsStateBenefitsRoyalties = Some(5000.99),
            chargeableAllOtherIncomeReceivedWhilstAbroad = Some(5000.99),
            overseasIncomeAndGains = Some(OverseasIncomeAndGains(gainAmount = 5000.99)),
            totalForeignBenefitsAndGifts = Some(5000.99)
          )),
          giftAid = Some(GiftAid(
            grossGiftAidPayments = 12500,
            giftAidTax = 5000.99
          )),
          incomeSummaryTotals = Some(IncomeSummaryTotals(
            totalSelfEmploymentProfit = Some(12500),
            totalPropertyProfit = Some(12500),
            totalFHLPropertyProfit = Some(12500),
            totalForeignPropertyProfit = Some(12500),
            totalEeaFhlProfit = Some(12500)
          )),
          marriageAllowanceTransferredIn = Some(MarriageAllowanceTransferredIn(amount = Some(5000.99))),
          reliefs = Some(Reliefs(reliefsClaimed = Seq(ReliefsClaimed(
            `type` = "vctSubscriptions",
            amountUsed = Some(5000.99)),
            ReliefsClaimed(
              `type` = "vctSubscriptions2",
              amountUsed = Some(5000.99)),
          ),
            residentialFinanceCosts = Some(ResidentialFinanceCosts(totalResidentialFinanceCostsRelief = 5000.99)),
            foreignTaxCreditRelief = Some(ForeignTaxCreditRelief(totalForeignTaxCreditRelief = 5000.99)),
            topSlicingRelief = Some(TopSlicingRelief(amount = Some(5000.99))))),
          savingsAndGainsIncome = Some(SavingsAndGainsIncome(
            chargeableForeignSavingsAndGains = Some(12500)
          )),
          shareSchemesIncome = Some(ShareSchemesIncome(
            totalIncome = 5000.99
          )),
          stateBenefitsIncome = Some(StateBenefitsIncome(totalStateBenefitsIncome = Some(5000.99))),
          taxCalculation = Some(TaxCalculation(
            incomeTax = IncomeTax(
              totalIncomeReceivedFromAllSources = 12500,
              totalAllowancesAndDeductions = 12500,
              totalTaxableIncome = 12500,
              payPensionsProfit = Some(PayPensionsProfit(
                taxBands = Seq(TaxBands(
                  name = "SSR",
                  rate = 20,
                  bandLimit = 12500,
                  apportionedBandLimit = 12500,
                  income = 12500,
                  taxAmount = 5000.99
                ))
              )),
              savingsAndGains = Some(SavingsAndGains(
                taxableIncome = 12500,
                taxBands = Seq(TaxBands(
                  name = "SSR",
                  rate = 20,
                  bandLimit = 12500,
                  apportionedBandLimit = 12500,
                  income = 12500,
                  taxAmount = 5000.99
                ))
              )),
              dividends = Some(Dividends(
                taxableIncome = 12500,
                taxBands = Seq(TaxBands(
                  name = "SSR",
                  rate = 20,
                  bandLimit = 12500,
                  apportionedBandLimit = 12500,
                  income = 12500,
                  taxAmount = 5000.99
                ))
              )),
              lumpSums = Some(LumpSums(
                taxBands = Seq(TaxBands(
                  name = "SSR",
                  rate = 20,
                  bandLimit = 12500,
                  apportionedBandLimit = 12500,
                  income = 12500,
                  taxAmount = 5000.99
                ))
              )),
              gainsOnLifePolicies = Some(GainsOnLifePolicies(
                taxBands = Seq(TaxBands(
                  name = "SSR",
                  rate = 20,
                  bandLimit = 12500,
                  apportionedBandLimit = 12500,
                  income = 12500,
                  taxAmount = 5000.99
                ))
              )),
              totalReliefs = Some(5000.99),
              totalNotionalTax = Some(5000.99),
              incomeTaxDueAfterTaxReductions = Some(5000.99),
              totalPensionSavingsTaxCharges = Some(5000.99),
              statePensionLumpSumCharges = Some(5000.99),
              payeUnderpaymentsCodedOut = Some(5000.99)
            ),
            nics = Some(Nics(
              class4Nics = Some(Class4Nics(nic4Bands = Seq(Nic4Bands(
                name = "ZRT",
                income = 12500,
                rate = 20,
                amount = 5000.99
              )))),
              class2Nics = Some(Class2Nics(amount = Some(5000.99)))
            )),
            capitalGainsTax = Some(CapitalGainsTax(
              totalCapitalGainsIncome = 5000.99,
              adjustments = Some(-99999999999.99),
              foreignTaxCreditRelief = Some(5000.99),
              taxOnGainsAlreadyPaid = Some(5000.99),
              capitalGainsTaxDue = 5000.99,
              capitalGainsOverpaid = Some(5000.99),
              residentialPropertyAndCarriedInterest = Some(ResidentialPropertyAndCarriedInterest(
                cgtTaxBands = Seq(CgtTaxBands(
                  name = "lowerRate",
                  rate = 20,
                  income = 5000.99,
                  taxAmount = 5000.99
                ),
                  CgtTaxBands(
                    name = "lowerRate2",
                    rate = 21,
                    income = 5000.99,
                    taxAmount = 5000.99
                  ))
              )),
              businessAssetsDisposalsAndInvestorsRel = Some(BusinessAssetsDisposalsAndInvestorsRel(
                taxableGains = Some(5000.99),
                rate = Some(20),
                taxAmount = Some(5000.99)
              ))
            )),
            totalStudentLoansRepaymentAmount = Some(5000.99),
            saUnderpaymentsCodedOut = Some(-99999999999.99),
            totalIncomeTaxAndNicsDue = -99999999999.99,
            totalTaxDeducted = Some(-99999999999.99)
          )),
          taxDeductedAtSource = Some(TaxDeductedAtSource(
            ukLandAndProperty = Some(5000.99),
            bbsi = Some(5000.99),
            cis = Some(5000.99),
            voidedIsa = Some(5000.99),
            payeEmployments = Some(5000.99),
            occupationalPensions = Some(5000.99),
            stateBenefits = Some(-99999999999.99),
            specialWithholdingTaxOrUkTaxPaid = Some(5000.99),
            inYearAdjustmentCodedInLaterTaxYear = Some(5000.99),
          )))),
        metadata = Metadata(
          calculationTimestamp = "2019-02-15T09:35:15.094Z",
          crystallised = true)
      )

      val source = Source.fromURL(getClass.getResource("/liabilityResponsePruned.json"))
      val expectedJsonPruned = try source.mkString finally source.close()

      "be translated to Json correctly" in {
        Json.toJson(successModelFull) shouldBe Json.parse(expectedJsonPruned)
      }

      "should convert from json to model" in {
        val calcResponse = Json.fromJson[LiabilityCalculationResponse](Json.parse(expectedJsonPruned))
        Json.toJson(calcResponse.get) shouldBe Json.parse(expectedJsonPruned)
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

}
