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

package models

import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.{Gen, Properties}

object BusinessNameFormV2Spec  extends Properties("BusinessNameForm.validation") {
  import models.incomeSourceDetails.BusinessNameForm.validation

  // Set of permitted characters / required to generate strings representing BusinessName
  val businessNamePermittedCharacters = (('a' to 'z')  ++ ('A' to 'Z') ++ ('0' to '9') ) ++ Seq(' ', ',','.', '&', '\'')

  val businessNameGenerator : Gen[List[Char]] = Gen.listOf( Gen.oneOf(businessNamePermittedCharacters) )

  property("validation") = forAll(businessNameGenerator){ (charsList: List[Char])  =>
    (charsList.length > 0  && charsList.length <= 105) ==> {
      val businessName = charsList.mkString("")
      println(s"Generate business name: ${businessName}")
      validation(businessName).isDefined
    }
  }

}
