#!/bin/bash

echo ""
echo "Applying migration CorrectionReturnSinglePeriod"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/correctionReturnSinglePeriod                        controllers.CorrectionReturnSinglePeriodController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/correctionReturnSinglePeriod                        controllers.CorrectionReturnSinglePeriodController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeCorrectionReturnSinglePeriod                  controllers.CorrectionReturnSinglePeriodController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeCorrectionReturnSinglePeriod                  controllers.CorrectionReturnSinglePeriodController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "correctionReturnSinglePeriod.title = correctionReturnSinglePeriod" >> ../conf/messages.en
echo "correctionReturnSinglePeriod.heading = correctionReturnSinglePeriod" >> ../conf/messages.en
echo "correctionReturnSinglePeriod.checkYourAnswersLabel = correctionReturnSinglePeriod" >> ../conf/messages.en
echo "correctionReturnSinglePeriod.error.required = Select yes if correctionReturnSinglePeriod" >> ../conf/messages.en
echo "correctionReturnSinglePeriod.change.hidden = CorrectionReturnSinglePeriod" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCorrectionReturnSinglePeriodUserAnswersEntry: Arbitrary[(CorrectionReturnSinglePeriodPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[CorrectionReturnSinglePeriodPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCorrectionReturnSinglePeriodPage: Arbitrary[CorrectionReturnSinglePeriodPage.type] =";\
    print "    Arbitrary(CorrectionReturnSinglePeriodPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(CorrectionReturnSinglePeriodPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration CorrectionReturnSinglePeriod completed"
