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

package enums

import play.api.libs.json._

enum MTDUserRole:
  case MTDPrimaryAgent, MTDSupportingAgent, MTDIndividual
  
object MTDUserRole:
  given writes: Writes[MTDUserRole] = Writes {
    case MTDPrimaryAgent => JsString("MTDPrimaryAgent")
    case MTDSupportingAgent => JsString("MTDSupportingAgent")
    case MTDIndividual => JsString("MTDIndividual")
  }

  given reads: Reads[MTDUserRole] = Reads {
    case JsString("MTDPrimaryAgent") => JsSuccess(MTDPrimaryAgent)
    case JsString("MTDSupportingAgent") => JsSuccess(MTDSupportingAgent)
    case JsString("MTDIndividual") => JsSuccess(MTDIndividual)
    case other => JsError(s"Unknown MTDUserRole: $other")
  }

  given format: Format[MTDUserRole] = Format(reads, writes)
