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

  lazy val poaOneReviewAndReconcileDebit = "4911"
  lazy val poaTwoReviewAndReconcileDebit = "4913"

  private val poaOneReviewAndReconcileCredit = "4912"
  private val poaTwoReviewAndReconcileCredit = "4914"

  def isReconcilePoaOneDebit(mainTransaction: Option[String]): Boolean =
    mainTransaction.contains(poaOneReviewAndReconcileDebit)

  def isReconcilePoaTwoDebit(mainTransaction: Option[String]): Boolean =
    mainTransaction.contains(poaTwoReviewAndReconcileDebit)

  def isReconcilePoaOneCredit(mainTransaction: Option[String]): Boolean =
    mainTransaction.contains(poaOneReviewAndReconcileCredit)

  def isReconcilePoaTwoCredit(mainTransaction: Option[String]): Boolean =
    mainTransaction.contains(poaTwoReviewAndReconcileCredit)

  val isReviewAndReconcileCredit: FinancialDetail => Boolean = financialDetail =>
    isReconcilePoaOneCredit(financialDetail.mainTransaction) ||
      isReconcilePoaTwoCredit(financialDetail.mainTransaction)

  def getCreditKey(mainTransaction: Option[String]): String =
    mainTransaction match {
      case Some(`poaOneReviewAndReconcileCredit`) => "POA1RR-credit"
      case Some(`poaTwoReviewAndReconcileCredit`) => "POA2RR-credit"
      case e                                      => throw new Exception(s"Could not create message key from invalid mainTransaction: $e")
    }
}
