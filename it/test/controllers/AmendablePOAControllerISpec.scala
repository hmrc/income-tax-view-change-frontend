/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import config.featureswitch.AdjustPaymentsOnAccount
import helpers.ComponentSpecBase
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}

class AmendablePOAControllerISpec extends ComponentSpecBase {

  private val amendPoaUrl = controllers.routes.AmendablePOAController.show(isAgent = false).url

  s"calling GET $amendPoaUrl" should {
    "render the Adjusting your payments on account page" when {
      "User is authorised" in {
        enable(AdjustPaymentsOnAccount)
      }
    }
    s"return status $SEE_OTHER and redirect to the home page" when {
      "AdjustPaymentsOnAccount FS is disabled" in {
        disable(AdjustPaymentsOnAccount)
      }
    }
    s"return $INTERNAL_SERVER_ERROR" when {
      "no non-crystallised financial details are found" in {
        enable(AdjustPaymentsOnAccount)
      }
    }
    s"return $INTERNAL_SERVER_ERROR" when {
      "an error model is returned when fetching financial details data" in {
        enable(AdjustPaymentsOnAccount)
      }
    }
  }
}
