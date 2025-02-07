/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2023 The JReleaser authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jreleaser.model.internal.hooks;

import org.jreleaser.model.Active;
import org.jreleaser.model.api.hooks.ExecutionEvent;
import org.jreleaser.model.internal.JReleaserContext;

import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static org.jreleaser.mustache.Templates.resolveTemplate;

/**
 * @author Andres Almiray
 * @since 1.2.0
 */
public final class CommandHook extends AbstractHook<CommandHook> {
    private static final long serialVersionUID = -3088744643196315501L;

    private String cmd;

    private final org.jreleaser.model.api.hooks.CommandHook immutable = new org.jreleaser.model.api.hooks.CommandHook() {
        private static final long serialVersionUID = 4950097179887952669L;

        @Override
        public String getCmd() {
            return cmd;
        }

        @Override
        public Filter getFilter() {
            return CommandHook.this.getFilter().asImmutable();
        }

        @Override
        public boolean isContinueOnError() {
            return CommandHook.this.isContinueOnError();
        }

        @Override
        public Active getActive() {
            return CommandHook.this.getActive();
        }

        @Override
        public boolean isEnabled() {
            return CommandHook.this.isEnabled();
        }

        @Override
        public Map<String, Object> asMap(boolean full) {
            return unmodifiableMap(CommandHook.this.asMap(full));
        }
    };

    public org.jreleaser.model.api.hooks.CommandHook asImmutable() {
        return immutable;
    }

    @Override
    public void merge(CommandHook source) {
        super.merge(source);
        this.cmd = merge(this.cmd, source.cmd);
    }

    public String getResolvedCmd(JReleaserContext context, ExecutionEvent event) {
        Map<String, Object> props = context.fullProps();
        props.put("event", event);
        return resolveTemplate(cmd, props);
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public void asMap(boolean full, Map<String, Object> map) {
        map.put("cmd", cmd);
    }
}
