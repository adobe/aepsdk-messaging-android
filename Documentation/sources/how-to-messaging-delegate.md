# Programmatically control the display of in-app messages

You can now implement a `MessagingDelegate` in order to be alerted when specific events occur during the lifecycle of an in-app message.

### Register the delegate with the Adobe Service Provider

The `ServiceProvider` class maintains an optional property that holds reference to the `FullscreenMessaageDelegate`.

```java
// defined in public class ServiceProvider 
public void setMessageDelegate(FullscreenMessageDelegate messageDelegate)
```

The `setMessageDelegate` API is then called with a custom `MessagingDelegate` object which must extend the `MessagingDelegate` class.

```java
CustomDelegate myMessagingDelegate = new CustomDelegate();
ServiceProvider.getInstance().setMessageDelegate(myMessagingDelegate);
```

### FullscreenMessageDelegate interface

The `FullscreenMessageDelegate` interface, which is implemented in the Android Messaging extension in the `MessagingDelegate` class, is defined below:

```java
/**
 * Delegate for Messaging extension in-app message events.
 */
public interface FullscreenMessageDelegate {
	/**
	 * Invoked when the in-app message is displayed.
	 *
	 * @param message FullscreenMessage the in-app message being displayed
	 */
	void onShow(final FullscreenMessage message);

	/**
	 * Invoked when the in-app message is dismissed.
	 *
	 * @param message FullscreenMessage the in-app message being dismissed
	 */
	void onDismiss(final FullscreenMessage message);

	/**
	 * Used to determine if the in-app message should be shown.
	 *
	 * @param message FullscreenMessage the in-app message that is about to get displayed
	 */
	boolean shouldShowMessage(final FullscreenMessage message);

	/**
	 * Invoked when the in-app message is attempting to load a url.
	 *
	 * @param message FullscreenMessage the in-app message attempting to load the url
	 * @param url     String the url being loaded by the message
	 *
	 * @return True if the core wants to handle the URL (and not the fullscreen message view implementation)
	 */
	boolean overrideUrlLoad(final FullscreenMessage message, final String url);

	/**
	 * Invoked when the in-app message failed to be displayed.
	 */
	void onShowFailure();
}
```

### Retrieving the Message object from the implemented interface methods

The user interface methods (except for `onShowFailure()`) in a `FullscreenMessageDelegate` implementation will be passed an `AEPMessage` object. An `AEPMessage` object is the Android Core implementation of the `FullscreenMessage` interface. It contains a reference to the parent `Message` class and is your primary way to interact with the message.

A reference to the `AEPMessage` object can be obtained by calling `fullscreenMessage.getParent()` . An example of how to access the `Message` in the `onShow` delegate method can be seen below:

```java
@Override
public void onShow(FullscreenMessage fullscreenMessage) {
  Message message = (Message) fullscreenMessage.getParent();
  System.out.println("message was shown: " + message.id);
}
```

### Controlling when a message should be shown to the end user

If a custom  `FullscreenMessageDelegate` has been set in the `ServiceProvider`, this delegate's `shouldShowMessage` method will be called prior to displaying an in-app message for which the end user has qualified. You are responsible for returning `true` if the message should be shown, or `false` if the message should be suppressed.

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
private MessageWebView inAppMessageView;

@Override
public boolean shouldShowMessage(FullscreenMessage fullscreenMessage) {
  if (shouldIntegrateMessageDirectly) {
    this.currentMessage = (Message) fullscreenMessage.getParent();
    
    // cast to MessageWebView to access the startInAppMessage function
    inAppMessageView = (MessageWebView) currentMessage.getWebView();
    
    return false;
  }
  
  return true;
}
```
