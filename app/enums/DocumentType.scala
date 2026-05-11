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

enum DocumentType(val key: String):
  case Poa1Charge extends DocumentType("ITSA- POA 1")
  case Poa2Charge extends DocumentType("ITSA- POA 2")
  case Poa1ReconciliationDebit extends DocumentType("POA 1 Reconciliation Debit")
  case Poa2ReconciliationDebit extends DocumentType("POA 2 Reconciliation Debit")
  case BalancingCharge extends DocumentType("ITSA BCD")
  case TRMNewCharge extends DocumentType("TRM New Charge")
  case TRMAmendCharge extends DocumentType("TRM Amend Charge")
  //EXTEND WITH OTHER CHARGES
  case OtherCharge extends DocumentType("Other")