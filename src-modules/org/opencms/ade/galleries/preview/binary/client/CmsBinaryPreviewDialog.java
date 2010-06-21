/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/ade/galleries/preview/binary/client/Attic/CmsBinaryPreviewDialog.java,v $
 * Date   : $Date: 2010/06/10 08:45:04 $
 * Version: $Revision: 1.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
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

package org.opencms.ade.galleries.preview.binary.client;

import org.opencms.ade.galleries.client.preview.ui.A_CmsPreviewDialog;
import org.opencms.ade.galleries.client.preview.ui.CmsPropertiesTab;
import org.opencms.ade.galleries.shared.CmsResourceInfoBean;
import org.opencms.ade.galleries.shared.I_CmsGalleryProviderConstants.GalleryMode;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HTML;

/**
 * Provides a widget for the binary preview dialog .<p>
 *  
 * @author Polina Smagina
 * @author Tobias Herrmann
 * 
 * @version $Revision: 1.1 $
 * 
 * @since 8.0.
 */
public class CmsBinaryPreviewDialog extends A_CmsPreviewDialog<CmsResourceInfoBean> {

    /** The properties tab. */
    private CmsPropertiesTab m_propertiesTab;

    /**
     * The constructor.<p>
     * 
     * @param dialogMode the dialog mode
     * @param dialogHeight the dialog height to set
     * @param dialogWidth the dialog width to set     
     */
    public CmsBinaryPreviewDialog(GalleryMode dialogMode, int dialogHeight, int dialogWidth) {

        super(dialogMode, dialogHeight, dialogWidth);
    }

    /**
     * @see org.opencms.ade.galleries.client.preview.ui.A_CmsPreviewDialog#confirmSaveChanges(java.lang.String, com.google.gwt.user.client.Command, com.google.gwt.user.client.Command)
     */
    @Override
    public void confirmSaveChanges(String message, Command onConfirm, Command onCancel) {

        //TODO: implement
    }

    /**
     * @see org.opencms.ade.galleries.client.preview.ui.A_CmsPreviewDialog#fillContent(org.opencms.ade.galleries.shared.CmsResourceInfoBean)
     */
    @Override
    public void fillContent(CmsResourceInfoBean infoBean) {

        //TODO: use proper preview content
        fillPreviewPanel("<p>" + m_galleryMode.name() + "</p>");
        m_propertiesTab.fillProperties(infoBean.getProperties());
    }

    /**
     * Fills the content of the preview panel part.<p>
     * 
     * @param html the content html
     */
    public void fillPreviewPanel(String html) {

        m_previewPanel.add(new HTML(html));
    }

    /**
     * Returns if the properties tab has unsaved changes.<p>
     * 
     * @return <code>true</code> if the properties tab has unsaved changes
     */
    public boolean hasChangedProperties() {

        return m_propertiesTab.isChanged();
    }

    /**
     * @see org.opencms.ade.galleries.client.preview.ui.A_CmsPreviewDialog#hasChanges()
     */
    @Override
    public boolean hasChanges() {

        return m_propertiesTab.isChanged();
    }

    /**
     * Initializes the preview.<p>
     * 
     * @param handler the preview handler
     */
    public void init(CmsBinaryPreviewHandler handler) {

        m_handler = handler;
        m_propertiesTab = new CmsPropertiesTab(m_galleryMode, m_dialogHeight, m_dialogWidth, m_handler);
        m_tabbedPanel.add(m_propertiesTab, m_propertiesTab.getTabName());
    }
}