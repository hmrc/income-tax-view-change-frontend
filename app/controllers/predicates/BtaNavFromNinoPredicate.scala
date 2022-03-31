/*
 * Copyright 2022 HM Revenue & Customs
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

/*
 * Copyright 2022 HM Revenue & Customs
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

/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.predicates

import auth.MtdItUserWithNino
import config.featureswitch.{NavBar, FeatureSwitching}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.bta.BtaNavBarController
import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import models.navBar.{TaxAccountType,PersonalTaxAccount}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BtaNavFromNinoPredicate @Inject()(btaNavBarController: BtaNavBarController,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                       taxAccountType: TaxAccountType)
                                       (implicit val appConfig: FrontendAppConfig,
                                        val executionContext: ExecutionContext) extends ActionRefiner[MtdItUserWithNino, MtdItUserWithNino] with FeatureSwitching {

  override def refine[A](request: MtdItUserWithNino[A]): Future[Either[Result, MtdItUserWithNino[A]]] = {
    val header: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    implicit val hc: HeaderCarrier = header.copy(extraHeaders = header.headers(Seq(HeaderNames.COOKIE)))

    if (isDisabled(NavBar)) {
      Future.successful(Right(request))
    } else {
      btaNavBarController.btaNavBarPartial(request) map {
        case Some(partial) =>
          Right(MtdItUserWithNino[A](mtditid = request.mtditid, nino = request.nino, userName = request.userName, btaNavPartial = Some(partial),
            saUtr = request.saUtr, credId = request.credId, userType = request.userType, arn = request.arn)(request))
        case _ => Left(itvcErrorHandler.showInternalServerError()(request))
      }
    }
  }

}
