EXTENSION-LIBRARY-FOLDER-NAME = messaging
TEST-APP-FOLDER-NAME = testapp
CURRENT_DIRECTORY := ${CURDIR}
MESSAGING_GRADLE_FILE = $(CURRENT_DIRECTORY)/code/messaging/build.gradle.kts
MESSAGING_GRADLE_TEMP_FILE = $(MESSAGING_GRADLE_FILE).backup

init:
	git config core.hooksPath .githooks

clean:
	(./code/gradlew -p code clean)

format:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) spotlessApply)
		
format-license:
	(./code/gradlew -p code licenseFormat)

checkformat:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) spotlessCheck)

checkstyle:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) checkstyle)

ci-lint: checkformat checkstyle

unit-test:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) testPhoneDebugUnitTest)

unit-test-coverage:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) createPhoneDebugUnitTestCoverageReport)

functional-test:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) uninstallPhoneDebugAndroidTest)
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) connectedPhoneDebugAndroidTest)

functional-test-coverage:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) uninstallPhoneDebugAndroidTest)
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) createPhoneDebugAndroidTestCoverageReport)

e2e-functional-test:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) uninstallPhoneDebugAndroidTest)
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) connectedPhoneDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.adobe.marketing.mobile.messaging.E2EFunctionalTests)

javadoc:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) javadocJar)

assemble-phone:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) assemblePhone)

assemble-phone-debug:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) assemblePhoneDebug)
		
assemble-phone-release:
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) assemblePhoneRelease)

assemble-app:
	(./code/gradlew -p code/$(TEST-APP-FOLDER-NAME) assemble)

ci-publish-maven-local-jitpack: assemble-phone-release
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) publishReleasePublicationToMavenLocal -Pjitpack  -x signReleasePublication)

ci-publish-staging: assemble-phone-release
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) publishReleasePublicationToSonatypeRepository)

ci-publish: assemble-phone-release
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) publishReleasePublicationToSonatypeRepository -Prelease)

# usage - 
# make set-environment ENV=[environment]
set-environment:
	@echo "Setting E2E functional testing to run in environment '$(ENV)'"
	sed -i.backup 's|prodVA7|$(ENV)|g' $(MESSAGING_GRADLE_FILE)
	sed -i.backup 's|prodAUS5|$(ENV)|g' $(MESSAGING_GRADLE_FILE)
	sed -i.backup 's|prodNLD2|$(ENV)|g' $(MESSAGING_GRADLE_FILE)
	sed -i.backup 's|stageVA7|$(ENV)|g' $(MESSAGING_GRADLE_FILE)
	rm ${MESSAGING_GRADLE_TEMP_FILE}