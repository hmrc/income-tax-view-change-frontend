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

package mocks.repositories

import models.admin.FeatureSwitch
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import repositories.admin.FeatureSwitchRepository
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockFeatureSwitchRepository extends UnitSpec with BeforeAndAfterEach{

  val mockFeatureSwitchRepository: FeatureSwitchRepository = mock(classOf[FeatureSwitchRepository])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFeatureSwitchRepository)
  }

  def mockRepositoryGetFeatureSwitch(response: Option[FeatureSwitch]): Unit = {
    when(mockFeatureSwitchRepository.getFeatureSwitch(any()))
      .thenReturn(Future.successful(response))
  }

  def mockRepositoryGetFeatureSwitches(response: List[FeatureSwitch]): Unit = {
    when(mockFeatureSwitchRepository.getFeatureSwitches)
      .thenReturn(Future.successful(response))
  }

  def mockRepositorySetFeatureSwitch(response: Boolean): Unit = {
    when(mockFeatureSwitchRepository.setFeatureSwitch(any(), any()))
      .thenReturn(Future.successful(response))
  }

  def mockRepositorySetFeatureSwitches(): Unit = {
    when(mockFeatureSwitchRepository.setFeatureSwitches(any()))
      .thenReturn(Future.successful(()))
  }



}
