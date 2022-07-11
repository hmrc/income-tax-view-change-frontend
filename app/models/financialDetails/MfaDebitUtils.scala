/*
 * Copyright 2022 HM Revenue & Customs
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

object MfaDebitUtils {
  private val mfaDebitDescription: Map[Int, String] = Map(
    4000 -> "HMRC Adjustment",
    4001 -> "HMRC Adjustment",
    4002 -> "HMRC Adjustment",
    4003 -> "HMRC Adjustment"
  )

  def validMFADebitDescription(documentDescription: Option[String]): Boolean = {
    documentDescription
      .map(mfaDebitDescription.values.toList.contains(_))
      .getOrElse(false)
  }
}
