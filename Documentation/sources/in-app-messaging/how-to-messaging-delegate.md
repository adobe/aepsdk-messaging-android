# Programmatically control the display of in-app messages

You can now implement a `MessagingDelegate` in order to be alerted when specific events occur during the lifecycle of an in-app message.

### Register the delegate with the Adobe Service Provider

The `ServiceProvider` class maintains an optional property that holds reference to the `MessagingDelegate`.

```java
// defined in public class ServiceProvider 
public void setMessageDelegate(MessagingDelegate messageDelegate)
```

The `setMessageDelegate` API is then called with a custom `MessagingDelegate` object which must extend the `MessagingDelegate` class.

```java
CustomDelegate myMessagingDelegate = new CustomDelegate();
ServiceProvider.getInstance().setMessageDelegate(myMessagingDelegate);
```

### MessagingDelegate interface

The `MessagingDelegate` interface which is available in the `Services` package is defined below:

```java
/**
 * UI Message delegate which is used to listen for current message lifecycle events and control if
 * the message should be displayed.
 */
public interface MessagingDelegate {
    /**
     * Invoked when a message is displayed.
     *
     * @param message {@link FullscreenMessage} that is being displayed
     */
    default void onShow(final FullscreenMessage message) {
        Log.debug(ServiceConstants.LOG_TAG, "MessagingDelegate", "Fullscreen message shown.");
    }

    /**
     * Invoked when a message is dismissed.
     *
     * @param message {@link FullscreenMessage} that is being dismissed
     */
    default void onDismiss(final FullscreenMessage message) {
        Log.debug(ServiceConstants.LOG_TAG, "MessagingDelegate", "Fullscreen message dismissed.");
    }

    /**
     * Used to determine if a message should be shown.
     *
     * @param message {@link FullscreenMessage} that is about to get displayed
     * @return true if the message should be displayed, false otherwise
     */
    boolean shouldShowMessage(final FullscreenMessage message);

    /**
     * Called when the {@link FullscreenMessage} loads a url.
     *
     * @param url {@code String} being loaded by the {@code FullscreenMessage}
     * @param message {@link FullscreenMessage} loading a url {@code String}
     */
    default void urlLoaded(final String url, final FullscreenMessage message) {
        Log.debug(
                ServiceConstants.LOG_TAG,
                "MessagingDelegate",
                "Fullscreen message loaded url: %s",
                url);
    }
}
```

### Retrieving the Message object from the implemented interface methods

The user interface methods in a `MessagingDelegate` implementation will be passed a `FullscreenMessage` object. An  `AEPMessage` object is the Android Core implementation of the `FullscreenMessage` interface and it contains a reference to the parent `Message` class. The reference to the `Message` object is your primary way to interact with an AJO in-app message.

The reference can be obtained by calling `fullscreenMessage.getParent()` . An example of how to access the `Message` in the `onShow` delegate method can be seen below:

```java
@Override
public void onShow(FullscreenMessage fullscreenMessage) {
  Message message = (Message) fullscreenMessage.getParent();
  System.out.println("message was shown: " + message.id);
}
```

### Controlling when a message should be shown to the end user

If a custom  `MessagingDelegate`  implementation has been set in the `ServiceProvider`, the delegate's `shouldShowMessage` method will be called prior to displaying an in-app message for which the end user has qualified. You are responsible for returning `true` if the message should be shown, or `false` if the message should be suppressed.

An example of when you may choose to suppress an in-app message due to the status of some other workflow within the app can be seen below:

```java
@Override
public boolean shouldShowMessage(FullscreenMessage fullscreenMessage) {
   if (someOtherWorkflowStatus == "inProgress") {
        return false;
    }
    return true;
}
```

Another option is to store a reference to the  `FullscreenMessage` object, and call the `show()` method on it at a later time.

Continuing with the above example, after you have stored the message that was triggered initially, you can choose to show it upon completion of the other workflow:

```java
Message currentMessage = null;
String anotherWorkflowStatus;

public void otherWorkflowFinished() {
    anotherWorkflowStatus = "complete";
    currentMessage.show();
}

@Override
public boolean shouldShowMessage(FullscreenMessage fullscreenMessage) {
   if (someOtherWorkflowStatus.equals("inProgress")) {
     // store the current message for later use
     currentMessage = (Message) fullscreenMessage.getParent();
     return false;
   }
  
  return true;
}
```

### Integrating the message into an existing UI

If you would like to manually integrate the `View` that contains the UI for an in-app message, you can do so by accessing the `WebView` directly in a `MessagingDelegate` method.  

In the example below, you can decide whether or not the in-app message should be directly integrated into your existing UI. If so, you capture a reference to the message's `WebView` and return `false` to prevent the message from being shown by the SDK:

```java
private Message currentMessage = null;
private boolean shouldIntegrateMessageDirectly = true;
private WebView inAppMessageView;

@Override
public boolean shouldShowMessage(FullscreenMessage fullscreenMessage) {
  if (shouldIntegrateMessageDirectly) {
    this.currentMessage = (Message) fullscreenMessage.getParent();
    inAppMessageView = currentMessage.getWebView();
    
    return false;
  }
  
  return true;
}
```
