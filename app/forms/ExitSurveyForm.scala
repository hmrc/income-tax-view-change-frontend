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

package forms

import forms.preprocess.XssFilter
import forms.validation.{Constraints, ErrorMessageFactory}
import models.ExitSurveyModel
import play.api.data.Form
import play.api.data.Forms._

object ExitSurveyForm extends Constraints {

  val satisfaction = "satisfaction"
  val improvements = "improvements"
  val improvementsMaxLength = 1200

  val exitSurveyForm: Form[ExitSurveyModel] = Form(
    mapping(
      satisfaction -> optional(text),
      improvements -> optional(text)
        .verifying(optMaxLength(improvementsMaxLength, ErrorMessageFactory.error("exit_survey.error.maxLengthImprovements")))
        .transform[Option[String]](x => x.map(XssFilter.filter), x => x)
    )(ExitSurveyModel.apply)(ExitSurveyModel.unapply)
  )

}
