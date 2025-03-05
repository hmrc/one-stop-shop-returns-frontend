#!/bin/bash

echo ""
echo "Applying migration InterceptUnusableEmail"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/interceptUnusableEmail                       controllers.InterceptUnusableEmailController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "interceptUnusableEmail.title = interceptUnusableEmail" >> ../conf/messages.en
echo "interceptUnusableEmail.heading = interceptUnusableEmail" >> ../conf/messages.en

echo "Migration InterceptUnusableEmail completed"
