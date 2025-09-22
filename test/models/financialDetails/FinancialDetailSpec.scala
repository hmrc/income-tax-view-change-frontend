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

package models.financialDetails

import enums.ChargeType._
import models.financialDetails.FinancialDetail.{getMessageKeyByTypes, getMessageKeyForChargeType, getMessageKeyForMainType}
import testConstants.FinancialDetailsTestConstants.financialDetail
import testUtils.UnitSpec

class FinancialDetailSpec extends UnitSpec {

  "FinancialDetail" when {

    "working with Dunning Locks" when {
      val supportedLock = SubItem(dunningLock = Some("Stand over order"), amount = Some(123.45))
      val unsupportedLock = SubItem(dunningLock = Some("Disputed debt"), amount = Some(98.76))

      def financialDetailWithoutLocks: FinancialDetail = financialDetail()

      def financialDetailWithAllLocks: FinancialDetail = financialDetailWithoutLocks.copy(items = Some(Seq(supportedLock, unsupportedLock)))

      def financialDetailWithUnsupportedLocks: FinancialDetail = financialDetailWithoutLocks.copy(items = Some(Seq(unsupportedLock)))

      "calling a predicate for Dunning Locks" should {
        "return true when there are sub items with supported Dunning Lock" in {
          financialDetailWithAllLocks.dunningLockExists shouldBe true
        }

        "return false" when {
          "there are sub items with only unsupported Dunning Locks" in {
            financialDetailWithUnsupportedLocks.dunningLockExists shouldBe false
          }

          "there are no sub items with Dunning Locks" in {
            financialDetailWithoutLocks.dunningLockExists shouldBe false
          }
        }
      }

      "getting Dunning Locks" should {
        "return all sub items with supported Dunning Locks" in {
          financialDetailWithAllLocks.dunningLocks shouldBe Seq(supportedLock)
        }

        "return empty list" when {
          "there are sub items with only unsupported Dunning Locks" in {
            financialDetailWithUnsupportedLocks.dunningLocks shouldBe Nil
          }

          "there are no sub items with Dunning Locks" in {
            financialDetailWithoutLocks.dunningLocks shouldBe Nil
          }
        }
      }

    }
    "working with Interest Locks" when {
      def financialDetailWithoutLocks: FinancialDetail = financialDetail()

      def financialDetailWithLocks(lock: String): FinancialDetail = financialDetailWithoutLocks.copy(
        items = Some(Seq(interestLockSubItem(lock)))
      )

      def interestLockSubItem(lock: String) = SubItem(interestLock = Some(lock), amount = Some(123.45))

      val supportedLocks = List(
        "Clerical Interest Signal",
        "Manual Interest Calculated",
        "C18 Appeal in Progress",
        "Manual RPI Signal",
        "Breathing Space Moratorium Act")

      "calling a predicate for Interest Locks" should {
        for (supportedLock <- supportedLocks) {
          s"return true when there are sub items with the $supportedLock Interest Lock" in {
            financialDetailWithLocks(supportedLock).interestLockExists shouldBe true
          }
        }

        "return false there are no sub items with Interest Locks" in {
          financialDetailWithoutLocks.interestLockExists shouldBe false
        }

      }
    }

    "working with Accrued Interest" when {

      "should return true" in {
        financialDetail(accruedInterest = Some(3)).hasAccruedInterest shouldBe true
      }


      "should return false" in {
        financialDetail().hasAccruedInterest shouldBe false
      }
    }

    "checking credit types" should {
      "return correct credit type" in {
        financialDetail(mainType = "ITSA Misc Credit", mainTransaction = "4023").getChargeType shouldBe Some(MfaCreditType)
        financialDetail(mainType = "ITSA Cutover Credits", mainTransaction = "6110").getChargeType shouldBe Some(CutOverCreditType)
        financialDetail(mainType = "SA Balancing Charge Credit", mainTransaction = "4905").getChargeType shouldBe Some(BalancingChargeCreditType)
        financialDetail(mainType = "Not a valid type", mainTransaction = "9999").getChargeType shouldBe None

        val emptyMainTypeDetail =  FinancialDetail.apply(taxYear = "2022", mainType = None, items = None)
        emptyMainTypeDetail.getChargeType shouldBe None
      }
    }
  }

