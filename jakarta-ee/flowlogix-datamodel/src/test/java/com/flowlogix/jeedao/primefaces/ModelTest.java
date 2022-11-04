/*
 * Copyright (C) 2011-2022 Flow Logix, Inc. All Rights Reserved.
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
package com.flowlogix.jeedao.primefaces;

import static com.flowlogix.jeedao.primefaces.JPALazyDataModel.RESULT;
import static com.flowlogix.jeedao.primefaces.JPALazyDataModel.replaceFilter;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.FilterMeta;

/**
 *
 * @author lprimak
 */
@ExtendWith(MockitoExtension.class)
public class ModelTest {
    @Mock
    EntityManager em;
    @Mock
    CriteriaBuilder cb;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Root<Object> root;

    @Test
    void resultField() {
        assertEquals(String.format("%s.hello", RESULT), JPALazyDataModel.getResultField("hello"));
    }

    @Test
    void simpleFilter() {
        var impl = JPAModelImpl.builder()
                .entityManagerSupplier(() -> em)
                .entityClass(Object.class)
                .converter(s -> new Object())
                .filter(ModelTest::filter)
                .build();
        var fm = new FilterMeta();
        when(root.get(any(String.class)).getJavaType()).thenAnswer((a) -> String.class);
        fm.setFilterValue("hello");
        impl.getFilters(Map.of("column", fm), cb, root);
    }

    private static void filter(Map<String, Filter.FilterData> filters, CriteriaBuilder cb, Root<Object> root) {
        replaceFilter(filters, "column",
                (Predicate predicate, String value) -> cb.greaterThan(root.get("column2"), value));
    }
}
