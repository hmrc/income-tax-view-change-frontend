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

object BillsMessages {

  val billsTitle = "Previous statements"
  val billsHeading = "Income Tax bills"
  val finalBills = "View finalised bills."
  val taxYearText: Int => String = testYear => s"Tax year: ${testYear-1} to $testYear"
  val earlierBills = "For earlier bills, view your Self Assessment calculations."

}
