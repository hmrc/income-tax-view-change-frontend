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

package models.liabilitycalculation

import exceptions.MissingFieldException
import org.scalatest.matchers.should.Matchers
import testUtils.TestSupport

class CalculationSpec extends TestSupport with Matchers{

  "planTypeActual" should{
    "return value when value is populated" in {
      val studentLoan = StudentLoan(planType = Some("03"))

      studentLoan.planTypeActual shouldBe "3"
    }

    "throw MissingFieldException when value is missing" in {
      val studentLoan = StudentLoan()

      intercept[MissingFieldException]{
        studentLoan.planTypeActual
      }
    }
  }

  "studentLoanRepaymentAmountActual" should {
    "return value when value is populated" in {
      val amount = BigDecimal(10000.0)
      val studentLoan = StudentLoan(studentLoanRepaymentAmount = Some(amount))

      studentLoan.studentLoanRepaymentAmountActual shouldBe amount
    }
    "throw MissingFieldException when value is missing" in {
      val studentLoan = StudentLoan()

      intercept[MissingFieldException]{
        studentLoan.studentLoanRepaymentAmountActual
      }
    }
  }
}
