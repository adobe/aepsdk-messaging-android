#  MessagingPushPayload Usage
This document explains how to use `MessagingPushPayload` for getting the notification attributes such as title, body, actions etc for creating a push notification.

## Creating the MessagingPushPayload object
To do this, use the below constructors in the `FirebaseMessagingService` class's `onMessageReceived` method:

### Constructor

```java
    public MessagingPushPayload(RemoteMessage message)
```

```java
    public MessagingPushPayload(Map<String, String> data)
```

## Public APIs
This class provides APIs for getting attributes from the `RemoteMessage`  push data payload which are used while creating the push notification.

### Get title
Provides the title for the created push notification
```java
    public String getTitle()
```

### Get body
Provides the body of the push notification.
```java
    public String getBody()
```

### Get sound
Provides the sound to be used when the push notification is shown.
```java
    public String getSound()
```

### Get notification badge count
Provides the notification badge count.
```java
    public int getBadgeCount()
```

### Get notification priority
Provides the notification priority. See the firebase [documentation](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages#notificationpriority) for more info.
```java
    public int getNotificationPriority()
```

### Get channel id
Provides the channel id required for creating the push notification channel. [Notification channels](https://developer.android.com/training/notify-user/channels) are required to be used on Android 8.0 or newer.
```java
    public String getChannelId()
```

### Get icon
Provides the icon to be displayed in the push notification.
```java
    public String getIcon()
```

### Get image url
Provides the image url to be shown in the push notification.
```java
    public String getImageUrl()
```

### Get custom data 
Provides the custom data in a `String` map.
```java
    public Map<String, String> getData()
```

### Get action type
Provides the type of action which needs to be performed when the push notification is clicked.
```java
    public ActionType getActionType()
```
#### Enum `ActionType`
There are 4 types of actions available. These are used to determine which action needs to be performed when a push notification is clicked.
`DEEPLINK`, `WEBURL`, `OPENAPP`, `NONE`

### Get action buttons
Provides the list of buttons with label, type of action and URI/URL.
```java
    public List<ActionButton> getActionButtons()
```
#### Class `ActionButton`
This class provides action button with attributes (label, link and type) that are used while creating buttons in the push notification.
```java
    public ActionButton(final String label, final String link, final String type)
```

### Get action link
Provides the link if present which is used while handling the interaction with push notification.  
```java
    public String getActionUri()
```