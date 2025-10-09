/*
 * Copyright 2025 HM Revenue & Customs
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

package enums.TaxYearSummary

sealed trait CalcType {
  val value: String
}

object CalcType {
  private case object DECLARE_CRYSTALLISATION extends CalcType {
    override val value: String = "CR"
  }

  private case object AMENDMENT extends CalcType {
    override val value: String = "AM"
  }

  private case object DECLARE_FINALISATION extends CalcType {
    override val value: String = "DF"
  }

  private case object CONFIRM_AMENDMENT extends CalcType {
    override val value: String = "CA"
  }

  private case object CRYSTALLISATION extends CalcType {
    override val value: String = "crystallisation"
  }

  val crystallisedTypes: Set[String] = Set(
    DECLARE_CRYSTALLISATION,
    CRYSTALLISATION,
    DECLARE_FINALISATION
  ).map(_.value)

  val amendmentTypes: Set[String] = Set(
    AMENDMENT,
    CONFIRM_AMENDMENT
  ).map(_.value)
}
