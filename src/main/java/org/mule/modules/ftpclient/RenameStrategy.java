package org.mule.modules.ftpclient;

import java.util.function.Consumer;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;

/**
 * A strategy which can rename files (within the same directory).
 */
public class RenameStrategy implements CompletionStrategy {

	private MuleContext muleContext;
	private String filenameExpression;
	private String originalFilenameExpression;

	public RenameStrategy(MuleContext muleContext, String filenameExpression, String originalFilenameExpression) {
		this.muleContext = muleContext;
		this.filenameExpression = filenameExpression;
		this.originalFilenameExpression = originalFilenameExpression;
	}

	@Override
	public Consumer<ClientWrapper> createCompletionHandler(MuleMessage message, String filename, String translatedName) {
		ExpressionManager expressionManager = muleContext.getExpressionManager();
		String moveToFilename = (String) expressionManager.evaluate(nullToString(filenameExpression), null, message, true);
		String moveToOriginalFilename = (String) expressionManager.evaluate(nullToString(originalFilenameExpression), null, message, true);
		return new FileArchiver(translatedName, moveToFilename, filename, moveToOriginalFilename);
	}

	private String nullToString(String str) {
		return str == null ? "" : str;
	}

}
