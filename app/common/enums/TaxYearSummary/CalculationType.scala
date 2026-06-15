/*
 * Copyright 2026 HM Revenue & Customs
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

enum CalculationType(val value: String) {
  case DeclareCrystallisation extends CalculationType("CR")
  case Amendment              extends CalculationType("AM")
  case DeclareFinalisation    extends CalculationType("DF")
  case ConfirmAmendment       extends CalculationType("CA")
  case Correction             extends CalculationType("CO")
  case IntentToAmend          extends CalculationType("IA")
  case IntentToCrystallise    extends CalculationType("IC")
  case InYear                 extends CalculationType("IY")
  case IntentToFinalise       extends CalculationType("IF")
  case Crystallisation        extends CalculationType("crystallisation")
  case inYear                 extends CalculationType("inYear")
  case UnknownCalculationType extends CalculationType("UnknownCalculationType")
}

object CalculationType {

  def fromStringToCalculationTypeValue(string: String): CalculationType =
    string match {
      case "CR"              => CalculationType.DeclareCrystallisation
      case "AM"              => CalculationType.Amendment
      case "DF"              => CalculationType.DeclareFinalisation
      case "CA"              => CalculationType.ConfirmAmendment
      case "CO"              => CalculationType.Correction
      case "IA"              => CalculationType.IntentToAmend
      case "IC"              => CalculationType.IntentToCrystallise
      case "IY"              => CalculationType.InYear
      case "IF"              => CalculationType.IntentToFinalise
      case "crystallisation" => CalculationType.Crystallisation
      case "inYear"          => CalculationType.inYear
      case _                 => CalculationType.UnknownCalculationType
    }

  val notCrystallisedTypes: Set[CalculationType] =
    Set(
      CalculationType.InYear,
      CalculationType.IntentToFinalise
    )

  val crystallisedTypes: Set[CalculationType] =
    Set(
      CalculationType.Crystallisation,
      CalculationType.ConfirmAmendment,
      CalculationType.Correction,
      CalculationType.DeclareFinalisation,
      CalculationType.DeclareCrystallisation,
      CalculationType.IntentToAmend,
      CalculationType.Amendment,
    )

  val amendmentTypes: Set[CalculationType] =
    Set(
      CalculationType.Amendment,
      CalculationType.ConfirmAmendment
    )
}