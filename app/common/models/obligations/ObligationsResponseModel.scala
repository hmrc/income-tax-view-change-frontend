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

package common.models.obligations

import play.api.libs.json.*

sealed trait ObligationsResponseModel

case class ObligationsModel(obligations: Seq[GroupedObligationsModel]) extends ObligationsResponseModel

object ObligationsModel {
  implicit val format: OFormat[ObligationsModel] = Json.format[ObligationsModel]
}

case class ObligationsErrorModel(code: Int, message: String) extends ObligationsResponseModel

object ObligationsErrorModel {
  implicit val format: Format[ObligationsErrorModel] = Json.format[ObligationsErrorModel]
}
