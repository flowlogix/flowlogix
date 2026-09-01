/*
 * Copyright (C) 2011-2026 Flow Logix, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flowlogix.jeedao.primefaces.internal;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.PluralAttribute;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import static lombok.AccessLevel.PRIVATE;

/**
 * Resolves dotted field paths into criteria expressions, caching created joins
 * per query being built, keyed by the dotted path prefix, so that the same
 * association is only joined once per query.
 *
 * @param <TT> entity type
 * @hidden
 */
@RequiredArgsConstructor(access = PRIVATE)
public final class JoinResolver<TT> {
    private final Root<TT> root;
    private final JoinType joinType;
    private final Map<String, Join<?, ?>> joins = new HashMap<>();
    @Getter
    private boolean pluralJoin;

    /**
     * Creates a resolver for the given query root, using the given join type
     * for all joins created while resolving dotted field paths.
     *
     * @param root criteria root
     * @param joinType join type to use for created joins
     * @return resolver
     * @param <TT> entity type
     */
    public static <TT> JoinResolver<TT> of(Root<TT> root, JoinType joinType) {
        return new JoinResolver<>(root, joinType);
    }

    /**
     * Recursively resolves field name, possibly by joining other tables,
     * based on a dotted notation of the field. Joins are cached by their
     * dotted path prefix, so resolving the same association multiple times
     * only creates a single join.
     *
     * @param fieldName field name
     * @return expression
     * @param <YY> expression type
     */
    public <YY> Expression<YY> resolve(String fieldName) {
        StringBuilder prefix = new StringBuilder();
        From<?, ?> from = root;
        while (fieldName.contains(".")) {
            String partial = fieldName.substring(0, fieldName.indexOf('.'));
            fieldName = fieldName.substring(partial.length() + 1);
            prefix.append(prefix.isEmpty() ? partial : "." + partial);
            String key = prefix.toString();
            Join<?, ?> join = joins.get(key);
            if (join == null) {
                join = from.join(partial, joinType);
                joins.put(key, join);
                pluralJoin |= join.getModel() instanceof PluralAttribute;
            }
            from = join;
        }
        return from.get(fieldName);
    }
}
