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

package auth

import models.core.UserDetailsModel
import models.incomeSourcesWithDeadlines.IncomeSourcesWithDeadlinesModel
import play.api.mvc.{Request, WrappedRequest}

case class MtdItUserOptionNino[A](mtditid: String,
                                  nino: Option[String],
                                  userDetails: Option[UserDetailsModel])(implicit request: Request[A]) extends WrappedRequest[A](request)

case class MtdItUserWithNino[A](mtditid: String,
                                nino: String,
                                userDetails: Option[UserDetailsModel])(implicit request: Request[A]) extends WrappedRequest[A](request)

case class MtdItUser[A](mtditid: String,
                        nino: String,
                        userDetails: Option[UserDetailsModel],
                        incomeSources: IncomeSourcesWithDeadlinesModel
                       )(implicit request: Request[A]) extends WrappedRequest[A](request)

