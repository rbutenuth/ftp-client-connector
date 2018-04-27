package org.mule.modules.ftpclient.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.callback.SourceCallback;
import org.mule.api.transport.PropertyScope;

public class SourceCallbackRecorder implements SourceCallback {

    private List<MuleMessage> messages = new ArrayList<>();
    private Exception exception;

    public SourceCallbackRecorder() {

    }

    public SourceCallbackRecorder(Exception e) {
        exception = e;
    }

    public List<MuleMessage> getMessages() {
        return messages;
    }

    public synchronized List<MuleMessage> waitForMessages(int count, long timeoutInMillis) {
        long waitUntil = System.currentTimeMillis() + timeoutInMillis;
        long remainingTimeout = waitUntil - System.currentTimeMillis();
        while (messages.size() < count && remainingTimeout > 0) {
            try {
                wait(remainingTimeout);
            } catch (InterruptedException e) {
                // ignore
            }
            remainingTimeout = waitUntil - System.currentTimeMillis();
        }
        // Copy list to avoid unsynchronized acces after return
        return new ArrayList<>(messages);
    }

    @Override
    public MuleEvent processEvent(@SuppressWarnings("unused") MuleEvent event) {
        throw new UnsupportedOperationException("processEvent");
    }

    @Override
    public synchronized Object process(Object payload, Map<String, Object> properties) throws Exception {
        MuleMessage message = (MuleMessage) payload;
        message.clearProperties(PropertyScope.INBOUND);
        message.addProperties(properties, PropertyScope.INBOUND);
        messages.add(message);
        notifyAll();
        if (exception != null) {
            throw exception;
        }
        return null;
    }

    @Override
    public Object process(@SuppressWarnings("unused") Object payload) throws Exception {
        throw new UnsupportedOperationException("process");
    }

    @Override
    public Object process() throws Exception {
        throw new UnsupportedOperationException("process");
    }
}