/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.tuscany.das.rdb.test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.tuscany.das.rdb.Command;
import org.apache.tuscany.das.rdb.DAS;
import org.apache.tuscany.das.rdb.test.customer.Customer;
import org.apache.tuscany.das.rdb.test.customer.CustomerFactory;
import org.apache.tuscany.das.rdb.test.data.CustomerData;
import org.apache.tuscany.das.rdb.test.framework.DasTest;
import org.apache.tuscany.sdo.util.SDOUtil;

import commonj.sdo.DataObject;

/**
 * Tests the Converter framwork
 */
public class ConverterTests extends DasTest {

    private static DateFormat myformat = new SimpleDateFormat("yyyy.MM.dd");

    private static Date kbday;

    private static Date tbday;

    protected void setUp() throws Exception {
        super.setUp();
        new CustomerData(getAutoConnection()).refresh();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    static {
        try {
            kbday = myformat.parse("1957.09.27");
            tbday = myformat.parse("1966.12.20");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This tests the use of an arbitrary converter. The column 
     * converted is a VARCAHAR. ResultSetShape is used to specify 
     * that the property will be a
     * SDODataTypes.DATE.
     * 
     * So this example uses a converter that transforms a string column 
     * into a date property and conversely, a date property back to a string for the
     * underlying column.
     * 
     * The converter returns 1957.09.27 if the column value is "Williams" 
     * and 1966.12.20 if the value is "Pavick"
     * 
     * On write, the converter returns "Pavick" if the property value is 
     * 1966.12.20 and "Williams" if the property value is 1957.09.27
     * 
     */
    public void testArbitraryConverter() throws Exception {
        DAS das = DAS.FACTORY.createDAS(getConfig("CustomerConfigWithConverter.xml"), getConnection());

        // Create and initialize command to read customers
        Command read = das.getCommand("testArbitraryConverter");

        // Read
        DataObject root = read.executeQuery();

        // Verify
        assertEquals(kbday, root.getDate("CUSTOMER[1]/LASTNAME"));

        // Modify
        root.setDate("CUSTOMER[1]/LASTNAME", tbday);

        das.applyChanges(root);

        // Read
        root = read.executeQuery();

        // Verify
        assertEquals(tbday, root.getDate("CUSTOMER[1]/LASTNAME"));

    }

    public void testInvalidConverter1() throws Exception {

        DAS das = DAS.FACTORY.createDAS(getConfig("InvalidConverter.xml"), getConnection());

        // Build the select command to read a specific customer and related
        // orders
        Command select = das.createCommand("SELECT * FROM CUSTOMER LEFT JOIN ANORDER "
                + "ON CUSTOMER.ID = ANORDER.CUSTOMER_ID where CUSTOMER.ID = ?");

        // Parameterize the command
        select.setParameter(1, Integer.valueOf(1));

        // Get the graph
        try {
            select.executeQuery();
        } catch (Exception ex) {
            assertEquals("java.lang.ClassNotFoundException: not.a.valid.class", ex.getMessage());
        }

    }

    public void testInvalidConverter2() throws Exception {

        SDOUtil.registerStaticTypes(CustomerFactory.class);
        DAS das = DAS.FACTORY.createDAS(getConfig("InvalidConverter.xml"), getConnection());

        // Build the select command to read a specific customer and related
        // orders
        Command select = das.createCommand("SELECT * FROM ANORDER");

        // Get the graph

        DataObject root = select.executeQuery();
        DataObject order = root.getDataObject("AnOrder[1]");

        Customer customer = (Customer) root.createDataObject("Customer");
        customer.setID(700);
        customer.setLastName("Daniel");
        customer.setAddress("an address");

        customer.getOrders().add(order);

        try {
            das.applyChanges(root);
        } catch (Exception ex) {
            assertEquals("java.lang.ClassNotFoundException: not.a.valid.class", ex.getMessage());
        }

    }

}
