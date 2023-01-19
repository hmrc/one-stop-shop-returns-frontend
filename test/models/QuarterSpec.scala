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

import models.Quarter._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.TryValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.util.Failure

class QuarterSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with TryValues {

  ".fromString" - {

    "must resolve for valid quarters" in {

      Quarter.fromString("Q1").success.value mustEqual Q1
      Quarter.fromString("Q2").success.value mustEqual Q2
      Quarter.fromString("Q3").success.value mustEqual Q3
      Quarter.fromString("Q4").success.value mustEqual Q4
    }

    "must not resolve for invalid quarters" in {

      forAll(arbitrary[String]) {
        string =>

          whenever(!Quarter.values.map(_.toString).contains(string)) {

            Quarter.fromString(string) mustBe a[Failure[_]]
          }
      }
    }
  }
}
