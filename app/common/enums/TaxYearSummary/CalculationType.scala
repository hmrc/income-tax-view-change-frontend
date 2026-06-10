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

package common.enums.TaxYearSummary

sealed trait CalculationType {
  val value: String
}

case object DeclareCrystallisation extends CalculationType {
  override val value: String = "CR"
}

case object Amendment extends CalculationType {
  override val value: String = "AM"
}

case object DeclareFinalisation extends CalculationType {
  override val value: String = "DF"
}

case object ConfirmAmendment extends CalculationType {
  override val value: String = "CA"
}

case object Correction extends CalculationType {
  override val value: String = "CO"
}

case object IntentToAmend extends CalculationType {
  override val value: String = "IA"
}

case object InYear extends CalculationType {
  override val value: String = "IY"
}

case object IntentToFinalise extends CalculationType {
  override val value: String = "IF"
}

case object Crystallisation extends CalculationType {
  override val value: String = "crystallisation"
}

case object UnknownCalculationType extends CalculationType {
  override val value: String = "UnknownCalculationType"
}


object CalculationType {

  def fromCalculationTypeValueToString(string: String): CalculationType = {
    string match {
      case "CR" => DeclareCrystallisation
      case "AM" => Amendment
      case "DF" => DeclareFinalisation
      case "CA" => ConfirmAmendment
      case "IA" => IntentToAmend
      case "IY" => InYear
      case "IF" => IntentToFinalise
      case "crystallisation" => Crystallisation
      case _ => UnknownCalculationType
    }
  }

  val notCrystallisedTypes: Set[CalculationType] =
    Set(
      InYear,
      IntentToFinalise
    )

  val crystallisedTypes: Set[CalculationType] =
    Set(
      Crystallisation,
      ConfirmAmendment,
      Correction,
      DeclareFinalisation,
      DeclareCrystallisation,
      IntentToAmend,
    )

  val amendmentTypes: Set[CalculationType] =
    Set(
      Amendment,
      ConfirmAmendment
    )
}
