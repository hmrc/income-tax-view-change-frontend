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

package models.financialDetails

import assets.FinancialDetailsTestConstants.financialDetail
import models.financialDetails.FinancialDetail.{getChargeTypeKey, getMainTypeKey, getMessageKeyByTypes}
import uk.gov.hmrc.play.test.UnitSpec

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

  }

  "FinancialDetail.Types" when {

    "calling .getMainTypeKey with an optional value of a mainType" should {
      "return Some correct message key for a known mainType" in {
        getMainTypeKey(Some("SA Payment on Account 1")) shouldBe Some("poa1")
        getMainTypeKey(Some("SA Payment on Account 2")) shouldBe Some("poa2")
        getMainTypeKey(Some("SA Balancing Charge")) shouldBe Some("bcd")
      }

      "return None for an unknown or absent mainType" in {
        getMainTypeKey(Some("rubbish")) shouldBe None
        getMainTypeKey(None) shouldBe None
      }
    }

    "calling .getChargeTypeKey with an optional value of a chargeType" should {
      "return Some correct message key for a known chargeType" in {
        getChargeTypeKey(Some("ITSA England & NI")) shouldBe Some("incomeTax")
        getChargeTypeKey(Some("ITSA NI")) shouldBe Some("incomeTax")
        getChargeTypeKey(Some("ITSA Scotland")) shouldBe Some("incomeTax")
        getChargeTypeKey(Some("ITSA Wales")) shouldBe Some("incomeTax")

        getChargeTypeKey(Some("NIC4-GB")) shouldBe Some("nic4")
        getChargeTypeKey(Some("NIC4 Scotland")) shouldBe Some("nic4")
        getChargeTypeKey(Some("NIC4 Wales")) shouldBe Some("nic4")
        getChargeTypeKey(Some("NIC4-NI")) shouldBe Some("nic4")

        getChargeTypeKey(Some("NIC2 Scotland")) shouldBe Some("nic2")
        getChargeTypeKey(Some("NIC2 Wales")) shouldBe Some("nic2")
        getChargeTypeKey(Some("NIC2-GB")) shouldBe Some("nic2")
        getChargeTypeKey(Some("NIC2-NI")) shouldBe Some("nic2")

        getChargeTypeKey(Some("Voluntary NIC2-GB")) shouldBe Some("vcnic2")
        getChargeTypeKey(Some("Voluntary NIC2-NI")) shouldBe Some("vcnic2")
        getChargeTypeKey(Some("Voluntary NIC2-Scotland")) shouldBe Some("vcnic2")
        getChargeTypeKey(Some("Voluntary NIC2-Wales")) shouldBe Some("vcnic2")

        getChargeTypeKey(Some("CGT")) shouldBe Some("cgt")
        getChargeTypeKey(Some("SL")) shouldBe Some("sl")
      }

      "return None for an unknown or absent chargeType" in {
        getChargeTypeKey(Some("rubbish")) shouldBe None
        getMainTypeKey(None) shouldBe None
      }
    }

    "calling .getMessageKeyByTypes with optional values of mainType and chargeType" should {
      "return Some correct message key for a supported combination of mainType and chargeType" in {
        getMessageKeyByTypes(Some("SA Payment on Account 1"), Some("ITSA England & NI")) shouldBe Some("poa1.incomeTax")
        getMessageKeyByTypes(Some("SA Payment on Account 1"), Some("NIC4 Wales")) shouldBe Some("poa1.nic4")

        getMessageKeyByTypes(Some("SA Payment on Account 2"), Some("ITSA NI")) shouldBe Some("poa2.incomeTax")
        getMessageKeyByTypes(Some("SA Payment on Account 2"), Some("NIC4-GB")) shouldBe Some("poa2.nic4")

        getMessageKeyByTypes(Some("SA Balancing Charge"), Some("ITSA Scotland")) shouldBe Some("bcd.incomeTax")
        getMessageKeyByTypes(Some("SA Balancing Charge"), Some("NIC4-NI")) shouldBe Some("bcd.nic4")
        getMessageKeyByTypes(Some("SA Balancing Charge"), Some("NIC2-GB")) shouldBe Some("bcd.nic2")
        getMessageKeyByTypes(Some("SA Balancing Charge"), Some("Voluntary NIC2-GB")) shouldBe Some("bcd.vcnic2")
        getMessageKeyByTypes(Some("SA Balancing Charge"), Some("CGT")) shouldBe Some("bcd.cgt")
        getMessageKeyByTypes(Some("SA Balancing Charge"), Some("SL")) shouldBe Some("bcd.sl")
      }


      "return None for a combination of unsupported or absent mainType and chargeType" in {
        getMessageKeyByTypes(Some("SA Payment on Account 1"), Some("NIC2-GB")) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 1"), Some("Voluntary NIC2-GB")) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 1"), Some("CGT")) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 1"), Some("SL")) shouldBe None

        getMessageKeyByTypes(Some("SA Payment on Account 2"), Some("NIC2-GB")) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 2"), Some("Voluntary NIC2-GB")) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 2"), Some("CGT")) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 2"), Some("SL")) shouldBe None

        getMessageKeyByTypes(Some("SA Payment on Account 1"), Some("rubbish")) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 2"), Some("rubbish")) shouldBe None
        getMessageKeyByTypes(Some("SA Balancing Charge"), Some("rubbish")) shouldBe None

        getMessageKeyByTypes(Some("SA Payment on Account 1"), None) shouldBe None
        getMessageKeyByTypes(Some("SA Payment on Account 2"), None) shouldBe None
        getMessageKeyByTypes(Some("SA Balancing Charge"), None) shouldBe None

        getMessageKeyByTypes(Some("rubbish"), Some("ITSA England & NI")) shouldBe None
        getMessageKeyByTypes(None, Some("NIC4 Wales")) shouldBe None

        getMessageKeyByTypes(None, None) shouldBe None
      }

    }

  }
}
