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

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import connectors.RawResponseReads
import controllers.agent.predicates.ClientConfirmedController
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import testOnly.models.Identifier
import testOnly.services.EnrolmentService
import testOnly.utils.UserRepository
import testOnly.views.html.ListUTRs
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, groupIdentifier}
import uk.gov.hmrc.auth.core.retrieve.~
import utils.AuthenticatorPredicate

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UTRListController @Inject()(
                                   listUTRsView: ListUTRs,
                                   val authorisedFunctions: AuthorisedFunctions,
                                   enrolmentService: EnrolmentService,
                                   userRepository: UserRepository,
                                   val auth: AuthenticatorPredicate
                                 )(implicit
                                   val appConfig: FrontendAppConfig,
                                   mcc: MessagesControllerComponents,
                                   implicit val ec: ExecutionContext,
                                   val itvcErrorHandler: AgentItvcErrorHandler
                                 ) extends ClientConfirmedController with I18nSupport with FeatureSwitching with RawResponseReads {

  def listUTRs(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    authorisedFunctions.authorised().retrieve(allEnrolments and affinityGroup and groupIdentifier) {
      case _ ~ _ ~ Some(groupId) =>
        Logger("application").info(s"${Console.YELLOW} agent groupid found: $groupId" + Console.WHITE)
        enrolmentService.fetchEnrolments(groupId).flatMap {
          case Right(enrolmentsResponse) =>
            val agentMTDITIDs = enrolmentsResponse.enrolments.flatMap(_.identifiers.collect {
              case Identifier("MTDITID", value) => value
            })

            userRepository.findAll().map { userRecords =>
              val matchedRecords = enrolmentService.filterUserRecords(userRecords.toList, agentMTDITIDs)
              Ok(listUTRsView(utrs = matchedRecords))
            }

          case Left(errorMessage) =>
            Logger("application").error(errorMessage)
            Future.successful(InternalServerError(errorMessage))
        }

      case _ =>
        Logger("application").error("Group ID not found")
        Future.successful(BadRequest("Group ID not found"))
    }
  }
}
