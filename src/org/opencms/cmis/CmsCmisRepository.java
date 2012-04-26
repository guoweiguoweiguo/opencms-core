/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.cmis;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.CmsUser;
import org.opencms.file.CmsVfsResourceNotFoundException;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.loader.CmsResourceManager;
import org.opencms.lock.CmsLock;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.security.CmsPermissionSet;
import org.opencms.security.CmsSecurityException;
import org.opencms.util.CmsFileUtil;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ExtensionsData;
import org.apache.chemistry.opencmis.commons.data.FailedToDeleteData;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderContainer;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.ObjectParentData;
import org.apache.chemistry.opencmis.commons.data.PermissionMapping;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.PermissionDefinition;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinition;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionContainer;
import org.apache.chemistry.opencmis.commons.definitions.TypeDefinitionList;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.Action;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.CapabilityAcl;
import org.apache.chemistry.opencmis.commons.enums.CapabilityChanges;
import org.apache.chemistry.opencmis.commons.enums.CapabilityContentStreamUpdates;
import org.apache.chemistry.opencmis.commons.enums.CapabilityJoin;
import org.apache.chemistry.opencmis.commons.enums.CapabilityQuery;
import org.apache.chemistry.opencmis.commons.enums.CapabilityRenditions;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.SupportedPermissions;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisStreamNotSupportedException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisUnauthorizedException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AccessControlListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AclCapabilitiesDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.AllowableActionsImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderContainerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderListImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectParentDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PermissionDefinitionDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PermissionMappingDataImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyBooleanImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDateTimeImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyDecimalImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyHtmlImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIdImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyIntegerImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyStringImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.PropertyUriImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryCapabilitiesImpl;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.RepositoryInfoImpl;
import org.apache.chemistry.opencmis.commons.impl.server.ObjectInfoImpl;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.ObjectInfo;
import org.apache.chemistry.opencmis.commons.server.ObjectInfoHandler;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.apache.chemistry.opencmis.fileshare.TypeManager;

public class CmsCmisRepository {

    private String m_id;
    private CmsResource m_root;
    private CmsCmisTypeManager m_typeManager;
    private CmsObject m_adminCms;

    public String getId() {

        return m_id;
    }

    public CmsCmisRepository(CmsObject adminCms, CmsResource root, String id)
    throws CmsException {

        m_adminCms = adminCms;
        m_id = id;
        m_root = root;
        m_typeManager = new CmsCmisTypeManager(adminCms);
    }

    protected CmsObject getCmsObject(CallContext context) throws CmsException {

        if (context.getUsername() == null) {
            // user name can be null 
            CmsObject cms = OpenCms.initCmsObject(OpenCms.getDefaultUsers().getUserGuest());
            cms.getRequestContext().setCurrentProject(m_adminCms.getRequestContext().getCurrentProject());
            return cms;
        } else {
            CmsObject cms = OpenCms.initCmsObject(m_adminCms);
            CmsProject projectBeforeLogin = cms.getRequestContext().getCurrentProject();
            cms.loginUser(context.getUsername(), context.getPassword());
            cms.getRequestContext().setCurrentProject(projectBeforeLogin);
            return cms;
        }
    }

    private static final String CMIS_READ = "cmis:read";
    private static final String CMIS_WRITE = "cmis:write";
    private static final String CMIS_ALL = "cmis:all";

    /**
     * Compiles an object type object from a file or folder.�
     */
    private ObjectData compileObjectType(
        CallContext context,
        CmsResource resource,
        Set<String> filter,
        boolean includeAllowableActions,
        boolean includeAcl,
        ObjectInfoHandler objectInfos) {

        ObjectDataImpl result = new ObjectDataImpl();
        ObjectInfoImpl objectInfo = new ObjectInfoImpl();

        result.setProperties(compileProperties(context, resource, filter, objectInfo));

        if (includeAllowableActions) {
            result.setAllowableActions(compileAllowableActions(context, resource));
        }

        if (includeAcl) {
            result.setAcl(compileAcl(context, resource));
            result.setIsExactAcl(true);
        }

        if (context.isObjectInfoRequired()) {
            objectInfo.setObject(result);
            objectInfos.addObjectInfo(objectInfo);
        }
        return result;
    }

