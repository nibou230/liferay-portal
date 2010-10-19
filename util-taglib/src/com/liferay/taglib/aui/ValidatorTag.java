/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.taglib.aui;

import com.liferay.portal.kernel.servlet.taglib.BaseBodyTagSupport;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.util.PwdGenerator;

/**
 * @author Julio Camarero

 * @author Brian Wing Shun Chan
 */
public class ValidatorTag extends BaseBodyTagSupport {

	public ValidatorTag() {
	}

	public ValidatorTag(String name, String errorMessage, String body) {
		_body = body;
		_errorMessage = errorMessage;
		_name = name;

		processCustom();
	}

	public int doStartTag() {
		processCustom();

		InputTag parentInputTag = (InputTag)findAncestorWithClass(
			this, InputTag.class);

		parentInputTag.addValidatorTag(_name, this);

		return EVAL_PAGE;
	}

	public String getBody() {
		String body = StringPool.APOSTROPHE + StringPool.APOSTROPHE;

		if (Validator.isNotNull(_body)) {
			body = _body;
		}
		else if (getBodyContent() != null){
			body = getBodyContent().getString();
		}

		return body.trim();
	}

	public String getErrorMessage() {
		if (_errorMessage == null) {
			return StringPool.BLANK;
		}

		return _errorMessage;
	}

	public String getName() {
		return _name;
	}

	public boolean isCustom() {
		return _isCustom;
	}

	public void setBody(String body) {
		_body = body;
	}

	public void setErrorMessage(String errorMessage) {
		_errorMessage = errorMessage;
	}

	public void setName(String name) {
		_name = name;
	}

	protected void cleanUp() {
		_body = null;
		_errorMessage = null;
		_isCustom = false;
		_name = null;
	}

	private void processCustom() {
		if (_name.equals("custom")) {
			_isCustom = true;

			_name += StringPool.UNDERLINE +
				PwdGenerator.getPassword(PwdGenerator.KEY3, 4);
		}
	}

	private String _body;
	private String _errorMessage;
	private boolean _isCustom = false;
	private String _name;

}