#!/bin/bash

echo ""
echo "Applying migration WantToUploadFile"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/wantToUploadFile                        controllers.WantToUploadFileController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/wantToUploadFile                        controllers.WantToUploadFileController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeWantToUploadFile                  controllers.WantToUploadFileController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeWantToUploadFile                  controllers.WantToUploadFileController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "wantToUploadFile.title = wantToUploadFile" >> ../conf/messages.en
echo "wantToUploadFile.heading = wantToUploadFile" >> ../conf/messages.en
echo "wantToUploadFile.checkYourAnswersLabel = wantToUploadFile" >> ../conf/messages.en
echo "wantToUploadFile.error.required = Select yes if wantToUploadFile" >> ../conf/messages.en
echo "wantToUploadFile.change.hidden = WantToUploadFile" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryWantToUploadFileUserAnswersEntry: Arbitrary[(WantToUploadFilePage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[WantToUploadFilePage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryWantToUploadFilePage: Arbitrary[WantToUploadFilePage.type] =";\
    print "    Arbitrary(WantToUploadFilePage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(WantToUploadFilePage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration WantToUploadFile completed"
