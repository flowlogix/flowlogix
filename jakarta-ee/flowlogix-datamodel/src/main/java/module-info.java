module flowlogix.datamodel {
    exports com.flowlogix.jeedao.primefaces;

    requires flowlogix.jee;
    requires lombok;
    requires org.slf4j;
    requires org.apache.commons.lang3;
    requires jakarta.persistence;
    requires jakarta.faces.api;
    requires jakarta.cdi;
    requires jakarta.transaction;
    requires jakarta.el;
    requires java.desktop;
    requires primefaces;
    requires omnifaces;
}
