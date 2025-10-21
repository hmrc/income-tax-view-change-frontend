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

package utils.reportingObligations

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import models.admin.{FeatureSwitchName, OptInOptOutContentUpdateR17, OptOutFs, ReportingFrequencyPage, SignUpFs}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import scala.concurrent.Future

trait ReportingObligationsUtils extends FeatureSwitching {

  def withOptOutFS(comeBlock: => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    if (!isEnabled(OptOutFs)) {
      redirectHome(user.userType)
    } else {
      comeBlock
    }
  }

  def withOptOutRFChecks(codeBlock: => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    (isEnabled(OptOutFs), isEnabled(ReportingFrequencyPage), isEnabled(OptInOptOutContentUpdateR17)) match {
      case (true, true, true) => codeBlock
      case (true, true, false) => redirectReportingFrequency(user.userType)
      case _ => redirectHome(user.userType)
    }
  }

  def withSignUpRFChecks(codeBlock: => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    (isEnabled(SignUpFs), isEnabled(ReportingFrequencyPage), isEnabled(OptInOptOutContentUpdateR17)) match {
      case (true, true, true)  => codeBlock
      case (true, true, false) | (false, true, _) => redirectReportingFrequency(user.userType)
      case _ => redirectHome(user.userType)
    }
  }

  def withOptInRFChecks(comeBlock: => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    (isEnabled(SignUpFs), isEnabled(ReportingFrequencyPage)) match {
      case (true, true) => comeBlock
      case (false, true) => redirectReportingFrequency(user.userType)
      case _ => redirectHome(user.userType)
    }
  }

  private def redirectHome(userType: Option[AffinityGroup]): Future[Result] =
    userType match {
      case Some(Agent) => Future.successful(Redirect(controllers.routes.HomeController.showAgent()))
      case _ => Future.successful(Redirect(controllers.routes.HomeController.show()))
    }

  protected def redirectReportingFrequency(userType: Option[AffinityGroup]): Future[Result] =
    userType match {
      case Some(Agent) => Future.successful(Redirect(controllers.routes.ReportingFrequencyPageController.show(true)))
      case _ => Future.successful(Redirect(controllers.routes.ReportingFrequencyPageController.show(false)))
    }

}
