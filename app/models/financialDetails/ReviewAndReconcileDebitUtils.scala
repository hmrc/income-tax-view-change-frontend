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

object ReviewAndReconcileDebitUtils {

  lazy val poaOneMainTransaction = "4911"
  lazy val poaTwoMainTransaction = "4913"

  def isReviewAndReconcilePoaOne(mainTransaction: Option[String]): Boolean =
    mainTransaction.contains(poaOneMainTransaction)

  def isReviewAndReconcilePoaTwo(mainTransaction: Option[String]): Boolean =
    mainTransaction.contains(poaTwoMainTransaction)

  def filterReviewAndReconcileDebits(reviewAndReconcileDebitsIsEnabled: Boolean,
                                     documentDetailsWithDueDate: DocumentDetailWithDueDate,
                                     financialDetails: FinancialDetailsModel): Boolean =
    !reviewAndReconcileDebitsIsEnabled &&
      financialDetails.isReviewAndReconcileDebit(documentDetailsWithDueDate.documentDetail.transactionId)
}
