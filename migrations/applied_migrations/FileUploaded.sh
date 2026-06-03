#!/bin/bash

echo ""
echo "Applying migration FileUploaded"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/fileUploaded                        controllers.FileUploadedController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/fileUploaded                        controllers.FileUploadedController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeFileUploaded                  controllers.FileUploadedController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeFileUploaded                  controllers.FileUploadedController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "fileUploaded.title = fileUploaded" >> ../conf/messages.en
echo "fileUploaded.heading = fileUploaded" >> ../conf/messages.en
echo "fileUploaded.checkYourAnswersLabel = fileUploaded" >> ../conf/messages.en
echo "fileUploaded.error.required = Select yes if fileUploaded" >> ../conf/messages.en
echo "fileUploaded.change.hidden = FileUploaded" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryFileUploadedUserAnswersEntry: Arbitrary[(FileUploadedPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[FileUploadedPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryFileUploadedPage: Arbitrary[FileUploadedPage.type] =";\
    print "    Arbitrary(FileUploadedPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(FileUploadedPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration FileUploaded completed"
