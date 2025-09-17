/*
 * Copyright 2025 HM Revenue & Customs
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

package utils

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import models.admin.{OptInOptOutContentUpdateR17, OptOutFs, ReportingFrequencyPage}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import scala.concurrent.Future

trait ReportingObligationsUtils extends FeatureSwitching {

  def withReportingObligationsFS(comeBlock: => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    if (!isEnabled(ReportingFrequencyPage)) {
      user.userType match {
        case Some(Agent) => Future.successful(Redirect(controllers.routes.HomeController.showAgent()))
        case _ => Future.successful(Redirect(controllers.routes.HomeController.show()))
      }
    } else {
      comeBlock
    }
  }

  def withOptOutFS(comeBlock: => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    if (!isEnabled(OptOutFs)) {
      user.userType match {
        case Some(Agent) => Future.successful(Redirect(controllers.routes.HomeController.showAgent()))
        case _ => Future.successful(Redirect(controllers.routes.HomeController.show()))
      }
    } else {
      comeBlock
    }
  }

  def withRFAndOptInOptOutR17FS(codeBlock: => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    (isEnabled(ReportingFrequencyPage), isEnabled(OptInOptOutContentUpdateR17)) match {
      case (true, true) => codeBlock
      case (true, false) =>
        user.userType match {
          case Some(Agent) => Future.successful(Redirect(controllers.routes.ReportingFrequencyPageController.show(true)))
          case _ => Future.successful(Redirect(controllers.routes.ReportingFrequencyPageController.show(false)))
        }
      case (false, _) =>
        user.userType match {
          case Some(Agent) => Future.successful(Redirect(controllers.routes.HomeController.showAgent()))
          case _ => Future.successful(Redirect(controllers.routes.HomeController.show()))
        }
    }
  }

  def withOptOutRFChecks(codeBlock: => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    (isEnabled(OptOutFs), isEnabled(ReportingFrequencyPage), isEnabled(OptInOptOutContentUpdateR17)) match {
      case (true, true, true) => codeBlock
      case (true, true, false) =>
        user.userType match {
          case Some(Agent) => Future.successful(Redirect(controllers.routes.ReportingFrequencyPageController.show(true)))
          case _ => Future.successful(Redirect(controllers.routes.ReportingFrequencyPageController.show(false)))
        }
      case _ =>
        user.userType match {
          case Some(Agent) => Future.successful(Redirect(controllers.routes.HomeController.showAgent()))
          case _ => Future.successful(Redirect(controllers.routes.HomeController.show()))
        }
    }
  }
}
