package org.mule.modules.ftpclient.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.el.ExpressionLanguageExtension;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.MuleRegistry;
import org.mule.el.mvel.MVELExpressionLanguage;
import org.mule.expression.DefaultExpressionManager;

public class AbstractClientTest {
    protected FileManager fileManager;

    @Before
    public void prepare() throws Exception {
        fileManager = new FileManager("ftp-tests-");
    }

    @After
    public void cleanup() throws Exception {
        fileManager.destroy();
        fileManager = null;
    }

    protected byte[] createContent(int size) {
        byte[] content = new byte[size];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (i & 255);
        }
        return content;
    }

    protected void assertDeleted(File file, int timeout) {
        long waitUntil = System.currentTimeMillis() + timeout;
        long remainingTimeout = waitUntil - System.currentTimeMillis();
        while (file.isFile() && remainingTimeout >= 0) {
            try {
                Thread.sleep(remainingTimeout < 100 ? remainingTimeout : 100);
            } catch (InterruptedException e) {
                // ignore
            }
            remainingTimeout = waitUntil - System.currentTimeMillis();
        }
        assertFalse(file.getName(), file.isFile());
    }

    protected void checkFile(File file, byte[] content) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            byte[] bytes = IOUtils.toByteArray(is);
            assertArrayEquals(content, bytes);
        }
    }

    protected MuleContext createMuleContext() throws InitialisationException {
        SimpleMock<MuleContext> contextMock = new SimpleMock<>(MuleContext.class);
        contextMock.storeResult(createRegistry(), "getRegistry");
        contextMock.storeResult(createConfiguration(), "getConfiguration");
        contextMock.storeResult(createExpressionManager(contextMock.getMockObject()), "getExpressionManager");
        return contextMock.getMockObject();
    }

    protected MuleRegistry createRegistry() {
        SimpleMock<MuleRegistry> registryMock = new SimpleMock<>(MuleRegistry.class);
        registryMock.storeResult(new ArrayList<Object>(), "lookupObjectsForLifecycle",
                ExpressionLanguageExtension.class);
        return registryMock.getMockObject();
    }

    protected ExpressionManager createExpressionManager(MuleContext muleContext) throws InitialisationException {
        DefaultExpressionManager manager = new DefaultExpressionManager();
        MVELExpressionLanguage expressionLanguage = new MVELExpressionLanguage(muleContext);
        expressionLanguage.initialise();
        manager.setExpressionLanguage(expressionLanguage);
        return manager;
    }

    protected MuleConfiguration createConfiguration() {
        SimpleMock<MuleConfiguration> configurationMock = new SimpleMock<>(MuleConfiguration.class);
        configurationMock.storeResult("UTF-8", "getDefaultEncoding");
        return configurationMock.getMockObject();
    }
}
