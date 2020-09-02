/*
 * Copyright 2017 HM Revenue & Customs
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

package assets.messages
import java.time.LocalDate

import helpers.ComponentSpecBase

object ReportDeadlinesMessages extends ComponentSpecBase{

  //Report deadlines view messages
  val title = "Report deadlines - Business Tax account - GOV.UK"
  val overdue: LocalDate => String = date => s"${date.toLongDateShort} Overdue"
  val wholeTaxYear = "Whole tax year (final check)"
  val propertyHeading = "Property income"
  val errorp1 = "We can’t display your next report due date at the moment."
  val errorp2 = "Try refreshing the page in a few minutes."

  //Obligations view messages
  val obligationsTitle = "Updates - Business Tax account - GOV.UK"

  //Previous Obligations
  val noPreviousObligations = "No previously submitted updates"

  //No Reports
  val noReports = "You don’t have any reports due right now. Your next deadline will show here on the first Monday of next month."

}
