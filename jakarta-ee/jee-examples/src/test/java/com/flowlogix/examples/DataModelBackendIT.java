/*
 * Copyright (C) 2011-2023 Flow Logix, Inc. All Rights Reserved.
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
import com.flowlogix.util.SerializeTester;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.primefaces.model.FilterMeta;
import static com.flowlogix.examples.ExceptionPageIT.DEPLOYMENT_DEV_MODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ArquillianExtension.class)
public class DataModelBackendIT {
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
        assertEquals(5, model.count(Map.of()));
        var rows = model.findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName()).filterValue("jprimak")
                        .build()), Map.of());
        assertEquals(1, rows.size());
        assertEquals("Lovely Lady", rows.get(0).getFullName());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void qualifiedDataModel() {
        doQualifiedDataModel(models.getQualified().getUserModel());
    }

    @SuppressWarnings("MagicNumber")
    void doQualifiedDataModel(JPALazyDataModel<UserEntity> model) {
        assertEquals(5, model.count(Map.of()));
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void sortingDataModel() {
        doSortingDataModel(models.getSorting().getUserModel());
    }

    @SuppressWarnings("MagicNumber")
    void doSortingDataModel(JPALazyDataModel<UserEntity>  model) {
        var rows = model.findRows(0, 1, Map.of(), Map.of());
        assertEquals(10012, rows.get(0).getZipCode());
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
        assertEquals(4, rows.size());
        assertEquals(68502, rows.get(0).getZipCode());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    @SuppressWarnings("MagicNumber")
    void optimizedDataModel() {
        var rows = models.getOptimized().getUserModel().findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName()).filterValue("lprimak")
                        .build()), Map.of());
        assertEquals(1, rows.size());
        assertEquals("Lenny Primak", rows.get(0).getFullName());
        assertEquals(2, rows.get(0).getAlternateEmails().size());
        assertEquals("two@two.com", rows.get(0).getAlternateEmails().get(1).getEmail());
        assertEquals(2, rows.get(0).getUserSettings().size());
        assertEquals("LennySettingOne", rows.get(0).getUserSettings().get(0).getSettingName());
        assertEquals("Setting1Value", rows.get(0).getUserSettings().get(0).getSettingValue());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    @SuppressWarnings("MagicNumber")
    void converterModel() {
        var rows = models.getConverter().getUserModel().findRows(0, 100, Map.of(), Map.of());
        String binaryKey = Long.toBinaryString(rows.get(3).getId());
        var entity = models.getConverter().getUserModel().getRowData(binaryKey);
        assertEquals(rows.get(3), entity);
        assertEquals(binaryKey, models.getConverter().getUserModel().getRowKey(entity));
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void basicInjectedModel() {
        basicDataModel(injectedModel.getInjectedModel());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void overriddenInjectedModel() {
        assertEquals(UserEntity.class, injectedModel.getInjectedOverriddenModel().getEntityClass());
        assertFalse(injectedModel.getInjectedOverriddenModel().isCaseSensitiveFilter());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void caseInsensitiveInjectedModel() {
        assertFalse(injectedModel.getInjectedCaseInsensitiveModel().isCaseSensitiveFilter());
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void nonDefaultInjectedModel() {
        assertTrue(injectedModel.getInjectedNonDefaultModel().getEntityManagerQualifiers().contains(NonDefault.class));
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void invalidInjectedModel() {
        assertThrows(IllegalArgumentException.class, () -> basicDataModel(models.getInvalid().getModel()));
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
        assertEquals(0, rows.size());

    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void caseInSensitiveFilter() {
        var rows = injectedModel.getInjectedCaseInsensitiveModel().findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName()).filterValue("jpRimak")
                        .build()), Map.of());
        assertEquals(1, rows.size());
        assertEquals("Lovely Lady", rows.get(0).getFullName());

    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void caseInSensitiveUpperFilter() {
        var rows = injectedModel.getInjectedCaseInsensitiveUpperModel().findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName()).filterValue("jpRimak")
                        .build()), Map.of());
        assertEquals(1, rows.size());
        assertEquals("Lovely Lady", rows.get(0).getFullName());

    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_DEV_MODE)
    void caseInSensitiveLowerFilter() {
        var rows = injectedModel.getInjectedCaseInsensitiveLowerModel().findRows(0, 3,
                Map.of(UserEntity_.userId.getName(), FilterMeta.builder()
                        .field(UserEntity_.userId.getName()).filterValue("jpRimak")
                        .build()), Map.of());
        assertEquals(1, rows.size());
        assertEquals("Lovely Lady", rows.get(0).getFullName());

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
    public static WebArchive createDeployment() {
        return ExceptionPageIT.createDeploymentDev();
    }
}
