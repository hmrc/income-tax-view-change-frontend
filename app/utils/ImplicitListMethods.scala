/*
 * Copyright 2018 HM Revenue & Customs
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

trait ImplicitListMethods {
  implicit class ImplicitListMethods[A](x: List[A]) {
    def handleEmptyList(f: List[A] => A): List[A] = x match {
      case Nil => List()
      case _ => List(f(x))
    }
    def maxItemBy[B](f: A => B)(implicit cmp: Ordering[B]): List[A] = x.handleEmptyList(_.maxBy(f))
    def minItemBy[B](f: A => B)(implicit cmp: Ordering[B]): List[A] = x.handleEmptyList(_.minBy(f))
  }
}

object ImplicitListMethods extends ImplicitListMethods