  "FinancialDetail.Types" when {

    "calling .getMainTypeMessageKey with an optional value of a mainType" should {
      "return Some correct message key for a known mainType" in {
        getMessageKeyForMainType(Some("SA Payment on Account 1")) shouldBe Some("poa1")
        getMessageKeyForMainType(Some("SA Payment on Account 2")) shouldBe Some("poa2")
        getMessageKeyForMainType(Some("SA Balancing Charge")) shouldBe Some("bcd")
      }

      "return None for an unknown or absent mainType" in {
        getMessageKeyForMainType(Some("rubbish")) shouldBe None
        getMessageKeyForMainType(None) shouldBe None
      }
    }

    "calling .getMessageKeyForChargeType with an optional value of a chargeType" should {
      "return Some correct message key for a known chargeType" in {
        getMessageKeyForChargeType(Some(ITSA_ENGLAND_AND_NI)) shouldBe Some("incomeTax")
        getMessageKeyForChargeType(Some(ITSA_NI)) shouldBe Some("incomeTax")
        getMessageKeyForChargeType(Some(ITSA_SCOTLAND)) shouldBe Some("incomeTax")
        getMessageKeyForChargeType(Some(ITSA_WALES)) shouldBe Some("incomeTax")

        getMessageKeyForChargeType(Some(NIC4_GB)) shouldBe Some("nic4")
        getMessageKeyForChargeType(Some(NIC4_SCOTLAND)) shouldBe Some("nic4")
        getMessageKeyForChargeType(Some(NIC4_WALES)) shouldBe Some("nic4")
        getMessageKeyForChargeType(Some(NIC4_NI)) shouldBe Some("nic4")

        getMessageKeyForChargeType(Some("NIC2 Scotland")) shouldBe Some("nic2")
        getMessageKeyForChargeType(Some(NIC2_WALES)) shouldBe Some("nic2")
        getMessageKeyForChargeType(Some(NIC2_GB)) shouldBe Some("nic2")
        getMessageKeyForChargeType(Some(NIC2_GB)) shouldBe Some("nic2")

        getMessageKeyForChargeType(Some(VOLUNTARY_NIC2_GB)) shouldBe Some("vcnic2")
        getMessageKeyForChargeType(Some(VOLUNTARY_NIC2_NI)) shouldBe Some("vcnic2")
        getMessageKeyForChargeType(Some("Voluntary NIC2-Scotland")) shouldBe Some("vcnic2")
        getMessageKeyForChargeType(Some("Voluntary NIC2-Wales")) shouldBe Some("vcnic2")

        getMessageKeyForChargeType(Some(CGT)) shouldBe Some("cgt")
        getMessageKeyForChargeType(Some(SL)) shouldBe Some("sl")
      }

      "return None for an unknown or absent chargeType" in {
        getMessageKeyForChargeType(Some("rubbish")) shouldBe None
        getMessageKeyForChargeType(None) shouldBe None
      }
    }

    "calling .getMessageKeyByTypes with optional values of mainType and chargeType" should {
      "return Some correct message key for a supported combination of mainType and chargeType" in {
        getMessageKeyByTypes(Some("SA Payment on Account 1"), Some(ITSA_ENGLAND_AND_NI)) shouldBe Some("poa1.incomeTax")
        getMessageKeyByTypes(Some("SA Payment on Account 1"), Some(NIC4_WALES)) shouldBe Some("poa1.nic4")

        getMessageKeyByTypes(Some("SA Payment on Account 2"), Some(ITSA_NI)) shouldBe Some("poa2.incomeTax")
        getMessageKeyByTypes(Some("SA Payment on Account 2"), Some(NIC4_GB)) shouldBe Some("poa2.nic4")

        getMessageKeyByTypes(Some("SA Balancing Charge"), Some(ITSA_SCOTLAND)) shouldBe Some("bcd.incomeTax")
        getMessageKeyByTypes(Some("SA Balancing Charge"), Some(NIC4_NI)) shouldBe Some("bcd.nic4")
        getMessageKeyByTypes(Some("SA Balancing Charge"), Some(NIC2_GB)) shouldBe Some("bcd.nic2")
        getMessageKeyByTypes(Some("SA Balancing Charge"), Some(VOLUNTARY_NIC2_GB)) shouldBe Some("bcd.vcnic2")
        getMessageKeyByTypes(Some("SA Balancing Charge"), Some(CGT)) shouldBe Some("bcd.cgt")
        getMessageKeyByTypes(Some("SA Balancing Charge"), Some(SL)) shouldBe Some("bcd.sl")
        getMessageKeyByTypes(Some("ITSA PAYE Charge"), Some("test")) shouldBe Some("hmrcAdjustment.text")
        getMessageKeyByTypes(Some("ITSA Calc Error Correction"), Some("test")) shouldBe Some("hmrcAdjustment.text")
        getMessageKeyByTypes(Some("ITSA Manual Penalty Pre CY-4"), Some("test")) shouldBe Some("hmrcAdjustment.text")
        getMessageKeyByTypes(Some("ITSA Misc Charge"), Some("test")) shouldBe Some("hmrcAdjustment.text")
      }


      "return None for a combination of unsupported or absent mainType and chargeType" in {
        getMessageKeyByTypes(Some("SA Payment on Account 1"), Some(NIC2_GB)) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 1"), Some(VOLUNTARY_NIC2_GB)) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 1"), Some(CGT)) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 1"), Some(SL)) shouldBe None

        getMessageKeyByTypes(Some("SA Payment on Account 2"), Some(NIC2_GB)) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 2"), Some(VOLUNTARY_NIC2_GB)) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 2"), Some(CGT)) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 2"), Some(SL)) shouldBe None

        getMessageKeyByTypes(Some("SA Payment on Account 1"), Some("rubbish")) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 2"), Some("rubbish")) shouldBe None
        getMessageKeyByTypes(Some("SA Balancing Charge"), Some("rubbish")) shouldBe None

        getMessageKeyByTypes(Some("SA Payment on Account 1"), None) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 2"), None) shouldBe None
        getMessageKeyByTypes(Some("SA Balancing Charge"), None) shouldBe None

        getMessageKeyByTypes(Some("rubbish"), Some(ITSA_ENGLAND_AND_NI)) shouldBe None
        getMessageKeyByTypes(None, Some(NIC4_WALES)) shouldBe None

        getMessageKeyByTypes(None, None) shouldBe None
      }

    }

  }
}
