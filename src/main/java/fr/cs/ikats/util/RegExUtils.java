/**
 * Copyright 2018 CS Systèmes d'Information
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.cs.ikats.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExUtils {

	/**
	 * Get the groups names from the string pattern<br>
	 * 
	 *         <a href="http://stackoverflow.com/a/15588989">Accepted answer to
	 *         "Get group names in java regex"</a>
	 * @param regex
	 *            the regex wher to find groups
	 * @return
	 */
	public static Set<String> getNamedGroup(String regex) {
		Set<String> namedGroups = new TreeSet<String>();

		Matcher m = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>").matcher(regex);

		while (m.find()) {
			namedGroups.add(m.group(1));
		}

		return namedGroups;
	}

	/**
	 * Get the group names from the compiled {@link Pattern}<br>
	 * <br>
	 * Note from the author : That implementation uses the Java Reflextion API
	 * and could have
	 * <a href="http://docs.oracle.com/javase/tutorial/reflect/index.html">its
	 * drawbacks</a>
	 * 
	 *         <a href="http://stackoverflow.com/a/15596145">second voted answer
	 *         to "Get group names in java regex"</a>
	 * @param regex
	 *            the {@link Pattern}
	 * @return a Map that maps group names to the group numbers
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Integer> getNamedGroups(Pattern regex) throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		Method namedGroupsMethod = Pattern.class.getDeclaredMethod("namedGroups");
		namedGroupsMethod.setAccessible(true);

		Map<String, Integer> namedGroups = null;
		namedGroups = (Map<String, Integer>) namedGroupsMethod.invoke(regex);

		if (namedGroups == null) {
			throw new InternalError();
		}

		return Collections.unmodifiableMap(namedGroups);
	}
}
