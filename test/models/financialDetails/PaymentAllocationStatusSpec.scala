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

package models.financialDetails

import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.Properties

object PaymentAllocationStatusSpec extends Properties("PaymentAllocationStatus_allocationStatus") {

  val defaultPayment = Payment(reference = Some("ref"), amount = Some(0.00), method = Some("method"),
    lot = Some("lot2"), lotItem = Some("lotItem2"), documentDescription = None,
    dueDate = Some("2018-12-12"), documentDate = "2018-12-12", outstandingAmount = None, transactionId = Some("DOCID02"))

  property("allocationStatus_FullyAllocatedPaymentStatus") = forAll { outstanding: BigDecimal =>
    (outstanding != 0) ==> {
      val payment = defaultPayment.copy(amount = Some(outstanding), outstandingAmount = Some(BigDecimal(0.0)))
      payment.allocationStatus() == Some(FullyAllocatedPaymentStatus)
    }
  }

  property("allocationStatus_NotYetAllocatedPaymentStatus") = forAll { someAmount: BigDecimal =>
    (someAmount != 0) ==> {
      val payment = defaultPayment.copy(amount = Some(someAmount), outstandingAmount = Some(someAmount))
      payment.allocationStatus() == Some(NotYetAllocatedPaymentStatus)
    }
  }

  property("allocationStatus_PartiallyAllocatedPaymentStatus") = forAll { (originalAmount: BigDecimal, outstanding: BigDecimal) =>
    (outstanding != 0 && originalAmount != outstanding) ==> {
      val payment = defaultPayment.copy(amount = Some(originalAmount), outstandingAmount = Some(outstanding))
      payment.allocationStatus() == Some(PartiallyAllocatedPaymentStatus)
    }
  }

  property("allocationStatus_None") = forAll { _: BigDecimal =>
    val payment = defaultPayment.copy(amount = None, outstandingAmount = None)
    payment.allocationStatus() == None
  }
}