    /**
     * Gathers all base properties of a file or folder.
     */
    private Properties compileProperties(
        CallContext context,
        CmsResource file,
        Set<String> orgfilter,
        ObjectInfoImpl objectInfo) {

        if (file == null) {
            throw new IllegalArgumentException("Resource may not be null.");
        }

        // copy filter
        Set<String> filter = (orgfilter == null ? null : new HashSet<String>(orgfilter));

        // find base type
        String typeId = null;

        if (file.isFolder()) {
            typeId = CmsCmisTypeManager.FOLDER_TYPE_ID;
            objectInfo.setBaseType(BaseTypeId.CMIS_FOLDER);
            objectInfo.setTypeId(typeId);
            objectInfo.setContentType(null);
            objectInfo.setFileName(null);
            objectInfo.setHasAcl(false);
            objectInfo.setHasContent(false);
            objectInfo.setVersionSeriesId(null);
            objectInfo.setIsCurrentVersion(true);
            objectInfo.setRelationshipSourceIds(null);
            objectInfo.setRelationshipTargetIds(null);
            objectInfo.setRenditionInfos(null);
            objectInfo.setSupportsDescendants(true);
            objectInfo.setSupportsFolderTree(true);
            objectInfo.setSupportsPolicies(false);
            objectInfo.setSupportsRelationships(false);
            objectInfo.setWorkingCopyId(null);
            objectInfo.setWorkingCopyOriginalId(null);
        } else {
            typeId = CmsCmisTypeManager.DOCUMENT_TYPE_ID;
            objectInfo.setBaseType(BaseTypeId.CMIS_DOCUMENT);
            objectInfo.setTypeId(typeId);
            objectInfo.setHasAcl(false);
            objectInfo.setHasContent(true);
            objectInfo.setHasParent(true);
            objectInfo.setVersionSeriesId(null);
            objectInfo.setIsCurrentVersion(true);
            objectInfo.setRelationshipSourceIds(null);
            objectInfo.setRelationshipTargetIds(null);
            objectInfo.setRenditionInfos(null);
            objectInfo.setSupportsDescendants(false);
            objectInfo.setSupportsFolderTree(false);
            objectInfo.setSupportsPolicies(false);
            objectInfo.setSupportsRelationships(false);
            objectInfo.setWorkingCopyId(null);
            objectInfo.setWorkingCopyOriginalId(null);
        }

        // let's do it
        try {
            CmsObject cms = getCmsObject(context);
            PropertiesImpl result = new PropertiesImpl();

            // id
            String id = file.getStructureId().toString();
            addPropertyId(result, typeId, filter, PropertyIds.OBJECT_ID, id);
            objectInfo.setId(id);

            // name
            String name = file.getName();
            addPropertyString(result, typeId, filter, PropertyIds.NAME, name);
            objectInfo.setName(name);

            // created and modified by
            CmsUUID creatorId = file.getUserCreated();
            CmsUUID modifierId = file.getUserLastModified();
            String creatorName = creatorId.toString();
            String modifierName = modifierId.toString();
            try {
                CmsUser user = cms.readUser(creatorId);
                creatorName = user.getName();
            } catch (CmsException e) {
                // ignore, use id as name 
            }
            try {
                CmsUser user = cms.readUser(modifierId);
                modifierName = user.getName();
            } catch (CmsException e) {
                // ignore, use id as name
            }

            addPropertyString(result, typeId, filter, PropertyIds.CREATED_BY, creatorName);
            addPropertyString(result, typeId, filter, PropertyIds.LAST_MODIFIED_BY, modifierName);
            objectInfo.setCreatedBy(creatorName);

            // creation and modification date
            GregorianCalendar lastModified = millisToCalendar(file.getDateLastModified());
            GregorianCalendar created = millisToCalendar(file.getDateCreated());

            addPropertyDateTime(result, typeId, filter, PropertyIds.CREATION_DATE, created);
            addPropertyDateTime(result, typeId, filter, PropertyIds.LAST_MODIFICATION_DATE, lastModified);
            objectInfo.setCreationDate(created);
            objectInfo.setLastModificationDate(lastModified);

            // change token - always null
            addPropertyString(result, typeId, filter, PropertyIds.CHANGE_TOKEN, null);

            // directory or file
            if (file.isFolder()) {
                // base type and type name
                addPropertyId(result, typeId, filter, PropertyIds.BASE_TYPE_ID, BaseTypeId.CMIS_FOLDER.value());
                addPropertyId(result, typeId, filter, PropertyIds.OBJECT_TYPE_ID, TypeManager.FOLDER_TYPE_ID);
                String path = file.getRootPath();
                addPropertyString(result, typeId, filter, PropertyIds.PATH, (path.length() == 0 ? "/" : path));

                // folder properties
                if (!m_root.equals(file)) {
                    CmsResource parent = cms.readParentFolder(file.getStructureId());
                    addPropertyId(result, typeId, filter, PropertyIds.PARENT_ID, (m_root.equals(parent)
                    ? m_root.getStructureId().toString()
                    : parent.getStructureId().toString()));
                    objectInfo.setHasParent(true);
                } else {
                    addPropertyId(result, typeId, filter, PropertyIds.PARENT_ID, null);
                    objectInfo.setHasParent(false);
                }

                addPropertyIdList(result, typeId, filter, PropertyIds.ALLOWED_CHILD_OBJECT_TYPE_IDS, null);
            } else {
                // base type and type name
                addPropertyId(result, typeId, filter, PropertyIds.BASE_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
                addPropertyId(result, typeId, filter, PropertyIds.OBJECT_TYPE_ID, TypeManager.DOCUMENT_TYPE_ID);

                // file properties
                addPropertyBoolean(result, typeId, filter, PropertyIds.IS_IMMUTABLE, false);
                addPropertyBoolean(result, typeId, filter, PropertyIds.IS_LATEST_VERSION, true);
                addPropertyBoolean(result, typeId, filter, PropertyIds.IS_MAJOR_VERSION, true);
                addPropertyBoolean(result, typeId, filter, PropertyIds.IS_LATEST_MAJOR_VERSION, true);
                addPropertyString(result, typeId, filter, PropertyIds.VERSION_LABEL, file.getName());
                addPropertyId(result, typeId, filter, PropertyIds.VERSION_SERIES_ID, file.getStructureId().toString());
                addPropertyBoolean(result, typeId, filter, PropertyIds.IS_VERSION_SERIES_CHECKED_OUT, false);
                addPropertyString(result, typeId, filter, PropertyIds.VERSION_SERIES_CHECKED_OUT_BY, null);
                addPropertyString(result, typeId, filter, PropertyIds.VERSION_SERIES_CHECKED_OUT_ID, null);
                addPropertyString(result, typeId, filter, PropertyIds.CHECKIN_COMMENT, "");

                if (file.getLength() == 0) {
                    addPropertyBigInteger(result, typeId, filter, PropertyIds.CONTENT_STREAM_LENGTH, null);
                    addPropertyString(result, typeId, filter, PropertyIds.CONTENT_STREAM_MIME_TYPE, null);
                    addPropertyString(result, typeId, filter, PropertyIds.CONTENT_STREAM_FILE_NAME, null);

                    objectInfo.setHasContent(false);
                    objectInfo.setContentType(null);
                    objectInfo.setFileName(null);
                } else {
                    addPropertyInteger(result, typeId, filter, PropertyIds.CONTENT_STREAM_LENGTH, file.getLength());
                    addPropertyString(
                        result,
                        typeId,
                        filter,
                        PropertyIds.CONTENT_STREAM_MIME_TYPE,
                        OpenCms.getResourceManager().getMimeType(
                            file.getRootPath(),
                            null,
                            CmsResourceManager.MIMETYPE_TEXT));
                    addPropertyString(result, typeId, filter, PropertyIds.CONTENT_STREAM_FILE_NAME, file.getName());

                    objectInfo.setHasContent(true);
                    objectInfo.setContentType(OpenCms.getResourceManager().getMimeType(
                        file.getRootPath(),
                        null,
                        CmsResourceManager.MIMETYPE_TEXT));
                    objectInfo.setFileName(file.getName());
                }

                addPropertyId(result, typeId, filter, PropertyIds.CONTENT_STREAM_ID, null);
            }

            List<CmsProperty> props = cms.readPropertyObjects(file, true);
            Set<String> propertiesToAdd = new HashSet<String>(m_typeManager.getCmsPropertyNames());
            for (CmsProperty prop : props) {
                addPropertyString(
                    result,
                    typeId,
                    filter,
                    CmsCmisTypeManager.PROPERTY_PREFIX + prop.getName(),
                    prop.getValue());
                propertiesToAdd.remove(prop.getName());
            }
            for (String propName : propertiesToAdd) {
                addPropertyString(result, typeId, filter, CmsCmisTypeManager.PROPERTY_PREFIX + propName, null);
            }
            I_CmsResourceType resType = OpenCms.getResourceManager().getResourceType(file);
            addPropertyString(result, typeId, filter, CmsCmisTypeManager.PROPERTY_RESOURCE_TYPE, resType.getTypeName());
            return result;
        } catch (Exception e) {
            if (e instanceof CmisBaseException) {
                throw (CmisBaseException)e;
            }
            throw new CmisRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Converts milliseconds into a calendar object.
     */
    private static GregorianCalendar millisToCalendar(long millis) {

        GregorianCalendar result = new GregorianCalendar();
        result.setTimeZone(TimeZone.getTimeZone("GMT"));
        result.setTimeInMillis((long)(Math.ceil(millis / 1000) * 1000));
        return result;
    }

    /**
     * Compiles the allowable actions for a file or folder.
     */
    private AllowableActions compileAllowableActions(CallContext context, CmsResource file) {

        try {
            CmsObject cms = getCmsObject(context);

            if (file == null) {
                throw new IllegalArgumentException("File must not be null!");
            }
            CmsLock lock = cms.getLock(file);
            CmsUser user = cms.getRequestContext().getCurrentUser();
            boolean canWrite = !cms.getRequestContext().getCurrentProject().isOnlineProject()
                && (lock.isOwnedBy(user) || lock.isLockableBy(user))
                && cms.hasPermissions(file, CmsPermissionSet.ACCESS_WRITE, false, CmsResourceFilter.DEFAULT);
            boolean isReadOnly = !canWrite;
            boolean isFolder = file.isFolder();
            boolean isRoot = m_root.equals(file);

            Set<Action> aas = new HashSet<Action>();

            addAction(aas, Action.CAN_GET_OBJECT_PARENTS, !isRoot);
            addAction(aas, Action.CAN_GET_PROPERTIES, true);
            addAction(aas, Action.CAN_UPDATE_PROPERTIES, !isReadOnly);
            addAction(aas, Action.CAN_MOVE_OBJECT, !isReadOnly);
            addAction(aas, Action.CAN_DELETE_OBJECT, !isReadOnly && !isRoot);
            addAction(aas, Action.CAN_GET_ACL, true);

            if (isFolder) {
                addAction(aas, Action.CAN_GET_DESCENDANTS, true);
                addAction(aas, Action.CAN_GET_CHILDREN, true);
                addAction(aas, Action.CAN_GET_FOLDER_PARENT, !isRoot);
                addAction(aas, Action.CAN_GET_FOLDER_TREE, true);
                addAction(aas, Action.CAN_CREATE_DOCUMENT, !isReadOnly);
                addAction(aas, Action.CAN_CREATE_FOLDER, !isReadOnly);
                addAction(aas, Action.CAN_DELETE_TREE, !isReadOnly);
            } else {
                addAction(aas, Action.CAN_GET_CONTENT_STREAM, true);
                addAction(aas, Action.CAN_SET_CONTENT_STREAM, !isReadOnly);
                addAction(aas, Action.CAN_GET_ALL_VERSIONS, true);
            }

            AllowableActionsImpl result = new AllowableActionsImpl();
            result.setAllowableActions(aas);

            return result;
        } catch (CmsException e) {
            handleCmsException(e);
            return null;
        }
    }

    /**
     * Wrap OpenCms into OpenCMIS exceptions and rethrow them.<p>
     * 
     * @param e the exception to handle
     */
    protected void handleCmsException(CmsException e) {

        if (e instanceof CmsVfsResourceNotFoundException) {
            throw new CmisObjectNotFoundException(e.getLocalizedMessage(), e);
        } else if (e instanceof CmsSecurityException) {
            throw new CmisUnauthorizedException(e.getLocalizedMessage(), e);
        } else {
            throw new CmisRuntimeException(e.getLocalizedMessage(), e);
        }
    }

    private void addPropertyId(PropertiesImpl props, String typeId, Set<String> filter, String id, String value) {

        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyIdImpl(id, value));
    }

    private void addPropertyIdList(
        PropertiesImpl props,
        String typeId,
        Set<String> filter,
        String id,
        List<String> value) {

        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyIdImpl(id, value));
    }

    /**
     * Adds a string property to a PropertiesImpl.<p>
     *  
     * @param props the properties 
     * @param typeId the type id 
     * @param filter the property filter string 
     * @param id the property id 
     * @param value the property value 
     */
    private void addPropertyString(PropertiesImpl props, String typeId, Set<String> filter, String id, String value) {

        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyStringImpl(id, value));
    }

