#!/bin/bash

echo ""
echo "Applying migration RemovePeriodCorrection"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/removePeriodCorrection                        controllers.RemovePeriodCorrectionController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/removePeriodCorrection                        controllers.RemovePeriodCorrectionController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeRemovePeriodCorrection                  controllers.RemovePeriodCorrectionController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeRemovePeriodCorrection                  controllers.RemovePeriodCorrectionController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "removePeriodCorrection.title = removePeriodCorrection" >> ../conf/messages.en
echo "removePeriodCorrection.heading = removePeriodCorrection" >> ../conf/messages.en
echo "removePeriodCorrection.checkYourAnswersLabel = removePeriodCorrection" >> ../conf/messages.en
echo "removePeriodCorrection.error.required = Select yes if removePeriodCorrection" >> ../conf/messages.en
echo "removePeriodCorrection.change.hidden = RemovePeriodCorrection" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryRemovePeriodCorrectionUserAnswersEntry: Arbitrary[(RemovePeriodCorrectionPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[RemovePeriodCorrectionPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryRemovePeriodCorrectionPage: Arbitrary[RemovePeriodCorrectionPage.type] =";\
    print "    Arbitrary(RemovePeriodCorrectionPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(RemovePeriodCorrectionPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration RemovePeriodCorrection completed"
