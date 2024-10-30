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

object ReviewAndReconcileUtils {

  lazy val paymentOnAccountOneReviewAndReconcileDebit = "4911"
  lazy val paymentOnAccountTwoReviewAndReconcileDebit = "4913"

  private val paymentOnAccountOneReviewAndReconcileCredit = "4912"
  private val paymentOnAccountTwoReviewAndReconcileCredit = "4914"

  def isReviewAndReconcilePoaOneDebit(mainTransaction: Option[String]): Boolean =
    mainTransaction.contains(paymentOnAccountOneReviewAndReconcileDebit)

  def isReviewAndReconcilePoaTwoDebit(mainTransaction: Option[String]): Boolean =
    mainTransaction.contains(paymentOnAccountTwoReviewAndReconcileDebit)

  def isReviewAndReconcilePoaOneCredit(mainTransaction: Option[String]): Boolean =
    mainTransaction.contains(paymentOnAccountOneReviewAndReconcileCredit)

  def isReviewAndReconcilePoaTwoCredit(mainTransaction: Option[String]): Boolean =
    mainTransaction.contains(paymentOnAccountTwoReviewAndReconcileCredit)

  def isReviewAndReconcileCredit(mainTransaction: Option[String]): Boolean =
    isReviewAndReconcilePoaOneCredit(mainTransaction) ||
      isReviewAndReconcilePoaTwoCredit(mainTransaction)

}