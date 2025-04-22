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

package models.paymentAllocationCharges

import exceptions.MissingFieldException
import models.paymentAllocations.AllocationDetail
import org.scalatest.matchers.should.Matchers
import testUtils.TestSupport

import java.time.LocalDate

class PaymentAllocationViewModelSpec extends TestSupport with Matchers{


  "allocationDetailActual" should{
    "return value when value is present" in {
      val allocationDetail = AllocationDetail(None, None, None, None, None, None, None, None)

      val allocationDetailWithClearingDate = AllocationDetailWithClearingDate(Some(allocationDetail), None)

      allocationDetailWithClearingDate.allocationDetailActual shouldBe allocationDetail
    }

    "throw MissingFieldException when value is not present" in {
      val allocationDetailWithClearingDate = AllocationDetailWithClearingDate(None, None)

      intercept[MissingFieldException]{
        allocationDetailWithClearingDate.allocationDetailActual
      }
    }
  }

  "clearingDateActual" should{
    "return value when value is present" in {
      val clearingDate = LocalDate.parse("2018-08-04")

      val allocationDetailWithClearingDate = AllocationDetailWithClearingDate(None, Some((clearingDate)))

      allocationDetailWithClearingDate.clearingDateActual shouldBe clearingDate
    }

    "throw MissingFieldException when value is not present" in {
      val allocationDetailWithClearingDate = AllocationDetailWithClearingDate(None, None)

      intercept[MissingFieldException]{
        allocationDetailWithClearingDate.clearingDateActual
      }
    }
  }
}