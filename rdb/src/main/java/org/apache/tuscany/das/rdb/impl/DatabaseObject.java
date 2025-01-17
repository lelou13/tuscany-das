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
package org.apache.tuscany.das.rdb.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tuscany.das.rdb.config.Column;
import org.apache.tuscany.das.rdb.config.Config;
import org.apache.tuscany.das.rdb.config.KeyPair;
import org.apache.tuscany.das.rdb.config.Relationship;
import org.apache.tuscany.das.rdb.config.Table;
import org.apache.tuscany.das.rdb.config.wrapper.MappingWrapper;
import org.apache.tuscany.das.rdb.util.LoggerFactory;

import commonj.sdo.DataObject;
import commonj.sdo.Property;

/**
 * DatabaseObject wraps DataObject. If a field is an FK field, it will return the value from the parent.
 * 
 * 
 */
public class DatabaseObject {

    private final Logger logger = LoggerFactory.INSTANCE.getLogger(DatabaseObject.class);

    private final MappingWrapper mappingWrapper;

    private final DataObject dataObject;

    private Property parentReference;

    private Map keyMappings = new HashMap();

    public DatabaseObject(Config model, DataObject changedObject) {
        this.mappingWrapper = new MappingWrapper(model);
        this.dataObject = changedObject;
        initialize();
    }

    // Initialize Key Mappings
    private void initialize() {
        if (mappingWrapper.getConfig() != null) {
            List relationships = mappingWrapper.getConfig().getRelationship();
            Iterator i = relationships.iterator();
            while (i.hasNext()) {
                Relationship r = (Relationship) i.next();
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Initializing relationship: " + r.getName());
                }
                if (r.getForeignKeyTable().equals(getTypeName())) {
                    List pairs = r.getKeyPair();
                    Iterator iter = pairs.iterator();
                    while (iter.hasNext()) {
                        KeyPair pair = (KeyPair) iter.next();
                        keyMappings.put(pair.getForeignKeyColumn(), r);
                    }
                }
            }
        }
    }

    public Object get(String parameter) {

        if (isPartOfPrimaryKey(parameter)) {
            return dataObject.get(parameter);
        }

        Relationship r = (Relationship) keyMappings.get(parameter);
        if (r == null) {
            return dataObject.get(parameter);
        }

        Property parentRef = getParentReference(r.getPrimaryKeyTable());
        DataObject parent = dataObject.getDataObject(parentRef);
        if (parent == null) {
            return null;
        }
        String parentKey = getParentKey(r, parameter);
        return parent.get(parentKey);

    }

    private String getParentKey(Relationship r, String parameter) {
        List keyPairs = r.getKeyPair();
        Iterator i = keyPairs.iterator();
        while (i.hasNext()) {
            KeyPair pair = (KeyPair) i.next();
            if (pair.getForeignKeyColumn().equals(parameter)) {
                return pair.getPrimaryKeyColumn();
            }
        }
        return null;
    }

    public Property getParentReference(String parentName) {
        if (this.parentReference == null) {

            Iterator i = dataObject.getType().getProperties().iterator();
            while (i.hasNext()) {
                Property ref = (Property) i.next();
                if ((!ref.getType().isDataType()) && (ref.getType().getName().equals(parentName))) {
                    this.parentReference = ref;
                }
            }
        }
        return this.parentReference;
    }

    public String getTableName() {
        if (mappingWrapper.getConfig() != null) {
            return mappingWrapper.getTableByTypeName(getTypeName()).getTableName();
        } 
        return null;       
    }

    public String getTypeName() {
        return dataObject.getType().getName();
    }

    public void setPropagatedID(String propagatedID, int id) {
        dataObject.setInt(propagatedID, id);
    }

    private boolean isPartOfPrimaryKey(String parameter) {
        if (mappingWrapper.getConfig() == null) {
            return false;
        }
        
        Table t = mappingWrapper.getTable(getTableName());
        if (t == null) {
            return false;
        }
        Column c = mappingWrapper.getColumnByPropertyName(t, parameter);
        if (c == null) {
            return false;
        }
        
        return c.isPrimaryKey();
    }

}
