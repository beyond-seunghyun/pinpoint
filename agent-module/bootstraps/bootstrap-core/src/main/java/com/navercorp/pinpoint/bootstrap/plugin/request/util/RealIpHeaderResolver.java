/*
 * Copyright 2018 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.bootstrap.plugin.request.util;

import com.navercorp.pinpoint.bootstrap.context.RemoteAddressResolver;
import com.navercorp.pinpoint.bootstrap.plugin.request.RequestAdaptor;
import com.navercorp.pinpoint.common.util.StringUtils;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Woonduk Kang(emeroad)
 */
public class RealIpHeaderResolver<T> implements RemoteAddressResolver<T> {

    private static final String FORWARDED_HEADER_NAME = "Forwarded";
    private static final Pattern FORWARDED_FOR_PATTERN = Pattern.compile("(?i:for)=\"?([^;,\"]+)\"?");

    private final String realIpHeaderName;
    private final String realIpHeaderEmptyValue;

    public RealIpHeaderResolver(final String realIpHeaderName, final String realIpHeaderEmptyValue) {
        this.realIpHeaderName = Objects.requireNonNull(realIpHeaderName, "realIpHeaderName");
        this.realIpHeaderEmptyValue = realIpHeaderEmptyValue;
    }

    @Override
    public String resolve(RequestAdaptor<T> requestAdaptor, T request) {
        final String realIpHeaderValue = requestAdaptor.getHeader(request, realIpHeaderName);
        if (StringUtils.isEmpty(realIpHeaderValue)) {
            return null;
        }

        if (realIpHeaderEmptyValue != null && realIpHeaderEmptyValue.equalsIgnoreCase(realIpHeaderValue)) {
            return null;
        }

        String firstRealIpHeaderValue = getFirstRealIpHeaderValue(realIpHeaderValue);

        if ("Forwarded".equalsIgnoreCase(realIpHeaderName)) {
            Matcher matcher = FORWARDED_FOR_PATTERN.matcher(firstRealIpHeaderValue);
            if (matcher.find()) {
                String ip = matcher.group(1).trim();
                int portSeparatorIdx = ip.lastIndexOf(':');
                int squareBracketIdx = ip.lastIndexOf(']');
                if (portSeparatorIdx > squareBracketIdx) {
                    return ip.substring(0, portSeparatorIdx);
                }

                return ip;
            }
        }

        return firstRealIpHeaderValue;
    }

    private static String getFirstRealIpHeaderValue(String realIp) {
        final int firstIndex = realIp.indexOf(',');

        if (firstIndex == -1) {
            return realIp;
        } else {
            return realIp.substring(0, firstIndex);
        }
    }
}
