/*
 * Copyright 2019 HM Revenue & Customs
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

import models.financialTransactions.{SubItemModel, TransactionModel}
import org.scalatest.Matchers
import implicits.ImplicitDateFormatter._
import testUtils.TestSupport

class TransactionModelSpec extends TestSupport with Matchers {

  lazy val charge = SubItemModel(dueDate = Some("2018-07-01"))
  lazy val payment = SubItemModel(paymentReference = Some("XYZ"))

  "TransactionModel.isPaid method" should {

    "return true when the outstanding amount is £0" in {
      val model = TransactionModel(outstandingAmount = Some(0))
      model.isPaid shouldBe true
    }

    "return true when there is no outstandingAmount" in {
      val model = TransactionModel(outstandingAmount = None)
      model.isPaid shouldBe true
    }

    "return true when the outstanding amount is < £0" in {
      val model = TransactionModel(outstandingAmount = Some(-0.01))
      model.isPaid shouldBe true
    }

    "return false when the outstanding amount is > £0" in {
      val model = TransactionModel(outstandingAmount = Some(0.01))
      model.isPaid shouldBe false
    }
  }

  "TransactionModel.payments method" when {

    "return just the payments when a sequence of payments and charges is returned (3xPayment)" in {
      val model = TransactionModel(items = Some(Seq(charge, charge, payment, charge, payment, charge, payment)))
      model.payments().length shouldBe 3
    }

    "return no payments when a sequence of charges is held" in {
      val model = TransactionModel(items = Some(Seq(charge, charge, charge, charge)))
      model.payments().length shouldBe 0
    }

    "return no payments when no sub items are held" in {
      val model = TransactionModel()
      model.payments().length shouldBe 0
    }
  }

  "TransactionModel.charges method" when {

    "return just the charges when a sequence of payments and charges is returned (4xCharges)" in {
      val model = TransactionModel(items = Some(Seq(charge, charge, payment, charge, payment, charge, payment)))
      model.charges().length shouldBe 4
    }

    "return no charges when a sequence of payments is held" in {
      val model = TransactionModel(items = Some(Seq(payment, payment, payment)))
      model.charges().length shouldBe 0
    }

    "return no charges when no sub items are held" in {
      val model = TransactionModel()
      model.charges().length shouldBe 0
    }
  }

  "TransactionModel.eligibleToPay" should {

    "return a true" when {

      "payment is enabled and has not been made" in {
        frontendAppConfig.features.paymentEnabled(true)
        TransactionModel(outstandingAmount = Some(1)).eligibleToPay(frontendAppConfig) shouldBe true
      }
    }

    "return a false" when {

      "payment is disabled" in {
        frontendAppConfig.features.paymentEnabled(false)
        TransactionModel(outstandingAmount = Some(1)).eligibleToPay(frontendAppConfig) shouldBe false
      }

      "payment has been made" in {
        frontendAppConfig.features.paymentEnabled(true)
        TransactionModel(outstandingAmount = Some(0)).eligibleToPay(frontendAppConfig) shouldBe false

      }
    }
  }

}
