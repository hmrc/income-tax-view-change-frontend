/*
 * Copyright 2020 HM Revenue & Customs
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

package models

import models.core.breadcrumb.BreadcrumbItem
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec

class BreadcrumbModelSpec extends UnitSpec with Matchers {

  "The BreadcrumbItem Model toPage method" should {
    "return 'ToHomePage'" in {
      BreadcrumbItem("breadcrumb-it", None).toPage shouldBe "ToHomePage"
    }
    "return 'ToAccountDetailsPage'" in {
      BreadcrumbItem("breadcrumb-account", None).toPage shouldBe "ToAccountDetailsPage"
    }
    "return 'ToBillsPage'" in {
      BreadcrumbItem("breadcrumb-bills", None).toPage shouldBe "ToBillsPage"
    }
    "return 'ToEstimatesPage'" in {
      BreadcrumbItem("breadcrumb-estimates", None).toPage shouldBe "ToEstimatesPage"
    }
  }

}
