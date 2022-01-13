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

package services

import base.SpecBase
import cats.data.Validated.Valid
import models.requests.SaveForLaterRequest
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class SaveForLaterServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit private lazy val ec: ExecutionContext = ExecutionContext.global

  val mockSaveForLaterService = new SaveForLaterService

  ".fromUserAnswers" - {

    "must return a Save For Later Request for userAnswers" in {

        val answers =
            emptyUserAnswers
              .set(SoldGoodsFromNiPage, false).success.value
              .set(SoldGoodsFromEuPage, false).success.value

        val expectedResult = SaveForLaterRequest(vrn, period, answers.data)

        mockSaveForLaterService.fromUserAnswers(answers, vrn, period) mustEqual Valid(expectedResult)
      }
  }

//  ".get" - {
//
//    "must return a " in {
//
//      val answers =
//        emptyUserAnswers
//          .set(SoldGoodsFromNiPage, false).success.value
//          .set(SoldGoodsFromEuPage, false).success.value
//
//      val expectedResult = SaveForLaterRequest(vrn, period, answers.data)
//
//      mockSaveForLaterService.get(answers, vrn, period) mustEqual Valid(expectedResult)
//    }
//  }
}
