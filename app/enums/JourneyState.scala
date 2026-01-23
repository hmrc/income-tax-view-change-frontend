/*
 * Copyright 2023 HM Revenue & Customs
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

package enums

enum JourneyState(val key:String):
  //for the start page of every Journey (any page the user can hit without an open mongo session)
  case InitialPage extends JourneyState("Initial")
  //any You Cannot Go Back page, to prevent infinite redirects
  case CannotGoBackPage extends JourneyState("CannotGoBack")
  //for any page before the first (or only) submission of data to an API, including the page which submits the data (on submit)
  case BeforeSubmissionPage extends JourneyState("BeforeSubmission")
  //for any page after the first (or only) submission of data to an API
  case AfterSubmissionPage extends JourneyState("AfterSubmission")
  //for any page after the first (or only) submission of data to an API
  case ReportingFrequencyPages extends JourneyState("ReportingFrequencyPages")
  //any You Cannot Go Back page, to prevent infinite redirects
  case JourneyCompleted extends JourneyState("JourneyCompleted")