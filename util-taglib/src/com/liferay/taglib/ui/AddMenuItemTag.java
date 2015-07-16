/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
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

package com.liferay.taglib.ui;

import com.liferay.portal.kernel.servlet.taglib.ui.AddMenuItem;
import com.liferay.taglib.util.IncludeTag;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Ambrin Chaudhary
 */
public class AddMenuItemTag extends IncludeTag {

	@Override
	public int doStartTag() {
		return EVAL_BODY_INCLUDE;
	}

	public void setTitle(String title) {
		_title = title;
	}

	public void setUrl(String url) {
		_url = url;
	}

	@Override
	protected void cleanUp() {
		_title = null;
		_url = null;
	}

	@Override
	protected String getPage() {
		return "/html/taglib/ui/add_menu_item/page.jsp";
	}

	@Override
	protected void setAttributes(HttpServletRequest request) {
		List<AddMenuItem> menuItemList =
			(List<AddMenuItem>)request.getAttribute(
				"liferay-ui:add-menu:menuItems");

		if (menuItemList != null) {
			AddMenuItem menuItem = new AddMenuItem(_title, _url);

			menuItemList.add(menuItem);
		}
	}

	private String _title;
	private String _url;

}