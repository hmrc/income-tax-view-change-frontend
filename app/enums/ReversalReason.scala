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

enum ReversalReason(val value: String):
  // Not part of String -> ReversalReason conversion:: do we need this item?
  case CreateReversalReason extends ReversalReason("create")
  case AmendedReturnReversalReason extends ReversalReason("amend")
  case AdjustmentReversalReason extends ReversalReason("adjustment")
  case CustomerRequestReason extends ReversalReason("request")

  override def toString: String = value