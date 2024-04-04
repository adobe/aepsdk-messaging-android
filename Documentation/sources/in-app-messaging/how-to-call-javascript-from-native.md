# Execute JavaScript code in an in-app message from native code

You can execute JavaScript in an in-app message from native code by completing the following steps:

* [Implement and assign a `PresentationDelegate`](#android_presentation_delegate)
* [Obtain a reference to the `InAppEventHandler`](#android_event_handler)
* [Call the JavaScript method](#android_call_javascript)

### Implement and assign a `PresentationDelegate`<a name="android_presentation_delegate"></a>

To register a JavaScript event handler with a `Message` object, you will first need to implement and set a `PresentationDelegate`.

For more detailed instructions on implementing and using a PresentationDelegate, please read the [tutorial on using PresentationDelegate](./how-to-presentation-delegate.md).

### Obtain a reference to the `InAppEventHandler`<a name="android_event_handler"></a>

In the `onShow` function of the `PresentationDelegate`, get a reference to the `InAppEventHandler` which can be used for Javascript interactions.

#### Kotlin
```kotlin
var eventHandler: InAppMessageEventHandler? = null
var currentMessagePresentable: Presentable<InAppMessage>? = null

override fun onShow(presentable: Presentable<*>) {
  if (!isInAppMessage(presentable)) return
  currentMessagePresentable = presentable as Presentable<InAppMessage>
  eventHandler = currentMessagePresentable?.getPresentation()?.eventHandler

}
```
#### Java

```java
InAppMessageEventHandler eventHandler = null;
Presentable<InAppMessage> currentMessagePresentable = null;

@Override
public void onShow(Presentable<?> presentable) {
    if (!isInAppMessage(presentable)) return;
    currentMessagePresentable = (Presentable<InAppMessage>) presentable;
    eventHandler = currentMessagePresentable.getPresentation().getEventHandler();
}
```

### Call the JavaScript method<a name="android_call_javascript"></a>

With a reference to the `InAppEventHandler`, the instance method `fun evaluateJavascript(jsContent: String, callback: AdobeCallback<String>)` can now be leveraged to call a JavaScript method.

Further details of this API are explained in the [Android documentation](https://developer.android.com/reference/android/webkit/WebView#evaluateJavascript(java.lang.String,%20android.webkit.ValueCallback%3Cjava.lang.String%3E)) - the example below is provided for the purpose of demonstration:

#### Kotlin
```kotlin
var eventHandler: InAppMessageEventHandler? = null
var currentMessagePresentable: Presentable<InAppMessage>? = null

override fun onShow(presentable: Presentable<*>) {
  if (!isInAppMessage(presentable)) return
  currentMessagePresentable = presentable as Presentable<InAppMessage>
  eventHandler = currentMessagePresentable?.getPresentation()?.eventHandler
  eventHandler?.evaluateJavascript("startTimer()") { content ->
    // do something with the content
  }
}
```
#### Java
```java
InAppMessageEventHandler eventHandler = null;
Presentable<InAppMessage> currentMessagePresentable = null;

@Override
public void onShow(Presentable<?> presentable) {
  if (!isInAppMessage(presentable)) return;
  currentMessagePresentable = (Presentable<InAppMessage>) presentable;
  eventHandler = currentMessagePresentable.getPresentation().getEventHandler();
  if (eventHandler != null) {
    eventHandler.evaluateJavascript("startTimer()", s -> {
    // do something with the content
    });
  }
}
```
