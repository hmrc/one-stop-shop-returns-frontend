#!/bin/bash

echo ""
echo "Applying migration FileUpload"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:period/fileUpload                        controllers.FileUploadController.onPageLoad(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/fileUpload                        controllers.FileUploadController.onSubmit(mode: Mode = NormalMode, period: Period)" >> ../conf/app.routes

echo "GET        /:period/changeFileUpload                  controllers.FileUploadController.onPageLoad(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes
echo "POST       /:period/changeFileUpload                  controllers.FileUploadController.onSubmit(mode: Mode = CheckMode, period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "fileUpload.title = fileUpload" >> ../conf/messages.en
echo "fileUpload.heading = fileUpload" >> ../conf/messages.en
echo "fileUpload.checkYourAnswersLabel = fileUpload" >> ../conf/messages.en
echo "fileUpload.error.required = Select yes if fileUpload" >> ../conf/messages.en
echo "fileUpload.change.hidden = FileUpload" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryFileUploadUserAnswersEntry: Arbitrary[(FileUploadPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[FileUploadPage.type]";\
    print "        value <- arbitrary[Boolean].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryFileUploadPage: Arbitrary[FileUploadPage.type] =";\
    print "    Arbitrary(FileUploadPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(FileUploadPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Migration FileUpload completed"
