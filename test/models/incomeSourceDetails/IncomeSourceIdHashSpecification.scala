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

package models.incomeSourceDetails

import models.core.IncomeSourceId._
import models.core.IncomeSourceIdHash
import models.core.IncomeSourceIdHash._
import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Properties}

object IncomeSourceIdHashSpecification extends Properties("IncomeSourceId") {

  val range : Seq[Char] = ( 'a' to'z').toList ++ ( 'A' to'Z') ++ ('0' to '9').toList

  val incomeSourceIdGen = Gen.listOfN(7000, Gen.pick(15, range) )

  property("make sure hash is unique") = forAll(incomeSourceIdGen) { ids =>
    val hashSet: List[String] = ids.distinct.map { i =>
      val incomeSourceId = mkIncomeSourceId( i.mkString(""))
      mkIncomeSourceIdHash(incomeSourceId).hash
    }
    ids.forall(x => x.mkString("").nonEmpty) &&
      hashSet.forall(x => x.nonEmpty) &&
        hashSet.distinct.length == ids.distinct.length
  }

}
