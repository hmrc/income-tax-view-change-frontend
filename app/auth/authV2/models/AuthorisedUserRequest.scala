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

package auth.authV2.models

import enums.MTDUserRole
import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.auth.core.retrieve.Name

case class AuthorisedUserRequest[A](authUserDetails: AuthUserDetails)
                                   (implicit request: Request[A]) extends WrappedRequest[A](request)


case class AuthorisedAgentWithClientDetailsRequest[A](authUserDetails: AuthUserDetails,
                                                      clientDetails: AgentClientDetails)
                                                     (implicit request: Request[A]) extends WrappedRequest[A](request)


case class AuthorisedAndEnrolledRequest[A](mtditId: String,
                                           mtdUserRole: MTDUserRole,
                                           authUserDetails: AuthUserDetails,
                                           clientDetails: Option[AgentClientDetails])
                                           (implicit request: Request[A]) extends WrappedRequest[A](request) {
  val saUtr: Option[String] = if(clientDetails.isDefined) clientDetails.map(_.utr) else authUserDetails.saUtr
  def optClientNameAsString: Option[String] = {
    val optClientName = clientDetails.fold[Option[Name]](None)(_.clientName)
    val firstName = optClientName.fold[Option[String]](None)(_.name)
    val lastName  = optClientName.fold[Option[String]](None)(_.lastName)
    (firstName, lastName) match {
      case (Some(fn), Some(ln)) => Some(s"$fn $ln")
      case _ => None
    }
  }

}
