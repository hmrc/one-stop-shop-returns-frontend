#!/bin/bash

echo ""
echo "Applying migration CorrectionReturnPeriod"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/correctionReturnPeriod                        controllers.CorrectionReturnPeriodController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/correctionReturnPeriod                        controllers.CorrectionReturnPeriodController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeCorrectionReturnPeriod                  controllers.CorrectionReturnPeriodController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeCorrectionReturnPeriod                  controllers.CorrectionReturnPeriodController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "correctionReturnPeriod.title = Which return period do you want to correct?" >> ../conf/messages.en
echo "correctionReturnPeriod.heading = Which return period do you want to correct?" >> ../conf/messages.en
echo "correctionReturnPeriod.period1 = 1 January to 31 March 2022" >> ../conf/messages.en
echo "correctionReturnPeriod.period2 = 1 October to 31 December 2021" >> ../conf/messages.en
echo "correctionReturnPeriod.checkYourAnswersLabel = Which return period do you want to correct?" >> ../conf/messages.en
echo "correctionReturnPeriod.error.required = Select correctionReturnPeriod" >> ../conf/messages.en
echo "correctionReturnPeriod.change.hidden = CorrectionReturnPeriod" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCorrectionReturnPeriodUserAnswersEntry: Arbitrary[(CorrectionReturnPeriodPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[CorrectionReturnPeriodPage.type]";\
    print "        value <- arbitrary[CorrectionReturnPeriod].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCorrectionReturnPeriodPage: Arbitrary[CorrectionReturnPeriodPage.type] =";\
    print "    Arbitrary(CorrectionReturnPeriodPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to ModelGenerators"
awk '/trait ModelGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryCorrectionReturnPeriod: Arbitrary[CorrectionReturnPeriod] =";\
    print "    Arbitrary {";\
    print "      Gen.oneOf(CorrectionReturnPeriod.values.toSeq)";\
    print "    }";\
    next }1' ../test/generators/ModelGenerators.scala > tmp && mv tmp ../test/generators/ModelGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(CorrectionReturnPeriodPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration CorrectionReturnPeriod completed"
