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

package enums

sealed trait DocumentType {
  val key: String
}

case object Poa1Charge extends DocumentType {
  override val key: String = "ITSA- POA 1"
}

case object Poa2Charge extends DocumentType {
  override val key: String = "ITSA- POA 2"
}

case object Poa1ReconciliationDebit extends DocumentType {
  override val key: String = "POA 1 Reconciliation Debit"
}

case object Poa2ReconciliationDebit extends DocumentType {
  override val key: String = "POA 2 Reconciliation Debit"
}

case object BalancingCharge extends DocumentType {
  override val key: String = "ITSA BCD"
}

case object TRMNewCharge extends DocumentType {
  override val key: String = "TRM New Charge"
}

case object TRMAmendCharge extends DocumentType {
  override val key: String = "TRM Amend Charge"
}

//EXTEND WITH OTHER CHARGES

case object OtherCharge extends DocumentType {
  override val key: String = "Other"
}
