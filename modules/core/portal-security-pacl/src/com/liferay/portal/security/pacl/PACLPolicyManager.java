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

package com.liferay.portal.security.pacl;

import com.liferay.portal.kernel.url.URLContainer;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.security.lang.PortalSecurityManager;
import com.liferay.portal.security.lang.SecurityManagerUtil;
import com.liferay.portal.util.PropsValues;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import java.security.AccessController;
import java.security.CodeSource;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Brian Wing Shun Chan
 * @author Raymond Augé
 */
public class PACLPolicyManager {

	public static PACLPolicy buildPACLPolicy(
		String contextName, URLContainer urlContainer, ClassLoader classLoader,
		Properties properties) {

		String value = properties.getProperty(
			"security-manager-enabled", "false");

		if (value.equals("generate")) {
			return new GeneratingPACLPolicy(
				contextName, urlContainer, classLoader, properties);
		}

		if (GetterUtil.getBoolean(value)) {
			return new ActivePACLPolicy(
				contextName, urlContainer, classLoader, properties);
		}

		return new InactivePACLPolicy(
			contextName, urlContainer, classLoader, properties);
	}

	public static PACLPolicy getDefaultPACLPolicy() {
		return _defaultPACLPolicy;
	}

	public static PACLPolicy getPACLPolicy(ClassLoader classLoader) {
		if (classLoader == null) {
			return null;
		}

		return AccessController.doPrivileged(
			new PACLPolicyPrivilegedAction(classLoader));
	}

	public static PACLPolicy getPACLPolicy(ProtectionDomain protectionDomain) {
		if (protectionDomain == null) {
			return null;
		}

		return AccessController.doPrivileged(
			new PACLPolicyPrivilegedAction(protectionDomain));
	}

	public static PACLPolicy getPACLPolicy(URL locationURL) {
		if (locationURL == null) {
			return null;
		}

		return AccessController.doPrivileged(
			new PACLPolicyPrivilegedAction(locationURL));
	}

	public static void register(
		ClassLoader classLoader, PACLPolicy paclPolicy) {

		List<URL> urLs = paclPolicy.getURLs();

		if (classLoader instanceof URLClassLoader) {
			URLClassLoader urlClassLoader = (URLClassLoader)classLoader;

			for (URL url : urlClassLoader.getURLs()) {
				String path = url.getPath();

				if (path.startsWith(
						PropsValues.LIFERAY_LIB_GLOBAL_SHARED_DIR)) {

					continue;
				}

				urLs.add(url);

				_urlPACLPolicies.put(new URLWrapper(url), paclPolicy);
			}
		}

		URLContainer urlContainer = paclPolicy.getURLContainer();

		URL path = urlContainer.getResource(StringPool.SLASH);

		String realPath = path.getPath();

		if (realPath.endsWith(StringPool.SLASH)) {
			realPath = realPath.substring(0, realPath.length() - 1);
		}

		try {
			URL url = new URL("file", "", -1, realPath);

			urLs.add(url);

			_urlPACLPolicies.put(new URLWrapper(url), paclPolicy);

			url = new URL("file", "", -1, realPath + StringPool.SLASH);

			urLs.add(url);

			_urlPACLPolicies.put(new URLWrapper(url), paclPolicy);

			url = new URL("file", "", -1, realPath + "/WEB-INF/classes/*");

			urLs.add(url);

			_urlPACLPolicies.put(new URLWrapper(url), paclPolicy);
		}
		catch (MalformedURLException murle) {
			throw new RuntimeException(murle);
		}

		_classLoaderPACLPolicies.put(classLoader, paclPolicy);

		refresh();
	}

	public static void unregister(ClassLoader classLoader) {
		PACLPolicy paclPolicy = _classLoaderPACLPolicies.remove(classLoader);

		for (URL url : paclPolicy.getURLs()) {
			_urlPACLPolicies.remove(url);
		}

		refresh();
	}

	private static void refresh() {
		PortalSecurityManager portalSecurityManager =
			SecurityManagerUtil.getPortalSecurityManager();

		Policy policy = portalSecurityManager.getPolicy();

		policy.refresh();
	}

	private static final Map<ClassLoader, PACLPolicy> _classLoaderPACLPolicies =
		new ConcurrentHashMap<>();
	private static final PACLPolicy _defaultPACLPolicy = new InactivePACLPolicy(
		StringPool.BLANK, new InactiveURLContext(),
		PACLPolicyManager.class.getClassLoader(), new Properties());
	private static final Map<URLWrapper, PACLPolicy> _urlPACLPolicies =
		new ConcurrentHashMap<>();

	private static class InactiveURLContext implements URLContainer {

		@Override
		public URL getResource(String name) {
			return null;
		}

		@Override
		public Set<String> getResources(String path) {
			return Collections.emptySet();
		}

	}

	private static class PACLPolicyPrivilegedAction
		implements PrivilegedAction<PACLPolicy> {

		public PACLPolicyPrivilegedAction(ClassLoader classLoader) {
			_classLoader = classLoader;
		}

		public PACLPolicyPrivilegedAction(ProtectionDomain protectionDomain) {
			_classLoader = protectionDomain.getClassLoader();

			CodeSource codeSource = protectionDomain.getCodeSource();

			if (codeSource == null) {
				return;
			}

			_locationURL = codeSource.getLocation();
		}

		public PACLPolicyPrivilegedAction(URL locationURL) {
			_locationURL = locationURL;
		}

		@Override
		public PACLPolicy run() {
			PACLPolicy paclPolicy = getFromClassLoader();

			if ((paclPolicy != null) || (_classLoader != null) ||
				(_locationURL == null)) {

				return paclPolicy;
			}

			return _urlPACLPolicies.get(new URLWrapper(_locationURL));
		}

		private PACLPolicy getFromClassLoader() {
			if (_classLoader == null) {
				return null;
			}

			PACLPolicy paclPolicy = _classLoaderPACLPolicies.get(_classLoader);

			while ((paclPolicy == null) && (_classLoader.getParent() != null)) {
				_classLoader = _classLoader.getParent();

				paclPolicy = _classLoaderPACLPolicies.get(_classLoader);
			}

			return paclPolicy;
		}

		private ClassLoader _classLoader;
		private URL _locationURL;

	}

}