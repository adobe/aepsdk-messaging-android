# Call native code from the Javascript of an in-app message

You can handle events from in-app message interactions natively within your application by completing the following steps:

* [Implement and assign a `PresentationDelegate`](#android_presentation_delegate)
* [Register a JavaScript handler for your In-App Message](#android_javascript_handler)
* [Post the JavaScript message from your In-App Message](#post-the-javascript-message-from-your-in-app-message)

### Implement and assign a `PresentationDelegate`<a name="android_presentation_delegate"></a>

To register a JavaScript event handler with a `Message` object, you will first need to implement and set a `PresentationDelegate`.

For more detailed instructions on implementing and using a PresentationDelegate, please read the [tutorial on using PresentationDelegate](./how-to-presentation-delegate.md).

### Register a JavaScript handler for your In-App Message<a name="android_javascript_handler"></a>

In the `onShow` function of the `PresentationDelegate`, call `fun handleJavascriptMessage(handlerName: String, callback: AdobeCallback<String>)` to register your handler.

The name of the message you intend to pass from the JavaScript side should be specified in the first parameter.

The following example shows a handler that dispatches an `decisioning.propositionInteract` Experience Event natively when the JavaScript of the in-app message posts a `myInappCallback` message:

#### Kotlin
```kotlin
var eventHandler: InAppMessageEventHandler? = null
var currentMessagePresentable: Presentable<InAppMessage>? = null

override fun onShow(presentable: Presentable<*>) {
  if (!isInAppMessage(presentable)) return
  currentMessagePresentable = presentable as Presentable<InAppMessage>
  eventHandler = currentMessagePresentable?.getPresentation()?.eventHandler
  // in-line handling of JavaScript calls
  eventHandler?.handleJavascriptMessage("myInappCallback") { content ->
    if (content != null) {
        println("JavaScript body passed to native callback: $content")
        val message: Message? = MessagingUtils.getMessageForPresentable(currentMessagePresentable)
        message?.track(content, MessagingEdgeEventType.INTERACT);
    }
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
    // in-line handling of JavaScript calls
    eventHandler.handleJavascriptMessage("myInappCallback", content -> {
        if (content != null) {
            System.out.println("JavaScript body passed to native callback: " + content);
            Message message = MessagingUtils.getMessageForPresentable(currentMessagePresentable);
            if (message != null) {
                message.track(content, MessagingEdgeEventType.INTERACT);
            }
        }
    });
}
```

### Post the JavaScript message from your In-App Message

Now that the in-app message has been displayed, the final step is to post the JavaScript message.

Continuing from the previous example, the developer is going to post the `myInappCallback` message from their HTML, which will in turn call the handler previously configured:

#### HTML

```html
<html>
    <head>
        <script type="text/javascript">
            function callNative(action) {
                try {
                    // the name of the message handler is the same name that must be registered in native code.
                    // in this case the message name is "myInappCallback"
                    myInappCallback.run(action);
                } catch(err) {
                    console.log('The native context does not exist yet'); }
                }
            </script>
    </head>
    <body>
        <button onclick="callNative('native callbacks are cool!')">Native callback!</button>
    </body>
</html>
```

(The above HTML is not representative of a valid in-app message, and is intended only to demonstrate how to post the JavaScript message).

When the user clicks the button inside of this in-app message, the handler configured in the previous step will be called. The handler will send an Experience Event tracking the interaction, and print the following message to the console:

```text
JavaScript body passed to native callback: native callbacks are cool!
```
