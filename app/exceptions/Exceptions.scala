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

package exceptions

case class MissingFieldException(fieldName: String) extends RuntimeException(s"Missing Mandatory Expected Field: $fieldName")

case class MissingSessionKey(key:String) extends RuntimeException(s"Missing session key: $key")

case class NoIncomeSourceFound(hash: String) extends RuntimeException(s"User has no matching incomeSources. Hash: <$hash>")

case class MultipleIncomeSourcesFound(hash: String, incomeSourceIds: List[String]) extends
  RuntimeException(s"User has multiple matching incomeSources. hash: <$hash>. incomeSourceIds: <$incomeSourceIds>")

case class CouldNotCreateChargeItem(msg: String) extends RuntimeException(s"Could not create charge item: $msg")
