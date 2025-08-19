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

package auth.authV2

import uk.gov.hmrc.auth.core.AuthorisationException

object AuthExceptions {

  case class MissingMtdId(r:String = "Could not retrieve MTD ID from request")
    extends AuthorisationException(r)
  case class MissingAgentReferenceNumber(r:String = "Agent Reference Number was not found in user's enrolments")
    extends AuthorisationException(r)
  case class NoAssignment(r: String = "NO_ASSIGNMENT") extends AuthorisationException(r)
}
