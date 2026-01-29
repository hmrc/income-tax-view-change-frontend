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

enum CalcType(val value: String):
  case DECLARE_CRYSTALLISATION extends CalcType("CR")
  case AMENDMENT extends CalcType("AM")
  case DECLARE_FINALISATION extends CalcType("DF")
  case CONFIRM_AMENDMENT extends CalcType("CA")
  case CRYSTALLISATION extends CalcType("crystallisation")
  
object CalcType:
  val crystallisedTypes: Set[String] = Set(
    DECLARE_CRYSTALLISATION,
    CRYSTALLISATION,
    DECLARE_FINALISATION
  ).map(_.value)

  val amendmentTypes: Set[String] = Set(
    AMENDMENT,
    CONFIRM_AMENDMENT
  ).map(_.value)