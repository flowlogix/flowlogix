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
package com.flowlogix.demo.jeedao.primefaces;

import com.flowlogix.demo.jeedao.entities.UserEntity;
import com.flowlogix.demo.viewscoped.ViewScoped;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import lombok.Getter;

/*
// @start region="basicUsageHtml"
// tag::basicUsageHtml[] // @replace regex='.*\n' replacement=""
<p:dataTable lazy="true" value="#{userViewer.lazyModel}" var="user">
    ... specify columns as usual ...
</p:dataTable>
// end::basicUsageHtml[] // @replace regex='.*\n' replacement=""
// @end
*/
// @start region="basicUsage"
// tag::basicUsage[] // @replace regex='.*\n' replacement=""
@Named
@ViewScoped
@SuppressWarnings("TrailingComment") // @replace regex='.*\n' replacement=""
public class BasicDataModel implements Serializable { // @replace regex='BasicDataModel' replacement="UserViewer"
    @Inject
    @Getter
    JPALazyDataModel<UserEntity> lazyModel;
}
// end::basicUsage[] // @replace regex='.*\n' replacement=""
// @end
