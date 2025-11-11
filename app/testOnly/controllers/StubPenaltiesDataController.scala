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

package testOnly.controllers

import config.FrontendAppConfig
import controllers.BaseController
import play.api.i18n.I18nSupport
import play.api.libs.json.JsValue
import play.api.mvc._
import testOnly.connectors.PenaltiesStubConnector
import testOnly.models.PenaltiesDataModel

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class StubPenaltiesDataController @Inject()(implicit val appConfig: FrontendAppConfig,
                                   implicit val mcc: MessagesControllerComponents,
                                   implicit val executionContext: ExecutionContext,
                                   val penaltiesStubConnector: PenaltiesStubConnector
                                  ) extends BaseController with I18nSupport {

  val submitStubbedPenaltyDetails: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      withJsonBody[PenaltiesDataModel](
        json => {
          penaltiesStubConnector.deletePenaltiesData(json.nino).flatMap(_ =>
            penaltiesStubConnector.addPenaltiesData(json).map(response =>
            if(response.status == OK) {
              Ok("The penalty details data was added to the penalties stub")
            } else {
              InternalServerError(response.body)
            }
            )
          )
        }
      )
  }

  val submitStubbedFinancialData: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      withJsonBody[PenaltiesDataModel](
        json => {
          penaltiesStubConnector.deletePenaltiesFinancialData(json.nino).flatMap(_ =>
            penaltiesStubConnector.addPenaltiesFinancialData(json).map(response =>
              if(response.status == OK) {
                Ok("The financial data json was added to the penalties stub")
              } else {
                InternalServerError(response.body)
              }
            )
          )
        }
      )
  }
}