    /**
     * Adds an integer property to a PropertiesImpl.<p>
     *  
     * @param props the properties 
     * @param typeId the type id 
     * @param filter the property filter string 
     * @param id the property id 
     * @param value the property value 
     */
    private void addPropertyInteger(PropertiesImpl props, String typeId, Set<String> filter, String id, long value) {

        addPropertyBigInteger(props, typeId, filter, id, BigInteger.valueOf(value));
    }

    /**
     * Adds bigint property to a PropertiesImpl.<p>
     *  
     * @param props the properties 
     * @param typeId the type id 
     * @param filter the property filter string 
     * @param id the property id 
     * @param value the property value 
     */
    private void addPropertyBigInteger(
        PropertiesImpl props,
        String typeId,
        Set<String> filter,
        String id,
        BigInteger value) {

        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyIntegerImpl(id, value));
    }

    /**
     * Adds a boolean property to a PropertiesImpl.<p>
     *  
     * @param props the properties 
     * @param typeId the type id 
     * @param filter the property filter string 
     * @param id the property id 
     * @param value the property value 
     */
    private void addPropertyBoolean(PropertiesImpl props, String typeId, Set<String> filter, String id, boolean value) {

        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyBooleanImpl(id, value));
    }

    /**
     * Adds a date/time property to a PropertiesImpl.<p>
     *  
     * @param props the properties 
     * @param typeId the type id 
     * @param filter the property filter string 
     * @param id the property id 
     * @param value the property value 
     */
    private void addPropertyDateTime(
        PropertiesImpl props,
        String typeId,
        Set<String> filter,
        String id,
        GregorianCalendar value) {

        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }

        props.addProperty(new PropertyDateTimeImpl(id, value));
    }

    /**
     * Checks whether a property can be added to a Properties.
     *  
     * @param properties the properties object
     * @param typeId the type id 
     * @param filter the property filter
     * @param id the property id
     *  
     * @return true if the property should be added 
     */
    private boolean checkAddProperty(Properties properties, String typeId, Set<String> filter, String id) {

        if ((properties == null) || (properties.getProperties() == null)) {
            throw new IllegalArgumentException("Properties must not be null!");
        }

        if (id == null) {
            throw new IllegalArgumentException("Id must not be null!");
        }

        TypeDefinition type = m_typeManager.getType(typeId);
        if (type == null) {
            throw new IllegalArgumentException("Unknown type: " + typeId);
        }
        if (!type.getPropertyDefinitions().containsKey(id)) {
            throw new IllegalArgumentException("Unknown property: " + id);
        }

        String queryName = type.getPropertyDefinitions().get(id).getQueryName();

        if ((queryName != null) && (filter != null)) {
            if (!filter.contains(queryName)) {
                return false;
            } else {
                filter.remove(queryName);
            }
        }

        return true;
    }

    /**
     * Adds the default value of property if defined.
     *  
     * @param props the Properties object
     * @param propDef the property definition
     *  
     * @return
     */
    @SuppressWarnings("unchecked")
    private static boolean addPropertyDefault(PropertiesImpl props, PropertyDefinition<?> propDef) {

        if ((props == null) || (props.getProperties() == null)) {
            throw new IllegalArgumentException("Props must not be null!");
        }

        if (propDef == null) {
            return false;
        }

        List<?> defaultValue = propDef.getDefaultValue();
        if ((defaultValue != null) && (!defaultValue.isEmpty())) {
            switch (propDef.getPropertyType()) {
                case BOOLEAN:
                    props.addProperty(new PropertyBooleanImpl(propDef.getId(), (List<Boolean>)defaultValue));
                    break;
                case DATETIME:
                    props.addProperty(new PropertyDateTimeImpl(propDef.getId(), (List<GregorianCalendar>)defaultValue));
                    break;
                case DECIMAL:
                    props.addProperty(new PropertyDecimalImpl(propDef.getId(), (List<BigDecimal>)defaultValue));
                    break;
                case HTML:
                    props.addProperty(new PropertyHtmlImpl(propDef.getId(), (List<String>)defaultValue));
                    break;
                case ID:
                    props.addProperty(new PropertyIdImpl(propDef.getId(), (List<String>)defaultValue));
                    break;
                case INTEGER:
                    props.addProperty(new PropertyIntegerImpl(propDef.getId(), (List<BigInteger>)defaultValue));
                    break;
                case STRING:
                    props.addProperty(new PropertyStringImpl(propDef.getId(), (List<String>)defaultValue));
                    break;
                case URI:
                    props.addProperty(new PropertyUriImpl(propDef.getId(), (List<String>)defaultValue));
                    break;
                default:
                    throw new RuntimeException("Unknown datatype! Spec change?");
            }

            return true;
        }

        return false;
    }

    private static void addAction(Set<Action> aas, Action action, boolean condition) {

        if (condition) {
            aas.add(action);
        }
    }

    /**
     * Splits a filter statement into a collection of properties. If
     * <code>filter</code> is <code>null</code>, empty or one of the properties
     * is '*' , an empty collection will be returned.
     */
    private static Set<String> splitFilter(String filter) {

        if (filter == null) {
            return null;
        }

        if (filter.trim().length() == 0) {
            return null;
        }

        Set<String> result = new HashSet<String>();
        for (String s : filter.split(",")) {
            s = s.trim();
            if (s.equals("*")) {
                return null;
            } else if (s.length() > 0) {
                result.add(s);
            }
        }

        // set a few base properties
        // query name == id (for base type properties)
        result.add(PropertyIds.OBJECT_ID);
        result.add(PropertyIds.OBJECT_TYPE_ID);
        result.add(PropertyIds.BASE_TYPE_ID);

        return result;
    }

    /**
     * Compiles the ACL for a file or folder.
     */
    private Acl compileAcl(CallContext context, CmsResource file) {

        try {
            CmsObject cms = getCmsObject(context);

            AccessControlListImpl result = new AccessControlListImpl();
            result.setAces(new ArrayList<Ace>());

            return result;
        } catch (CmsException e) {
            handleCmsException(e);
            return null;
        }
    }

    private static PermissionDefinition createPermission(String permission, String description) {

        PermissionDefinitionDataImpl pd = new PermissionDefinitionDataImpl();
        pd.setPermission(permission);
        pd.setDescription(description);

        return pd;
    }

    private static PermissionMapping createMapping(String key, String permission) {

        PermissionMappingDataImpl pm = new PermissionMappingDataImpl();
        pm.setKey(key);
        pm.setPermissions(Collections.singletonList(permission));

        return pm;
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.RepositoryService#getRepositoryInfo(java.lang.String, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public RepositoryInfo getRepositoryInfo(ExtensionsData extension) {

        // compile repository info
        RepositoryInfoImpl repositoryInfo = new RepositoryInfoImpl();

        repositoryInfo.setId(m_id);
        repositoryInfo.setName(m_id);
        repositoryInfo.setDescription(m_id);

        repositoryInfo.setCmisVersionSupported("1.0");

        repositoryInfo.setProductName("OpenCms");
        repositoryInfo.setProductVersion(OpenCms.getSystemInfo().getVersion());
        repositoryInfo.setVendorName("Alkacon Software GmbH");

        repositoryInfo.setRootFolder(m_root.getStructureId().toString());

        repositoryInfo.setThinClientUri("");

        RepositoryCapabilitiesImpl capabilities = new RepositoryCapabilitiesImpl();
        capabilities.setCapabilityAcl(CapabilityAcl.NONE);
        capabilities.setAllVersionsSearchable(Boolean.FALSE);
        capabilities.setCapabilityJoin(CapabilityJoin.NONE);
        capabilities.setSupportsMultifiling(Boolean.FALSE);
        capabilities.setSupportsUnfiling(Boolean.FALSE);
        capabilities.setSupportsVersionSpecificFiling(Boolean.FALSE);
        capabilities.setIsPwcSearchable(Boolean.FALSE);
        capabilities.setIsPwcUpdatable(Boolean.FALSE);
        capabilities.setCapabilityQuery(CapabilityQuery.NONE);
        capabilities.setCapabilityChanges(CapabilityChanges.NONE);
        //capabilities.setCapabilityContentStreamUpdates(CapabilityContentStreamUpdates.ANYTIME);
        capabilities.setCapabilityContentStreamUpdates(CapabilityContentStreamUpdates.ANYTIME);

        capabilities.setSupportsGetDescendants(Boolean.TRUE);
        capabilities.setSupportsGetFolderTree(Boolean.TRUE);
        capabilities.setCapabilityRendition(CapabilityRenditions.NONE);

        repositoryInfo.setCapabilities(capabilities);

        AclCapabilitiesDataImpl aclCapability = new AclCapabilitiesDataImpl();
        aclCapability.setSupportedPermissions(SupportedPermissions.BASIC);
        aclCapability.setAclPropagation(AclPropagation.OBJECTONLY);

        // permissions
        List<PermissionDefinition> permissions = new ArrayList<PermissionDefinition>();
        permissions.add(createPermission(CMIS_READ, "Read"));
        permissions.add(createPermission(CMIS_WRITE, "Write"));
        permissions.add(createPermission(CMIS_ALL, "All"));
        aclCapability.setPermissionDefinitionData(permissions);

        // mapping
        List<PermissionMapping> list = new ArrayList<PermissionMapping>();
        list.add(createMapping(PermissionMapping.CAN_CREATE_DOCUMENT_FOLDER, CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_CREATE_FOLDER_FOLDER, CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_DELETE_CONTENT_DOCUMENT, CMIS_WRITE));
        list.add(createMapping(PermissionMapping.CAN_DELETE_OBJECT, CMIS_ALL));
        list.add(createMapping(PermissionMapping.CAN_DELETE_TREE_FOLDER, CMIS_ALL));
        list.add(createMapping(PermissionMapping.CAN_GET_ACL_OBJECT, CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_GET_ALL_VERSIONS_VERSION_SERIES, CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_GET_CHILDREN_FOLDER, CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_GET_DESCENDENTS_FOLDER, CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_GET_FOLDER_PARENT_OBJECT, CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_GET_PARENTS_FOLDER, CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_GET_PROPERTIES_OBJECT, CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_MOVE_OBJECT, CMIS_WRITE));
        list.add(createMapping(PermissionMapping.CAN_MOVE_SOURCE, CMIS_READ));
        list.add(createMapping(PermissionMapping.CAN_MOVE_TARGET, CMIS_WRITE));
        list.add(createMapping(PermissionMapping.CAN_SET_CONTENT_DOCUMENT, CMIS_WRITE));
        list.add(createMapping(PermissionMapping.CAN_UPDATE_PROPERTIES_OBJECT, CMIS_WRITE));
        list.add(createMapping(PermissionMapping.CAN_VIEW_CONTENT_OBJECT, CMIS_READ));
        Map<String, PermissionMapping> map = new LinkedHashMap<String, PermissionMapping>();
        for (PermissionMapping pm : list) {
            map.put(pm.getKey(), pm);
        }
        aclCapability.setPermissionMappingData(map);

        //repositoryInfo.setAclCapabilities(aclCapability);
        return repositoryInfo;
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.RepositoryService#getTypeChildren(java.lang.String, java.lang.String, java.lang.Boolean, java.math.BigInteger, java.math.BigInteger, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public TypeDefinitionList getTypeChildren(
        CallContext context,
        String typeId,
        Boolean includePropertyDefinitions,
        BigInteger maxItems,
        BigInteger skipCount,
        ExtensionsData extension) {

        return m_typeManager.getTypesChildren(context, typeId, includePropertyDefinitions, maxItems, skipCount);
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.RepositoryService#getTypeDescendants(java.lang.String, java.lang.String, java.math.BigInteger, java.lang.Boolean, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public List<TypeDefinitionContainer> getTypeDescendants(
        CallContext context,
        String typeId,
        BigInteger depth,
        Boolean includePropertyDefinitions,
        ExtensionsData extension) {

        return m_typeManager.getTypesDescendants(context, typeId, depth, includePropertyDefinitions);
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.RepositoryService#getTypeDefinition(java.lang.String, java.lang.String, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public TypeDefinition getTypeDefinition(CallContext context, String typeId, ExtensionsData extension) {

        return m_typeManager.getTypeDefinition(context, typeId);
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.NavigationService#getChildren(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Boolean, org.apache.chemistry.opencmis.commons.enums.IncludeRelationships, java.lang.String, java.lang.Boolean, java.math.BigInteger, java.math.BigInteger, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public ObjectInFolderList getChildren(
        CallContext context,
        String folderId,
        String filter,
        String orderBy,
        Boolean includeAllowableActions,
        IncludeRelationships includeRelationships,
        String renditionFilter,
        Boolean includePathSegment,
        BigInteger maxItems,
        BigInteger skipCount,
        ExtensionsData extension,
        ObjectInfoHandler objectInfos) {

        try {

            // split filter
            Set<String> filterCollection = splitFilter(filter);

            // set defaults if values not set
            boolean iaa = (includeAllowableActions == null ? false : includeAllowableActions.booleanValue());
            boolean ips = (includePathSegment == null ? false : includePathSegment.booleanValue());

            // skip and max
            int skip = (skipCount == null ? 0 : skipCount.intValue());
            if (skip < 0) {
                skip = 0;
            }

            int max = (maxItems == null ? Integer.MAX_VALUE : maxItems.intValue());
            if (max < 0) {
                max = Integer.MAX_VALUE;
            }

            CmsObject cms = getCmsObject(context);
            CmsUUID structureId = new CmsUUID(folderId);
            CmsResource folder = cms.readResource(structureId);
            if (!folder.isFolder()) {
                throw new CmisObjectNotFoundException("Not a folder!");
            }

            // set object info of the the folder
            if (context.isObjectInfoRequired()) {
                compileObjectType(context, folder, null, false, false, objectInfos);
            }

            // prepare result
            ObjectInFolderListImpl result = new ObjectInFolderListImpl();
            result.setObjects(new ArrayList<ObjectInFolderData>());
            result.setHasMoreItems(false);
            int count = 0;

            List<CmsResource> children = new ArrayList<CmsResource>();
            String folderSitePath = cms.getRequestContext().getSitePath(folder);
            List<CmsResource> fileChildren = cms.getFilesInFolder(folderSitePath);
            List<CmsResource> folderChildren = cms.getSubFolders(folderSitePath);
            children.addAll(folderChildren);
            children.addAll(fileChildren);
            for (CmsResource child : children) {
                count++;
                if (skip > 0) {
                    skip--;
                    continue;
                }
                if (result.getObjects().size() >= max) {
                    result.setHasMoreItems(true);
                    continue;
                }

                // build and add child object
                ObjectInFolderDataImpl objectInFolder = new ObjectInFolderDataImpl();
                objectInFolder.setObject(compileObjectType(context, child, filterCollection, iaa, false, objectInfos));
                if (ips) {
                    objectInFolder.setPathSegment(child.getName());
                }

                result.getObjects().add(objectInFolder);
            }

            result.setNumItems(BigInteger.valueOf(count));

            return result;
        } catch (CmsException e) {
            handleCmsException(e);
            return null;
        }

    }

    private List<CmsResource> getChildren(CmsObject cms, CmsResource resource) throws CmsException {

        List<CmsResource> result = new ArrayList<CmsResource>();
        List<CmsResource> fileChildren = cms.getFilesInFolder(cms.getSitePath(resource));
        List<CmsResource> folderChildren = cms.getSubFolders(cms.getSitePath(resource));
        result.addAll(folderChildren);
        result.addAll(fileChildren);
        return result;
    }

    /**
     * Gather the children of a folder.
     */
    private void gatherDescendants(
        CallContext context,
        CmsResource folder,
        List<ObjectInFolderContainer> list,
        boolean foldersOnly,
        int depth,
        Set<String> filter,
        boolean includeAllowableActions,
        boolean includePathSegments,
        ObjectInfoHandler objectInfos) {

        try {
            CmsObject cms = getCmsObject(context);
            List<CmsResource> children = getChildren(cms, folder);
            Collections.sort(children, new Comparator<CmsResource>() {

                public int compare(CmsResource a, CmsResource b) {

                    return a.getName().compareTo(b.getName());
                }
            });
            // iterate through children
            for (CmsResource child : children) {

                // folders only?
                if (foldersOnly && !child.isFolder()) {
                    continue;
                }

                // add to list
                ObjectInFolderDataImpl objectInFolder = new ObjectInFolderDataImpl();
                objectInFolder.setObject(compileObjectType(
                    context,
                    child,
                    filter,
                    includeAllowableActions,
                    false,
                    objectInfos));
                if (includePathSegments) {
                    objectInFolder.setPathSegment(child.getName());
                }

                ObjectInFolderContainerImpl container = new ObjectInFolderContainerImpl();
                container.setObject(objectInFolder);

                list.add(container);

                // move to next level
                if ((depth != 1) && child.isFolder()) {
                    container.setChildren(new ArrayList<ObjectInFolderContainer>());
                    gatherDescendants(
                        context,
                        child,
                        container.getChildren(),
                        foldersOnly,
                        depth - 1,
                        filter,
                        includeAllowableActions,
                        includePathSegments,
                        objectInfos);
                }
            }
        } catch (CmsException e) {
            handleCmsException(e);
        }
    }

    /**
     * CMIS getDescendants.
     */
    public List<ObjectInFolderContainer> getDescendants(
        CallContext context,
        String folderId,
        BigInteger depth,
        String filter,
        Boolean includeAllowableActions,
        Boolean includePathSegment,
        ObjectInfoHandler objectInfos,
        boolean foldersOnly) {

        try {

            // check depth
            int d = (depth == null ? 2 : depth.intValue());
            if (d == 0) {
                throw new CmisInvalidArgumentException("Depth must not be 0!");
            }
            if (d < -1) {
                d = -1;
            }

            // split filter
            Set<String> filterCollection = splitFilter(filter);

            // set defaults if values not set
            boolean iaa = (includeAllowableActions == null ? false : includeAllowableActions.booleanValue());
            boolean ips = (includePathSegment == null ? false : includePathSegment.booleanValue());

            CmsObject cms = getCmsObject(context);
            CmsUUID folderStructureId = new CmsUUID(folderId);
            CmsResource folder = cms.readResource(folderStructureId);
            if (!folder.isFolder()) {
                throw new CmisObjectNotFoundException("Not a folder!");
            }

            // set object info of the the folder
            if (context.isObjectInfoRequired()) {
                compileObjectType(context, folder, null, false, false, objectInfos);
            }

            // get the tree
            List<ObjectInFolderContainer> result = new ArrayList<ObjectInFolderContainer>();
            gatherDescendants(context, folder, result, foldersOnly, d, filterCollection, iaa, ips, objectInfos);

            return result;
        } catch (CmsException e) {
            handleCmsException(e);
            return null;
        }
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.NavigationService#getObjectParents(java.lang.String, java.lang.String, java.lang.String, java.lang.Boolean, org.apache.chemistry.opencmis.commons.enums.IncludeRelationships, java.lang.String, java.lang.Boolean, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public List<ObjectParentData> getObjectParents(
        CallContext context,
        String objectId,
        String filter,
        Boolean includeAllowableActions,
        Boolean includeRelativePathSegment,
        ExtensionsData extension,
        ObjectInfoHandler objectInfos) {

        try {

            // split filter
            Set<String> filterCollection = splitFilter(filter);

            // set defaults if values not set
            boolean iaa = (includeAllowableActions == null ? false : includeAllowableActions.booleanValue());
            boolean irps = (includeRelativePathSegment == null ? false : includeRelativePathSegment.booleanValue());

            CmsObject cms = getCmsObject(context);
            CmsUUID structureId = new CmsUUID(objectId);
            CmsResource file = cms.readResource(structureId);
            // don't climb above the root folder

            if (m_root.equals(file)) {
                return Collections.emptyList();
            }

            // set object info of the the object
            if (context.isObjectInfoRequired()) {
                compileObjectType(context, file, null, false, false, objectInfos);
            }

            // get parent folder
            CmsResource parent = cms.readParentFolder(file.getStructureId());
            ObjectData object = compileObjectType(context, parent, filterCollection, iaa, false, objectInfos);

            ObjectParentDataImpl result = new ObjectParentDataImpl();
            result.setObject(object);
            if (irps) {
                result.setRelativePathSegment(file.getName());
            }

            return Collections.singletonList((ObjectParentData)result);
        } catch (CmsException e) {
            handleCmsException(e);
            return null;
        }

    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.NavigationService#getFolderParent(java.lang.String, java.lang.String, java.lang.String, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public ObjectData getFolderParent(
        CallContext context,
        String folderId,
        String filter,
        ExtensionsData extension,
        ObjectInfoHandler objectInfos) {

        List<ObjectParentData> parents = getObjectParents(
            context,
            folderId,
            filter,
            false,
            false,
            extension,
            objectInfos);
        if (parents.size() == 0) {
            throw new CmisInvalidArgumentException("The root folder has no parent!");
        }
        return parents.get(0).getObject();
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.NavigationService#getCheckedOutDocs(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Boolean, org.apache.chemistry.opencmis.commons.enums.IncludeRelationships, java.lang.String, java.math.BigInteger, java.math.BigInteger, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public ObjectList getCheckedOutDocs(
        CallContext context,
        String folderId,
        String filter,
        String orderBy,
        Boolean includeAllowableActions,
        IncludeRelationships includeRelationships,
        String renditionFilter,
        BigInteger maxItems,
        BigInteger skipCount,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#createDocument(java.lang.String, org.apache.chemistry.opencmis.commons.data.Properties, java.lang.String, org.apache.chemistry.opencmis.commons.data.ContentStream, org.apache.chemistry.opencmis.commons.enums.VersioningState, java.util.List, org.apache.chemistry.opencmis.commons.data.Acl, org.apache.chemistry.opencmis.commons.data.Acl, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public String createDocument(
        CallContext context,
        Properties properties,
        String folderId,
        ContentStream contentStream,
        VersioningState versioningState,
        List<String> policies,
        Acl addAces,
        Acl removeAces,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#createDocumentFromSource(java.lang.String, java.lang.String, org.apache.chemistry.opencmis.commons.data.Properties, java.lang.String, org.apache.chemistry.opencmis.commons.enums.VersioningState, java.util.List, org.apache.chemistry.opencmis.commons.data.Acl, org.apache.chemistry.opencmis.commons.data.Acl, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public String createDocumentFromSource(
        CallContext context,
        String sourceId,
        Properties properties,
        String folderId,
        VersioningState versioningState,
        List<String> policies,
        Acl addAces,
        Acl removeAces,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#createFolder(java.lang.String, org.apache.chemistry.opencmis.commons.data.Properties, java.lang.String, java.util.List, org.apache.chemistry.opencmis.commons.data.Acl, org.apache.chemistry.opencmis.commons.data.Acl, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public String createFolder(
        CallContext context,
        Properties properties,
        String folderId,
        List<String> policies,
        Acl addAces,
        Acl removeAces,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#createRelationship(java.lang.String, org.apache.chemistry.opencmis.commons.data.Properties, java.util.List, org.apache.chemistry.opencmis.commons.data.Acl, org.apache.chemistry.opencmis.commons.data.Acl, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public String createRelationship(
        CallContext context,
        Properties properties,
        List<String> policies,
        Acl addAces,
        Acl removeAces,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#createPolicy(java.lang.String, org.apache.chemistry.opencmis.commons.data.Properties, java.lang.String, java.util.List, org.apache.chemistry.opencmis.commons.data.Acl, org.apache.chemistry.opencmis.commons.data.Acl, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public String createPolicy(
        CallContext context,
        Properties properties,
        String folderId,
        List<String> policies,
        Acl addAces,
        Acl removeAces,
        ExtensionsData extension) {

        // TODO: Auto-generated method stub
        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#getAllowableActions(java.lang.String, java.lang.String, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public AllowableActions getAllowableActions(CallContext context, String objectId, ExtensionsData extension) {

        try {
            CmsObject cms = getCmsObject(context);
            CmsUUID structureId = new CmsUUID(objectId);
            CmsResource file = cms.readResource(structureId);
            return compileAllowableActions(context, file);
        } catch (CmsException e) {
            handleCmsException(e);
            return null;
        }
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#getObject(java.lang.String, java.lang.String, java.lang.String, java.lang.Boolean, org.apache.chemistry.opencmis.commons.enums.IncludeRelationships, java.lang.String, java.lang.Boolean, java.lang.Boolean, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public ObjectData getObject(
        CallContext context,
        String objectId,
        String filter,
        Boolean includeAllowableActions,
        IncludeRelationships includeRelationships,
        String renditionFilter,
        Boolean includePolicyIds,
        Boolean includeAcl,
        ExtensionsData extension,
        ObjectInfoHandler objectInfos) {

        try {

            // check id
            if (objectId == null) {
                throw new CmisInvalidArgumentException("Object Id must be set.");
            }
            CmsObject cms = getCmsObject(context);
            // get the file or folder
            CmsResource file = cms.readResource(new CmsUUID(objectId));

            // set defaults if values not set
            boolean iaa = (includeAllowableActions == null ? false : includeAllowableActions.booleanValue());
            boolean iacl = (includeAcl == null ? false : includeAcl.booleanValue());

            // split filter
            Set<String> filterCollection = splitFilter(filter);

            // gather properties
            return compileObjectType(context, file, filterCollection, iaa, iacl, objectInfos);
        } catch (CmsException e) {
            handleCmsException(e);
            return null;
        }
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#getProperties(java.lang.String, java.lang.String, java.lang.String, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public Properties getProperties(
        CallContext context,
        String objectId,
        String filter,
        ExtensionsData extension,
        ObjectInfoHandler objectInfos) {

        ObjectData object = getObject(
            context,
            objectId,
            null,
            Boolean.FALSE,
            null,
            null,
            Boolean.FALSE,
            Boolean.FALSE,
            null,
            objectInfos);
        return object.getProperties();
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#getRenditions(java.lang.String, java.lang.String, java.lang.String, java.math.BigInteger, java.math.BigInteger, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public List<RenditionData> getRenditions(
        CallContext context,
        String objectId,
        String renditionFilter,
        BigInteger maxItems,
        BigInteger skipCount,
        ExtensionsData extension) {

        List<RenditionData> result = new ArrayList<RenditionData>();
        return result;
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#getObjectByPath(java.lang.String, java.lang.String, java.lang.String, java.lang.Boolean, org.apache.chemistry.opencmis.commons.enums.IncludeRelationships, java.lang.String, java.lang.Boolean, java.lang.Boolean, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public ObjectData getObjectByPath(
        CallContext context,
        String path,
        String filter,
        Boolean includeAllowableActions,
        IncludeRelationships includeRelationships,
        String renditionFilter,
        Boolean includePolicyIds,
        Boolean includeAcl,
        ExtensionsData extension,
        ObjectInfoHandler objectInfos) {

        try {

            // split filter
            Set<String> filterCollection = splitFilter(filter);

            // check path
            if (CmsStringUtil.isEmptyOrWhitespaceOnly(path)) {
                throw new CmisInvalidArgumentException("Invalid folder path!");
            }
            CmsObject cms = getCmsObject(context);
            CmsResource file = cms.readResource(path);

            return compileObjectType(context, file, filterCollection, includeAllowableActions, includeAcl, objectInfos);

        } catch (CmsException e) {
            handleCmsException(e);
            return null;
        }
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#getContentStream(java.lang.String, java.lang.String, java.lang.String, java.math.BigInteger, java.math.BigInteger, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public ContentStream getContentStream(
        CallContext context,
        String objectId,
        String streamId,
        BigInteger offset,
        BigInteger length,
        ExtensionsData extension) {

        try {

            if ((offset != null) || (length != null)) {
                throw new CmisInvalidArgumentException("Offset and Length are not supported!");
            }

            CmsObject cms = getCmsObject(context);
            CmsResource resource = cms.readResource(new CmsUUID(objectId));
            if (resource.isFolder()) {
                throw new CmisStreamNotSupportedException("Not a file!");
            }
            CmsFile file = cms.readFile(resource);
            byte[] contents = file.getContents();
            InputStream stream = new ByteArrayInputStream(contents);

            ContentStreamImpl result = new ContentStreamImpl();
            result.setFileName(file.getName());
            result.setLength(BigInteger.valueOf(file.getLength()));
            result.setMimeType(OpenCms.getResourceManager().getMimeType(file.getRootPath(), null, "text/plain"));
            result.setStream(stream);

            return result;
        } catch (CmsException e) {
            handleCmsException(e);
            return null;
        }
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#updateProperties(java.lang.String, org.apache.chemistry.opencmis.commons.spi.Holder, org.apache.chemistry.opencmis.commons.spi.Holder, org.apache.chemistry.opencmis.commons.data.Properties, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public void updateProperties(
        CallContext context,
        Holder<String> objectId,
        Holder<String> changeToken,
        Properties properties,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");

    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#moveObject(java.lang.String, org.apache.chemistry.opencmis.commons.spi.Holder, java.lang.String, java.lang.String, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public void moveObject(
        CallContext context,
        Holder<String> objectId,
        String targetFolderId,
        String sourceFolderId,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");

    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#deleteObject(java.lang.String, java.lang.String, java.lang.Boolean, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public void deleteObject(CallContext context, String objectId, Boolean allVersions, ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");

    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#deleteTree(java.lang.String, java.lang.String, java.lang.Boolean, org.apache.chemistry.opencmis.commons.enums.UnfileObject, java.lang.Boolean, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public FailedToDeleteData deleteTree(
        CallContext context,
        String folderId,
        Boolean allVersions,
        UnfileObject unfileObjects,
        Boolean continueOnFailure,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");
    }

    private boolean ensureLock(CmsObject cms, CmsResource resource) throws CmsException {

        CmsLock lock = cms.getLock(resource);
        if (lock.isOwnedBy(cms.getRequestContext().getCurrentUser())) {
            return false;
        }
        cms.lockResourceTemporary(resource);
        return true;
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#setContentStream(java.lang.String, org.apache.chemistry.opencmis.commons.spi.Holder, java.lang.Boolean, org.apache.chemistry.opencmis.commons.spi.Holder, org.apache.chemistry.opencmis.commons.data.ContentStream, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public void setContentStream(
        CallContext context,
        Holder<String> objectId,
        Boolean overwriteFlag,
        Holder<String> changeToken,
        ContentStream contentStream,
        ExtensionsData extension) {

        try {
            CmsObject cms = getCmsObject(context);
            CmsUUID structureId = new CmsUUID(objectId.getValue());
            boolean overwrite = (overwriteFlag == null) || overwriteFlag.booleanValue();
            if (!overwrite) {
                throw new CmisContentAlreadyExistsException();
            }
            CmsResource resource = cms.readResource(structureId);
            if (resource.isFolder()) {
                throw new CmisStreamNotSupportedException("Folders may not have content streams.");
            }
            CmsFile file = cms.readFile(resource);
            InputStream contentInput = contentStream.getStream();
            byte[] newContent = CmsFileUtil.readFully(contentInput);
            file.setContents(newContent);
            boolean wasLocked = ensureLock(cms, resource);
            CmsFile newFile = cms.writeFile(file);
            if (wasLocked) {
                cms.unlockResource(newFile);
            }
        } catch (CmsException e) {
            handleCmsException(e);
        } catch (IOException e) {
            throw new CmisRuntimeException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.ObjectService#deleteContentStream(java.lang.String, org.apache.chemistry.opencmis.commons.spi.Holder, org.apache.chemistry.opencmis.commons.spi.Holder, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public void deleteContentStream(
        CallContext context,
        Holder<String> objectId,
        Holder<String> changeToken,
        ExtensionsData extension) {

        throw new CmisConstraintException("Content streams may not be deleted.");

    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.VersioningService#checkOut(java.lang.String, org.apache.chemistry.opencmis.commons.spi.Holder, org.apache.chemistry.opencmis.commons.data.ExtensionsData, org.apache.chemistry.opencmis.commons.spi.Holder)
     */
    public void checkOut(
        CallContext context,
        Holder<String> objectId,
        ExtensionsData extension,
        Holder<Boolean> contentCopied) {

        throw new CmisNotSupportedException("Not supported!");

    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.VersioningService#cancelCheckOut(java.lang.String, java.lang.String, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public void cancelCheckOut(CallContext context, String objectId, ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");

    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.VersioningService#checkIn(java.lang.String, org.apache.chemistry.opencmis.commons.spi.Holder, java.lang.Boolean, org.apache.chemistry.opencmis.commons.data.Properties, org.apache.chemistry.opencmis.commons.data.ContentStream, java.lang.String, java.util.List, org.apache.chemistry.opencmis.commons.data.Acl, org.apache.chemistry.opencmis.commons.data.Acl, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public void checkIn(
        CallContext context,
        Holder<String> objectId,
        Boolean major,
        Properties properties,
        ContentStream contentStream,
        String checkinComment,
        List<String> policies,
        Acl addAces,
        Acl removeAces,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");

    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.VersioningService#getObjectOfLatestVersion(java.lang.String, java.lang.String, java.lang.String, java.lang.Boolean, java.lang.String, java.lang.Boolean, org.apache.chemistry.opencmis.commons.enums.IncludeRelationships, java.lang.String, java.lang.Boolean, java.lang.Boolean, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public ObjectData getObjectOfLatestVersion(
        CallContext context,
        String objectId,
        String versionSeriesId,
        Boolean major,
        String filter,
        Boolean includeAllowableActions,
        IncludeRelationships includeRelationships,
        String renditionFilter,
        Boolean includePolicyIds,
        Boolean includeAcl,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * @see org.apache.chemistry.opencmis.commons.spi.VersioningService#getPropertiesOfLatestVersion(java.lang.String, java.lang.String, java.lang.String, java.lang.Boolean, java.lang.String, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public Properties getPropertiesOfLatestVersion(
        CallContext context,
        String objectId,
        String versionSeriesId,
        Boolean major,
        String filter,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * 
     * @see org.apache.chemistry.opencmis.commons.spi.VersioningService#getAllVersions(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Boolean, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public List<ObjectData> getAllVersions(
        CallContext context,
        String objectId,
        String versionSeriesId,
        String filter,
        Boolean includeAllowableActions,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * 
     * @see org.apache.chemistry.opencmis.commons.spi.DiscoveryService#query(java.lang.String, java.lang.String, java.lang.Boolean, java.lang.Boolean, org.apache.chemistry.opencmis.commons.enums.IncludeRelationships, java.lang.String, java.math.BigInteger, java.math.BigInteger, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public ObjectList query(
        CallContext context,
        String statement,
        Boolean searchAllVersions,
        Boolean includeAllowableActions,
        IncludeRelationships includeRelationships,
        String renditionFilter,
        BigInteger maxItems,
        BigInteger skipCount,
        ExtensionsData extension) {

        // TODO: Auto-generated method stub
        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * 
     * @see org.apache.chemistry.opencmis.commons.spi.DiscoveryService#getContentChanges(java.lang.String, org.apache.chemistry.opencmis.commons.spi.Holder, java.lang.Boolean, java.lang.String, java.lang.Boolean, java.lang.Boolean, java.math.BigInteger, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public ObjectList getContentChanges(
        CallContext context,
        Holder<String> changeLogToken,
        Boolean includeProperties,
        String filter,
        Boolean includePolicyIds,
        Boolean includeAcl,
        BigInteger maxItems,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * 
     * @see org.apache.chemistry.opencmis.commons.spi.MultiFilingService#addObjectToFolder(java.lang.String, java.lang.String, java.lang.String, java.lang.Boolean, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public void addObjectToFolder(
        CallContext context,
        String objectId,
        String folderId,
        Boolean allVersions,
        ExtensionsData extension) {

        // TODO: Auto-generated method stub
        throw new CmisNotSupportedException("Not supported!");

    }

    /**
     * 
     * @see org.apache.chemistry.opencmis.commons.spi.MultiFilingService#removeObjectFromFolder(java.lang.String, java.lang.String, java.lang.String, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public void removeObjectFromFolder(CallContext context, String objectId, String folderId, ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");

    }

    /**
     * 
     * @see org.apache.chemistry.opencmis.commons.spi.RelationshipService#getObjectRelationships(java.lang.String, java.lang.String, java.lang.Boolean, org.apache.chemistry.opencmis.commons.enums.RelationshipDirection, java.lang.String, java.lang.String, java.lang.Boolean, java.math.BigInteger, java.math.BigInteger, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public ObjectList getObjectRelationships(
        CallContext context,
        String objectId,
        Boolean includeSubRelationshipTypes,
        RelationshipDirection relationshipDirection,
        String typeId,
        String filter,
        Boolean includeAllowableActions,
        BigInteger maxItems,
        BigInteger skipCount,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * 
     * @see org.apache.chemistry.opencmis.commons.spi.AclService#getAcl(java.lang.String, java.lang.String, java.lang.Boolean, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public Acl getAcl(CallContext context, String objectId, Boolean onlyBasicPermissions, ExtensionsData extension) {

        // TODO: Auto-generated method stub
        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * 
     * @see org.apache.chemistry.opencmis.commons.spi.AclService#applyAcl(java.lang.String, java.lang.String, org.apache.chemistry.opencmis.commons.data.Acl, org.apache.chemistry.opencmis.commons.data.Acl, org.apache.chemistry.opencmis.commons.enums.AclPropagation, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public Acl applyAcl(
        CallContext context,
        String objectId,
        Acl addAces,
        Acl removeAces,
        AclPropagation aclPropagation,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * 
     * @see org.apache.chemistry.opencmis.commons.spi.PolicyService#applyPolicy(java.lang.String, java.lang.String, java.lang.String, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public void applyPolicy(CallContext context, String policyId, String objectId, ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");

    }

    /**
     * 
     * @see org.apache.chemistry.opencmis.commons.spi.PolicyService#removePolicy(java.lang.String, java.lang.String, java.lang.String, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public void removePolicy(CallContext context, String policyId, String objectId, ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");

    }

    /**
     * 
     * @see org.apache.chemistry.opencmis.commons.spi.PolicyService#getAppliedPolicies(java.lang.String, java.lang.String, java.lang.String, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public List<ObjectData> getAppliedPolicies(
        CallContext context,
        String objectId,
        String filter,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * 
     * @see org.apache.chemistry.opencmis.commons.server.CmisService#create(java.lang.String, org.apache.chemistry.opencmis.commons.data.Properties, java.lang.String, org.apache.chemistry.opencmis.commons.data.ContentStream, org.apache.chemistry.opencmis.commons.enums.VersioningState, java.util.List, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public String create(
        CallContext context,
        Properties properties,
        String folderId,
        ContentStream contentStream,
        VersioningState versioningState,
        List<String> policies,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * 
     * @see org.apache.chemistry.opencmis.commons.server.CmisService#deleteObjectOrCancelCheckOut(java.lang.String, java.lang.String, java.lang.Boolean, org.apache.chemistry.opencmis.commons.data.ExtensionsData)
     */
    public void deleteObjectOrCancelCheckOut(
        CallContext context,
        String objectId,
        Boolean allVersions,
        ExtensionsData extension) {

        throw new CmisNotSupportedException("Not supported!");

    }

    /**
     * 
     * @see org.apache.chemistry.opencmis.commons.server.CmisService#applyAcl(java.lang.String, java.lang.String, org.apache.chemistry.opencmis.commons.data.Acl, org.apache.chemistry.opencmis.commons.enums.AclPropagation)
     */
    public Acl applyAcl(CallContext context, String objectId, Acl aces, AclPropagation aclPropagation) {

        throw new CmisNotSupportedException("Not supported!");
    }

    /**
     * 
     * @see org.apache.chemistry.opencmis.commons.server.CmisService#getObjectInfo(java.lang.String, java.lang.String)
     */
    public ObjectInfo getObjectInfo(CallContext context, String objectId) {

        throw new CmisNotSupportedException("Not supported!");
    }

}