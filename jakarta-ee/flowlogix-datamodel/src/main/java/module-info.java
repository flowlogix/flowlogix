/**
 * PrimeFaces JPA-backed {@link com.flowlogix.jeedao.primefaces.JPALazyDataModel}
 */
module com.flowlogix.datamodel {
    exports com.flowlogix.jeedao.primefaces;

    requires com.flowlogix.jee;
    requires com.flowlogix.jee.demo;
    requires lombok;
    requires org.slf4j;
    requires org.apache.commons.lang3;
    requires org.omnifaces;
    requires jakarta.persistence;
    requires jakarta.faces.api;
    requires jakarta.cdi;
    requires jakarta.transaction;
    requires jakarta.el;
    requires java.desktop;
    requires org.primefaces;
}
