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

  lazy val poaOneReviewAndReconcileCredit = "4912"
  lazy val poaTwoReviewAndReconcileCredit = "4914"

  def isReviewAndReconcilePoaOne(mainTransaction: Option[String]): Boolean =
    mainTransaction.contains(poaOneReviewAndReconcileDebit)

  def isReviewAndReconcilePoaTwo(mainTransaction: Option[String]): Boolean =
    mainTransaction.contains(poaTwoReviewAndReconcileDebit)

  def getCreditKey(mainTransaction: Option[String]): Either[ReviewAndReconcileMessageKeyError, String] =
    mainTransaction match {
      case Some(`poaOneReviewAndReconcileCredit`) => Right("POA1RR-credit")
      case Some(`poaTwoReviewAndReconcileCredit`) => Right("POA2RR-credit")
      case Some(e)                                => Left(ReviewAndReconcileMessageKeyError(s"Invalid mainTransaction found: $e"))
      case None                                   => Left(ReviewAndReconcileMessageKeyError("mainTransaction field empty"))
    }
}

sealed trait ReviewAndReconcileError
case class ReviewAndReconcileMessageKeyError(message: String) extends ReviewAndReconcileError
