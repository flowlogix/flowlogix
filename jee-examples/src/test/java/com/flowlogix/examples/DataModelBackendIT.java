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
package com.flowlogix.examples;

import com.flowlogix.demo.jeedao.NonDefault;
import com.flowlogix.demo.jeedao.entities.UserEntity;
import com.flowlogix.demo.jeedao.entities.UserEntity_;
import java.io.IOException;
import java.util.Map;
import com.flowlogix.demo.jeedao.primefaces.DataModelWrapper;
import com.flowlogix.demo.jeedao.primefaces.InjectedDataModel;
import com.flowlogix.jeedao.primefaces.JPALazyDataModel;
import com.flowlogix.test.PayaraServerLifecycle;
import com.flowlogix.util.SerializeTester;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.MatchMode;
import static com.flowlogix.examples.ExceptionPageIT.DEPLOYMENT_DEV_MODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

@PayaraServerLifecycle
class DataModelBackendIT {
    @Inject
    DataModelWrapper models;
    @Inject
    InjectedDataModel injectedModel;

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void basicDataModel() {
        basicDataModel(models.getBasic().getLazyModel());
    }

    @SuppressWarnings("MagicNumber")
    private void basicDataModel(JPALazyDataModel<UserEntity> model) {
        assertThat(model.count(Map.of())).isEqualTo(5);
        var rows = model.findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName()).filterValue("jprimak")
                        .build()), Map.of());
        assertThat(rows.size()).isEqualTo(1);
        assertThat(rows.get(0).getFullName()).isEqualTo("Lovely Lady");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void qualifiedDataModel() {
        doQualifiedDataModel(models.getQualified().getUserModel());
    }

    @SuppressWarnings("MagicNumber")
    void doQualifiedDataModel(JPALazyDataModel<UserEntity> model) {
        assertThat(model.count(Map.of())).isEqualTo(5);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void sortingDataModel() {
        doSortingDataModel(models.getSorting().getUserModel());
    }

    @SuppressWarnings("MagicNumber")
    void doSortingDataModel(JPALazyDataModel<UserEntity>  model) {
        var rows = model.findRows(0, 1, Map.of(), Map.of());
        assertThat(rows.get(0).getZipCode()).isEqualTo(10012);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void filteringDataModel() {
        doFilteringDataModel(models.getFiltering().getUserModel());
    }

    @SuppressWarnings("MagicNumber")
    void doFilteringDataModel(JPALazyDataModel<UserEntity>  model) {
        var rows = model.findRows(0, 10,
                Map.of(UserEntity_.zipCode.getName(), FilterMeta.builder().field(UserEntity_.zipCode.getName())
                        .filterValue(68501).build()), Map.of());
        assertThat(rows.size()).isEqualTo(4);
        assertThat(rows.get(0).getZipCode()).isEqualTo(68502);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    @SuppressWarnings("MagicNumber")
    void optimizedDataModel() {
        var rows = models.getOptimized().getUserModel().findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName()).filterValue("lprimak")
                        .build()), Map.of());
        assertThat(rows.size()).isEqualTo(1);
        assertThat(rows.get(0).getFullName()).isEqualTo("Lenny Primak");
        assertThat(rows.get(0).getAlternateEmails().size()).isEqualTo(2);
        assertThat(rows.get(0).getAlternateEmails().get(1).getEmail()).isEqualTo("two@two.com");
        assertThat(rows.get(0).getUserSettings().size()).isEqualTo(2);
        assertThat(rows.get(0).getUserSettings().get(0).getSettingName()).isEqualTo("LennySettingOne");
        assertThat(rows.get(0).getUserSettings().get(0).getSettingValue()).isEqualTo("Setting1Value");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    @SuppressWarnings("MagicNumber")
    void enrichedDataModel() {
        var rows = models.getEnriched().getUserModel().findRows(0, 3, Map.of(), Map.of());
        assertThat(rows.size()).isEqualTo(4);
        assertThat(rows.get(3).getFullName()).isEqualTo("Golden User");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    @SuppressWarnings("MagicNumber")
    void converterModel() {
        var rows = models.getConverter().getUserModel().findRows(0, 100, Map.of(), Map.of());
        String binaryKey = Long.toBinaryString(rows.get(3).getId());
        var entity = models.getConverter().getUserModel().getRowData(binaryKey);
        assertThat(entity).isEqualTo(rows.get(3));
        assertThat(models.getConverter().getUserModel().getRowKey(entity)).isEqualTo(binaryKey);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void basicInjectedModel() {
        basicDataModel(injectedModel.getInjectedModel());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void overriddenInjectedModel() {
        assertThat(injectedModel.getInjectedOverriddenModel().getEntityClass()).isEqualTo(UserEntity.class);
        assertThat(injectedModel.getInjectedOverriddenModel().isCaseSensitiveFilter()).isFalse();
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void caseInsensitiveInjectedModel() {
        assertThat(injectedModel.getInjectedCaseInsensitiveModel().isCaseSensitiveFilter()).isFalse();
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void nonDefaultInjectedModel() {
        assertThat(injectedModel.getInjectedNonDefaultModel().getEntityManagerQualifiers()
                .contains(NonDefault.class)).isTrue();
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void invalidInjectedModel() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> basicDataModel(models.getInvalid().getModel()));
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void directModel() {
        basicDataModel(models.getDirect().getUserModel());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void caseSensitiveFilter() {
        var rows = injectedModel.getInjectedModel().findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName()).filterValue("jpRimak")
                        .build()), Map.of());
        assertThat(rows.size()).isZero();
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void caseInsensitiveFilter() {
        var rows = injectedModel.getInjectedCaseInsensitiveModel().findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName()).filterValue("jpRimak")
                        .build()), Map.of());
        assertThat(rows.size()).isEqualTo(1);
        assertThat(rows.get(0).getFullName()).isEqualTo("Lovely Lady");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void caseInsensitiveUpperFilter() {
        var rows = injectedModel.getInjectedCaseInsensitiveUpperModel().findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName()).filterValue("jpRimak")
                        .build()), Map.of());
        assertThat(rows.size()).isEqualTo(1);
        assertThat(rows.get(0).getFullName()).isEqualTo("Lovely Lady");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void caseInsensitiveLowerFilter() {
        var rows = injectedModel.getInjectedCaseInsensitiveLowerModel().findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName()).filterValue("jpRimak")
                        .build()), Map.of());
        assertThat(rows.size()).isEqualTo(1);
        assertThat(rows.get(0).getFullName()).isEqualTo("Lovely Lady");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void wildcardFilter() {
        var rows = injectedModel.getInjectedWildcardModel().findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName())
                        .filterValue("jpr*a").build()), Map.of());
        assertThat(rows.size()).isEqualTo(1);
        assertThat(rows.get(0).getFullName()).isEqualTo("Lovely Lady");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void wildcardFilterExact() {
        var rows = injectedModel.getInjectedWildcardModel().findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName()).matchMode(MatchMode.EXACT)
                        .filterValue("jpr?mak").build()), Map.of());
        assertThat(rows.size()).isEqualTo(1);
        assertThat(rows.get(0).getFullName()).isEqualTo("Lovely Lady");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void wildcardFilterExactStar() {
        var rows = injectedModel.getInjectedWildcardModel().findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName()).matchMode(MatchMode.EXACT)
                        .filterValue("jpr*ak").build()), Map.of());
        assertThat(rows.size()).isEqualTo(1);
        assertThat(rows.get(0).getFullName()).isEqualTo("Lovely Lady");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void wildcardFilterNotFound() {
        var rows = injectedModel.getInjectedWildcardModel().findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName()).matchMode(MatchMode.EXACT)
                        .filterValue("pr?mak")
                        .build()), Map.of());
        assertThat(rows.size()).isZero();
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void wildcardEquals() {
        var rows = injectedModel.getInjectedWildcardModel().findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName()).matchMode(MatchMode.EQUALS)
                        .filterValue("jprimak").build()), Map.of());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getFullName()).isEqualTo("Lovely Lady");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void serialization() throws IOException, ClassNotFoundException {
        var qualified = SerializeTester.serializeAndDeserialize(models.getQualified().getUserModel());
        doQualifiedDataModel(qualified);

        var sorting = SerializeTester.serializeAndDeserialize(models.getSorting().getUserModel());
        doSortingDataModel(sorting);

        var filtering = SerializeTester.serializeAndDeserialize(models.getFiltering().getUserModel());
        doFilteringDataModel(filtering);
    }

    @Deployment(name = DEPLOYMENT_DEV_MODE)
    static WebArchive createDeployment() {
        return ExceptionPageIT.createDeploymentDev();
    }
}
