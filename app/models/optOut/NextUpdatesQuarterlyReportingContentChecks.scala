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

package models.optOut

case class NextUpdatesQuarterlyReportingContentChecks(currentYearItsaStatus: Boolean, previousYearItsaStatus: Boolean, previousYearCrystallisedStatus: Option[Boolean]) {

  private def showOptOutContent: Option[Unit] = if (currentYearItsaStatus || (previousYearItsaStatus && !previousYearCrystallisedStatus.getOrElse(false))) Some({}) else None

  def showUpdateTypeDetailsSection: Option[Unit] = showOptOutContent

  def showUseCompatibleSoftwareSection: Option[Unit] = showOptOutContent



//  OneYearOptOut = ExactlyOneOf(OptOutCY-1, OptOutCY, OptOutCY+1)
//  OptOutCY-1: ITSA(CY-1) == V and CY-1-Not-Crystallised
//  OptOutCY : ITSA(CY) == V
//  OptOutCY+1: ITSA(CY+1) == V or (ITSA(CY) == V AND ITSA(CY+1) = Unknown)
}
