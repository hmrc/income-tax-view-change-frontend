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

package models.financialDetails

import scala.util.Try

object MfaDebitUtils {
  private val mfaMainTransactionToMainType: Map[Int, String] = Map(
    (4000 -> "ITSA PAYE Charge"),
    (4001 -> "ITSA Calc Error Correction"),
    (4002 -> "ITSA Manual Penalty Pre CY-4"),
    (4003 -> "ITSA Misc Charge"))

  def isMFADebitMainTransaction(mainTransaction: Option[String]): Boolean = {
    mainTransaction
      .flatMap(t => Try(t.toInt).toOption)
      .exists(mfaMainTransactionToMainType.contains)
  }

  def isMFADebitMainType(mainType: Option[String]): Boolean = {
    mainType.exists(mfaMainTransactionToMainType.values.toList.contains(_))
  }

  def filterMFADebits(MFADebitsEnabled: Boolean, documentDetailWithDueDate: DocumentDetailWithDueDate): Boolean = {
    !MFADebitsEnabled && documentDetailWithDueDate.isMFADebit
  }
}
