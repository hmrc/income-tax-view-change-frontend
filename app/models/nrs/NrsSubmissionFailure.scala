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

package models.nrs

import play.api.http.Status

sealed trait NrsSubmissionFailure {
  def retryable: Boolean
}

object NrsSubmissionFailure {
  case class ErrorResponse(status: Int) extends NrsSubmissionFailure {
    override def retryable: Boolean = Status.isServerError(status)
  }
  case object ExceptionThrown extends NrsSubmissionFailure {
    override def retryable: Boolean = false
  }
}
