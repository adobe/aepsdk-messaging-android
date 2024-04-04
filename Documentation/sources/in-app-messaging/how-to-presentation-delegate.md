# Programmatically control the display of in-app messages

You can now implement a `PresentationDelegate` in order to be alerted when specific events occur during the lifecycle of an in-app message.

### Register the delegate with the Adobe Service Provider

The `AEPUIService` class maintains an optional property that holds a reference to the `PresentationDelegate`.

#### Kotlin
```kotlin
// accessed via the public class ServiceProvider which contains a getter for the UIService implementation
interface AEPUIService {
    fun setPresentationDelegate(presentationDelegate: PresentationDelegate)
}
```

The `setPresentationDelegate` API is then called with a custom `CustomDelegate` object which must extend the `PresentationDelegate` class.

#### Kotlin
```kotlin
val myPresentationDelegate = CustomDelegate()
ServiceProvider.getInstance().uiService.setPresentationDelegate(myPresentationDelegate)
```
#### Java
```java
CustomDelegate myPresentationDelegate = new CustomDelegate();
ServiceProvider.getInstance().getUIService().setPresentationDelegate(myPresentationDelegate);
```

### PresentationDelegate interface

The `PresentationDelegate` interface extends the `PresentationListener` and `PresentationLever` interfaces. It is available in the `Services.ui` package and the interfaces are defined below:
#### Kotlin
```kotlin
/**
 * A delegate that can be used to observe and control the lifecycle of [Presentation]'s managed by the SDK.
 */
interface PresentationDelegate : PresentationListener, PresentationLever
```
#### Kotlin
```kotlin
/**
 * A listener for observing the lifecycle of presentations managed by the SDK.
 */
interface PresentationListener {
    /**
     * Invoked when a the presentable is shown.
     * @param presentable the [Presentable] that was shown
     */
    fun onShow(presentable: Presentable<*>)

    /**
     * Invoked when a presentable is hidden.
     * @param presentable the [Presentable] that was hidden
     */
    fun onHide(presentable: Presentable<*>)

    /**
     * Invoked when a presentable is dismissed.
     * @param presentable the [Presentable] that was dismissed
     */
    fun onDismiss(presentable: Presentable<*>)

    /**
     * Invoked when the content in the presentable is loaded.
     * @param presentable the [Presentable] into which that was loaded
     * @param presentationContent optional [PresentationContent] that was loaded into the presentable
     */
    fun onContentLoaded(presentable: Presentable<*>, presentationContent: PresentationContent?)

    /**
     * Defines the types of content that can be loaded into a [Presentable].
     */
    sealed class PresentationContent {
        /**
         * Content loaded from a URL.
         * @param url the URL from which the content was loaded
         */
        class UrlContent(val url: String) : PresentationContent()
    }
}
```
#### Kotlin
```kotlin
/**
 * A gating mechanism for implementers to restrict the display of a [Presentable] based on specific
 * set of conditions.
 */
interface PresentationLever {
    /**
     * Returns true if [presentable] can be shown, false otherwise.
     * @param presentable the [Presentable] to check if it can be shown
     * @return true if [presentable] can be shown, false otherwise
     */
    fun canShow(presentable: Presentable<*>): Boolean
}
```

### Retrieving the Presentable object from the implemented interface methods

The user interface methods in a `PresentationDelegate` implementation will be passed a `Presentable` object. An `InAppMessage` object is the Android Core implementation for an in-app `Presentable` and the Messaging extension maintains an internal mapping of a `Message` object associated with a `Presentable`. The reference to the `Message` object is your primary way to interact with an AJO in-app message.

#### Kotlin
```kotlin
val currentMessagePresentable: Presentable<InAppMessage>? = null
override fun onShow(presentable: Presentable<*>) {
    if (!presentable.getPresentation() is InAppMessage) return
    currentMessagePresentable = presentable as Presentable<InAppMessage>
    val message = MessagingUtils.getMessageForPresentable(currentMessagePresentable)
 }
```
#### Java
```java
Presentable<InAppMessage> currentMessagePresentable = null;
 @Override
 public void onShow(Presentable<?> presentable) {
    if (!(presentable.getPresentation() instanceof InAppMessage)) return;
    currentMessagePresentable = (Presentable<InAppMessage>) presentable;
    Message message = MessagingUtils.getMessageForPresentable(currentMessagePresentable);
 }
```

### Controlling when a message should be shown to the end user

If a custom  `PresentationDelegate`  implementation has been set in the `ServiceProvider`, the delegate's `canShow` method will be called prior to displaying an in-app message for which the end user has qualified. You are responsible for returning `true` if the message should be shown, or `false` if the message should be suppressed.

An example of when you may choose to suppress an in-app message due to the status of some other workflow within the app can be seen below:

#### Kotlin
```kotlin
val currentMessagePresentable: Presentable<InAppMessage>? = null
var showMessages = true // if false then the message will be supressed
override fun canShow(presentable: Presentable<*>): Boolean {
    if (!presentable.getPresentation() is InAppMessage) return

    // can hold this reference for later use
    currentMessagePresentable = presentable as Presentable<InAppMessage>

    if(!showMessages) {
        println("message was suppressed: ${presentable.getPresentation().id}")
        val message = MessagingUtils.getMessageForPresentable(currentMessagePresentable)
        message?.track("message suppressed", MessagingEdgeEventType.TRIGGER)
    }

    return showMessages
}
```
#### Java
```java
Presentable<InAppMessage> currentMessagePresentable = null;
boolean showMessages = true; // if false then the message will be suppressed

@Override
public boolean canShow(Presentable<?> presentable) {
    if (!(presentable.getPresentation() instanceof InAppMessage)) {
            return true;
    }

    // can hold this reference for later use
    currentMessagePresentable = (Presentable<InAppMessage>) presentable;

    if (!showMessages) {
        System.out.println("message was suppressed: " + currentMessagePresentable.getPresentation().getId());
        Message message = MessagingUtils.getMessageForPresentable(currentMessagePresentable);
        if (message != null) {
            message.track("message suppressed", MessagingEdgeEventType.TRIGGER);
        }
    }

    return showMessages;
}
```

Another option is to store a reference to the `Message` object, and call the `show()` method on it at a later time.

Continuing with the above example, after you have stored the message that was triggered initially, you can choose to show it upon completion of the other workflow:

#### Kotlin
```kotlin
var currentMessage: Message? = null
var anotherWorkflowStatus: String? = null

fun otherWorkflowFinished() {
    anotherWorkflowStatus = "complete";
    currentMessage?.show()
}

override fun canShow(presentable: Presentable<*>): Boolean {
   if (anotherWorkflowStatus.equals("inProgress")) {
    // store the current message for later use
    currentMessage = MessagingUtils.getMessageForPresentable(currentMessagePresentable)
    return false
    }

    return true
}
```
#### Java
```java
Message currentMessage = null;
String anotherWorkflowStatus = null;

public void otherWorkflowFinished() {
    anotherWorkflowStatus = "complete";
    if (currentMessage != null) {
        currentMessage.show();
    }
}

@Override
public boolean canShow(Presentable<?> presentable) {
    if (anotherWorkflowStatus != null && anotherWorkflowStatus.equals("inProgress")) {
        // store the current message for later use
        currentMessage = MessagingUtils.getMessageForPresentable(currentMessagePresentable);
        return false;
    }

    return true;
}
```
