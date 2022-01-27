/*
 * Copyright 2022 HM Revenue & Customs
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

package testOnly.controllers

import controllers.BaseController
import forms.utils.SessionKeys
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import testOnly.forms.TestHeadersForm
import testOnly.models.TestHeadersModel
import testOnly.views.html.TestHeaders

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TestHeadersController @Inject()(testHeaders: TestHeaders)
                                     (implicit mcc: MessagesControllerComponents,
                                     val executionContext: ExecutionContext) extends BaseController {

  def showTestHeaders: Action[AnyContent] = Action { implicit request =>
    Ok(testHeaders(TestHeadersForm.form.fill(TestHeadersModel(
      request.session.get("Gov-Test-Scenario").getOrElse("DEFAULT"))), TestHeadersModel.validTestHeaders))
  }

  def submitTestHeaders: Action[AnyContent] = Action { implicit request =>
    TestHeadersForm.form.bindFromRequest().fold(
      _ => Redirect(routes.TestHeadersController.showTestHeaders()),
      success => {
        Redirect(routes.TestHeadersController.showTestHeaders())
            .withSession(request.session.data.updated("Gov-Test-Scenario", success.headerName).toSeq: _*)
      }
    )
  }
}
