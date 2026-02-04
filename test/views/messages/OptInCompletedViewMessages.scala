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

package views.messages

object OptInCompletedViewMessages {

  val titleContent = "Opt in completed - Manage your Self Assessment - GOV.UK"

  val panelTitleContent = "Opt in completed"

  val panelBodyContent = "You opted in to quarterly reporting from 2022 to 2023 tax year onwards"

  def overdueUpdatesGenericInset(yearStart: String, yearEnd: String) = s"You may have overdue updates for the $yearStart to $yearEnd tax year. You must submit these updates with all required income and expenses through your compatible software."

  val yourRevisedDeadlineH2 = "Your revised deadlines"

  val yourRevisedDeadlineInset = "Your revised deadlines will be available in the next few minutes."

  val yourRevisedDeadlineContentP1 = "Even if they are not displayed right away on the submission deadlines page, your account has been updated."

  val yourRevisedDeadlineContentP2 = "You can decide at any time to opt out of reporting quarterly for all your businesses on your reporting frequency page."

  val optinCompletedViewP3 = "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."

  val optinCompletedViewP4 = "When reporting annually, you can submit your tax return directly through your HMRC online account or compatible software."

  val optinCompletedViewP5 = "You are voluntarily opted in to reporting quarterly from the next tax year onwards, but in the future it could be mandatory for you if:"

  val bullet1Content =  "HMRC lowered the income threshold for Making Tax Digital for Income Tax"
  val bullet2Content = "you reported an increase in your qualifying income in a tax return"

  val optinCompletedViewP6 = "You can check the threshold for qualifying income in the criteria for people who will " +
    "need to sign up for Making Tax Digital for Income Tax (opens in new tab)."

}
