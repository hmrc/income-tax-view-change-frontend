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

package config.featureswitch

import auth.MtdItUser
import models.admin.{FeatureSwitch, FeatureSwitchName}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.retrieve.Name

import scala.annotation.nowarn

@nowarn
class FeatureSwitchingSpec extends TestSupport with FeatureSwitching {

  val expectedDisabledFeatures: Set[FeatureSwitchName] = FeatureSwitchName.allFeatureSwitches
  val mtdItUser = MtdItUser(
    mtditid = "mtditid",
    nino = "nino",
    userName = Some(Name(Some("firstName"), Some("lastName"))),
    incomeSources = IncomeSourceDetailsModel("nino", "mtditid", None, Nil, Nil),
    btaNavPartial = None,
    saUtr = Some("saUtr"),
    credId = Some("credId"),
    userType = None,
    arn = None
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    FeatureSwitchName.allFeatureSwitches.map(_.name).foreach(sys.props.remove)
  }

  "Features" should {

    "fold depending on its state, calling the respective branch only" when {
      trait FoldSetup {
        val aValue = 123987

        def expectedBranch(): Int = {
          aValue
        }

        def unexpectedBranch(): Int = throw new IllegalStateException
      }

      "a feature is disabled" in new FoldSetup {
        FeatureSwitchName.allFeatureSwitches.forall { featureSwitchName =>
          disable(featureSwitchName)
          val result = expectedDisabledFeatures.headOption match {
            case Some(fs) =>
              fs.fold(ifEnabled = unexpectedBranch(), ifDisabled = expectedBranch())
            case _ => -1
          }
          result == aValue
        } shouldBe true
      }

      "a feature is disabled v2" in new FoldSetup {
        FeatureSwitchName.allFeatureSwitches.forall { featureSwitchName =>
          val r = isEnabled(featureSwitchName)(mtdItUser) || isDisabled(featureSwitchName,mtdItUser.featureSwitches)
          println(s"Name: $featureSwitchName - $r")
          r
        } shouldBe true

      }

    }
  }

}
