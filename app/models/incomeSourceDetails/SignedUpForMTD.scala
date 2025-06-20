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

package models.incomeSourceDetails

sealed trait SignedUpForMTD

case object SignUpNextYearOnly extends SignedUpForMTD

case object NotSigningUp extends SignedUpForMTD

case object SignUpCurrentYearOnly extends SignedUpForMTD

case object SignUpBothYears extends SignedUpForMTD

case object OnlyOneBusinessInLatency extends SignedUpForMTD

case object OptedOut extends SignedUpForMTD

case object Unknown extends SignedUpForMTD