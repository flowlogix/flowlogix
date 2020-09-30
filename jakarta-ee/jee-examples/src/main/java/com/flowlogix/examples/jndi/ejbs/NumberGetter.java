/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.flowlogix.examples.jndi.ejbs;

import javax.ejb.ConcurrencyManagement;
import static javax.ejb.ConcurrencyManagementType.BEAN;
import javax.ejb.Singleton;

/**
 *
 * @author lprimak
 */
@Singleton @ConcurrencyManagement(BEAN)
public class NumberGetter {
    public int getNumber() {
        return 5;
    }
}
