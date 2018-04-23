/*
 * Copyright 2018 HM Revenue & Customs
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
import utils.ImplicitDateFormatter._

object AccountDetailsMessages {

  val accountHeading = "Account details"
  val accountTitle = "Account details"
  val businessHeading = "Your businesses"
  val propertyHeading = "Your properties"
  val reportingPeriod: (LocalDate, LocalDate) => String = (start,end) => s"Reporting period: ${start.toLongDateNoYear} - ${end.toLongDateNoYear}"

}
