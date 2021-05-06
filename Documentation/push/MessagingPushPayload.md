#  MessagingPushPayload Usage
This document explains how to use `MessagingPushPayload` for getting the notification attributes like title, body, actions etc for creating the push notifications.

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
This class provides APIs for getting attributes from push payload which are used while creating the push notification.

### Get title
Provides the title for creating push notification
```java
    public String getTitle()
```

### Get body
Provides the body of the push notification.
```java
    public String getBody()
```

### Get sound
Provides the sound to be used when push notification is shown.
```java
    public String getSound()
```

### Get notification badge count
Provides the notification badge count.
```java
    public int getBadgeCount()
```

### Get notification priority
Provides the notification priority. Check out the firebase [documentation](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages#notificationpriority).
```java
    public int getNotificationPriority()
```

### Get channel id
Provides the channel id required for creating the push notification.
```java
    public String getChannelId()
```

### Get icon
Provides the icon required for creating the push notification.
```java
    public String getIcon()
```

### Get image url
Provides the image url for creating push notification with image.
```java
    public String getImageUrl()
```

### Get custom data 
Provides the custom data in map.
```java
    public Map<String, String> getData()
```

### Get action type
Provides the type of action which needs to be performed when push notification is clicked.
```java
    public ActionType getActionType()
```
#### Enum `ActionType`
There are 4 types of action available. These are used to determine which action needs to be performed when push notification is clicked.
`DEEPLINK`, `WEBURL`, `DISMISS`, `NONE`

### Get action buttons
Provides the list of buttons with label, type of action and uri/url.
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