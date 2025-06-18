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

package viewUtils

import config.FrontendAppConfig
import enums.{ChosenTaxYear, CurrentTaxYear, NextTaxYear}
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, Voluntary}
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}

import javax.inject.Inject

class YourSelfAssessmentChargesViewUtils @Inject()(
                                          link: views.html.components.link,
                                          h2: views.html.components.h2,
                                          p: views.html.components.p,
                                          appConfig: FrontendAppConfig
                                        ) {

   def getMessage(key: String, args: String*)(implicit messages: Messages): String =
    messages(s"selfAssessmentCharges.$key", args: _*)

  def getPrefix(key: String)(implicit messages: Messages): String =
    s"whatYouOwe.$key"
}
