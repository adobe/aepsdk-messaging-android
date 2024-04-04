#!/bin/bash

# Make this script executable from terminal:
# chmod 755 version.sh
set -e # Any subsequent(*) commands which fail will cause the shell script to exit immediately

ROOT_DIR=$(git rev-parse --show-toplevel)
LINE="================================================================================"
VERSION_REGEX="[0-9]+\.[0-9]+\.[0-9]+"

GRADLE_PROPERTIES_FILE=$ROOT_DIR"/code/gradle.properties"

CONSTANTS_FILE=$ROOT_DIR"/code/messaging/src/main/java/com/adobe/marketing/mobile/messaging/MessagingConstants.java"
TEST_CONSTANTS_FILE=$ROOT_DIR"/code/messagingtestutils/src/main/java/com/adobe/marketing/mobile/messaging/MessagingTestConstants.java"
# Java files
EXTENSION_VERSION_REGEX="^.*String EXTENSION_VERSION *= *"
# Kotlin files
#EXTENSION_VERSION_REGEX="^ +const val VERSION *= *"

help()
{
   echo ""
   echo "Usage: $0 -v VERSION -d DEPENDENCIES"
   echo ""
   echo -e "    -v\t- Version to update or verify for the extension. \n\t  Example: 3.0.2\n"
   echo -e "    -d\t- Comma seperated dependecies to update along with their version. \n\t  Example: "Core 3.1.1, Edge 3.2.1"\n"
   echo -e "    -u\t- Updates the version. If this flag is absent, the script verifies if the version is correct\n"
   exit 1 # Exit script after printing help
}

sed_platform() {
    # Ensure sed works properly in linux and mac-os.
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
}

update() {
    echo "Changing version to $VERSION"

    # Replace version in Constants file
    echo "Changing 'EXTENSION_VERSION' to '$VERSION' in '$CONSTANTS_FILE'"    
    sed_platform -E "/$EXTENSION_VERSION_REGEX/{s/$VERSION_REGEX/$VERSION/;}" $CONSTANTS_FILE

    # Replace version in Test Constants file
    echo "Changing 'EXTENSION_VERSION' to '$VERSION' in '$TEST_CONSTANTS_FILE'"    
    sed_platform -E "/$EXTENSION_VERSION_REGEX/{s/$VERSION_REGEX/$VERSION/;}" $TEST_CONSTANTS_FILE

    # Replace version in gradle.properties
    echo "Changing 'moduleVersion' to '$VERSION' in '$GRADLE_PROPERTIES_FILE'"
    sed_platform -E "/^moduleVersion/{s/$VERSION_REGEX/$VERSION/;}" $GRADLE_PROPERTIES_FILE  

    # Replace dependencies in gradle.properties
    if [ "$DEPENDENCIES" != "none" ]; then
        IFS="," 
        dependenciesArray=($(echo "$DEPENDENCIES"))

        IFS=" "
        for dependency in "${dependenciesArray[@]}"; do
            dependencyArray=(${dependency// / })
            dependencyName=${dependencyArray[0]}
            dependencyVersion=${dependencyArray[1]}

            if [ "$dependencyVersion" != "" ]; then
                echo "Changing 'maven${dependencyName}Version' to '$dependencyVersion' in '$GRADLE_PROPERTIES_FILE'"            
                sed_platform -E "/^maven${dependencyName}Version/{s/$VERSION_REGEX/$dependencyVersion/;}" $GRADLE_PROPERTIES_FILE  
            fi        
        done
    fi
}

verify() {    
    echo "Verifing version is $VERSION"

    if ! grep -E "$EXTENSION_VERSION_REGEX\"$VERSION\"" "$CONSTANTS_FILE" >/dev/null; then
        echo "'EXTENSION_VERSION' does not match '$VERSION' in '$CONSTANTS_FILE'"            
        exit 1
    fi

    if ! grep -E "^moduleVersion=.*$VERSION" "$GRADLE_PROPERTIES_FILE" >/dev/null; then
        echo "'moduleVersion' does not match '$VERSION' in '$GRADLE_PROPERTIES_FILE'"            
        exit 1
    fi

    if [ "$DEPENDENCIES" != "none" ]; then
        IFS="," 
        dependenciesArray=($(echo "$DEPENDENCIES"))

        IFS=" "
        for dependency in "${dependenciesArray[@]}"; do
            dependencyArray=(${dependency// / })
            dependencyName=${dependencyArray[0]}
            dependencyVersion=${dependencyArray[1]}

            if [ "$dependencyVersion" != "" ]; then
                if ! grep -E "^maven${dependencyName}Version=.*$dependencyVersion" "$GRADLE_PROPERTIES_FILE" >/dev/null; then
                    echo "maven${dependencyName}Version does not match '$dependencyVersion' in '$GRADLE_PROPERTIES_FILE'"
                    exit 1
                fi
            fi        
        done
    fi

    echo "Success"
}


while getopts "v:d:u" opt
do
   case "$opt" in    
      v ) VERSION="$OPTARG" ;;
      d ) DEPENDENCIES="$OPTARG" ;;
      u ) UPDATE="true" ;;   
      ? ) help ;; # Print help in case parameter is non-existent
   esac
done

# Print help in case parameters are empty
if [ -z "$VERSION" ]
then
   echo "********** USAGE ERROR **********"
   echo "Some or all of the parameters are empty. See usage below:";
   help
fi



echo "$LINE"
if [[ ${UPDATE} = "true" ]];
then
    update 
else 
    verify
fi
echo "$LINE"