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

package org.opencms.workflow;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsUser;
import org.opencms.main.CmsException;
import org.opencms.notification.A_CmsNotification;

import java.util.List;

/**
 * Notification class for the workflow 'release' action.<p>
 */
public class CmsWorkflowNotification extends A_CmsNotification {

    /** The path of the notification XML content. */
    private String m_notificationContent;

    /** The admin CMS context. */
    private CmsObject m_adminCms;

    /** The user's cms context. */
    private CmsObject m_userCms;

    /** The workflow project. */
    @SuppressWarnings("unused")
    private CmsProject m_project;

    /** The released resources. */
    private List<CmsResource> m_resources;
    /** The publish link. */
    private String m_link;

    /**
     * Creates a new workflow notification mail object.<p>
     * 
     * @param adminCms the admin CMS context 
     * @param userCms the user CMS context 
     * @param receiver the mail recipient 
     * @param notificationContent the file from which to read the notification configuration 
     * @param project the workflow project 
     * @param resources the workflow resources 
     * @param link the link used for publishing the resources 
     */
    public CmsWorkflowNotification(
        CmsObject adminCms,
        CmsObject userCms,
        CmsUser receiver,
        String notificationContent,
        CmsProject project,
        List<CmsResource> resources,
        String link) {

        // TODO: Auto-generated constructor stub
        super(userCms, receiver);
        m_notificationContent = notificationContent;
        m_adminCms = adminCms;
        m_userCms = userCms;
        m_project = project;
        m_resources = resources;
        m_link = link;
    }

    /**
     * Gets the fields which should be displayed for a single resource.<p>
     * 
     * @param resource the resource for which we should fetch the fields 
     * 
     * @return a string array containing the information for the given resource 
     */
    public String[] getResourceInfo(CmsResource resource) {

        String rootPath = resource.getRootPath();
        String title = "-";

        try {
            CmsProperty titleProp = m_adminCms.readPropertyObject(resource, CmsPropertyDefinition.PROPERTY_TITLE, false);
            if (!titleProp.isNullProperty()) {
                title = titleProp.getValue();
            }
        } catch (CmsException e) {
            // ignore 
        }
        return new String[] {rootPath, title};
    }

    /**
     * Gets the resource info headers.<p>
     * 
     * @return the resource info headers 
     */
    public String[] getResourceInfoHeaders() {

        return new String[] {"Resource", "Title"};
    }

    /**
     * @see org.opencms.notification.A_CmsNotification#generateHtmlMsg()
     */
    @Override
    protected String generateHtmlMsg() {

        StringBuffer buffer = new StringBuffer();
        //----------INTRODUCTION LINE---------------------------------
        buffer.append("<div class='user_line'>");
        buffer.append(getMessage(Messages.MAIL_USER_LINE_1, m_userCms.getRequestContext().getCurrentUser().getName()));
        buffer.append("</div>");

        //----------RESOURCE TABLE-------------------------------------
        buffer.append("<table cellspacing='0' cellpadding='4' class='resource_table'>");
        String[] tableHeaders = getResourceInfoHeaders();
        buffer.append("<tr>");
        for (String header : tableHeaders) {
            buffer.append("<th>");
            buffer.append(header);
            buffer.append("</th>");
        }
        buffer.append("</tr>");

        for (CmsResource resource : m_resources) {
            String[] resourceInfos = getResourceInfo(resource);
            buffer.append("<tr>");
            for (String resourceInfo : resourceInfos) {
                buffer.append("<td>");
                buffer.append(resourceInfo);
                buffer.append("</td>");
            }
            buffer.append("</tr>");
        }
        buffer.append("</table>");

        //---------PUBLISH LINK-----------------------------------------
        buffer.append("<div class='publish_link'>");
        buffer.append(getMessage(Messages.MAIL_PUBLISH_LINK_1, m_link));
        buffer.append("</div>");
        return buffer.toString();
    }

    /**
     * Gets a message from the message bundle.<p>
     * 
     * @param key the message key 
     * @param args the message parameters 
     * 
     * @return the message from the message bundle
     */
    protected String getMessage(String key, String... args) {

        return Messages.get().getBundle(getLocale()).key(key, args);
    }

    /**
     * @see org.opencms.notification.A_CmsNotification#getNotificationContent()
     */
    @Override
    protected String getNotificationContent() {

        return m_notificationContent;
    }

}