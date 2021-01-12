EXTENSION-LIBRARY-FOLDER-NAME = messaging

BUILD-ASSEMBLE-LOCATION = ./ci/assemble
ROOT_DIR=$(shell git rev-parse --show-toplevel)

PROJECT_NAME = $(shell cat $(ROOT_DIR)/code/gradle.properties | grep "moduleProjectName" | cut -d'=' -f2)
AAR_NAME = $(shell cat $(ROOT_DIR)/code/gradle.properties | grep "moduleAARName" | cut -d'=' -f2)
MODULE_NAME = $(shell cat $(ROOT_DIR)/code/gradle.properties | grep "moduleName" | cut -d'=' -f2)
LIB_VERSION = $(shell cat $(ROOT_DIR)/code/gradle.properties | grep "moduleVersion" | cut -d'=' -f2)
SOURCE_FILE_DIR =  $(ROOT_DIR)/code/$(PROJECT_NAME)
AAR_FILE_DIR =  $(ROOT_DIR)/code/$(PROJECT_NAME)/build/outputs/aar

create-ci: clean
	(mkdir -p ci)

clean:
	(rm -rf ci)
	(rm -rf $(AAR_FILE_DIR))
	(./code/gradlew -p code clean)

ci-build: create-ci
	(mkdir -p ci/assemble)

	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) assemblePhone)
	(mv $(AAR_FILE_DIR)/$(EXTENSION-LIBRARY-FOLDER-NAME)-phone-release.aar  $(AAR_FILE_DIR)/$(MODULE_NAME)-release-$(LIB_VERSION).aar)
	(cp -r ./code/$(EXTENSION-LIBRARY-FOLDER-NAME)/build $(BUILD-ASSEMBLE-LOCATION))

ci-unit-test: create-ci
	(mkdir -p ci/unit-test)
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) platformUnitTestJacocoReport)
	(cp -r ./code/$(EXTENSION-LIBRARY-FOLDER-NAME)/build ./ci/unit-test/)

ci-javadoc: create-ci
	(mkdir -p ci/javadoc)
	(./code/gradlew -p code/$(EXTENSION-LIBRARY-FOLDER-NAME) javadocPublic > ci/javadocPublic.log 2>&1)
	(cp -r ./code/$(EXTENSION-LIBRARY-FOLDER-NAME)/build ./ci/javadoc)

ci-generate-library-debug:
		(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME}  assemblePhoneDebug)
		cp code/messaging/build/outputs/aar/messaging-phone-debug.aar sdk-build

ci-generate-library-release:
		(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME}  assemblePhoneRelease)

ci-publish-staging-all:
		(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} clean artifactoryPublish)

ci-publish-master-all:
		(./code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} clean artifactoryPublish -PisMaster=true)

ci-publish:
		(code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} assemblePhone)
		(code/gradlew -p code/${EXTENSION-LIBRARY-FOLDER-NAME} bintrayUpload -PapiKey=$(APIKEY))