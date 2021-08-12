#!/bin/bash

echo ""
echo "Applying migration ContactHmrc"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /:period/contactHmrc                       controllers.ContactHmrcController.onPageLoad(period: Period)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "contactHmrc.title = contactHmrc" >> ../conf/messages.en
echo "contactHmrc.heading = contactHmrc" >> ../conf/messages.en

echo "Migration ContactHmrc completed"
